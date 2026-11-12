package com.hnmlabs.hnmui.ui.surfaces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.core.formfactor.LauncherGridSpec
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.model.ResolvedHomePage
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.FolderIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import kotlin.math.roundToInt

/**
 * Ana ekran ızgara sayfası (4x4'ten 7x7'ye kadar dinamik ızgara destekli).
 * - Sağ üstten aşağı kaydırma -> iOS Denetim Merkezi (Control Center)
 * - Sol üstten aşağı kaydırma -> Bildirimler
 * - Ortadan aşağı kaydırma -> Spotlight Arama
 * - Boş alana uzun basma -> Düzenleme (Jiggle) Modu
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGridPage(
    page: ResolvedHomePage,
    gridSpec: LauncherGridSpec,
    isDefaultLauncher: Boolean,
    isEditMode: Boolean,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onRequestDefaultLauncher: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onRemoveAppFromHome: (AppComponentKey) -> Unit,
    onFolderClick: (ResolvedHomeItem.Folder) -> Unit,
    onPullDownSpotlight: () -> Unit,
    onPullDownControlCenter: () -> Unit,
    onPullDownNotifications: () -> Unit,
    onOpenHnmSettings: () -> Unit,
    onToggleEditMode: () -> Unit,
    onPickWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val topZoneHeightPx = remember(density) { with(density) { 135.dp.toPx() } }

    var pageWidthPx by remember { mutableFloatStateOf(1f) }
    var dragStartXPx by remember { mutableFloatStateOf(0f) }
    var dragStartYPx by remember { mutableFloatStateOf(0f) }
    var downwardDragPx by remember { mutableFloatStateOf(0f) }

    val bottomDockClearanceDp = if (gridSpec.dockVisible) {
        (134 * gridSpec.dockScaleFactor).roundToInt().coerceIn(108, 168).dp
    } else {
        48.dp
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { pageWidthPx = it.width.toFloat().coerceAtLeast(1f) }
            .statusBarsPadding()
            .pointerInput(topZoneHeightPx) {
                detectVerticalDragGestures(
                    onDragStart = { startOffset ->
                        dragStartXPx = startOffset.x
                        dragStartYPx = startOffset.y
                        downwardDragPx = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        downwardDragPx += dragAmount
                        if (downwardDragPx > 88f) {
                            downwardDragPx = 0f
                            when {
                                dragStartYPx < topZoneHeightPx && dragStartXPx > pageWidthPx * 0.54f -> {
                                    onPullDownControlCenter()
                                }
                                dragStartYPx < topZoneHeightPx -> {
                                    onPullDownNotifications()
                                }
                                else -> {
                                    onPullDownSpotlight()
                                }
                            }
                        }
                    }
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {
                    if (isEditMode) onToggleEditMode()
                },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleEditMode()
                }
            )
            .padding(
                start = gridSpec.horizontalPaddingDp.dp,
                end = gridSpec.horizontalPaddingDp.dp,
                top = 10.dp,
                bottom = bottomDockClearanceDp
            )
    ) {
        // HNMUI henüz varsayılan Ana Ekran değilse doğrudan ana sayfada tek dokunuşlu kurulum çubuğu göster
        AnimatedVisibility(
            visible = !isDefaultLauncher && !isEditMode,
            enter = fadeIn(),
            exit = fadeOut()
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
                        tint = HnmPalette.AccentCyan,
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
                            .background(HnmPalette.AccentBlue)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(text = "Ayarla", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Düzenleme modu üst aksiyon çubuğu (Duvar Kağıdı, Düzen/7x7 Ayarlar & Bitti)
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut()
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
                                tint = Color.White,
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
                                tint = HnmPalette.AccentCyan,
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
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(text = "Bitti", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Dinamik columns x rows ana ekran ızgarası (4x4'ten 7x7'ye kadar)
        Column(
            modifier = Modifier.fillMaxSize(),
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

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when (item) {
                                is ResolvedHomeItem.App -> {
                                    AppIconTile(
                                        app = item.app,
                                        iconSize = gridSpec.iconSizeDp.dp,
                                        showLabel = gridSpec.showHomeLabels,
                                        isEditMode = isEditMode,
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = {
                                            if (!isEditMode) onAppClick(item.app)
                                        },
                                        onLongClick = { onAppLongPress(item.app) },
                                        onRemoveInEditMode = {
                                            onRemoveAppFromHome(item.app.key)
                                        }
                                    )
                                }

                                is ResolvedHomeItem.Folder -> {
                                    FolderIconTile(
                                        folder = item,
                                        iconSize = gridSpec.iconSizeDp.dp,
                                        previewColumns = gridSpec.folderPreviewColumns,
                                        showLabel = gridSpec.showHomeLabels,
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = { onFolderClick(item) },
                                        onLongClick = { onFolderClick(item) }
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
 * Ana ekran sayfalarının altında sabit duran iOS "Ara" hapı, sayfa göstergesi ve
 * büyütülüp küçültülebilen (dockScaleFactor) buzlu cam Dock bileşeni.
 */
@Composable
fun LauncherBottomDockBar(
    dockApps: List<AppItem>,
    gridSpec: LauncherGridSpec,
    currentHomePageIndex: Int,
    totalHomePages: Int,
    isEditMode: Boolean,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onSearchPillClick: () -> Unit,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = gridSpec.dockScaleFactor.coerceIn(0.75f, 1.25f)
    val outerHPad = (16f / scale).roundToInt().coerceIn(8, 22).dp
    val innerHPad = (16f * scale).roundToInt().coerceIn(10, 22).dp
    val innerVPad = (11f * scale).roundToInt().coerceIn(7, 16).dp
    val dockCornerRadius = (30f * scale).roundToInt().coerceIn(22, 38).dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = outerHPad, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // iOS tarzı "Ara" (Spotlight) hapı ve sayfa noktaları
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
                        tint = Color.White,
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
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalHomePages) { idx ->
                        val active = idx == currentHomePageIndex
                        Box(
                            modifier = Modifier
                                .size(if (active) 7.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) Color.White else HnmPalette.SubtleBorder
                                )
                        )
                    }
                }
            }
        }

        if (gridSpec.dockVisible && dockApps.isNotEmpty()) {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dockCornerRadius),
                tone = GlassTone.DOCK
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = innerHPad, vertical = innerVPad),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dockApps.take(gridSpec.dockMaxItems).forEach { app ->
                        AppIconTile(
                            app = app,
                            iconSize = gridSpec.dockIconSizeDp.dp,
                            showLabel = false,
                            isEditMode = isEditMode,
                            cachedIconProvider = cachedIconProvider,
                            loadIconSuspend = loadIconSuspend,
                            onClick = {
                                if (!isEditMode) onAppClick(app)
                            },
                            onLongClick = { onAppLongPress(app) }
                        )
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
