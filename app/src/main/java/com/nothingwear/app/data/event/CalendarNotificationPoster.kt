package com.nothingwear.app.data.event

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nothingwear.app.MainActivity
import com.nothingwear.app.R
import com.nothingwear.app.core.permissions.NotificationPermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Posts concise, watch-optimized Android notifications designed to be mirrored cleanly
 * onto Xiaomi Watch S4 by Mi Fitness's official NotifySyncService.
 */
@Singleton
class CalendarNotificationPoster @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val permissionChecker: NotificationPermissionChecker,
) {

    fun ensureChannel() {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Calendar reminders formatted for Xiaomi Watch S4 forwarding via Mi Fitness."
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    fun postReminder(
        notificationId: Int,
        title: String,
        timeRangeLabel: String,
        calendarName: String,
        location: String?,
    ): Boolean {
        if (!permissionChecker.hasPostNotificationsPermission()) return false
        ensureChannel()

        val bodyText = buildString {
            append(timeRangeLabel)
            if (!location.isNullOrBlank()) {
                append(" · ")
                append(location)
            }
            append(" · ")
            append(calendarName)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    fun postTestWatchAlert(): Boolean = postReminder(
        notificationId = TEST_NOTIFICATION_ID,
        title = "NothingWear Watch Test Alert",
        timeRangeLabel = "Verified Bridge Check",
        calendarName = "Xiaomi Watch S4",
        location = "Enable NothingWear in Mi Fitness Notifications",
    )

    companion object {
        const val CHANNEL_ID = "nothingwear_calendar_reminders"
        const val CHANNEL_NAME = "Calendar Watch Reminders"
        private const val TEST_NOTIFICATION_ID = 40_001
    }
}
