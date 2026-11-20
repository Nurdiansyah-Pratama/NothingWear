package com.nothingwear.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationPermissionChecker {
    fun requiresRuntimePermission(): Boolean
    fun hasPostNotificationsPermission(): Boolean
}

@Singleton
class AndroidNotificationPermissionChecker @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : NotificationPermissionChecker {

    override fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    override fun hasPostNotificationsPermission(): Boolean {
        val enabledInSystem = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!enabledInSystem) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationPermissionModule {
    @Binds
    @Singleton
    abstract fun bindNotificationPermissionChecker(
        impl: AndroidNotificationPermissionChecker,
    ): NotificationPermissionChecker
}
