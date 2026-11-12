package com.hnmlabs.hnmui.ui.drag

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.core.formfactor.LauncherGridSpec
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.model.ResolvedHomePage
import com.hnmlabs.hnmui.ui.components.AppSquircleArtwork
import com.hnmlabs.hnmui.ui.components.FolderIconTile
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import kotlin.math.hypot

/**
 * Sürükleme işleminin başladığı kaynak yuva (Ana ekran ızgarası veya Alt Dock).
 */
sealed interface DragOrigin {
    val draggedItemId: String

    data class HomeSlot(
        val pageIndex: Int,
        val slotIndex: Int,
        val item: ResolvedHomeItem
    ) : DragOrigin {
        override val draggedItemId: String get() = item.itemId
    }

    data class DockSlot(
        val dockIndex: Int,
        val app: AppItem
    ) : DragOrigin {
        override val draggedItemId: String get() = "dock:${app.key.stableId}"
    }
}

/**
 * Parmak hareket ederken hesaplanan canlı bırakma (drop) hedefi.
 */
sealed interface HoverDropTarget {
    data class HomeCell(val pageIndex: Int, val slotIndex: Int) : HoverDropTarget
    data class MergeIntoFolder(
        val folderId: String,
        val pageIndex: Int,
        val slotIndex: Int
    ) : HoverDropTarget
    data class DockCell(val insertIndex: Int) : HoverDropTarget
}

/**
 * 120 FPS iOS SpringBoard tarzı sürükle-bırak durum yöneticisi.
 * Ana ekran ızgarası içinde sıralama, sayfalar arası kenar geçişi, klasöre bırakma,
 * Dock'a uygulama sürükleme ve Dock'tan uygulamayı ana ekrana çıkarma akışlarını yönetir.
 */
@Stable
class LauncherDragDropController {
    var activeDrag by mutableStateOf<DragOrigin?>(null)
        private set

    var pointerInRoot by mutableStateOf(Offset.Zero)
        private set

    var hoveredTarget by mutableStateOf<HoverDropTarget?>(null)
        private set

    var dockBoundsInRoot by mutableStateOf<Rect?>(null)
        private set

    val gridBoundsByPage = mutableStateMapOf<Int, Rect>()

    val isDragging: Boolean
        get() = activeDrag != null

    fun updateGridBounds(homePageIndex: Int, rect: Rect) {
        gridBoundsByPage[homePageIndex] = rect
    }

    fun updateDockBounds(rect: Rect?) {
        dockBoundsInRoot = rect
    }

    fun startDrag(origin: DragOrigin, initialRootPos: Offset) {
        activeDrag = origin
        pointerInRoot = initialRootPos
        hoveredTarget = null
    }

    fun updateDragPosition(
        rootPos: Offset,
        currentHomePageIndex: Int,
        homePages: List<ResolvedHomePage>,
        dockAppsCount: Int,
        gridSpec: LauncherGridSpec
    ): HoverDropTarget? {
        val origin = activeDrag ?: return null
        pointerInRoot = rootPos

        val draggingAppOnly = when (origin) {
            is DragOrigin.DockSlot -> true
            is DragOrigin.HomeSlot -> origin.item is ResolvedHomeItem.App
        }

        // 1) Alt Dock bırakma bölgesi kontrolü (klasörler Dock'a girmez, uygulamalar girer/çıkar)
        val dockRect = dockBoundsInRoot
        if (gridSpec.dockVisible && draggingAppOnly && dockRect != null) {
            val expandedDockZone = Rect(
                left = dockRect.left - 12f,
                top = dockRect.top - 18f,
                right = dockRect.right + 12f,
                bottom = dockRect.bottom + 24f
            )
            if (expandedDockZone.contains(rootPos)) {
                val effectiveSlots = when {
                    dockAppsCount <= 0 -> 1
                    origin is DragOrigin.DockSlot -> dockAppsCount
                    dockAppsCount < gridSpec.dockMaxItems -> dockAppsCount + 1
                    else -> dockAppsCount
                }
                val relX = ((rootPos.x - dockRect.left) / dockRect.width.coerceAtLeast(1f))
                    .coerceIn(0f, 0.999f)
                val insertIdx = (relX * effectiveSlots).toInt().coerceIn(0, effectiveSlots - 1)
                val target = HoverDropTarget.DockCell(insertIdx)
                hoveredTarget = target
                return target
            }
        }

        // 2) Ana ekran ızgara hücresi veya mevcut klasör içi kontrolü
        val safePageIdx = currentHomePageIndex.coerceIn(0, (homePages.size - 1).coerceAtLeast(0))
        val gridRect = gridBoundsByPage[safePageIdx]
        if (gridRect != null && gridRect.width > 1f && gridRect.height > 1f) {
            val cols = gridSpec.columns.coerceAtLeast(1)
            val rows = gridSpec.rows.coerceAtLeast(1)
            val cellW = gridRect.width / cols
            val cellH = gridRect.height / rows

            val col = ((rootPos.x - gridRect.left) / cellW).toInt().coerceIn(0, cols - 1)
            val row = ((rootPos.y - gridRect.top) / cellH).toInt().coerceIn(0, rows - 1)
            val slotIdx = row * cols + col

            val cellCenterX = gridRect.left + (col + 0.5f) * cellW
            val cellCenterY = gridRect.top + (row + 0.5f) * cellH
            val distFromCellCenter = hypot(rootPos.x - cellCenterX, rootPos.y - cellCenterY)

            val existingItem = homePages.getOrNull(safePageIdx)?.items?.getOrNull(slotIdx)
            if (draggingAppOnly &&
                existingItem is ResolvedHomeItem.Folder &&
                distFromCellCenter < minOf(cellW, cellH) * 0.34f
            ) {
                val mergeTarget = HoverDropTarget.MergeIntoFolder(
                    folderId = existingItem.id,
                    pageIndex = safePageIdx,
                    slotIndex = slotIdx
                )
                hoveredTarget = mergeTarget
                return mergeTarget
            }

            val cellTarget = HoverDropTarget.HomeCell(
                pageIndex = safePageIdx,
                slotIndex = slotIdx
            )
            hoveredTarget = cellTarget
            return cellTarget
        }

        hoveredTarget = null
        return null
    }

