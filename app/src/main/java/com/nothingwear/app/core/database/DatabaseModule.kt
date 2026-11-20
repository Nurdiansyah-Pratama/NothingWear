package com.nothingwear.app.core.database

import android.content.Context
import androidx.room.Room
import com.nothingwear.app.core.database.dao.CalendarDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideNothingWearDatabase(
        @ApplicationContext context: Context,
    ): NothingWearDatabase = Room.databaseBuilder(
        context,
        NothingWearDatabase::class.java,
        "nothingwear.db",
    ).build()

    @Provides
    fun provideCalendarDao(
        database: NothingWearDatabase,
    ): CalendarDao = database.calendarDao()
}
