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
import androidx.compose.material.icons.rounded.Search
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

/**
 * Ana ekran ızgara sayfası (4x6 telefon düzeni veya rezerve edilen 6x5 tablet düzeni).
 * Herhangi bir yerden aşağı kaydırıldığında Spotlight aramasını açar;
 * boş alana uzun basıldığında iOS tarzı düzenleme (Jiggle) moduna girer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeGridPage(
    page: ResolvedHomePage,
    gridSpec: LauncherGridSpec,
    isEditMode: Boolean,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onAppClick: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onRemoveAppFromHome: (AppComponentKey) -> Unit,
    onFolderClick: (ResolvedHomeItem.Folder) -> Unit,
    onPullDownSpotlight: () -> Unit,
    onToggleEditMode: () -> Unit,
    onPickWallpaper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var downwardDragPx by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { downwardDragPx = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        downwardDragPx += dragAmount
                        if (downwardDragPx > 96f) {
                            downwardDragPx = 0f
                            onPullDownSpotlight()
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
                top = 12.dp,
                bottom = 148.dp
            )
    ) {
        // Düzenleme modu üst aksiyon çubuğu (Duvar kağıdı seçimi & Bitti hapı)
        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    tone = GlassTone.PILL,
                    modifier = Modifier.clickable(onClick = onPickWallpaper)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Wallpaper,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = "Duvar Kağıdı", style = MaterialTheme.typography.labelSmall)
                    }
                }

                GlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    tone = GlassTone.PILL,
                    modifier = Modifier.clickable(onClick = onToggleEditMode)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(text = "Bitti", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Sabit oranlı columns x rows ana ekran ızgarası
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
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = { onFolderClick(item) },
                                        onLongClick = { onFolderClick(item) }
                                    )
                                }

                                null -> {
                                    Spacer(modifier = Modifier.height((gridSpec.iconSizeDp + 24).dp))
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
 * buzlu cam (Frosted Glass) Dock bileşeni.
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // iOS tarzı "Ara" (Spotlight) hapı ve sayfa noktaları
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GlassSurface(
                shape = RoundedCornerShape(18.dp),
                tone = GlassTone.PILL,
                shadowElevation = 4.dp,
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

        // Buzlu Cam (Frosted Glass) Dock
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            tone = GlassTone.DOCK,
            shadowElevation = 14.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
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
