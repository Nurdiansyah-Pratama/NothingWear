package com.nothingwear.app.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.work.WorkManager
import com.nothingwear.app.data.calendar.AndroidCalendarDataSource
import com.nothingwear.app.data.calendar.DeviceCalendarDataSource
import com.nothingwear.app.data.device.AndroidBleDiagnosticsRepository
import com.nothingwear.app.data.event.WatchEventSyncRepository
import com.nothingwear.app.data.preferences.DataStoreThemePreferencesRepository
import com.nothingwear.app.data.repository.FoundationDashboardRepository
import com.nothingwear.app.data.repository.OfflineFirstCalendarRepository
import com.nothingwear.app.data.watchface.RoomWatchFaceRepository
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DashboardRepository
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.repository.EventSyncRepository
import com.nothingwear.app.domain.repository.ThemePreferencesRepository
import com.nothingwear.app.domain.repository.WatchFaceRepository
import com.nothingwear.protocol.watchs4.WatchS4ProtocolAdapter
import com.nothingwear.protocol.watchs4.XiaomiWatchS4ProtocolAdapter
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

    @Binds
    @Singleton
    abstract fun bindDeviceDiagnosticsRepository(
        impl: AndroidBleDiagnosticsRepository,
    ): DeviceDiagnosticsRepository

    @Binds
    @Singleton
    abstract fun bindEventSyncRepository(
        impl: WatchEventSyncRepository,
    ): EventSyncRepository

    @Binds
    @Singleton
    abstract fun bindWatchFaceRepository(
        impl: RoomWatchFaceRepository,
    ): WatchFaceRepository
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

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context,
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideWatchS4ProtocolAdapter(): WatchS4ProtocolAdapter =
        XiaomiWatchS4ProtocolAdapter()
}
