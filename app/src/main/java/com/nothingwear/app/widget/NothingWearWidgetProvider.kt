package com.nothingwear.app.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * NothingWear Home Screen Widget (4x2 resizable).
 * Displays verified Steps, Heart Rate, Sleep, and Next Calendar Event with 1-tap Sync.
 */
class NothingWearWidgetProvider : AppWidgetProvider() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WidgetEntryPoint {
        fun widgetUpdater(): NothingWearWidgetUpdater
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        triggerAsyncRefresh(context, refreshHealthFromProvider = true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            triggerAsyncRefresh(context, refreshHealthFromProvider = true)
        }
    }

    private fun triggerAsyncRefresh(context: Context, refreshHealthFromProvider: Boolean) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    appContext,
                    WidgetEntryPoint::class.java,
                )
                entryPoint.widgetUpdater().refreshAllWidgets(
                    refreshHealthFromProvider = refreshHealthFromProvider,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.nothingwear.app.widget.ACTION_REFRESH_WIDGET"

        fun requestSyncBroadcast(context: Context) {
            val intent = Intent(context, NothingWearWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
