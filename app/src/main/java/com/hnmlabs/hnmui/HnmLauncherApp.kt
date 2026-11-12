package com.hnmlabs.hnmui

import android.app.Application
import com.hnmlabs.hnmui.core.formfactor.LauncherFeatureFlags
import com.hnmlabs.hnmui.data.HomeLayoutRepository
import com.hnmlabs.hnmui.data.InstalledAppsRepository
import com.hnmlabs.hnmui.widget.LauncherWidgetHostManager

/**
 * HNMUI uygulama sınıfı ve hafif manuel bağımlılık konteyneri (DI).
 * Harici yansıma (reflection) kütüphaneleri kullanmadan hızlı soğuk başlangıç (cold-start) sağlar.
 */
class HnmLauncherApp : Application() {

    val featureFlags = LauncherFeatureFlags(
        enableTabletLayout = false // İlk aşamada telefon aktif; tablet mimaride rezerve tutulur
    )

    var hasAutoPromptedDefaultRole: Boolean = false

    lateinit var installedAppsRepository: InstalledAppsRepository
        private set

    lateinit var homeLayoutRepository: HomeLayoutRepository
        private set

    lateinit var widgetHostManager: LauncherWidgetHostManager
        private set

    override fun onCreate() {
        super.onCreate()
        installedAppsRepository = InstalledAppsRepository(this)
        homeLayoutRepository = HomeLayoutRepository(this)
        widgetHostManager = LauncherWidgetHostManager(this)
    }
}
