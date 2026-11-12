package com.hnmlabs.hnmui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.data.InstalledAppsRepository
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.ui.components.LocalGlassOpacityFactor
import com.hnmlabs.hnmui.ui.overlays.ControlCenterOverlay
import com.hnmlabs.hnmui.ui.overlays.FolderModalOverlay
import com.hnmlabs.hnmui.ui.overlays.HnmSettingsOverlay
import com.hnmlabs.hnmui.ui.overlays.QuickActionMenuOverlay
import com.hnmlabs.hnmui.ui.overlays.SpotlightOverlay
import com.hnmlabs.hnmui.ui.surfaces.AppLibraryPage
import com.hnmlabs.hnmui.ui.surfaces.HomeGridPage
import com.hnmlabs.hnmui.ui.surfaces.LauncherBottomDockBar
import com.hnmlabs.hnmui.ui.surfaces.TodayViewPage
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.widget.LauncherWidgetHostManager
import kotlinx.coroutines.launch

/**
 * HNMUI kök uzamsal ekranı.
 * Yatay düzlem: [0: Today View] <-> [1..N: Ana Ekran Sayfaları (4x4..7x7)] <-> [N+1: App Library]
 * Katman düzlemi: Buzlu Cam Dock, Denetim Merkezi, HNM Ayarlar, Spotlight, Klasör (3x3..7x7), Haptic Menü.
 */
