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
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.ui.overlays.FolderModalOverlay
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
 * Yatay düzlem: [0: Today View] <-> [1..N: Ana Ekran Sayfaları] <-> [N+1: App Library]
 * Dikey / Katman düzlemi: Buzlu Cam Dock, Spotlight Arama, Klasör Penceresi, Haptic Kısayol Menüsü.
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
    BackHandler(enabled = true) {
        when {
            state.quickMenu != null -> viewModel.dismissQuickMenu()
            state.openFolderModal != null -> viewModel.closeFolderModal()
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

    // Sistem duvar kağıdı üzerinde üst durum çubuğu ve alt Dock okunabilirliğini artıran hafif vinyet
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
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            when (pageIndex) {
                0 -> {
                    TodayViewPage(
                        isDefaultLauncher = state.isDefaultLauncher,
                        isNotificationAccessEnabled = state.isNotificationAccessEnabled,
                        isTabletHardwareDetected = state.gridSpec.isTabletHardwareDetected,
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
                        isEditMode = state.isEditMode,
                        cachedIconProvider = viewModel::getCachedIcon,
                        loadIconSuspend = viewModel::loadIconIfNeeded,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = { viewModel.openQuickMenu(it) },
                        onRemoveAppFromHome = viewModel::removeAppFromHomeScreen,
                        onFolderClick = viewModel::openHomeFolder,
                        onPullDownSpotlight = viewModel::openSpotlight,
                        onToggleEditMode = viewModel::toggleEditMode,
                        onPickWallpaper = {
                            DefaultLauncherHelper.openWallpaperPicker(context)
                        }
                    )
                }
            }
        }

        // Ana ekran sayfalarında sabitlenen Buzlu Cam Dock + "Ara" hapı
        // Today View veya App Library sayfasına geçildiğinde iOS gibi yumuşakça gizlenir.
        AnimatedVisibility(
            visible = isOnHomeScreenPages &&
                !state.isSpotlightOpen &&
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

        // Klasör ve App Library Kategori Genişletme Katmanı
        val folderModal = state.openFolderModal
        FolderModalOverlay(
            visible = folderModal != null,
            title = folderModal?.title.orEmpty(),
            isEditableTitle = folderModal?.isEditableTitle ?: false,
            columns = state.gridSpec.folderExpandedColumns,
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
                viewModel.closeSpotlight()
                DefaultLauncherHelper.openSystemSettings(context)
            },
            onDismiss = viewModel::closeSpotlight
        )

        // İkona uzun basma (Haptic Touch) Kısayol ve Düzenleme Menüsü
        val activeMenu = state.quickMenu
        val isTargetOnHome = remember(activeMenu, state.homePages, state.dockApps) {
            val id = activeMenu?.app?.key?.stableId ?: return@remember false
            state.dockApps.any { it.key.stableId == id } ||
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
            cachedIconProvider = viewModel::getCachedIcon,
            loadIconSuspend = viewModel::loadIconIfNeeded,
            onLaunchShortcut = viewModel::launchShortcut,
            onAddToHome = viewModel::addAppToHomeScreen,
            onRemoveFromHome = { viewModel.removeAppFromHomeScreen(it.key) },
            onCreateNewFolderWithApp = viewModel::createNewFolderWithApp,
            onMoveToFolder = viewModel::moveAppToFolder,
            onExtractFromFolderToHome = viewModel::extractAppFromFolderToHome,
            onEnterEditMode = {
                if (!state.isEditMode) viewModel.toggleEditMode()
            },
            onAppInfo = { DefaultLauncherHelper.openAppDetails(context, it.key.packageName) },
            onUninstallApp = { DefaultLauncherHelper.requestUninstall(context, it.key.packageName) },
            onDismiss = viewModel::dismissQuickMenu
        )
    }
}
