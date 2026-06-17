package de.igbdsandzakkassel.vaktija.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.igbdsandzakkassel.vaktija.data.remote.RemoteVaktijaSource
import de.igbdsandzakkassel.vaktija.data.remote.VaktijaEuSource
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.EventsRepository
import de.igbdsandzakkassel.vaktija.data.repository.FirestoreCommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.FirestoreEventsRepository
import de.igbdsandzakkassel.vaktija.data.repository.FirestoreNewsRepository
import de.igbdsandzakkassel.vaktija.data.repository.NewsRepository
import de.igbdsandzakkassel.vaktija.data.repository.OfflinePrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import javax.inject.Singleton

/**
 * Binds repository/source interfaces to their implementations.
 * - Prayer times: real (vaktija.eu + Room).
 * - Community rules: Firestore-backed (admin-edited, cached offline).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindRemoteVaktijaSource(impl: VaktijaEuSource): RemoteVaktijaSource

    @Binds
    @Singleton
    abstract fun bindPrayerTimesRepository(impl: OfflinePrayerTimesRepository): PrayerTimesRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRuleProvider(impl: FirestoreCommunityRuleProvider): CommunityRuleProvider

    @Binds
    @Singleton
    abstract fun bindNewsRepository(impl: FirestoreNewsRepository): NewsRepository

    @Binds
    @Singleton
    abstract fun bindEventsRepository(impl: FirestoreEventsRepository): EventsRepository
}
