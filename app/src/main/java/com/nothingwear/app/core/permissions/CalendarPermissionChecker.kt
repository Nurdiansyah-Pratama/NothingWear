package com.nothingwear.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

fun interface CalendarPermissionChecker {
    fun hasReadCalendarPermission(): Boolean

    fun hasWriteCalendarPermission(): Boolean = hasReadCalendarPermission()
}

@Singleton
class AndroidCalendarPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : CalendarPermissionChecker {

    override fun hasReadCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED

    override fun hasWriteCalendarPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CALENDAR,
        ) == PackageManager.PERMISSION_GRANTED
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    @Singleton
    abstract fun bindCalendarPermissionChecker(
        impl: AndroidCalendarPermissionChecker,
    ): CalendarPermissionChecker
}
