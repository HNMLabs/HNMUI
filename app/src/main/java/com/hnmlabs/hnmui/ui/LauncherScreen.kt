package com.hnmlabs.hnmui.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.data.InstalledAppsRepository
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.model.ResolvedHomePage
import com.hnmlabs.hnmui.ui.components.HnmIosStatusBar
import com.hnmlabs.hnmui.ui.components.LocalEditJigglePhase
import com.hnmlabs.hnmui.ui.components.LocalGlassOpacityFactor
import com.hnmlabs.hnmui.ui.components.LocalHnmTopBarHeight
import com.hnmlabs.hnmui.ui.drag.DraggingGhostOverlay
import com.hnmlabs.hnmui.ui.drag.HoverDropTarget
import com.hnmlabs.hnmui.ui.drag.rememberLauncherDragDropController
import com.hnmlabs.hnmui.ui.overlays.ControlCenterOverlay
import com.hnmlabs.hnmui.ui.overlays.FolderModalOverlay
import com.hnmlabs.hnmui.ui.overlays.HnmSettingsOverlay
import com.hnmlabs.hnmui.ui.overlays.NotificationCenterOverlay
import com.hnmlabs.hnmui.ui.overlays.QuickActionMenuOverlay
import com.hnmlabs.hnmui.ui.overlays.SpotlightOverlay
import com.hnmlabs.hnmui.ui.surfaces.AppLibraryPage
import com.hnmlabs.hnmui.ui.surfaces.HomeGridPage
import com.hnmlabs.hnmui.ui.surfaces.LauncherBottomDockBar
import com.hnmlabs.hnmui.ui.surfaces.TodayViewPage
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.widget.LauncherWidgetHostManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs

private val RootScrimBrush = Brush.verticalGradient(
    colorStops = arrayOf(
        0.0f to Color(0x3D070B14),
        0.22f to Color(0x12070B14),
        0.78f to Color(0x16070B14),
        1.0f to Color(0x4D070B14)
    )
)

