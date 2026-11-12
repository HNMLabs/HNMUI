package com.hnmlabs.hnmui.core.formfactor

import android.content.pm.ActivityInfo

/**
 * HNMUI özellik bayrakları.
 * İlk aşamada telefon deneyimine odaklanılır; tablet düzeni mimaride rezerve tutulur.
 */
data class LauncherFeatureFlags(
    val enableTabletLayout: Boolean = false
)

enum class DeviceProfile {
    PHONE_ACTIVE,
    TABLET_RESERVED
}

/**
 * Ana ekran, Dock, klasörler ve Today View için tüm ölçü ve ızgara kurallarını soyutlar.
 * Bu sayede UI bileşenleri sabit sayılara bağımlı kalmaz ve tablet modu açıldığında
 * hiçbir ekran yeniden yazılmadan geniş ızgaraya adapte olur.
 */
data class LauncherGridSpec(
    val profile: DeviceProfile,
    val isTabletHardwareDetected: Boolean,
    val columns: Int,
    val rows: Int,
    val dockMaxItems: Int,
    val iconSizeDp: Int,
    val dockIconSizeDp: Int,
    val horizontalPaddingDp: Int,
    val folderPreviewColumns: Int,
    val folderExpandedColumns: Int,
    val todayDualColumn: Boolean,
    val dockShowRecentsDivider: Boolean
) {
    val maxItemsPerPage: Int get() = columns * rows
}

object FormFactorPolicy {
    private const val TABLET_SW_THRESHOLD_DP = 600

    fun resolve(
        smallestScreenWidthDp: Int,
        flags: LauncherFeatureFlags = LauncherFeatureFlags()
    ): LauncherGridSpec {
        val isTabletHardware = smallestScreenWidthDp >= TABLET_SW_THRESHOLD_DP

        return if (isTabletHardware && flags.enableTabletLayout) {
            // Rezerve edilen Tablet / Katlanabilir İç Ekran ızgara profili (Faz 4'te aktifleşir)
            LauncherGridSpec(
                profile = DeviceProfile.TABLET_RESERVED,
                isTabletHardwareDetected = true,
                columns = 6,
                rows = 5,
                dockMaxItems = 6,
                iconSizeDp = 64,
                dockIconSizeDp = 62,
                horizontalPaddingDp = 36,
                folderPreviewColumns = 3,
                folderExpandedColumns = 4,
                todayDualColumn = true,
                dockShowRecentsDivider = true
            )
        } else {
            // Aktif Telefon profili: iOS oranlarına sadık 4x6 ızgara ve 4'lü buzlu cam Dock
            LauncherGridSpec(
                profile = DeviceProfile.PHONE_ACTIVE,
                isTabletHardwareDetected = isTabletHardware,
                columns = 4,
                rows = 6,
                dockMaxItems = 4,
                iconSizeDp = 60,
                dockIconSizeDp = 58,
                horizontalPaddingDp = 20,
                folderPreviewColumns = 2,
                folderExpandedColumns = 3,
                todayDualColumn = false,
                dockShowRecentsDivider = false
            )
        }
    }

    /**
     * Telefon profilinde dikey yönelimi korur; tablet rezervi etkinleştirildiğinde serbest bırakır.
     */
    fun preferredScreenOrientation(flags: LauncherFeatureFlags = LauncherFeatureFlags()): Int {
        return if (flags.enableTabletLayout) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
