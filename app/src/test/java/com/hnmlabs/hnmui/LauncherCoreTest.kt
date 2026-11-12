package com.hnmlabs.hnmui

import android.content.pm.ApplicationInfo
import com.hnmlabs.hnmui.core.formfactor.DeviceProfile
import com.hnmlabs.hnmui.core.formfactor.FormFactorPolicy
import com.hnmlabs.hnmui.core.formfactor.LauncherFeatureFlags
import com.hnmlabs.hnmui.data.AppCategoryClassifier
import com.hnmlabs.hnmui.data.HomeLayoutRepository
import com.hnmlabs.hnmui.model.AppCategory
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.HomeGridItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherCoreTest {

    @Test
    fun `phone screen resolves to active 4x6 phone grid`() {
        val spec = FormFactorPolicy.resolve(
            smallestScreenWidthDp = 392,
            flags = LauncherFeatureFlags(enableTabletLayout = false)
        )
        assertEquals(DeviceProfile.PHONE_ACTIVE, spec.profile)
        assertFalse(spec.isTabletHardwareDetected)
        assertEquals(4, spec.columns)
        assertEquals(6, spec.rows)
        assertEquals(4, spec.dockMaxItems)
    }

    @Test
    fun `tablet screen keeps phone layout while tablet flag is reserved`() {
        val spec = FormFactorPolicy.resolve(
            smallestScreenWidthDp = 768,
            flags = LauncherFeatureFlags(enableTabletLayout = false)
        )
        assertEquals(DeviceProfile.PHONE_ACTIVE, spec.profile)
        assertTrue(spec.isTabletHardwareDetected)
        assertEquals(4, spec.columns)
        assertEquals(6, spec.rows)
    }

    @Test
    fun `tablet screen activates 6x5 grid when tablet reserve flag is enabled`() {
        val spec = FormFactorPolicy.resolve(
            smallestScreenWidthDp = 768,
            flags = LauncherFeatureFlags(enableTabletLayout = true)
        )
        assertEquals(DeviceProfile.TABLET_RESERVED, spec.profile)
        assertTrue(spec.isTabletHardwareDetected)
        assertEquals(6, spec.columns)
        assertEquals(5, spec.rows)
        assertEquals(6, spec.dockMaxItems)
        assertTrue(spec.todayDualColumn)
        assertTrue(spec.dockShowRecentsDivider)
    }

    @Test
    fun `custom 7x7 home grid and 7x7 folder grid fit safely within phone screen bounds`() {
        val custom = com.hnmlabs.hnmui.model.LauncherCustomizationSettings(
            gridColumns = 7,
            gridRows = 7,
            folderColumns = 7,
            folderPreviewColumns = 3,
            dockMaxItems = 6,
            dockScalePercent = 85
        )
        val spec = FormFactorPolicy.resolve(
            smallestScreenWidthDp = 384,
            flags = LauncherFeatureFlags(enableTabletLayout = false),
            customization = custom
        )
        assertEquals(7, spec.columns)
        assertEquals(7, spec.rows)
        assertEquals(49, spec.maxItemsPerPage)
        assertEquals(7, spec.folderExpandedColumns)
        assertEquals(3, spec.folderPreviewColumns)
        assertEquals(6, spec.dockMaxItems)
        assertTrue(spec.iconSizeDp in 28..45)
        assertTrue(spec.dockIconSizeDp in 28..52)
    }

    @Test
    fun `classifier categorizes common apps accurately for App Library`() {
        assertEquals(
            AppCategory.SOCIAL,
            AppCategoryClassifier.classify(
                packageName = "com.whatsapp",
                label = "WhatsApp",
                androidCategory = ApplicationInfo.CATEGORY_UNDEFINED
            )
        )
        assertEquals(
            AppCategory.MEDIA,
            AppCategoryClassifier.classify(
                packageName = "com.spotify.music",
                label = "Spotify",
                androidCategory = ApplicationInfo.CATEGORY_UNDEFINED
            )
        )
        assertEquals(
            AppCategory.PRODUCTIVITY,
            AppCategoryClassifier.classify(
                packageName = "com.garanti.cepsubesi",
                label = "Garanti BBVA",
                androidCategory = ApplicationInfo.CATEGORY_UNDEFINED
            )
        )
        assertEquals(
            AppCategory.GAMES,
            AppCategoryClassifier.classify(
                packageName = "com.supercell.brawlstars",
                label = "Brawl Stars",
                androidCategory = ApplicationInfo.CATEGORY_GAME
            )
        )
    }

    @Test
    fun `initial home seed places communication and camera in dock without duplication`() {
        val sampleApps = listOf(
            sampleApp("com.google.android.dialer", "Telefon", AppCategory.SOCIAL),
            sampleApp("com.google.android.apps.messaging", "Mesajlar", AppCategory.SOCIAL),
            sampleApp("com.android.chrome", "Chrome", AppCategory.UTILITIES),
            sampleApp("com.android.camera2", "Kamera", AppCategory.MEDIA),
            sampleApp("com.android.settings", "Ayarlar", AppCategory.UTILITIES),
            sampleApp("com.android.calculator2", "Hesap Makinesi", AppCategory.UTILITIES),
            sampleApp("com.spotify.music", "Spotify", AppCategory.MEDIA)
        )

        val seeded = HomeLayoutRepository.buildInitialSeedState(
            installedApps = sampleApps,
            maxPerPage = 24,
            dockMaxItems = 4
        )

        assertEquals(4, seeded.dock.size)
        val dockIds = seeded.dock.map { it.stableId }.toSet()

        val pageAppIds = seeded.pages.flatMap { page ->
            page.items.flatMap { item ->
                when (item) {
                    is HomeGridItem.AppTile -> listOf(item.key.stableId)
                    is HomeGridItem.FolderTile -> item.apps.map { it.stableId }
                }
            }
        }

        // Dock içindeki hiçbir uygulama ana ekran ızgarasında tekrarlanmamalıdır
        assertTrue(pageAppIds.none { it in dockIds })
        // Ayarlar + Hesap Makinesi örnek Araçlar klasörünü oluşturmalıdır
        assertTrue(seeded.pages.first().items.any { it is HomeGridItem.FolderTile })
    }

    private fun sampleApp(pkg: String, label: String, category: AppCategory): AppItem {
        return AppItem(
            key = AppComponentKey(packageName = pkg, className = "$pkg.MainActivity"),
            label = label,
            category = category,
            firstInstallTimeMillis = 1_700_000_000_000L
        )
    }
}
