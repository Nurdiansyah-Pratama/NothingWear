package com.nothingwear.app.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.nothingwear.app.data.calendar.AndroidCalendarDataSource
import com.nothingwear.app.data.calendar.DeviceCalendarDataSource
import com.nothingwear.app.data.preferences.DataStoreThemePreferencesRepository
import com.nothingwear.app.data.repository.FoundationDashboardRepository
import com.nothingwear.app.data.repository.OfflineFirstCalendarRepository
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DashboardRepository
import com.nothingwear.app.domain.repository.ThemePreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataBindingsModule {

    @Binds
    @Singleton
    abstract fun bindThemePreferencesRepository(
        impl: DataStoreThemePreferencesRepository,
    ): ThemePreferencesRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        impl: FoundationDashboardRepository,
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindDeviceCalendarDataSource(
        impl: AndroidCalendarDataSource,
    ): DeviceCalendarDataSource

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(
        impl: OfflineFirstCalendarRepository,
    ): CalendarRepository
}

@Module
@InstallIn(SingletonComponent::class)
object DataProvidersModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("nothingwear_preferences") },
    )
}
