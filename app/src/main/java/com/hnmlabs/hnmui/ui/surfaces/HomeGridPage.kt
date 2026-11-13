package com.hnmlabs.hnmui.ui.surfaces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.hnmlabs.hnmui.core.formfactor.LauncherGridSpec
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.model.ResolvedHomePage
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.FolderIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.drag.DragOrigin
import com.hnmlabs.hnmui.ui.drag.HoverDropTarget
import com.hnmlabs.hnmui.ui.drag.LauncherDragDropController
import com.hnmlabs.hnmui.ui.drag.launcherDraggableItem
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import kotlin.math.roundToInt

/**
 * Ana ekran ızgara sayfası (4x4'ten 7x7'ye kadar dinamik ızgara ve 120 FPS sürükle-bırak destekli).
 * Boş alana uzun basma -> Düzenleme (Jiggle) Modu.
 * İkona basılı tutup hareket ettirme -> Haptic Menü'den kesintisiz sürüklemeye geçiş.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGridPage(
    homePageIndex: Int,
    page: ResolvedHomePage,
    gridSpec: LauncherGridSpec,
    isDefaultLauncher: Boolean,
    isEditMode: Boolean,
    dragController: LauncherDragDropController,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onRequestDefaultLauncher: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismissQuickMenuAndEnterEdit: () -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: (DragOrigin, HoverDropTarget?) -> Unit,
    onRemoveAppFromHome: (AppComponentKey) -> Unit,
    onFolderClick: (ResolvedHomeItem.Folder) -> Unit,
    onOpenHnmSettings: () -> Unit,
    onToggleEditMode: () -> Unit,
    onPickWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HnmTheme.colors
    val haptics = LocalHapticFeedback.current

    val bottomDockClearanceDp = if (gridSpec.dockVisible) {
        (134 * gridSpec.dockScaleFactor).roundToInt().coerceIn(108, 168).dp
    } else {
        48.dp
    }

    val isDragging = dragController.isDragging
    val hoveredTarget = if (isDragging) dragController.hoveredTarget else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .hnmSafeTopPadding()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isEditMode && !dragController.isDragging) onToggleEditMode()
                },
                onLongClick = {
                    if (!dragController.isDragging) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleEditMode()
                    }
                }
            )
            .padding(
                start = gridSpec.horizontalPaddingDp.dp,
                end = gridSpec.horizontalPaddingDp.dp,
                top = 8.dp,
                bottom = bottomDockClearanceDp
            )
    ) {
        // HNMUI henüz varsayılan Ana Ekran değilse doğrudan ana sayfada tek dokunuşlu kurulum çubuğu göster
        AnimatedVisibility(
            visible = !isDefaultLauncher && !isEditMode,
            enter = fadeIn(animationSpec = HnmMotion.sheetSpring()),
            exit = fadeOut(animationSpec = HnmMotion.sheetSpring())
        ) {
            GlassSurface(
                shape = RoundedCornerShape(20.dp),
                tone = GlassTone.CARD,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable(onClick = onRequestDefaultLauncher)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = colors.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Ana Ekran tuşunu kilitlemek için varsayılan yap",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.accentBlue)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "Ayarla",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Düzenleme modu üst aksiyon çubuğu (Duvar Kağıdı, Izgara & Dock Ayarları, Bitti)
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
                scaleIn(initialScale = 0.95f, animationSpec = HnmMotion.sheetSpring()),
            exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
                scaleOut(targetScale = 0.95f, animationSpec = HnmMotion.sheetSpring())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable(onClick = onPickWallpaper)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Wallpaper,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(text = "Duvar", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    GlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable(onClick = onOpenHnmSettings)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = colors.accentCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(text = "Izgara & Dock", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                GlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    tone = GlassTone.PILL,
                    modifier = Modifier.clickable(onClick = onToggleEditMode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(text = "Bitti", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Dinamik columns x rows ana ekran ızgarası (4x4'ten 7x7'ye kadar)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    if (coords.isAttached) {
                        dragController.updateGridBounds(
                            homePageIndex = homePageIndex,
                            rect = Rect(coords.positionInRoot(), coords.size.toSize())
                        )
                    }
                },
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            for (rowIndex in 0 until gridSpec.rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    for (colIndex in 0 until gridSpec.columns) {
                        val slotIndex = rowIndex * gridSpec.columns + colIndex
                        val item = page.items.getOrNull(slotIndex)

                        val isCellHovered = hoveredTarget is HoverDropTarget.HomeCell &&
                            hoveredTarget.pageIndex == homePageIndex &&
                            hoveredTarget.slotIndex == slotIndex

                        val isFolderMergeHovered = hoveredTarget is HoverDropTarget.MergeIntoFolder &&
                            hoveredTarget.pageIndex == homePageIndex &&
                            hoveredTarget.slotIndex == slotIndex

                        val isCreateFolderHovered = hoveredTarget is HoverDropTarget.CreateFolderWithApp &&
                            hoveredTarget.pageIndex == homePageIndex &&
                            hoveredTarget.slotIndex == slotIndex

                        val showCellHighlight = isCellHovered && dragController.isDragging
                        val showCreateFolderHighlight = isCreateFolderHovered && dragController.isDragging

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            if (showCellHighlight) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = if (gridSpec.iconSizeDp <= 42) 2.dp else 4.dp)
                                        .size(gridSpec.iconSizeDp.dp)
                                        .clip(SquircleShape.Icon)
                                        .background(Color(0x2838BDF8))
                                        .border(
                                            width = 1.2.dp,
                                            color = Color(0x9938BDF8),
                                            shape = SquircleShape.Icon
                                        )
                                )
                            }

                            if (showCreateFolderHighlight) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = if (gridSpec.iconSizeDp <= 42) 0.dp else 1.dp)
                                        .size((gridSpec.iconSizeDp + 8).dp)
                                        .graphicsLayer {
                                            scaleX = 1.08f
                                            scaleY = 1.08f
                                        }
                                        .clip(SquircleShape.Icon)
                                        .background(colors.glassPillFillTop)
                                        .border(
                                            width = 1.5.dp,
                                            color = colors.accentCyan,
                                            shape = SquircleShape.Icon
                                        )
                                )
                            }

                            when (item) {
                                is ResolvedHomeItem.App -> {
                                    AppIconTile(
                                        app = item.app,
                                        iconSize = gridSpec.iconSizeDp.dp,
                                        showLabel = gridSpec.showHomeLabels,
                                        isEditMode = isEditMode,
                                        enableInternalClickHandling = false,
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = {},
                                        onLongClick = {},
                                        onRemoveInEditMode = {
                                            onRemoveAppFromHome(item.app.key)
                                        },
                                        modifier = Modifier.launcherDraggableItem(
                                            keyId = item.itemId,
                                            originProvider = {
                                                DragOrigin.HomeSlot(
                                                    pageIndex = homePageIndex,
                                                    slotIndex = slotIndex,
                                                    item = item
                                                )
                                            },
                                            controller = dragController,
                                            isEditMode = isEditMode,
                                            onTap = {
                                                if (!isEditMode) onAppClick(item.app)
                                            },
                                            onOpenQuickMenu = { onAppLongPress(item.app) },
                                            onDismissQuickMenuAndEnterEdit = onDismissQuickMenuAndEnterEdit,
                                            onDragMoved = onDragMoved,
                                            onDragEnded = onDragEnded
                                        )
                                    )
                                }

                                is ResolvedHomeItem.Folder -> {
                                    FolderIconTile(
                                        folder = item,
                                        iconSize = gridSpec.iconSizeDp.dp,
                                        previewColumns = gridSpec.folderPreviewColumns,
                                        showLabel = gridSpec.showHomeLabels,
                                        isEditMode = isEditMode,
                                        isDropTargetHighlighted = isFolderMergeHovered,
                                        enableInternalClickHandling = false,
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = {},
                                        onLongClick = {},
                                        modifier = Modifier.launcherDraggableItem(
                                            keyId = item.itemId,
                                            originProvider = {
                                                DragOrigin.HomeSlot(
                                                    pageIndex = homePageIndex,
                                                    slotIndex = slotIndex,
                                                    item = item
                                                )
                                            },
                                            controller = dragController,
                                            isEditMode = isEditMode,
                                            onTap = { onFolderClick(item) },
                                            onOpenQuickMenu = { onFolderClick(item) },
                                            onDismissQuickMenuAndEnterEdit = onDismissQuickMenuAndEnterEdit,
                                            onDragMoved = onDragMoved,
                                            onDragEnded = onDragEnded
                                        )
                                    )
                                }

                                null -> {
                                    val emptySlotHeight = if (gridSpec.showHomeLabels) {
                                        (gridSpec.iconSizeDp + 18).dp
                                    } else {
                                        (gridSpec.iconSizeDp + 6).dp
                                    }
                                    Spacer(modifier = Modifier.height(emptySlotHeight))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Ana ekran sayfalarının altında sabit duran iOS "Ara" hapı, akıcı sayfa kapsülü ve
 * sürükle-bırak destekli (uygulama eklenip çıkarılabilen) buzlu cam Dock bileşeni.
 */