@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    widgetHostManager: LauncherWidgetHostManager,
    onRequestDefaultLauncher: () -> Unit,
    onRequestPickWidget: () -> Unit,
    onRemoveHostedWidget: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Sayfa dizilimi: 0 = Today View, 1..homePages.size = Ana Ekran, son sayfa = App Library
    val homePageCount = state.homePages.size.coerceAtLeast(1)
    val totalPages = 1 + homePageCount + 1
    val primaryHomeIndex = 1
    val appLibraryIndex = totalPages - 1

    val pagerState = rememberPagerState(
        initialPage = primaryHomeIndex,
        pageCount = { totalPages }
    )

    // Fiziksel/jest Home tuşu sinyali geldiğinde birincil ana ekran sayfasına yay animasyonuyla dön
    LaunchedEffect(viewModel) {
        viewModel.scrollToPrimaryHomeSignal.collect {
            pagerState.animateScrollToPage(
                page = primaryHomeIndex,
                animationSpec = HnmMotion.sheetSpring()
            )
        }
    }

    // Geri tuşu öncelik zinciri (Home Launcher asla ana ekrandan kapanmamalıdır)
    BackHandler(enabled = !state.isHnmSettingsOpen) {
        when {
            state.quickMenu != null -> viewModel.dismissQuickMenu()
            state.openFolderModal != null -> viewModel.closeFolderModal()
            state.isControlCenterOpen -> viewModel.closeControlCenter()
            state.isSpotlightOpen -> viewModel.closeSpotlight()
            state.isEditMode -> viewModel.toggleEditMode()
            pagerState.currentPage != primaryHomeIndex -> {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = primaryHomeIndex,
                        animationSpec = HnmMotion.sheetSpring()
                    )
                }
            }
        }
    }

    val isOnHomeScreenPages = pagerState.currentPage in 1..homePageCount

    CompositionLocalProvider(
        LocalGlassOpacityFactor provides state.gridSpec.glassOpacityFactor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color(0x3D070B14),
                            0.22f to Color(0x12070B14),
                            0.78f to Color(0x16070B14),
                            1.0f to Color(0x4D070B14)
                        )
                    )
                )
        ) {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                userScrollEnabled = !state.isHnmSettingsOpen && !state.isControlCenterOpen && !state.isSpotlightOpen,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                when (pageIndex) {
                    0 -> {
                        TodayViewPage(
                            isDefaultLauncher = state.isDefaultLauncher,
                            isNotificationAccessEnabled = state.isNotificationAccessEnabled,
                            isTabletHardwareDetected = state.gridSpec.isTabletHardwareDetected,
                            targetRefreshRateHz = state.targetRefreshRateHz,
                            suggestedApps = state.suggestedApps,
                            installedAppCount = state.allApps.size,
                            totalUnreadBadges = state.totalUnreadBadges,
                            quickNotes = state.quickNotes,
                            boundWidgetIds = state.boundWidgetIds,
                            widgetHostManager = widgetHostManager,
                            cachedIconProvider = viewModel::getCachedIcon,
                            loadIconSuspend = viewModel::loadIconIfNeeded,
                            onRequestDefaultLauncher = onRequestDefaultLauncher,
                            onRequestNotificationAccess = {
                                DefaultLauncherHelper.openNotificationListenerSettings(context)
                            },
                            onLaunchApp = viewModel::launchApp,
                            onAppLongPress = { viewModel.openQuickMenu(it) },
                            onAddQuickNote = viewModel::addQuickNote,
                            onToggleQuickNote = viewModel::toggleQuickNote,
                            onDeleteQuickNote = viewModel::deleteQuickNote,
                            onPickWidget = onRequestPickWidget,
                            onRemoveWidget = onRemoveHostedWidget
                        )
                    }

                    appLibraryIndex -> {
                        AppLibraryPage(
                            groups = state.appLibraryGroups,
                            allApps = state.allApps,
                            cachedIconProvider = viewModel::getCachedIcon,
                            loadIconSuspend = viewModel::loadIconIfNeeded,
                            onLaunchApp = viewModel::launchApp,
                            onAppLongPress = { viewModel.openQuickMenu(it) },
                            onExpandCategory = viewModel::openAppLibraryCategory
                        )
                    }

                    else -> {
                        val homeIdx = (pageIndex - 1).coerceIn(0, state.homePages.lastIndex)
                        val homePage = state.homePages[homeIdx]
                        HomeGridPage(
                            page = homePage,
                            gridSpec = state.gridSpec,
                            isDefaultLauncher = state.isDefaultLauncher,
                            isEditMode = state.isEditMode,
                            cachedIconProvider = viewModel::getCachedIcon,
                            loadIconSuspend = viewModel::loadIconIfNeeded,
                            onRequestDefaultLauncher = onRequestDefaultLauncher,
                            onAppClick = viewModel::launchApp,
                            onAppLongPress = { viewModel.openQuickMenu(it) },
                            onRemoveAppFromHome = viewModel::removeAppFromHomeScreen,
                            onFolderClick = viewModel::openHomeFolder,
                            onPullDownSpotlight = viewModel::openSpotlight,
                            onPullDownControlCenter = viewModel::openControlCenter,
                            onPullDownNotifications = {
                                if (!DeviceSystemController.expandNotificationShade(context)) {
                                    viewModel.openSpotlight()
                                }
                            },
                            onOpenHnmSettings = viewModel::openHnmSettings,
                            onToggleEditMode = viewModel::toggleEditMode,
                            onPickWallpaper = {
                                DefaultLauncherHelper.openWallpaperPicker(context)
                            }
                        )
                    }
                }
            }

            // Ana ekran sayfalarında sabitlenen Buzlu Cam Dock + "Ara" hapı
            AnimatedVisibility(
                visible = isOnHomeScreenPages &&
                    !state.isSpotlightOpen &&
                    !state.isControlCenterOpen &&
                    !state.isHnmSettingsOpen &&
                    state.openFolderModal == null,
                enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
                    slideInVertically(
                        initialOffsetY = { it / 2 },
                        animationSpec = HnmMotion.sheetSpring()
                    ),
                exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
                    slideOutVertically(
                        targetOffsetY = { it / 2 },
                        animationSpec = HnmMotion.sheetSpring()
                    ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                LauncherBottomDockBar(
                    dockApps = state.dockApps,
                    gridSpec = state.gridSpec,
                    currentHomePageIndex = (pagerState.currentPage - 1).coerceAtLeast(0),
                    totalHomePages = homePageCount,
                    isEditMode = state.isEditMode,
                    cachedIconProvider = viewModel::getCachedIcon,
                    loadIconSuspend = viewModel::loadIconIfNeeded,
                    onSearchPillClick = viewModel::openSpotlight,
                    onAppClick = viewModel::launchApp,
                    onAppLongPress = { viewModel.openQuickMenu(it) }
                )
            }

            // Klasör ve App Library Kategori Genişletme Katmanı (3x3'ten 7x7'ye kadar)
            val folderModal = state.openFolderModal
            FolderModalOverlay(
                visible = folderModal != null,
                title = folderModal?.title.orEmpty(),
                isEditableTitle = folderModal?.isEditableTitle ?: false,
                columns = state.gridSpec.folderExpandedColumns,
                showLabels = state.gridSpec.showHomeLabels,
                apps = folderModal?.apps.orEmpty(),
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded,
                onRenameTitle = viewModel::renameOpenFolder,
                onLaunchApp = viewModel::launchApp,
                onAppLongPress = { app ->
                    viewModel.openQuickMenu(app, containingFolderId = folderModal?.folderId)
                },
                onDismiss = viewModel::closeFolderModal
            )

            // Sağ üstten aşağı kaydırma ile açılan iOS Denetim Merkezi (Control Center)
            ControlCenterOverlay(
                visible = state.isControlCenterOpen,
                allApps = state.allApps,
                onOpenHnmSettings = viewModel::openHnmSettings,
                onLaunchApp = viewModel::launchApp,
                onDismiss = viewModel::closeControlCenter
            )

            // Aşağı kaydırma ile açılan Spotlight Hızlı Arama Katmanı
            SpotlightOverlay(
                visible = state.isSpotlightOpen,
                query = state.spotlightQuery,
                suggestedApps = state.suggestedApps,
                matchedApps = state.spotlightMatches,
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded,
                onQueryChange = viewModel::updateSpotlightQuery,
                onLaunchApp = viewModel::launchApp,
                onAppLongPress = { viewModel.openQuickMenu(it) },
                onSearchWeb = { query ->
                    viewModel.closeSpotlight()
                    DefaultLauncherHelper.openWebSearch(context, query)
                },
                onOpenSystemSettings = {
                    viewModel.openHnmSettings()
                },
                onDismiss = viewModel::closeSpotlight
            )

            // One UI'dan tamamen bağımsız Tam Ekran HNM Ayarlar Katmanı
            HnmSettingsOverlay(
                visible = state.isHnmSettingsOpen,
                customization = state.customization,
                isDefaultLauncher = state.isDefaultLauncher,
                targetRefreshRateHz = state.targetRefreshRateHz,
                allApps = state.allApps,
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded,
                onUpdateCustomization = viewModel::updateCustomization,
                onRequestDefaultLauncher = onRequestDefaultLauncher,
                onLaunchApp = viewModel::launchApp,
                onDismiss = viewModel::closeHnmSettings
            )

            // İkona uzun basma (Haptic Touch) Kısayol ve Düzenleme Menüsü
            val activeMenu = state.quickMenu
            val isTargetInDock = remember(activeMenu, state.dockApps) {
                val id = activeMenu?.app?.key?.stableId ?: return@remember false
                state.dockApps.any { it.key.stableId == id }
            }
            val isTargetOnHome = remember(activeMenu, state.homePages, isTargetInDock) {
                val id = activeMenu?.app?.key?.stableId ?: return@remember false
                isTargetInDock ||
                    state.homePages.any { page ->
                        page.items.any { item ->
                            when (item) {
                                is ResolvedHomeItem.App -> item.app.key.stableId == id
                                is ResolvedHomeItem.Folder -> item.apps.any { it.key.stableId == id }
                            }
                        }
                    }
            }

            QuickActionMenuOverlay(
                targetApp = activeMenu?.app,
                shortcuts = activeMenu?.shortcuts.orEmpty(),
                existingFolders = state.existingFolders,
                containingFolderId = activeMenu?.containingFolderId,
                isAlreadyOnHome = isTargetOnHome,
                isInDock = isTargetInDock,
                canAddMoreToDock = state.dockApps.size < state.gridSpec.dockMaxItems,
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded,
                onLaunchShortcut = viewModel::launchShortcut,
                onAddToHome = viewModel::addAppToHomeScreen,
                onRemoveFromHome = { viewModel.removeAppFromHomeScreen(it.key) },
                onToggleDockPin = viewModel::toggleDockPin,
                onCreateNewFolderWithApp = viewModel::createNewFolderWithApp,
                onMoveToFolder = viewModel::moveAppToFolder,
                onExtractFromFolderToHome = viewModel::extractAppFromFolderToHome,
                onEnterEditMode = {
                    if (!state.isEditMode) viewModel.toggleEditMode()
                },
                onAppInfo = { app ->
                    if (app.key.stableId == InstalledAppsRepository.HNM_SETTINGS_KEY.stableId) {
                        viewModel.openHnmSettings()
                    } else {
                        DefaultLauncherHelper.openAppDetails(context, app.key.packageName)
                    }
                },
                onUninstallApp = { app ->
                    if (app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId) {
                        DefaultLauncherHelper.requestUninstall(context, app.key.packageName)
                    }
                },
                onDismiss = viewModel::dismissQuickMenu
            )
        }
    }
}
