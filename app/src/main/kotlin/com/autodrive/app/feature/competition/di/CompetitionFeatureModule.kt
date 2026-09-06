package com.autodrive.app.feature.competition.di

import com.autodrive.app.feature.competition.data.CompetitionAvailabilityRepositoryImpl
import com.autodrive.app.feature.competition.data.WeeklyCompetitionRepositoryImpl
import com.autodrive.app.feature.competition.domain.repository.CompetitionAvailabilityRepository
import com.autodrive.app.feature.competition.domain.repository.WeeklyCompetitionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CompetitionFeatureModule {
    @Binds @Singleton
    abstract fun bindWeeklyCompetitionRepository(
        impl: WeeklyCompetitionRepositoryImpl
    ): WeeklyCompetitionRepository

    @Binds @Singleton
    abstract fun bindCompetitionAvailabilityRepository(
        impl: CompetitionAvailabilityRepositoryImpl
    ): CompetitionAvailabilityRepository
}
