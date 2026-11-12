package com.hnmlabs.hnmui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnmlabs.hnmui.core.formfactor.FormFactorPolicy
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.core.system.HighRefreshRateHelper
import com.hnmlabs.hnmui.ui.LauncherScreen
import com.hnmlabs.hnmui.ui.LauncherViewModel
import com.hnmlabs.hnmui.ui.theme.HnmLauncherTheme

/**
 * HNMUI Ana Ekran (CATEGORY_HOME) Aktivitesi.
 * 120Hz yüksek yenileme hızı kilidi, duvar kağıdı destekli uçtan uca pencere,
 * One UI üst durum çubuğunu gizleyip iOS durum çubuğu sunma modu
 * ve varsayılan Ana Ekran (RoleManager.ROLE_HOME) geçiş akışını yönetir.
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
    private var explicitUserRoleTap: Boolean = false

    private val defaultLauncherRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val wasExplicitTap = explicitUserRoleTap
        explicitUserRoleTap = false
        syncDeviceAndPermissionState()

        // Kullanıcı butona dokunduğu halde sistem penceresi bastırıldıysa doğrudan Ana Ekran Ayarları'nı aç
        if (wasExplicitTap &&
            result.resultCode != Activity.RESULT_OK &&
            !DefaultLauncherHelper.isDefaultLauncher(this)
        ) {
            DefaultLauncherHelper.openHomeSettings(this)
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        HighRefreshRateHelper.enableMaxRefreshRate(this)
        applySystemStatusBarPolicy(hideForIosBar = viewModel.uiState.value.customization.useIosStatusBar)
        syncDeviceAndPermissionState()

        // İlk açılışta HNMUI henüz varsayılan Ana Ekran değilse sistem seçim penceresini otomatik sun
        if (savedInstanceState == null &&
            !app.hasAutoPromptedDefaultRole &&
            !DefaultLauncherHelper.isDefaultLauncher(this)
        ) {
            app.hasAutoPromptedDefaultRole = true
            promptDefaultLauncherRole(isExplicitTap = false)
        }

        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state.customization.useIosStatusBar) {
                applySystemStatusBarPolicy(hideForIosBar = state.customization.useIosStatusBar)
            }

            HnmLauncherTheme {
                LauncherScreen(
                    viewModel = viewModel,
                    widgetHostManager = app.widgetHostManager,
                    onRequestDefaultLauncher = {
                        promptDefaultLauncherRole(isExplicitTap = true)
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
        HighRefreshRateHelper.enableMaxRefreshRate(this)
        applySystemStatusBarPolicy(hideForIosBar = viewModel.uiState.value.customization.useIosStatusBar)
        syncDeviceAndPermissionState()
        app.installedAppsRepository.refreshAppsAsync()
    }

    private fun applySystemStatusBarPolicy(hideForIosBar: Boolean) {
        val applyNow = {
            runCatching {
                val controller = WindowCompat.getInsetsController(window, window.decorView)
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                if (hideForIosBar) {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
        if (window.decorView.isAttachedToWindow) {
            applyNow()
        } else {
            window.decorView.post { applyNow() }
        }
    }

    override fun onStop() {
        app.widgetHostManager.stopListening()
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME) || intent.action == Intent.ACTION_MAIN) {
            viewModel.onHomeIntentReceived()
        }
    }

    private fun promptDefaultLauncherRole(isExplicitTap: Boolean) {
        val roleIntent = DefaultLauncherHelper.createRoleRequestIntentOrNull(this)
        if (roleIntent != null) {
            explicitUserRoleTap = isExplicitTap
            runCatching { defaultLauncherRoleLauncher.launch(roleIntent) }
                .onFailure {
                    explicitUserRoleTap = false
                    if (isExplicitTap) DefaultLauncherHelper.openHomeSettings(this)
                }
        } else if (isExplicitTap) {
            DefaultLauncherHelper.openHomeSettings(this)
        }
    }

    private fun syncDeviceAndPermissionState() {
        val hz = HighRefreshRateHelper.enableMaxRefreshRate(this)
        viewModel.updateDeviceAndPermissionStatus(
            smallestScreenWidthDp = resources.configuration.smallestScreenWidthDp,
            targetRefreshRateHz = hz,
            isDefaultLauncher = DefaultLauncherHelper.isDefaultLauncher(this),
            isNotificationAccessEnabled = DefaultLauncherHelper.isNotificationAccessEnabled(this)
        )
    }
}
