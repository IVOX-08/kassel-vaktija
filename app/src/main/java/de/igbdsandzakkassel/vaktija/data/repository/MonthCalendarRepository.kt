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
import java.util.Locale
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
        // Locale.ROOT → ASCII digits. Using the default locale here would emit Arabic-Indic digits
        // under an Arabic UI ("٢٠٢٦-٠٦%"), which never matches the ASCII dates stored in Room and
        // left the whole calendar empty (blank/black screen).
        val prefix = String.format(Locale.ROOT, "%04d-%02d%%", month.year, month.monthValue)

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

    // Nearest signed difference in seconds, wrapped to (−12h, +12h]. This is essential at Kassel's
    // latitude: in summer the locally-computed Isha can fall *past midnight* (e.g. 01:21), so a naive
    // `official − raw` would yield a bogus ~+22h offset instead of the true ~−2h. Picking the nearest
    // wrap fixes that. (A previous ±15 min clamp here is what made the whole month "completely
    // incorrect" — the real high-latitude offset is far larger than 15 min.)
    private fun diff(a: LocalTime, b: LocalTime): Int {
        val raw = a.toSecondOfDay() - b.toSecondOfDay()
        return (raw + HALF_DAY_SEC).mod(DAY_SEC) - HALF_DAY_SEC
    }

    private companion object {
        const val DAY_SEC = 24 * 60 * 60
        const val HALF_DAY_SEC = DAY_SEC / 2
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
        // Wrap around midnight (mod 24h) so a large offset maps to the correct time of day instead of
        // being clamped to 00:00 / 23:59 (needed when the computed Isha sits just past midnight).
        LocalTime.ofSecondOfDay((time.toSecondOfDay() + seconds).mod(86_400).toLong())

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
