package de.igbdsandzakkassel.vaktija.data.local

import de.igbdsandzakkassel.vaktija.data.model.DailyTimes
import java.time.LocalDate
import java.time.LocalTime

fun DailyTimesEntity.toModel(): DailyTimes = DailyTimes(
    date = LocalDate.parse(date),
    fajr = LocalTime.parse(fajr),
    sunrise = LocalTime.parse(sunrise),
    dhuhr = LocalTime.parse(dhuhr),
    asr = LocalTime.parse(asr),
    maghrib = LocalTime.parse(maghrib),
    isha = LocalTime.parse(isha),
)

fun DailyTimes.toEntity(fetchedAt: Long): DailyTimesEntity = DailyTimesEntity(
    date = date.toString(),
    fajr = fajr.toString(),
    sunrise = sunrise.toString(),
    dhuhr = dhuhr.toString(),
    asr = asr.toString(),
    maghrib = maghrib.toString(),
    isha = isha.toString(),
    fetchedAt = fetchedAt,
)
