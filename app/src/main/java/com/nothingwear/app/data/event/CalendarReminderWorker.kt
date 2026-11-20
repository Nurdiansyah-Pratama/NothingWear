package com.nothingwear.app.data.event

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nothingwear.app.core.permissions.AndroidNotificationPermissionChecker

class CalendarReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: return Result.failure()
        val timeRange = inputData.getString(KEY_TIME_RANGE).orEmpty().ifBlank { "Upcoming Event" }
        val calendarName = inputData.getString(KEY_CALENDAR_NAME).orEmpty().ifBlank { "Calendar" }
        val location = inputData.getString(KEY_LOCATION)
        val notificationId = inputData.getInt(KEY_NOTIFICATION_ID, title.hashCode())

        val poster = CalendarNotificationPoster(
            context = applicationContext,
            permissionChecker = AndroidNotificationPermissionChecker(applicationContext),
        )
        poster.postReminder(
            notificationId = notificationId,
            title = title,
            timeRangeLabel = timeRange,
            calendarName = calendarName,
            location = location,
        )
        return Result.success()
    }

    companion object {
        const val WORK_TAG = "nothingwear_calendar_reminder_bridge"
        const val KEY_TITLE = "title"
        const val KEY_TIME_RANGE = "time_range"
        const val KEY_CALENDAR_NAME = "calendar_name"
        const val KEY_LOCATION = "location"
        const val KEY_NOTIFICATION_ID = "notification_id"
    }
}
