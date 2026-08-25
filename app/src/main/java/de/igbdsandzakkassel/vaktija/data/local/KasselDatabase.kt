package de.igbdsandzakkassel.vaktija.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [DailyTimesEntity::class, MonthDayTimesEntity::class],
    // 3: both cache tables gained locationId in their primary key (multi-community).
    // This DB is a rebuildable cache, so the destructive migration in DatabaseModule is fine.
    version = 3,
    exportSchema = false,
)
abstract class KasselDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun monthTimesDao(): MonthTimesDao
}
