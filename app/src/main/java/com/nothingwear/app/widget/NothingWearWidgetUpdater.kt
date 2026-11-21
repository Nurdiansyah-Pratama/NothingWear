package com.nothingwear.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nothingwear.app.MainActivity
import com.nothingwear.app.R
import com.nothingwear.app.core.common.TimeProvider
import com.nothingwear.app.domain.repository.CalendarRepository
import com.nothingwear.app.domain.repository.DeviceDiagnosticsRepository
import com.nothingwear.app.domain.repository.HealthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NothingWearWidgetUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val calendarRepository: CalendarRepository,
    private val healthRepository: HealthRepository,
    private val deviceDiagnosticsRepository: DeviceDiagnosticsRepository,
    private val timeProvider: TimeProvider,
) {

    suspend fun refreshAllWidgets(refreshHealthFromProvider: Boolean = false) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, NothingWearWidgetProvider::class.java)
        val widgetIds = manager.getAppWidgetIds(component)
        if (widgetIds.isEmpty()) return

        if (refreshHealthFromProvider) {
            runCatching { healthRepository.refreshHealthData() }
        }

        val events = runCatching { calendarRepository.observeSelectedEvents().first() }.getOrDefault(emptyList())
        val health = runCatching { healthRepository.observeHealthSnapshot().first() }
            .getOrNull() ?: return
        val bleHr = runCatching {
            deviceDiagnosticsRepository.observeDiagnostics().first().gattSession.heartRateState
        }.getOrNull() ?: com.nothingwear.app.domain.model.BleHeartRateMonitorState()

        val viewData = WidgetSnapshotFormatter.format(
            events = events,
            health = health,
            bleHeartRate = bleHr,
            nowMillis = timeProvider.nowMillis(),
            zoneId = timeProvider.zoneId(),
        )

        val views = buildRemoteViews(viewData)
        widgetIds.forEach { id ->
            manager.updateAppWidget(id, views)
        }
    }

    private fun buildRemoteViews(data: NothingWidgetViewData): RemoteViews {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            REQ_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val refreshIntent = Intent(context, NothingWearWidgetProvider::class.java).apply {
            action = NothingWearWidgetProvider.ACTION_REFRESH_WIDGET
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_REFRESH_WIDGET,
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return RemoteViews(context.packageName, R.layout.widget_nothingwear).apply {
            setTextViewText(R.id.widget_steps_value, data.stepsText)
            setTextViewText(R.id.widget_hr_value, data.heartRateText)
            setTextViewText(R.id.widget_sleep_value, data.sleepText)
            setTextViewText(R.id.widget_event_meta, data.nextEventMeta)
            setTextViewText(R.id.widget_event_title, data.nextEventTitle)
            setTextViewText(R.id.widget_updated_at, data.updatedAtText)

            setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_event_container, openAppPendingIntent)
            setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)
        }
    }

    private companion object {
        const val REQ_OPEN_APP = 701
        const val REQ_REFRESH_WIDGET = 702
    }
}