    fun finishDrag(): Pair<DragOrigin, HoverDropTarget?>? {
        val origin = activeDrag ?: return null
        val target = hoveredTarget
        activeDrag = null
        hoveredTarget = null
        return origin to target
    }

    fun cancelDrag() {
        activeDrag = null
        hoveredTarget = null
    }
}

@Composable
fun rememberLauncherDragDropController(): LauncherDragDropController {
    return remember { LauncherDragDropController() }
}

/**
 * İkon ve klasör kutularına iOS SpringBoard dokunma + uzun basma menüsü + kesintisiz sürükleme
 * davranışı kazandıran birleşik jest değiştiricisi:
 * - Kısa dokunuş -> Uygulamayı / klasörü açar.
 * - Uzun basıp parmağı sabit bırakma -> Haptic Kısayol Menüsü açılır.
 * - Uzun basıp parmağı hareket ettirme (veya Düzenleme Modu'nda basılı tutup çekme) ->
 *   Menü kapanır, Düzenleme Modu açılır ve ikon 120 FPS parmak altında sürüklenir!
 */
fun Modifier.launcherDraggableItem(
    keyId: String,
    originProvider: () -> DragOrigin,
    controller: LauncherDragDropController,
    isEditMode: Boolean,
    onTap: () -> Unit,
    onOpenQuickMenu: () -> Unit,
    onDismissQuickMenuAndEnterEdit: () -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: (DragOrigin, HoverDropTarget?) -> Unit
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val moveToDragThresholdPx = remember(density) { with(density) { 14.dp.toPx() } }

    var itemCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val currentOriginProvider by rememberUpdatedState(originProvider)
    val currentIsEditMode by rememberUpdatedState(isEditMode)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnOpenQuickMenu by rememberUpdatedState(onOpenQuickMenu)
    val currentOnDismissAndEnterEdit by rememberUpdatedState(onDismissQuickMenuAndEnterEdit)
    val currentOnDragMoved by rememberUpdatedState(onDragMoved)
    val currentOnDragEnded by rememberUpdatedState(onDragEnded)

    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = HnmMotion.iconPressSpring(),
        label = "draggablePressScale"
    )

    val isThisItemActivelyDragged = controller.activeDrag?.draggedItemId == keyId

    this
        .onGloballyPositioned { coords -> itemCoords = coords }
        .graphicsLayer {
            scaleX = pressScale
            scaleY = pressScale
            alpha = if (isThisItemActivelyDragged) 0f else 1f
        }
        .pointerInput(keyId) {
            val touchSlop = viewConfiguration.touchSlop
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var draggingStarted = false
                try {
                    isPressed = true
                    val startLocal = down.position
                    val holdTimeoutMs = if (currentIsEditMode) 185L else 360L

                    var longPressed = false
                    var movedBeforeLongPress = false
                    var endedEarly = false

                    try {
                        withTimeout(holdTimeoutMs) {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) {
                                    isPressed = false
                                    if (!movedBeforeLongPress && !change.isConsumed) {
                                        change.consume()
                                        currentOnTap()
                                    }
                                    endedEarly = true
                                    return@withTimeout
                                }
                                val dist = hypot(
                                    change.position.x - startLocal.x,
                                    change.position.y - startLocal.y
                                )
                                if (dist > touchSlop) {
                                    movedBeforeLongPress = true
                                    isPressed = false
                                    endedEarly = true
                                    return@withTimeout
                                }
                            }
                        }
                    } catch (_: PointerEventTimeoutCancellationException) {
                        longPressed = true
                    }

                    isPressed = false
                    if (endedEarly || !longPressed) return@awaitEachGesture

                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                    var currentLocalPos = startLocal

                    fun toRoot(local: Offset): Offset {
                        val coords = itemCoords
                        return if (coords != null && coords.isAttached) {
                            coords.positionInRoot() + local
                        } else {
                            local
                        }
                    }

                    if (currentIsEditMode) {
                        draggingStarted = true
                        val rootStart = toRoot(currentLocalPos)
                        controller.startDrag(currentOriginProvider(), rootStart)
                        currentOnDragMoved(rootStart)
                    } else {
                        // Normal modda önce Haptic Menüyü göster; parmak kalkmadan sürüklenirse sürüklemeye geç!
                        currentOnOpenQuickMenu()
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id }
                        if (change == null || !change.pressed) {
                            change?.consume()
                            if (draggingStarted) {
                                val result = controller.finishDrag()
                                draggingStarted = false
                                if (result != null) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentOnDragEnded(result.first, result.second)
                                }
                            }
                            break
                        }

                        currentLocalPos = change.position
                        val distFromStart = hypot(
                            currentLocalPos.x - startLocal.x,
                            currentLocalPos.y - startLocal.y
                        )

                        if (!draggingStarted && distFromStart >= moveToDragThresholdPx) {
                            draggingStarted = true
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            currentOnDismissAndEnterEdit()
                            val rootPos = toRoot(currentLocalPos)
                            controller.startDrag(currentOriginProvider(), rootPos)
                            currentOnDragMoved(rootPos)
                            change.consume()
                        } else if (draggingStarted) {
                            if (change.positionChange() != Offset.Zero) {
                                change.consume()
                            }
                            val rootPos = toRoot(currentLocalPos)
                            currentOnDragMoved(rootPos)
                        }
                    }
                } finally {
                    isPressed = false
                    if (draggingStarted && controller.isDragging) {
                        controller.cancelDrag()
                    }
                }
            }
        }
}

