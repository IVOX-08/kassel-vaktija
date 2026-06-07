package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyTimesEntity::class, MonthDayTimesEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class KasselDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun monthTimesDao(): MonthTimesDao
}