@Composable
fun LauncherBottomDockBar(
    dockApps: List<AppItem>,
    gridSpec: LauncherGridSpec,
    currentHomePageIndex: Int,
    totalHomePages: Int,
    isEditMode: Boolean,
    dragController: LauncherDragDropController,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onSearchPillClick: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismissQuickMenuAndEnterEdit: () -> Unit,
    onDragMoved: (Offset) -> Unit,
    onDragEnded: (DragOrigin, HoverDropTarget?) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HnmTheme.colors
    val scale = gridSpec.dockScaleFactor.coerceIn(0.75f, 1.25f)
    val outerHPad = (16f / scale).roundToInt().coerceIn(8, 22).dp
    val innerHPad = (16f * scale).roundToInt().coerceIn(10, 22).dp
    val innerVPad = (11f * scale).roundToInt().coerceIn(7, 16).dp
    val dockCornerRadius = (30f * scale).roundToInt().coerceIn(22, 38).dp

    val isDragging = dragController.isDragging
    val isDockDropHovered = isDragging && dragController.hoveredTarget is HoverDropTarget.DockCell

    val dockHoverScale = if (isDragging) {
        val animated by animateFloatAsState(
            targetValue = if (isDockDropHovered) 1.025f else 1f,
            animationSpec = HnmMotion.slotHighlightSpring(),
            label = "dockHoverScale"
        )
        animated
    } else {
        1f
    }
    val dockBorderColor = if (isDragging) {
        val animated by animateColorAsState(
            targetValue = if (isDockDropHovered) Color(0xAA38BDF8) else Color.Transparent,
            animationSpec = HnmMotion.slotHighlightSpring(),
            label = "dockBorderColor"
        )
        animated
    } else {
        Color.Transparent
    }

    val showDockContainer = gridSpec.dockVisible &&
        (dockApps.isNotEmpty() || isEditMode || isDragging)

    DisposableEffect(showDockContainer) {
        if (!showDockContainer) {
            dragController.updateDockBounds(null)
        }
        onDispose {}
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerHPad, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // iOS tarzı "Ara" (Spotlight) hapı ve akıcı animasyonlu sayfa kapsülleri
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassSurface(
                shape = RoundedCornerShape(18.dp),
                tone = GlassTone.PILL,
                modifier = Modifier.clickable(onClick = onSearchPillClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Ara",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Ara",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (totalHomePages > 1) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalHomePages) { idx ->
                        val active = idx == currentHomePageIndex
                        val pillWidth by animateDpAsState(
                            targetValue = if (active) 16.dp else 6.dp,
                            animationSpec = HnmMotion.slotHighlightSpring(),
                            label = "pageIndicatorWidth_$idx"
                        )
                        val pillColor by animateColorAsState(
                            targetValue = if (active) {
                                colors.onWallpaperPrimary
                            } else {
                                colors.onWallpaperSecondary.copy(alpha = 0.38f)
                            },
                            animationSpec = HnmMotion.slotHighlightSpring(),
                            label = "pageIndicatorColor_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .width(pillWidth)
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(pillColor)
                        )
                    }
                }
            }
        }

        if (showDockContainer) {
            val visibleDockApps = dockApps.take(gridSpec.dockMaxItems)
            val hoveredDockIdx = if (isDockDropHovered) {
                (dragController.hoveredTarget as? HoverDropTarget.DockCell)?.insertIndex
            } else {
                null
            }

            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (dockHoverScale != 1f) {
                            Modifier.graphicsLayer {
                                scaleX = dockHoverScale
                                scaleY = dockHoverScale
                            }
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (dockBorderColor != Color.Transparent) {
                            Modifier.border(
                                width = 1.2.dp,
                                color = dockBorderColor,
                                shape = RoundedCornerShape(dockCornerRadius)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .onGloballyPositioned { coords ->
                        if (coords.isAttached) {
                            dragController.updateDockBounds(
                                Rect(coords.positionInRoot(), coords.size.toSize())
                            )
                        }
                    },
                shape = RoundedCornerShape(dockCornerRadius),
                tone = GlassTone.DOCK
            ) {
                if (visibleDockApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(gridSpec.dockIconSizeDp.dp + innerVPad * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Uygulamayı sabitlemek için Dock'a sürükleyin",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = innerHPad, vertical = innerVPad),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        visibleDockApps.forEachIndexed { idx, app ->
                            val isThisSlotHovered = isDockDropHovered && hoveredDockIdx == idx
                            val slotScale = if (isDragging) {
                                val animated by animateFloatAsState(
                                    targetValue = if (isThisSlotHovered) 1.07f else 1f,
                                    animationSpec = HnmMotion.slotHighlightSpring(),
                                    label = "dockSlotScale_$idx"
                                )
                                animated
                            } else {
                                1f
                            }

                            Box(
                                modifier = if (slotScale != 1f) {
                                    Modifier.graphicsLayer {
                                        scaleX = slotScale
                                        scaleY = slotScale
                                    }
                                } else {
                                    Modifier
                                },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isThisSlotHovered) {
                                    Box(
                                        modifier = Modifier
                                            .size(gridSpec.dockIconSizeDp.dp + 6.dp)
                                            .clip(SquircleShape.Icon)
                                            .background(Color(0x2838BDF8))
                                    )
                                }

                                AppIconTile(
                                    app = app,
                                    iconSize = gridSpec.dockIconSizeDp.dp,
                                    showLabel = false,
                                    isEditMode = isEditMode,
                                    enableInternalClickHandling = false,
                                    cachedIconProvider = cachedIconProvider,
                                    loadIconSuspend = loadIconSuspend,
                                    onClick = {},
                                    onLongClick = {},
                                    modifier = Modifier.launcherDraggableItem(
                                        keyId = "dock:${app.key.stableId}",
                                        originProvider = {
                                            DragOrigin.DockSlot(
                                                dockIndex = idx,
                                                app = app
                                            )
                                        },
                                        controller = dragController,
                                        isEditMode = isEditMode,
                                        onTap = {
                                            if (!isEditMode) onAppClick(app)
                                        },
                                        onOpenQuickMenu = { onAppLongPress(app) },
                                        onDismissQuickMenuAndEnterEdit = onDismissQuickMenuAndEnterEdit,
                                        onDragMoved = onDragMoved,
                                        onDragEnded = onDragEnded
                                    )
                                )
                            }
                        }

                        if (gridSpec.dockShowRecentsDivider) {
                            Spacer(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(42.dp)
                                    .background(HnmPalette.SubtleBorder)
                            )
                        }
                    }
                }
            }
        }
    }
}
