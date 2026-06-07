package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.calendar.PrayerTimeCalculator
import de.igbdsandzakkassel.vaktija.data.local.MonthTimesDao
import de.igbdsandzakkassel.vaktija.data.local.toModel
import de.igbdsandzakkassel.vaktija.data.local.toMonthEntity
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import de.igbdsandzakkassel.vaktija.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the month calendar: computes each day locally (adhan2), caches the raw values in Room, then
 * calibrates against today's official scraped value (per-prayer offset) so the table stays within
 * ~1 min of the official figures. Today's row shows the exact scraped value.
 *
 * The calibration offset is persisted, so offline (when today's official value isn't available) the
 * calendar reuses the last-known calibration instead of falling back to uncalibrated values — this
 * keeps a given day's times consistent across online/offline reads.
 */
@Singleton
class MonthCalendarRepository @Inject constructor(
    private val calculator: PrayerTimeCalculator,
    private val monthDao: MonthTimesDao,
    private val timesRepository: PrayerTimesRepository,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun getMonth(month: YearMonth): List<DailyTimes> = withContext(Dispatchers.Default) {
        val prefix = "%04d-%02d%%".format(month.year, month.monthValue)

        var cached = monthDao.getMonth(prefix)
        if (cached.size < month.lengthOfMonth()) {
            val rows = (1..month.lengthOfMonth()).map { day ->
                calculator.compute(month.atDay(day)).toMonthEntity()
            }
            monthDao.upsertAll(rows)
            cached = monthDao.getMonth(prefix)
        }

        val today = LocalDate.now()
        val scrapedToday = timesRepository.observeToday().first()
        val rawToday = cached.firstOrNull { it.date == today.toString() }?.toModel()

        val offsets = if (scrapedToday != null && scrapedToday.date == today && rawToday != null) {
            offsetsBetween(scrapedToday, rawToday).also { settingsRepository.saveCalibrationOffsets(it.toList()) }
        } else {
            // Offline / no fresh value → reuse the last-saved calibration for consistency.
            settingsRepository.getCalibrationOffsets()?.let { CalibrationOffsets.from(it) } ?: CalibrationOffsets.ZERO
        }

        cached.map { entity ->
            val model = entity.toModel()
            if (scrapedToday != null && model.date == today) scrapedToday else offsets.applyTo(model)
        }
    }

    private fun offsetsBetween(official: DailyTimes, raw: DailyTimes) = CalibrationOffsets(
        fajr = diff(official.fajr, raw.fajr),
        sunrise = diff(official.sunrise, raw.sunrise),
        dhuhr = diff(official.dhuhr, raw.dhuhr),
        asr = diff(official.asr, raw.asr),
        maghrib = diff(official.maghrib, raw.maghrib),
        isha = diff(official.isha, raw.isha),
    )

    // Difference in seconds, clamped to ±15 min. Real calibration is a couple of minutes; the clamp
    // guards against a pathological offset (e.g. a bad scrape) that could otherwise distort the month.
    private fun diff(a: LocalTime, b: LocalTime): Int =
        (a.toSecondOfDay() - b.toSecondOfDay()).coerceIn(-MAX_OFFSET_SEC, MAX_OFFSET_SEC)

    private companion object {
        const val MAX_OFFSET_SEC = 15 * 60
    }
}

private data class CalibrationOffsets(
    val fajr: Int,
    val sunrise: Int,
    val dhuhr: Int,
    val asr: Int,
    val maghrib: Int,
    val isha: Int,
) {
    fun applyTo(t: DailyTimes): DailyTimes = t.copy(
        fajr = shift(t.fajr, fajr),
        sunrise = shift(t.sunrise, sunrise),
        dhuhr = shift(t.dhuhr, dhuhr),
        asr = shift(t.asr, asr),
        maghrib = shift(t.maghrib, maghrib),
        isha = shift(t.isha, isha),
    )

    fun toList(): List<Int> = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)

    private fun shift(time: LocalTime, seconds: Int): LocalTime =
        // Clamp within the same day so a shift can never silently wrap past midnight.
        LocalTime.ofSecondOfDay((time.toSecondOfDay() + seconds).coerceIn(0, 86_399).toLong())

    companion object {
        val ZERO = CalibrationOffsets(0, 0, 0, 0, 0, 0)
        fun from(values: List<Int>): CalibrationOffsets =
            if (values.size == 6) {
                CalibrationOffsets(values[0], values[1], values[2], values[3], values[4], values[5])
            } else {
                ZERO
            }
    }
}
