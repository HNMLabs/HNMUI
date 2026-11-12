package com.hnmlabs.hnmui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.hnmlabs.hnmui.core.formfactor.FormFactorPolicy
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.ui.LauncherScreen
import com.hnmlabs.hnmui.ui.LauncherViewModel
import com.hnmlabs.hnmui.ui.theme.HnmLauncherTheme

/**
 * HNMUI Ana Ekran (CATEGORY_HOME) Aktivitesi.
 * Duvar kağıdı destekli tam ekran uçtan uca (edge-to-edge) pencere kurar,
 * telefon profilinde dikey yönelimi kilitler ve Widget / Varsayılan Launcher izin akışlarını yönetir.
 */
class LauncherActivity : ComponentActivity() {

    private val app: HnmLauncherApp
        get() = application as HnmLauncherApp

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.factory(
            appsRepository = app.installedAppsRepository,
            layoutRepository = app.homeLayoutRepository,
            featureFlags = app.featureFlags
        )
    }

    private var pendingAppWidgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    private val defaultLauncherRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        syncDeviceAndPermissionState()
    }

    private val pickWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = result.data?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            pendingAppWidgetId
        ) ?: pendingAppWidgetId

        if (result.resultCode == Activity.RESULT_OK &&
            widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            val info = app.widgetHostManager.getWidgetInfo(widgetId)
            if (info?.configure != null) {
                val configureIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                    component = info.configure
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                }
                pendingAppWidgetId = widgetId
                configureWidgetLauncher.launch(configureIntent)
            } else {
                viewModel.onWidgetBound(widgetId)
                pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
            }
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            app.widgetHostManager.deleteWidgetId(widgetId)
            pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        }
    }

    private val configureWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val widgetId = pendingAppWidgetId
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        if (result.resultCode == Activity.RESULT_OK &&
            widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
        ) {
            viewModel.onWidgetBound(widgetId)
        } else if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            app.widgetHostManager.deleteWidgetId(widgetId)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = FormFactorPolicy.preferredScreenOrientation(app.featureFlags)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        syncDeviceAndPermissionState()

        setContent {
            HnmLauncherTheme {
                LauncherScreen(
                    viewModel = viewModel,
                    widgetHostManager = app.widgetHostManager,
                    onRequestDefaultLauncher = {
                        val intent = DefaultLauncherHelper.createRequestDefaultLauncherIntent(this)
                        runCatching { defaultLauncherRoleLauncher.launch(intent) }
                    },
                    onRequestPickWidget = {
                        val id = app.widgetHostManager.allocateWidgetId()
                        pendingAppWidgetId = id
                        val pickIntent = app.widgetHostManager.createPickWidgetIntent(id)
                        runCatching { pickWidgetLauncher.launch(pickIntent) }
                            .onFailure {
                                app.widgetHostManager.deleteWidgetId(id)
                                pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
                            }
                    },
                    onRemoveHostedWidget = { widgetId ->
                        app.widgetHostManager.deleteWidgetId(widgetId)
                        viewModel.onWidgetRemoved(widgetId)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        app.widgetHostManager.startListening()
    }

    override fun onResume() {
        super.onResume()
        syncDeviceAndPermissionState()
        app.installedAppsRepository.refreshAppsAsync()
    }

    override fun onStop() {
        app.widgetHostManager.stopListening()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            viewModel.onHomeIntentReceived()
        }
    }

    private fun syncDeviceAndPermissionState() {
        viewModel.updateDeviceAndPermissionStatus(
            smallestScreenWidthDp = resources.configuration.smallestScreenWidthDp,
            isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(this),
            isNotificationAccessEnabled = DefaultLauncherHelper.isNotificationAccessEnabled(this)
        )
    }
}
