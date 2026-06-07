package de.igbdsandzakkassel.vaktija.data.repository

import de.igbdsandzakkassel.vaktija.data.calendar.PrayerTimeCalculator
import de.igbdsandzakkassel.vaktija.data.local.MonthTimesDao
import de.igbdsandzakkassel.vaktija.data.local.toModel
import de.igbdsandzakkassel.vaktija.data.local.toMonthEntity
import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
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
 */
@Singleton
class MonthCalendarRepository @Inject constructor(
    private val calculator: PrayerTimeCalculator,
    private val monthDao: MonthTimesDao,
    private val timesRepository: PrayerTimesRepository,
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
            offsetsBetween(scrapedToday, rawToday)
        } else {
            CalibrationOffsets.ZERO
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

    private fun diff(a: LocalTime, b: LocalTime): Int = a.toSecondOfDay() - b.toSecondOfDay()
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
        fajr = t.fajr.plusSeconds(fajr.toLong()),
        sunrise = t.sunrise.plusSeconds(sunrise.toLong()),
        dhuhr = t.dhuhr.plusSeconds(dhuhr.toLong()),
        asr = t.asr.plusSeconds(asr.toLong()),
        maghrib = t.maghrib.plusSeconds(maghrib.toLong()),
        isha = t.isha.plusSeconds(isha.toLong()),
    )

    companion object {
        val ZERO = CalibrationOffsets(0, 0, 0, 0, 0, 0)
    }
}
