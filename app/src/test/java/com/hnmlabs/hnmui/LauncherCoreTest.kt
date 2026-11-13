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

    @Test
    fun `drag and drop supports grid reorder, dock extraction, dock insertion and full dock swap`() {
        val dialer = AppComponentKey("com.dialer", "Main")
        val chat = AppComponentKey("com.chat", "Main")
        val browser = AppComponentKey("com.browser", "Main")
        val camera = AppComponentKey("com.camera", "Main")
        val spotify = AppComponentKey("com.spotify", "Main")
        val notes = AppComponentKey("com.notes", "Main")

        val initial = com.hnmlabs.hnmui.model.PersistedHomeState(
            dock = listOf(dialer, chat, browser),
            pages = listOf(
                com.hnmlabs.hnmui.model.HomePageModel(
                    id = "page_1",
                    items = listOf(
                        HomeGridItem.AppTile(camera),
                        HomeGridItem.AppTile(spotify),
                        HomeGridItem.AppTile(notes)
                    )
                )
            )
        )

        // 1) Ana ekrandan Dock'a sürükleyip ekleme (3 -> 4 öğe)
        val afterAddToDock = HomeLayoutRepository.applyDragDropToState(
            state = initial,
            source = HomeLayoutRepository.Companion.LayoutDragSource.HomeSlot(0, 1),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.DockCell(1),
            maxPerPage = 24,
            maxDockItems = 4
        )
        assertEquals(listOf(dialer, spotify, chat, browser), afterAddToDock.dock)
        assertEquals(2, afterAddToDock.pages.first().items.size)

        // 2) Dolu Dock'a (4/4) başka bir uygulama sürüklenirse hedef yuvadaki uygulama ana ekrana takas edilir
        val afterFullSwap = HomeLayoutRepository.applyDragDropToState(
            state = afterAddToDock,
            source = HomeLayoutRepository.Companion.LayoutDragSource.HomeSlot(0, 0),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.DockCell(2),
            maxPerPage = 24,
            maxDockItems = 4
        )
        assertEquals(listOf(dialer, spotify, camera, browser), afterFullSwap.dock)
        assertTrue(
            afterFullSwap.pages.first().items.any {
                it is HomeGridItem.AppTile && it.key == chat
            }
        )

        // 3) Dock'tan uygulamayı sürükleyip ana ekrana çıkarma
        val afterExtractFromDock = HomeLayoutRepository.applyDragDropToState(
            state = afterFullSwap,
            source = HomeLayoutRepository.Companion.LayoutDragSource.DockSlot(0),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.HomeCell(0, 0),
            maxPerPage = 24,
            maxDockItems = 4
        )
        assertEquals(listOf(spotify, camera, browser), afterExtractFromDock.dock)
        val firstHomeItem = afterExtractFromDock.pages.first().items.first() as HomeGridItem.AppTile
        assertEquals(dialer, firstHomeItem.key)

        // 4) Ana ekran ızgarası içinde sıralama değiştirme
        val afterHomeReorder = HomeLayoutRepository.applyDragDropToState(
            state = afterExtractFromDock,
            source = HomeLayoutRepository.Companion.LayoutDragSource.HomeSlot(0, 0),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.HomeCell(0, 2),
            maxPerPage = 24,
            maxDockItems = 4
        )
        val thirdItem = afterHomeReorder.pages.first().items[2] as HomeGridItem.AppTile
        assertEquals(dialer, thirdItem.key)
    }

    @Test
    fun `turkish search normalizer matches dotted and dotless I across English and Turkish apps`() {
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("InShot", "inshot")
        )
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("Instagram", "insta")
        )
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("Instagram", "İnsta")
        )
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("İşCep", "iscep")
        )
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("İşCep", "işcep")
        )
        assertTrue(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("7×7 Izgara", "izgara")
        )
        assertFalse(
            com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.containsMatch("Sirket", "şirket")
        )
    }

    @Test
    fun `dragging app onto another app creates 2-app folder from home or dock without duplication`() {
        val dialer = AppComponentKey("com.dialer", "Main")
        val camera = AppComponentKey("com.camera", "Main")
        val gallery = AppComponentKey("com.gallery", "Main")
        val notes = AppComponentKey("com.notes", "Main")

        val initial = com.hnmlabs.hnmui.model.PersistedHomeState(
            dock = listOf(dialer),
            pages = listOf(
                com.hnmlabs.hnmui.model.HomePageModel(
                    id = "page_1",
                    items = listOf(
                        HomeGridItem.AppTile(camera),
                        HomeGridItem.AppTile(gallery),
                        HomeGridItem.AppTile(notes)
                    )
                )
            )
        )

        // 1) Ana ekrandaki Galeri ikonunu Kamera ikonunun üstüne bırakarak yeni klasör kurma
        val afterHomeFolderCreate = HomeLayoutRepository.applyDragDropToState(
            state = initial,
            source = HomeLayoutRepository.Companion.LayoutDragSource.HomeSlot(
                pageIndex = 0,
                slotIndex = 1,
                expectedItemId = "app:${gallery.stableId}"
            ),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.CreateFolderWithApp(
                targetAppKey = camera,
                folderTitle = "Medya"
            ),
            maxPerPage = 24,
            maxDockItems = 4
        )

        assertEquals(2, afterHomeFolderCreate.pages.first().items.size)
        val createdFolder = afterHomeFolderCreate.pages.first().items.first() as HomeGridItem.FolderTile
        assertEquals("Medya", createdFolder.title)
        assertEquals(listOf(camera, gallery), createdFolder.apps)

        // 2) Dock'taki Telefon ikonunu Notlar ikonunun üstüne bırakarak Dock'tan çıkarıp klasör kurma
        val afterDockFolderCreate = HomeLayoutRepository.applyDragDropToState(
            state = afterHomeFolderCreate,
            source = HomeLayoutRepository.Companion.LayoutDragSource.DockSlot(
                dockIndex = 0,
                expectedAppKey = dialer
            ),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.CreateFolderWithApp(
                targetAppKey = notes,
                folderTitle = "İletişim"
            ),
            maxPerPage = 24,
            maxDockItems = 4
        )

        assertTrue(afterDockFolderCreate.dock.isEmpty())
        assertEquals(2, afterDockFolderCreate.pages.first().items.size)
        val secondFolder = afterDockFolderCreate.pages.first().items[1] as HomeGridItem.FolderTile
        assertEquals(listOf(notes, dialer), secondFolder.apps)
    }

    @Test
    fun `identity verified drag locates shifted slot and uninstall pruning collapses 1-app folders`() {
        val appA = AppComponentKey("com.a", "Main")
        val appB = AppComponentKey("com.b", "Main")
        val appC = AppComponentKey("com.c", "Main")
        val uninstalled = AppComponentKey("com.removed", "Main")

        val state = com.hnmlabs.hnmui.model.PersistedHomeState(
            dock = listOf(appA, uninstalled),
            pages = listOf(
                com.hnmlabs.hnmui.model.HomePageModel(
                    id = "page_1",
                    items = listOf(
                        HomeGridItem.FolderTile(
                            id = "folder_1",
                            title = "Araçlar",
                            apps = listOf(appB, uninstalled)
                        ),
                        HomeGridItem.AppTile(appC)
                    )
                )
            )
        )

        // Kaldırılan uygulama temizlendiğinde 1 uygulamaya düşen klasör tekil ikona dönüşmeli
        val pruned = HomeLayoutRepository.pruneUninstalledAndNormalize(
            state = state,
            installedIds = setOf(appA.stableId, appB.stableId, appC.stableId),
            maxPerPage = 24
        )
        assertEquals(listOf(appA), pruned.dock)
        assertEquals(
            listOf(HomeGridItem.AppTile(appB), HomeGridItem.AppTile(appC)),
            pruned.pages.first().items
        )

        // Slot indeksi kaymış olsa bile expectedItemId doğru uygulamayı bulup Dock'a taşımalı
        val movedWithStaleSlot = HomeLayoutRepository.applyDragDropToState(
            state = pruned,
            source = HomeLayoutRepository.Companion.LayoutDragSource.HomeSlot(
                pageIndex = 0,
                slotIndex = 0, // Yanlış/eski slot indeksi (gerçekte appC slot 1'de)
                expectedItemId = "app:${appC.stableId}"
            ),
            destination = HomeLayoutRepository.Companion.LayoutDropDestination.DockCell(1),
            maxPerPage = 24,
            maxDockItems = 4
        )
        assertEquals(listOf(appA, appC), movedWithStaleSlot.dock)
        assertEquals(listOf(HomeGridItem.AppTile(appB)), movedWithStaleSlot.pages.first().items)
    }

    @Test
    fun `squircle shape caches outline instances per size and handles zero or non-finite bounds safely`() {
        val shape = com.hnmlabs.hnmui.ui.components.SquircleShape(exponent = 4.4f, steps = 72)
        assertEquals(72, shape.unitX.size)
        assertTrue(shape.unitX.all { it in -1.0001f..1.0001f })
        assertTrue(shape.unitY.all { it in -1.0001f..1.0001f })

        var buildCalls = 0
        val builder: (Float, Float) -> androidx.compose.ui.graphics.Outline = { halfW, halfH ->
            buildCalls++
            androidx.compose.ui.graphics.Outline.Rectangle(
                androidx.compose.ui.geometry.Rect(0f, 0f, halfW * 2f, halfH * 2f)
            )
        }

        val first = shape.resolveOutline(androidx.compose.ui.geometry.Size(144f, 144f), builder)
        val second = shape.resolveOutline(androidx.compose.ui.geometry.Size(144f, 144f), builder)
        val otherSize = shape.resolveOutline(androidx.compose.ui.geometry.Size(120f, 120f), builder)

        org.junit.Assert.assertSame(first, second)
        org.junit.Assert.assertNotSame(first, otherSize)
        assertEquals(2, buildCalls)

        val density = androidx.compose.ui.unit.Density(3f)
        val zeroOutline = shape.createOutline(
            size = androidx.compose.ui.geometry.Size.Zero,
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            density = density
        )
        val nanOutline = shape.createOutline(
            size = androidx.compose.ui.geometry.Size(Float.NaN, 100f),
            layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr,
            density = density
        )
        assertTrue(zeroOutline is androidx.compose.ui.graphics.Outline.Rectangle)
        assertTrue(nanOutline is androidx.compose.ui.graphics.Outline.Rectangle)
    }

    @Test
    fun `prepared turkish search query matches pre-folded app items without per-item folding`() {
        val isCep = sampleApp("com.pozitron.iscep", "İşCep", AppCategory.PRODUCTIVITY)
        val instagram = sampleApp("com.instagram.android", "Instagram", AppCategory.SOCIAL)
        val inShot = sampleApp("com.camerasideas.instashot", "InShot", AppCategory.MEDIA)

        val qIscep = com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.prepareQuery("iscep")
        assertTrue(isCep.matchesPreparedQuery(qIscep))
        assertTrue(isCep.labelStartsWithPreparedQuery(qIscep))
        assertFalse(instagram.matchesPreparedQuery(qIscep))

        val qInstaDotted = com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer.prepareQuery("İnsta")
        assertTrue(instagram.matchesPreparedQuery(qInstaDotted))
        assertTrue(instagram.labelStartsWithPreparedQuery(qInstaDotted))
        assertTrue(inShot.matchesPreparedQuery(qInstaDotted)) // package contains instashot
        assertFalse(inShot.labelStartsWithPreparedQuery(qInstaDotted))
    }

    @Test
    fun `single-pass app library classifier buckets categories and sorts suggested by launch count`() {
        val signal = sampleApp("org.thoughtcrime.securesms", "Signal", AppCategory.SOCIAL)
        val whatsapp = sampleApp("com.whatsapp", "WhatsApp", AppCategory.SOCIAL)
        val telegram = sampleApp("org.telegram.messenger", "Telegram", AppCategory.SOCIAL)
        val spotify = sampleApp("com.spotify.music", "Spotify", AppCategory.MEDIA)

        val groups = AppCategoryClassifier.buildAppLibraryGroups(
            apps = listOf(whatsapp, signal, spotify, telegram),
            launchCounts = mapOf(whatsapp.key.stableId to 12, telegram.key.stableId to 3)
        )

        val suggestedGroup = groups.first { it.category == AppCategory.SUGGESTED }
        assertEquals(listOf(whatsapp, telegram, signal, spotify), suggestedGroup.apps)

        val socialGroup = groups.first { it.category == AppCategory.SOCIAL }
        assertEquals(listOf(signal, telegram, whatsapp), socialGroup.apps)

        val mediaGroup = groups.first { it.category == AppCategory.MEDIA }
        assertEquals(listOf(spotify), mediaGroup.apps)
    }

    @Test
    fun `adaptive theme mode resolves system, light and dark palettes and preserves backwards-compatible JSON`() {
        assertTrue(com.hnmlabs.hnmui.ui.theme.resolveIsDarkTheme(com.hnmlabs.hnmui.model.HnmThemeMode.SYSTEM, systemDark = true))
        assertFalse(com.hnmlabs.hnmui.ui.theme.resolveIsDarkTheme(com.hnmlabs.hnmui.model.HnmThemeMode.SYSTEM, systemDark = false))
        assertFalse(com.hnmlabs.hnmui.ui.theme.resolveIsDarkTheme(com.hnmlabs.hnmui.model.HnmThemeMode.LIGHT, systemDark = true))
        assertTrue(com.hnmlabs.hnmui.ui.theme.resolveIsDarkTheme(com.hnmlabs.hnmui.model.HnmThemeMode.DARK, systemDark = false))

        assertEquals(
            com.hnmlabs.hnmui.model.HnmThemeMode.LIGHT,
            com.hnmlabs.hnmui.model.HnmThemeMode.SYSTEM.nextMode()
        )
        assertEquals(
            com.hnmlabs.hnmui.model.HnmThemeMode.DARK,
            com.hnmlabs.hnmui.model.HnmThemeMode.LIGHT.nextMode()
        )
        assertEquals(
            com.hnmlabs.hnmui.model.HnmThemeMode.SYSTEM,
            com.hnmlabs.hnmui.model.HnmThemeMode.DARK.nextMode()
        )

        // Geriye dönük uyumluluk: v0.3.3 ve öncesi JSON kaydında themeMode alanı yoksa otomatik SYSTEM çözümlenmeli
        val json = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
        val legacyJson = """{"customization":{"gridColumns":5,"gridRows":6}}"""
        val decodedLegacy = json.decodeFromString(
            com.hnmlabs.hnmui.model.PersistedHomeState.serializer(),
            legacyJson
        )
        assertEquals(com.hnmlabs.hnmui.model.HnmThemeMode.SYSTEM, decodedLegacy.customization.themeMode)
        assertEquals(5, decodedLegacy.customization.gridColumns)

        // Açık mod (LIGHT) kalıcılığı ve renk kontrast doğrulaması
        val lightState = decodedLegacy.copy(
            customization = decodedLegacy.customization.copy(
                themeMode = com.hnmlabs.hnmui.model.HnmThemeMode.LIGHT
            )
        )
        val roundTripped = json.decodeFromString(
            com.hnmlabs.hnmui.model.PersistedHomeState.serializer(),
            json.encodeToString(com.hnmlabs.hnmui.model.PersistedHomeState.serializer(), lightState)
        )
        assertEquals(com.hnmlabs.hnmui.model.HnmThemeMode.LIGHT, roundTripped.customization.themeMode)
        assertTrue(com.hnmlabs.hnmui.ui.theme.DarkHnmColors.isDark)
        assertFalse(com.hnmlabs.hnmui.ui.theme.LightHnmColors.isDark)
        org.junit.Assert.assertNotEquals(
            com.hnmlabs.hnmui.ui.theme.DarkHnmColors.textPrimary,
            com.hnmlabs.hnmui.ui.theme.LightHnmColors.textPrimary
        )
    }

    @Test
    fun `downward pull classifier opens Control Center reliably across cutout strips, diagonal thumb arcs and mid-right home zone`() {
        val w = 1080f
        val h = 2400f
        val topStrip = 128f // Kamera delikli (punch-hole) üst şerit
        val topPull = 42f
        val homePull = 72f
        val spotlightPull = 96f
        val slop = 24f

        // 1) Sağ üst durum çubuğu / kamera çentiği çekişi Today View, Ana Ekran, App Library ve Düzenleme Modunda dahi %100 açılmalı
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.OPEN_CONTROL_CENTER,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.78f,
                startY = 96f,
                dx = -28f,
                dy = 48f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = false, // Today View veya App Library sayfasında bile çalışmalı!
                isEditMode = true
            )
        )

        // 2) Sol üst durum çubuğu çekişi -> Bildirim Merkezi
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.OPEN_NOTIFICATION_CENTER,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.22f,
                startY = 60f,
                dx = 14f,
                dy = 46f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = false,
                isEditMode = false
            )
        )

        // 3) Doğal sağ başparmak kavisi (sola-aşağı diyagonal çekiş: dx = -44px, dy = 78px) kesilmemeli -> Denetim Merkezi!
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.OPEN_CONTROL_CENTER,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.82f,
                startY = h * 0.34f,
                dx = -44f,
                dy = 78f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = true,
                isEditMode = false
            )
        )

        // 4) Tek elle erişim için orta-sağ bölge (y = %62 ekran yüksekliği) -> Denetim Merkezi
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.OPEN_CONTROL_CENTER,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.72f,
                startY = h * 0.62f,
                dx = -18f,
                dy = 80f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = true,
                isEditMode = false
            )
        )

        // 5) Ana ekran orta-alt bölge -> Spotlight Arama
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.OPEN_SPOTLIGHT,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.50f,
                startY = h * 0.56f,
                dx = 8f,
                dy = 104f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = true,
                isEditMode = false
            )
        )

        // 6) Belirgin yatay sayfa kaydırma hareketi HorizontalPager'a bırakılmalı (ABORT)
        assertEquals(
            com.hnmlabs.hnmui.ui.DownwardPullAction.ABORT,
            com.hnmlabs.hnmui.ui.classifyDownwardPull(
                startX = w * 0.75f,
                startY = h * 0.40f,
                dx = -110f,
                dy = 14f,
                rootWidthPx = w,
                rootHeightPx = h,
                effectiveTopStripPx = topStrip,
                topPullThresholdPx = topPull,
                homePullThresholdPx = homePull,
                spotlightThresholdPx = spotlightPull,
                touchSlopPx = slop,
                isOnHomePages = true,
                isEditMode = false
            )
        )
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
