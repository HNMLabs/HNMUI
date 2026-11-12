package com.hnmlabs.hnmui.core.system

import android.app.Activity
import android.os.Build
import android.view.Display
import kotlin.math.roundToInt

/**
 * Cihaz ekranının desteklediği en yüksek yenileme hızını (90Hz / 120Hz / 144Hz)
 * pencere düzeyinde talep eder ve Android 15 (API 35) güç tasarrufu kaynaklı
 * 60Hz kısıtlamasını devre dışı bırakır.
 */
object HighRefreshRateHelper {

    fun enableMaxRefreshRate(activity: Activity): Int {
        val window = activity.window ?: return 60

        // Android 15+ (API 35): Adaptif 60Hz güç dengesini kapat, dokunma ivmesini aç
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            runCatching {
                window.isFrameRatePowerSavingsBalanced = false
                window.frameRateBoostOnTouchEnabled = true
            }
        }

        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching { activity.display }.getOrNull()
        } else {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }

        val supportedModes = display?.supportedModes.orEmpty()
        if (supportedModes.isEmpty()) return 60

        val currentMode = display?.mode
        // Mevcut fiziksel çözünürlüğü koruyarak en yüksek Hz değerine sahip modu seç
        val sameResModes = if (currentMode != null) {
            supportedModes.filter {
                it.physicalWidth == currentMode.physicalWidth &&
                    it.physicalHeight == currentMode.physicalHeight
            }
        } else {
            emptyList()
        }

        val bestMode = (sameResModes.ifEmpty { supportedModes.toList() })
            .maxWithOrNull(
                compareBy<Display.Mode> { it.refreshRate }
                    .thenBy { it.physicalWidth * it.physicalHeight }
            ) ?: return 60

        val attrs = window.attributes
        if (attrs.preferredDisplayModeId != bestMode.modeId ||
            attrs.preferredRefreshRate != bestMode.refreshRate
        ) {
            attrs.preferredDisplayModeId = bestMode.modeId
            attrs.preferredRefreshRate = bestMode.refreshRate
            window.attributes = attrs
        }

        return bestMode.refreshRate.roundToInt().coerceAtLeast(60)
    }
}