/**
 * HNMUI kök uzamsal ekranı.
 * Yatay düzlem: [0: Today View] <-> [1..N: Ana Ekran Sayfaları (4x4..7x7)] <-> [N+1: App Library]
 * Üst çekme bölgeleri: Sağ Üst -> Denetim Merkezi | Sol Üst -> HNM Bildirim Merkezi | Orta -> Spotlight
 * Katman düzlemi: iOS Durum Çubuğu, 120 FPS Sürükle-Bırak (Izgara + Klasör + Dock), HNM Ayarlar.
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
    val density = LocalDensity.current
    val rawHaptics = LocalHapticFeedback.current
    val haptics = remember(rawHaptics, state.customization.hapticFeedbackEnabled) {
        object : HapticFeedback {
            override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
                if (state.customization.hapticFeedbackEnabled) {
                    rawHaptics.performHapticFeedback(hapticFeedbackType)
                }
            }
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val dragController = rememberLauncherDragDropController()

    val homePageCount = state.homePages.size.coerceAtLeast(1)
    val totalPages = 1 + homePageCount + 1
    val primaryHomeIndex = 1
    val appLibraryIndex = totalPages - 1

    val pagerState = rememberPagerState(
        initialPage = primaryHomeIndex,
        pageCount = { totalPages }
    )

    LaunchedEffect(homePageCount) {
        dragController.prunePageBounds(homePageCount)
    }

    // Fiziksel/jest Home tuşu sinyali geldiğinde tüm panelleri kapatıp ana sayfaya yay animasyonuyla dön
    LaunchedEffect(viewModel) {
        viewModel.scrollToPrimaryHomeSignal.collect {
            dragController.cancelDrag()
            pagerState.animateScrollToPage(
                page = primaryHomeIndex,
                animationSpec = HnmMotion.sheetSpring()
            )
        }
    }

    // Geri tuşu öncelik zinciri (Home Launcher asla ana ekrandan kapanmamalıdır)
    BackHandler(enabled = !state.isHnmSettingsOpen) {
        when {
            dragController.isDragging -> dragController.cancelDrag()
            state.quickMenu != null -> viewModel.dismissQuickMenu()
            state.openFolderModal != null -> viewModel.closeFolderModal()
            state.isControlCenterOpen -> viewModel.closeControlCenter()
            state.isNotificationCenterOpen -> viewModel.closeNotificationCenter()
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

    val isOnHomeScreenPages by remember(homePageCount) {
        derivedStateOf { pagerState.currentPage in 1..homePageCount }
    }
    val currentHomePageZeroBased by remember(homePageCount) {
        derivedStateOf {
            (pagerState.currentPage - 1).coerceIn(0, (homePageCount - 1).coerceAtLeast(0))
        }
    }

    var rootWidthPx by remember { mutableFloatStateOf(1080f) }
    var rootHeightPx by remember { mutableFloatStateOf(2340f) }

    // İkon sürüklenirken ekranın sol/sağ kenarında bekletildiğinde yan ana ekran sayfasına yumuşak geçiş
    val edgeZonePx = remember(density) { with(density) { 36.dp.toPx() } }
    val dragEdgeDirection by remember {
        derivedStateOf {
            if (!dragController.isDragging) {
                0
            } else {
                val x = dragController.pointerInRoot.x
                when {
                    x > 1f && x < edgeZonePx -> -1
                    x > rootWidthPx - edgeZonePx -> 1
                    else -> 0
                }
            }
        }
    }

    LaunchedEffect(dragController.isDragging, dragEdgeDirection, currentHomePageZeroBased, homePageCount) {
        if (dragController.isDragging && dragEdgeDirection != 0) {
            delay(480L)
            val targetPagerPage = (pagerState.currentPage + dragEdgeDirection)
                .coerceIn(1, homePageCount)
            if (targetPagerPage != pagerState.currentPage) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                pagerState.animateScrollToPage(
                    page = targetPagerPage,
                    animationSpec = HnmMotion.sheetSpring()
                )
                dragController.updateDragPosition(
                    rootPos = dragController.pointerInRoot,
                    currentHomePageIndex = (targetPagerPage - 1).coerceAtLeast(0),
                    homePages = state.homePages,
                    dockAppsCount = state.dockApps.size,
                    gridSpec = state.gridSpec
                )
            }
        }
    }

    val anyOverlayOpen = state.isControlCenterOpen ||
        state.isNotificationCenterOpen ||
        state.isSpotlightOpen ||
        state.isHnmSettingsOpen ||
        state.openFolderModal != null ||
        state.quickMenu != null

    // Minimalist iOS derinlik çekilmesi: Değerler yalnızca graphicsLayer (GPU draw) aşamasında okunur!
    val backgroundRecedeScaleState = animateFloatAsState(
        targetValue = if (anyOverlayOpen) 0.945f else 1f,
        animationSpec = HnmMotion.recedeSpring(),
        label = "bgRecedeScale"
    )
    val backgroundRecedeAlphaState = animateFloatAsState(
        targetValue = if (anyOverlayOpen) 0.62f else 1f,
        animationSpec = HnmMotion.recedeSpring(),
        label = "bgRecedeAlpha"
    )

    // Düzenleme (Jiggle) modunda tüm ekran için TEK bir paylaşımlı sinüs faz saati çalıştır
    val sharedJigglePhaseState: State<Float>? = if (state.isEditMode) {
        val jiggleTransition = rememberInfiniteTransition(label = "sharedEditJiggleClock")
        jiggleTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 256, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "sharedEditJigglePhase"
        )
    } else {
        null
    }

    val topStripPx = remember(density) { with(density) { 62.dp.toPx() } }
    val topPullThresholdPx = remember(density) { with(density) { 24.dp.toPx() } }
    val homeTopZoneThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }
    val spotlightThresholdPx = remember(density) { with(density) { 36.dp.toPx() } }

    val latestAnyOverlayOpen by rememberUpdatedState(anyOverlayOpen)
    val latestIsOnHomePages by rememberUpdatedState(isOnHomeScreenPages)
    val latestIsEditMode by rememberUpdatedState(state.isEditMode)

    val latestHomePages by rememberUpdatedState(state.homePages)
    val latestDockSize by rememberUpdatedState(state.dockApps.size)
    val latestGridSpec by rememberUpdatedState(state.gridSpec)
    val latestHomePageIdx by rememberUpdatedState(currentHomePageZeroBased)

    val handleDragMoved: (Offset) -> Unit = remember(dragController, haptics) {
        { rootPos ->
            val prevTarget = dragController.hoveredTarget
            val nextTarget = dragController.updateDragPosition(
                rootPos = rootPos,
                currentHomePageIndex = latestHomePageIdx,
                homePages = latestHomePages,
                dockAppsCount = latestDockSize,
                gridSpec = latestGridSpec
            )
            if (nextTarget != null && nextTarget != prevTarget) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val onOpenQuickMenuSimple: (AppItem) -> Unit = remember(viewModel) {
        { app -> viewModel.openQuickMenu(app) }
    }
    val onOpenNotificationAccess = remember(context) {
        { DefaultLauncherHelper.openNotificationListenerSettings(context) }
    }
    val onPickWallpaper = remember(context) {
        { DefaultLauncherHelper.openWallpaperPicker(context) }
    }

    CompositionLocalProvider(
        LocalHapticFeedback provides haptics,
        LocalGlassOpacityFactor provides state.gridSpec.glassOpacityFactor,
        LocalHnmTopBarHeight provides if (state.customization.useIosStatusBar) 38.dp else 0.dp,
        LocalEditJigglePhase provides sharedJigglePhaseState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    rootWidthPx = it.width.toFloat().coerceAtLeast(1f)
                    rootHeightPx = it.height.toFloat().coerceAtLeast(1f)
                }
                .background(RootScrimBrush)
                // Kök Initial-pass aşağı çekme yakalayıcısı: HorizontalPager ve ikonlardan ÖNCE çalışır!
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        // KRİTİK: awaitFirstDown her döngüde İLK satırda çağrılmalı ki parmak basılana kadar
                        // coroutine askıda (suspend) kalsın; aksi halde panel açıkken Main Thread sonsuz döngüye girer!
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        if (latestAnyOverlayOpen || dragController.isDragging) {
                            return@awaitEachGesture
                        }

                        val startX = down.position.x
                        val startY = down.position.y
                        var aborted = false

                        while (!aborted) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            if (latestAnyOverlayOpen || dragController.isDragging) {
                                aborted = true
                                break
                            }

                            val dx = change.position.x - startX
                            val dy = change.position.y - startY

                            if (dy < -touchSlop) {
                                aborted = true
                                break
                            }

                            // 1) En üst iOS durum çubuğu şeridi (tüm sayfalarda çalışır)
                            if (startY <= topStripPx) {
                                if (abs(dx) > touchSlop * 1.6f && abs(dx) > abs(dy) * 1.4f) {
                                    aborted = true
                                    break
                                }
                                if (dy >= topPullThresholdPx && dy > abs(dx) * 0.50f) {
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (startX >= rootWidthPx * 0.50f) {
                                        viewModel.openControlCenter()
                                    } else {
                                        viewModel.openNotificationCenter()
                                    }
                                    break
                                }
                                continue
                            }

                            // 2) Ana ekran sayfalarında üst-sağ / üst-sol ve orta Spotlight çekme bölgeleri
                            if (!latestIsOnHomePages || latestIsEditMode) {
                                aborted = true
                                break
                            }

                            if (abs(dx) > touchSlop * 1.35f && abs(dx) > abs(dy) * 1.25f) {
                                aborted = true
                                break
                            }

                            val isUpperHalf = startY <= rootHeightPx * 0.44f
                            if (isUpperHalf) {
                                if (dy >= homeTopZoneThresholdPx && dy > abs(dx) * 0.55f) {
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (startX >= rootWidthPx * 0.50f) {
                                        viewModel.openControlCenter()
                                    } else {
                                        viewModel.openNotificationCenter()
                                    }
                                    break
                                }
                            } else if (startY < rootHeightPx * 0.84f) {
                                if (dy >= spotlightThresholdPx && dy > abs(dx) * 1.05f) {
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openSpotlight()
                                    break
                                }
                            }
                        }
                    }
                }
        ) {
            // Arka plan katmanı: iOS Durum Çubuğu + Sayfalar + Dock (derinlik animasyonlu)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val s = backgroundRecedeScaleState.value
                        scaleX = s
                        scaleY = s
                        alpha = backgroundRecedeAlphaState.value
                    }
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = !anyOverlayOpen && !dragController.isDragging,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    // KRİTİK 120 FPS: currentPageOffsetFraction YALNIZCA graphicsLayer içinde okunur;
                    // sayfa kaydırılırken hiçbir alt ekran yeniden derlenmez (0 recomposition)!
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                val rawOffset = (pagerState.currentPage - pageIndex) +
                                    pagerState.currentPageOffsetFraction
                                val clampedOffset = abs(rawOffset).coerceIn(0f, 1f)
                                val pageScale = lerp(0.968f, 1f, 1f - clampedOffset)
                                scaleX = pageScale
                                scaleY = pageScale
                                alpha = lerp(0.84f, 1f, 1f - clampedOffset)
                            }
                    ) {
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
                                    onRequestNotificationAccess = onOpenNotificationAccess,
                                    onLaunchApp = viewModel::launchApp,
                                    onAppLongPress = onOpenQuickMenuSimple,
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
                                    onAppLongPress = onOpenQuickMenuSimple,
                                    onExpandCategory = viewModel::openAppLibraryCategory
                                )
                            }

                            else -> {
                                val safeLastIdx = (state.homePages.size - 1).coerceAtLeast(0)
                                val homeIdx = (pageIndex - 1).coerceIn(0, safeLastIdx)
                                val homePage = state.homePages.getOrNull(homeIdx)
                                    ?: ResolvedHomePage(id = "page_empty", items = emptyList())
                                HomeGridPage(
                                    homePageIndex = homeIdx,
                                    page = homePage,
                                    gridSpec = state.gridSpec,
                                    isDefaultLauncher = state.isDefaultLauncher,
                                    isEditMode = state.isEditMode,
                                    dragController = dragController,
                                    cachedIconProvider = viewModel::getCachedIcon,
                                    loadIconSuspend = viewModel::loadIconIfNeeded,
                                    onRequestDefaultLauncher = onRequestDefaultLauncher,
                                    onAppClick = viewModel::launchApp,
                                    onAppLongPress = onOpenQuickMenuSimple,
                                    onDismissQuickMenuAndEnterEdit = viewModel::enterEditModeForDrag,
                                    onDragMoved = handleDragMoved,
                                    onDragEnded = viewModel::commitDragDrop,
                                    onRemoveAppFromHome = viewModel::removeAppFromHomeScreen,
                                    onFolderClick = viewModel::openHomeFolder,
                                    onOpenHnmSettings = viewModel::openHnmSettings,
                                    onToggleEditMode = viewModel::toggleEditMode,
                                    onPickWallpaper = onPickWallpaper
                                )
                            }
                        }
                    }
                }

                // Ana ekran sayfalarında sabitlenen Buzlu Cam Dock + "Ara" hapı
                AnimatedVisibility(
                    visible = isOnHomeScreenPages &&
                        !state.isSpotlightOpen &&
                        !state.isControlCenterOpen &&
                        !state.isNotificationCenterOpen &&
                        !state.isHnmSettingsOpen &&
                        state.openFolderModal == null,
                    enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
                        slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = HnmMotion.sheetOffsetSpring()
                        ),
                    exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
                        slideOutVertically(
                            targetOffsetY = { it / 2 },
                            animationSpec = HnmMotion.sheetOffsetSpring()
                        ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    LauncherBottomDockBar(
                        dockApps = state.dockApps,
                        gridSpec = state.gridSpec,
                        currentHomePageIndex = currentHomePageZeroBased,
                        totalHomePages = homePageCount,
                        isEditMode = state.isEditMode,
                        dragController = dragController,
                        cachedIconProvider = viewModel::getCachedIcon,
                        loadIconSuspend = viewModel::loadIconIfNeeded,
                        onSearchPillClick = viewModel::openSpotlight,
                        onAppClick = viewModel::launchApp,
                        onAppLongPress = onOpenQuickMenuSimple,
                        onDismissQuickMenuAndEnterEdit = viewModel::enterEditModeForDrag,
                        onDragMoved = handleDragMoved,
                        onDragEnded = viewModel::commitDragDrop
                    )
                }

                // One UI sistem durum çubuğu yerine en üstte duran sade iOS Durum Çubuğu
                HnmIosStatusBar(
                    visible = state.customization.useIosStatusBar && !state.isHnmSettingsOpen,
                    onTapLeftNotifications = viewModel::openNotificationCenter,
                    onTapRightControlCenter = viewModel::openControlCenter,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }

            // Parmak altında 120 FPS süzülen sürükleme hayalet ikonu
            DraggingGhostOverlay(
                controller = dragController,
                iconSize = if (dragController.hoveredTarget is HoverDropTarget.DockCell) {
                    state.gridSpec.dockIconSizeDp.dp
                } else {
                    state.gridSpec.iconSizeDp.dp
                },
                folderPreviewColumns = state.gridSpec.folderPreviewColumns,
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded
            )

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
                onOpenNotificationCenter = viewModel::openNotificationCenter,
                onOpenHnmSettings = viewModel::openHnmSettings,
                onLaunchApp = viewModel::launchApp,
                onDismiss = viewModel::closeControlCenter
            )

            // Sol üstten aşağı kaydırma ile açılan iOS Bildirim & Özet Merkezi (One UI yerine!)
            NotificationCenterOverlay(
                visible = state.isNotificationCenterOpen,
                liveNotifications = state.liveNotifications,
                quickNotes = state.quickNotes,
                suggestedApps = state.suggestedApps,
                allApps = state.allApps,
                cachedIconProvider = viewModel::getCachedIcon,
                loadIconSuspend = viewModel::loadIconIfNeeded,
                onAddQuickNote = viewModel::addQuickNote,
                onToggleQuickNote = viewModel::toggleQuickNote,
                onDeleteQuickNote = viewModel::deleteQuickNote,
                onDismissNotificationCard = viewModel::dismissNotificationCard,
                onClearAllNotificationCards = viewModel::clearAllNotificationCards,
                onLaunchApp = viewModel::launchApp,
                onOpenControlCenter = viewModel::openControlCenter,
                onOpenSystemShadeFallback = {
                    DeviceSystemController.expandNotificationShade(context)
                },
                onDismiss = viewModel::closeNotificationCenter
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
