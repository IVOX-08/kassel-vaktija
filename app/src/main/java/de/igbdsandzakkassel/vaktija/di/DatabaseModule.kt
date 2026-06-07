package de.igbdsandzakkassel.vaktija.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import de.igbdsandzakkassel.vaktija.data.local.KasselDatabase
import de.igbdsandzakkassel.vaktija.data.local.MonthTimesDao
import de.igbdsandzakkassel.vaktija.data.local.PrayerTimesDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KasselDatabase =
        Room.databaseBuilder(context, KasselDatabase::class.java, "kassel-vaktija.db")
            // This DB is a disposable CACHE only (scraped today + computed month). Destructive
            // migration on a schema bump simply rebuilds it on next refresh/open — no user data is
            // lost. (If the DB ever holds non-rebuildable data, replace this with real migrations.)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePrayerTimesDao(database: KasselDatabase): PrayerTimesDao = database.prayerTimesDao()

    @Provides
    fun provideMonthTimesDao(database: KasselDatabase): MonthTimesDao = database.monthTimesDao()
}
