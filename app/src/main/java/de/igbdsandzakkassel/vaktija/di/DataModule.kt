package de.igbdsandzakkassel.vaktija.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.igbdsandzakkassel.vaktija.data.remote.RemoteVaktijaSource
import de.igbdsandzakkassel.vaktija.data.remote.VaktijaEuSource
import de.igbdsandzakkassel.vaktija.data.repository.CommunityRuleProvider
import de.igbdsandzakkassel.vaktija.data.repository.OfflinePrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.repository.PrayerTimesRepository
import de.igbdsandzakkassel.vaktija.data.repository.StubCommunityRuleProvider
import javax.inject.Singleton

/**
 * Binds repository/source interfaces to their implementations.
 * - Prayer times: real (vaktija.eu + Room) as of Phase 1.
 * - Community rules: still the default stub until Firebase is wired (Phase 4b).
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
    abstract fun bindCommunityRuleProvider(impl: StubCommunityRuleProvider): CommunityRuleProvider
}
