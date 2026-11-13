package com.hnmlabs.hnmui.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import com.hnmlabs.hnmui.ui.theme.WallpaperLabelShadow
import kotlin.math.PI
import kotlin.math.sin

/**
 * Düzenleme (Jiggle) modunda 49 ayrı InfiniteTransition coroutine'i yerine tek bir paylaşımlı
 * GPU çizim fazı saatini tüm ikonlara dağıtan CompositionLocal.
 * Değeri yalnızca Modifier.graphicsLayer içinde okunarak 0 recomposition ile 120 FPS titreşim sağlar.
 */
val LocalEditJigglePhase = compositionLocalOf<State<Float>?> { null }

private val PlaceholderIconBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF334155))
)

/**
 * Squircle maskeli, yay tepkili, bildirim rozetli (badge) ve 120 FPS optimize
 * HNMUI uygulama ikonu bileşeni.
 *
 * [onGlassSurface] true olduğunda (Klasör içi, Spotlight, Özet kartları vb.) etiket rengi
 * Açık/Koyu cam temasıyla canlanır; false olduğunda (Ana ekran duvar kağıdı üstü) her duvar
 * kağıdında maksimum okunabilirlik için optik gölgeli beyaz etiket kullanılır.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconTile(
    app: AppItem,
    iconSize: Dp,
    showLabel: Boolean = true,
    onGlassSurface: Boolean = false,
    isEditMode: Boolean = false,
    enableInternalClickHandling: Boolean = true,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRemoveInEditMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val jigglePhaseState = LocalEditJigglePhase.current
    val colors = HnmTheme.colors

    var iconBitmap by remember(app.key.stableId) {
        mutableStateOf(cachedIconProvider(app.key))
    }
    if (iconBitmap == null) {
        LaunchedEffect(app.key.stableId) {
            iconBitmap = loadIconSuspend(app.key)
        }
    }

    val phaseOffsetRad = remember(app.key.stableId) {
        (((app.key.stableId.hashCode() and 0x7FFFFFFF) % 360) * (PI / 180.0)).toFloat()
    }

    val clickModifier = if (enableInternalClickHandling) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val internalScaleState = animateFloatAsState(
            targetValue = if (isPressed) 0.91f else 1f,
            animationSpec = HnmMotion.iconPressSpring(),
            label = "iconInternalPressScale"
        )
        Modifier
            .graphicsLayer {
                val s = internalScaleState.value
                scaleX = s
                scaleY = s
                rotationZ = if (isEditMode && jigglePhaseState != null) {
                    sin(jigglePhaseState.value + phaseOffsetRad) * 1.48f
                } else {
                    0f
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    } else {
        Modifier.graphicsLayer {
            rotationZ = if (isEditMode && jigglePhaseState != null) {
                sin(jigglePhaseState.value + phaseOffsetRad) * 1.48f
            } else {
                0f
            }
        }
    }

    Column(
        modifier = modifier.then(clickModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(iconSize + if (iconSize <= 42.dp) 4.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AppSquircleArtwork(
                label = app.label,
                bitmap = iconBitmap,
                size = iconSize
            )

            NotificationBadgePill(
                count = app.badgeCount,
                visible = app.badgeCount > 0 && !isEditMode,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
            )

            if (isEditMode && onRemoveInEditMode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .size(if (iconSize <= 42.dp) 18.dp else 22.dp)
                        .clip(CircleShape)
                        .background(Color(0xE6CBD5E1))
                        .combinedClickable(
                            onClick = onRemoveInEditMode,
                            onLongClick = {}
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Ana ekrandan kaldır",
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(if (iconSize <= 42.dp) 12.dp else 15.dp)
                    )
                }
            }
        }

        if (showLabel) {
            val isCompact = iconSize <= 44.dp
            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
            val baseStyle = MaterialTheme.typography.labelMedium
            val resolvedStyle = if (onGlassSurface) {
                baseStyle.copy(
                    fontSize = if (isCompact) 10.sp else baseStyle.fontSize,
                    color = colors.textPrimary
                )
            } else {
                baseStyle.copy(
                    fontSize = if (isCompact) 10.sp else baseStyle.fontSize,
                    color = colors.onWallpaperPrimary,
                    shadow = WallpaperLabelShadow
                )
            }
            Text(
                text = app.label,
                style = resolvedStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = iconSize + if (isCompact) 10.dp else 18.dp)
            )
        }
    }
}

/**
 * İkon görselini arkasında herhangi bir koyu kutu veya yapay gölge izi olmadan
 * doğrudan pürüzsüz Squircle formunda çizer.
 */
