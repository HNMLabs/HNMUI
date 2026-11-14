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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import com.hnmlabs.hnmui.widget.LauncherWidgetHostManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max

/**
 * Kök aşağı çekme hareketinin tetikleyeceği hedef katman.
 */
internal enum class DownwardPullAction {
    NONE,
    ABORT,
    OPEN_CONTROL_CENTER,
    OPEN_NOTIFICATION_CENTER,
    OPEN_SPOTLIGHT
}

/**
 * Tek elle sağ başparmak yaylarını (diyagonal sol-aşağı kavis), kamera delikli (cutout) uzun
 * ekranları ve tüm sayfalardaki üst durum çubuğu çekişlerini kaçırmadan sınıflandıran saf karar motoru.
 */
internal fun classifyDownwardPull(
    startX: Float,
    startY: Float,
    dx: Float,
    dy: Float,
    rootWidthPx: Float,
    rootHeightPx: Float,
    effectiveTopStripPx: Float,
    topPullThresholdPx: Float,
    homePullThresholdPx: Float,
    spotlightThresholdPx: Float,
    touchSlopPx: Float,
    isOnHomePages: Boolean,
    isEditMode: Boolean
): DownwardPullAction {
    val absDx = abs(dx)
    val absDy = abs(dy)

    // Yukarı doğru belirgin sürükleme aşağı çekme hareketini iptal eder
    if (dy < -touchSlopPx) {
        return DownwardPullAction.ABORT
    }

    // 1) Üst Durum Çubuğu & Kamera Çentiği Şeridi (Today View, Ana Ekran, App Library ve Düzenleme dahil TÜM sayfalarda aktif!)
    if (startY <= effectiveTopStripPx) {
        if (absDx > touchSlopPx * 2.8f && absDx > absDy * 2.0f) {
            return DownwardPullAction.ABORT
        }
        if (dy >= topPullThresholdPx && dy > absDx * 0.38f) {
            return if (startX >= rootWidthPx * 0.48f) {
                DownwardPullAction.OPEN_CONTROL_CENTER
            } else {
                DownwardPullAction.OPEN_NOTIFICATION_CENTER
            }
        }
        return DownwardPullAction.NONE
    }

    // 2) Ana ekran sayfalarında ergonomik tek-el Denetim Merkezi, Bildirim Merkezi ve Spotlight bölgeleri
    if (!isOnHomePages || isEditMode) {
        return DownwardPullAction.ABORT
    }

    // Yalnızca açıkça yatay sayfa kaydırma baskınsa HorizontalPager'a devret (sağ başparmak yayını erken kesme!)
    if (absDx > touchSlopPx * 2.6f && absDx > absDy * 1.85f) {
        return DownwardPullAction.ABORT
    }

    // Sağ yarı üst & orta-sağ başparmak bölgesi -> Denetim Merkezi (%100 tutarlı açılış)
    val inRightControlZone = (startY <= rootHeightPx * 0.48f && startX >= rootWidthPx * 0.48f) ||
        (startY <= rootHeightPx * 0.68f && startX >= rootWidthPx * 0.58f)
    if (inRightControlZone) {
        if (dy >= homePullThresholdPx && dy > absDx * 0.42f) {
            return DownwardPullAction.OPEN_CONTROL_CENTER
        }
        return DownwardPullAction.NONE
    }

    // Sol yarı üst & orta-sol başparmak bölgesi -> Bildirim Merkezi
    val inLeftNotificationZone = (startY <= rootHeightPx * 0.48f && startX < rootWidthPx * 0.48f) ||
        (startY <= rootHeightPx * 0.60f && startX <= rootWidthPx * 0.36f)
    if (inLeftNotificationZone) {
        if (dy >= homePullThresholdPx && dy > absDx * 0.42f) {
            return DownwardPullAction.OPEN_NOTIFICATION_CENTER
        }
        return DownwardPullAction.NONE
    }

    // Merkez / alt-orta bölge -> Spotlight Arama
    if (startY < rootHeightPx * 0.84f) {
        if (dy >= spotlightThresholdPx && dy > absDx * 0.65f) {
            return DownwardPullAction.OPEN_SPOTLIGHT
        }
        return DownwardPullAction.NONE
    }

    return DownwardPullAction.ABORT
}