/**
 * Parmak altında 120 FPS süzülen hayalet (ghost) uygulama veya klasör ikonu katmanı.
 */
@Composable
fun DraggingGhostOverlay(
    controller: LauncherDragDropController,
    iconSize: Dp,
    folderPreviewColumns: Int,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?
) {
    val active = controller.activeDrag ?: return
    val density = LocalDensity.current
    val halfPx = remember(iconSize, density) { with(density) { (iconSize * 1.16f / 2f).toPx() } }

    val liftScale by animateFloatAsState(
        targetValue = 1.16f,
        animationSpec = HnmMotion.dragLiftSpring(),
        label = "ghostLiftScale"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = controller.pointerInRoot.x - halfPx
                    translationY = controller.pointerInRoot.y - halfPx
                    scaleX = liftScale
                    scaleY = liftScale
                    alpha = 0.94f
                }
                .size(iconSize * 1.16f),
            contentAlignment = Alignment.Center
        ) {
            when (active) {
                is DragOrigin.DockSlot -> {
                    GhostAppArtwork(
                        app = active.app,
                        size = iconSize,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend
                    )
                }

                is DragOrigin.HomeSlot -> {
                    when (val item = active.item) {
                        is ResolvedHomeItem.App -> {
                            GhostAppArtwork(
                                app = item.app,
                                size = iconSize,
                                cachedIconProvider = cachedIconProvider,
                                loadIconSuspend = loadIconSuspend
                            )
                        }

                        is ResolvedHomeItem.Folder -> {
                            FolderIconTile(
                                folder = item,
                                iconSize = iconSize,
                                previewColumns = folderPreviewColumns,
                                showLabel = false,
                                cachedIconProvider = cachedIconProvider,
                                loadIconSuspend = loadIconSuspend,
                                onClick = {},
                                onLongClick = {}
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GhostAppArtwork(
    app: AppItem,
    size: Dp,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?
) {
    var bmp by remember(app.key.stableId) { mutableStateOf(cachedIconProvider(app.key)) }
    LaunchedEffect(app.key.stableId) {
        if (bmp == null) {
            bmp = loadIconSuspend(app.key)
        }
    }
    AppSquircleArtwork(
        label = app.label,
        bitmap = bmp,
        size = size
    )
}