@Composable
fun AppSquircleArtwork(
    label: String,
    bitmap: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier
) {
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(SquircleShape.Icon)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(SquircleShape.Icon)
                .background(PlaceholderIconBrush),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label.take(1).uppercase(),
                color = Color.White,
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Ana ekrandaki 2x2 veya 3x3 mini önizlemeli buzlu cam klasör kutucuğu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderIconTile(
    folder: ResolvedHomeItem.Folder,
    iconSize: Dp,
    previewColumns: Int = 2,
    showLabel: Boolean = true,
    isEditMode: Boolean = false,
    isDropTargetHighlighted: Boolean = false,
    enableInternalClickHandling: Boolean = true,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    val jigglePhaseState = LocalEditJigglePhase.current
    val colors = HnmTheme.colors

    val folderPhaseRad = remember(folder.id) {
        (((folder.id.hashCode() and 0x7FFFFFFF) % 360) * (PI / 180.0)).toFloat()
    }

    val gridN = previewColumns.coerceIn(2, 3)

    val folderLayerAndClickMod = if (enableInternalClickHandling) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val scaleState = animateFloatAsState(
            targetValue = when {
                isDropTargetHighlighted -> 1.11f
                isPressed -> 0.91f
                else -> 1f
            },
            animationSpec = HnmMotion.iconPressSpring(),
            label = "folderPressScale"
        )
        Modifier
            .graphicsLayer {
                val s = scaleState.value
                scaleX = s
                scaleY = s
                rotationZ = if (isEditMode && jigglePhaseState != null) {
                    sin(jigglePhaseState.value + folderPhaseRad) * 1.38f
                } else {
                    0f
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
    } else {
        val highlightScaleState = if (isDropTargetHighlighted) {
            animateFloatAsState(
                targetValue = 1.11f,
                animationSpec = HnmMotion.iconPressSpring(),
                label = "folderDropHighlightScale"
            )
        } else {
            null
        }
        Modifier.graphicsLayer {
            val s = highlightScaleState?.value ?: 1f
            scaleX = s
            scaleY = s
            rotationZ = if (isEditMode && jigglePhaseState != null) {
                sin(jigglePhaseState.value + folderPhaseRad) * 1.38f
            } else {
                0f
            }
        }
    }

    Column(
        modifier = modifier.then(folderLayerAndClickMod),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(iconSize + if (iconSize <= 42.dp) 4.dp else 8.dp),
            contentAlignment = Alignment.Center
        ) {
            GlassSurface(
                modifier = Modifier.size(iconSize),
                shape = SquircleShape.Icon,
                tone = if (isDropTargetHighlighted) GlassTone.PILL else GlassTone.DOCK
            ) {
                val maxPreviewCount = gridN * gridN
                val previewApps = folder.apps.take(maxPreviewCount)
                val innerPad = if (gridN >= 3 || iconSize <= 42.dp) 4.dp else 6.dp
                val cellGap = if (gridN >= 3 || iconSize <= 42.dp) 2.dp else 4.dp
                val miniSize = ((iconSize - innerPad * 2 - cellGap * (gridN - 1)) / gridN)
                    .coerceAtLeast(8.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPad),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (rowIdx in 0 until gridN) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(cellGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (colIdx in 0 until gridN) {
                                val index = rowIdx * gridN + colIdx
                                val item = previewApps.getOrNull(index)
                                if (item != null) {
                                    MiniAppIconPreview(
                                        app = item,
                                        size = miniSize,
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend
                                    )
                                } else {
                                    Spacer(modifier = Modifier.size(miniSize))
                                }
                            }
                        }
                    }
                }
            }

            NotificationBadgePill(
                count = folder.totalBadges,
                visible = folder.totalBadges > 0,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
            )
        }

        if (showLabel) {
            val isCompact = iconSize <= 44.dp
            Spacer(modifier = Modifier.height(if (isCompact) 2.dp else 4.dp))
            val baseStyle = MaterialTheme.typography.labelMedium
            Text(
                text = folder.title,
                style = baseStyle.copy(
                    fontSize = if (isCompact) 10.sp else baseStyle.fontSize,
                    color = colors.onWallpaperPrimary,
                    shadow = WallpaperLabelShadow
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = iconSize + if (isCompact) 10.dp else 18.dp)
            )
        }
    }
}

@Composable
fun MiniAppIconPreview(
    app: AppItem,
    size: Dp,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?
) {
    var bmp by remember(app.key.stableId) { mutableStateOf(cachedIconProvider(app.key)) }
    if (bmp == null) {
        LaunchedEffect(app.key.stableId) {
            bmp = loadIconSuspend(app.key)
        }
    }
    AppSquircleArtwork(
        label = app.label,
        bitmap = bmp,
        size = size
    )
}

@Composable
fun NotificationBadgePill(
    count: Int,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible && count > 0,
        enter = scaleIn(animationSpec = HnmMotion.badgePopSpring()),
        exit = scaleOut(animationSpec = HnmMotion.badgePopSpring()),
        modifier = modifier
    ) {
        val text = if (count > 99) "99+" else count.toString()
        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(HnmPalette.BadgeRed)
                .padding(horizontal = 5.dp, vertical = 1.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