/**
 * HNMUI kök uzamsal ekranı.
 * Yatay düzlem: [0: Today View] <-> [1..N: Ana Ekran Sayfaları (4x4..7x7)] <-> [N+1: App Library]
 * Üst çekme bölgeleri: Sağ Üst / Sağ Yarı -> Denetim Merkezi | Sol Üst / Sol Yarı -> HNM Bildirim Merkezi | Orta -> Spotlight
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
    val colors = HnmTheme.colors
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

    val rootScrimBrush = remember(
        colors.wallpaperScrimTop,
        colors.wallpaperScrimMid,
        colors.wallpaperScrimBottom
    ) {
        Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to colors.wallpaperScrimTop,
                0.24f to colors.wallpaperScrimMid,
                0.76f to colors.wallpaperScrimMid,
                1.0f to colors.wallpaperScrimBottom
            )
        )
    }

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
    // KRİTİK: Kullanıcı kaydırma sürerken ekrana dokunursa Pager MutationInterruptedException (CancellationException)
    // fırlatır; runCatching + ensureActive sayesinde Home tuşu dinleyicisi (collect) asla kalıcı olarak ölmez!
    LaunchedEffect(viewModel) {
        viewModel.scrollToPrimaryHomeSignal.collect {
            dragController.cancelDrag()
            runCatching {
                pagerState.animateScrollToPage(
                    page = primaryHomeIndex,
                    animationSpec = HnmMotion.sheetSpring()
                )
            }
            ensureActive()
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
                    runCatching {
                        pagerState.animateScrollToPage(
                            page = primaryHomeIndex,
                            animationSpec = HnmMotion.sheetSpring()
                        )
                    }
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
                runCatching {
                    pagerState.animateScrollToPage(
                        page = targetPagerPage,
                        animationSpec = HnmMotion.sheetSpring()
                    )
                }
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

    val cutoutTopDp = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val reservedTopBarDp = if (state.customization.useIosStatusBar) {
        androidx.compose.ui.unit.max(cutoutTopDp + 4.dp, 42.dp)
    } else {
        0.dp
    }
    val effectiveTopStripPx = remember(density, cutoutTopDp) {
        with(density) { max((cutoutTopDp + 52.dp).toPx(), 86.dp.toPx()) }
    }
    // KRİTİK: Üst çubuk çekme eşiği touchSlop (~8dp) ile hizalı tutulur ki hızlı tıklama ile kısa
    // aşağı kaydırma (flick) arasında hiçbir "ölü bölge" (dead-zone) kalmasın!
    val topPullThresholdPx = remember(density) { with(density) { 8.dp.toPx() } }
    val homeTopZoneThresholdPx = remember(density) { with(density) { 18.dp.toPx() } }
    val spotlightThresholdPx = remember(density) { with(density) { 28.dp.toPx() } }

    val latestTopStripPx by rememberUpdatedState(effectiveTopStripPx)
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
        LocalHnmTopBarHeight provides reservedTopBarDp,
        LocalEditJigglePhase provides sharedJigglePhaseState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    rootWidthPx = it.width.toFloat().coerceAtLeast(1f)
                    rootHeightPx = it.height.toFloat().coerceAtLeast(1f)
                }
                .background(rootScrimBrush)
                // Kök Initial-pass aşağı çekme yakalayıcısı: HorizontalPager ve ikonlardan ÖNCE çalışır!
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    val effectiveTopThreshold = kotlin.math.min(topPullThresholdPx, touchSlop * 0.9f)
                        .coerceAtLeast(1f)
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
                        var triggered = false

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break

                            if (triggered) {
                                // Katman açıldıktan sonra parmak kalkana kadar kalan olayları tüket;
                                // böylece alt ikon tıklaması veya yeni açılan panelin kapanması asla tetiklenmez!
                                event.changes.forEach { it.consume() }
                                continue
                            }

                            if (latestAnyOverlayOpen || dragController.isDragging) {
                                break
                            }

                            val action = classifyDownwardPull(
                                startX = startX,
                                startY = startY,
                                dx = change.position.x - startX,
                                dy = change.position.y - startY,
                                rootWidthPx = rootWidthPx,
                                rootHeightPx = rootHeightPx,
                                effectiveTopStripPx = latestTopStripPx,
                                topPullThresholdPx = effectiveTopThreshold,
                                homePullThresholdPx = homeTopZoneThresholdPx,
                                spotlightThresholdPx = spotlightThresholdPx,
                                touchSlopPx = touchSlop,
                                isOnHomePages = latestIsOnHomePages,
                                isEditMode = latestIsEditMode
                            )

                            when (action) {
                                DownwardPullAction.NONE -> continue
                                DownwardPullAction.ABORT -> break
                                DownwardPullAction.OPEN_CONTROL_CENTER -> {
                                    triggered = true
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openControlCenter()
                                }
                                DownwardPullAction.OPEN_NOTIFICATION_CENTER -> {
                                    triggered = true
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openNotificationCenter()
                                }
                                DownwardPullAction.OPEN_SPOTLIGHT -> {
                                    triggered = true
                                    event.changes.forEach { it.consume() }
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.openSpotlight()
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
                themeMode = state.customization.themeMode,
                allApps = state.allApps,
                onSetThemeMode = viewModel::setThemeMode,
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
