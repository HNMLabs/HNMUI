package com.hnmlabs.hnmui.widget

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent

/**
 * Today View ve ana ekranda yerel Android Widget'larını barındırmak için
 * AppWidgetHost ve AppWidgetManager köprüsü.
 */
class LauncherWidgetHostManager(
    context: Context
) {
    private val appContext = context.applicationContext
    val appWidgetManager: AppWidgetManager = AppWidgetManager.getInstance(appContext)
    val appWidgetHost: AppWidgetHost = AppWidgetHost(appContext, HOST_ID)

    fun startListening() {
        runCatching { appWidgetHost.startListening() }
    }

    fun stopListening() {
        runCatching { appWidgetHost.stopListening() }
    }

    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun deleteWidgetId(appWidgetId: Int) {
        runCatching { appWidgetHost.deleteAppWidgetId(appWidgetId) }
    }

    fun createPickWidgetIntent(appWidgetId: Int): Intent {
        return Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
    }

    fun getWidgetInfo(appWidgetId: Int): AppWidgetProviderInfo? {
        return runCatching { appWidgetManager.getAppWidgetInfo(appWidgetId) }.getOrNull()
    }

    fun createHostView(context: Context, appWidgetId: Int): AppWidgetHostView? {
        val info = getWidgetInfo(appWidgetId) ?: return null
        return runCatching {
            appWidgetHost.createView(context, appWidgetId, info).apply {
                setAppWidget(appWidgetId, info)
            }
        }.getOrNull()
    }

    companion object {
        const val HOST_ID = 2048
    }
}
