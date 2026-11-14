package com.hnmlabs.hnmui.ui.surfaces

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppLibraryGroup
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.NotificationBadgePill
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import com.hnmlabs.hnmui.ui.theme.WallpaperLabelShadow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs

private enum class AppLibraryDisplayMode {
    CATEGORIES,
    ALPHABETICAL
}

@Immutable
private data class AlphabeticalSection(
    val letter: String,
    val apps: List<AppItem>,
    val headerLazyIndex: Int
)

private val TurkishLocale = Locale("tr", "TR")

internal fun resolveAlphabeticalBucket(label: String): String {
    val firstCp = label.trim().firstOrNull() ?: return "#"
    if (!firstCp.isLetter()) return "#"
    return firstCp.toString().uppercase(TurkishLocale)
}

/**
 * En sağ sayfa (Sayfa N+1): Otomatik kategorilenen Uygulama Arşivi (App Library 2.0).
 * - Akıllı 2x2 kategori kutuları (okunmamış bildirim rozeti toplamlı ve başlığa dokunarak açılabilir)
 * - Sağ kenarda dokunsal (Haptic) iOS A–Z hızlı kaydırma rayına sahip Alfabetik Dizin görünümü
 * - Türkçe I/İ/ı/i duyarlı anlık arama
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppLibraryPage(
    groups: List<AppLibraryGroup>,
    allApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onExpandCategory: (AppLibraryGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = HnmTheme.colors
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    var filterQuery by remember { mutableStateOf("") }
    var displayMode by remember { mutableStateOf(AppLibraryDisplayMode.CATEGORIES) }

    val hideImeConnection = remember(keyboardController, focusManager) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && abs(available.y) > 6f) {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                return Offset.Zero
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .hnmSafeTopPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Üst buzlu cam Uygulama Arşivi arama çubuğu
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            tone = GlassTone.CARD,
            shadowElevation = 6.dp
        ) {
            OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                placeholder = {
                    Text(
                        text = "Uygulama Arşivi'nde Ara (${allApps.size})",
                        color = colors.textSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Temizle",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    cursorColor = colors.accentBlue
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (filterQuery.isBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                GlassSurface(
                    shape = RoundedCornerShape(16.dp),
                    tone = GlassTone.PILL
                ) {
                    Row(
                        modifier = Modifier.padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isCat = displayMode == AppLibraryDisplayMode.CATEGORIES
                        val catBg by animateColorAsState(
                            targetValue = if (isCat) colors.accentBlue else Color.Transparent,
                            animationSpec = HnmMotion.slotHighlightSpring(),
                            label = "libTabCat"
                        )
                        val azBg by animateColorAsState(
                            targetValue = if (!isCat) colors.accentBlue else Color.Transparent,
                            animationSpec = HnmMotion.slotHighlightSpring(),
                            label = "libTabAz"
                        )

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(catBg)
                                .clickable {
                                    if (!isCat) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        displayMode = AppLibraryDisplayMode.CATEGORIES
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GridView,
                                contentDescription = null,
                                tint = if (isCat) Color.White else colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Kategoriler",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isCat) Color.White else colors.textSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(13.dp))
                                .background(azBg)
                                .clickable {
                                    if (isCat) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        displayMode = AppLibraryDisplayMode.ALPHABETICAL
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Sort,
                                contentDescription = null,
                                tint = if (!isCat) Color.White else colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "A–Z Liste",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (!isCat) Color.White else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filterQuery.isNotBlank()) {
            // Alfabetik filtrelenmiş arama görünümü (Türkçe I/İ/ı/i duyarlı)
            val q = filterQuery.trim()
            val filtered = remember(allApps, q) {
                val prepared = TurkishSearchNormalizer.prepareQuery(q)
                allApps.filter { app ->
                    app.matchesPreparedQuery(prepared)
                }.sortedWith(
                    compareByDescending<AppItem> { it.labelStartsWithPreparedQuery(prepared) }
                        .thenBy { it.foldedLabel }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(hideImeConnection),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.key.stableId }) { app ->
                    AppLibraryListRow(
                        app = app,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend,
                        onLaunchApp = onLaunchApp,
                        onAppLongPress = onAppLongPress
                    )
                }
            }
        } else if (displayMode == AppLibraryDisplayMode.ALPHABETICAL) {
            val sections = remember(allApps) {
                val grouped = LinkedHashMap<String, MutableList<AppItem>>()
                for (app in allApps) {
                    val bucket = resolveAlphabeticalBucket(app.label)
                    grouped.getOrPut(bucket) { ArrayList() }.add(app)
                }
                // '#' kovasını en sona taşı
                val hashApps = grouped.remove("#")
                if (hashApps != null) {
                    grouped["#"] = hashApps
                }
                var runningLazyIdx = 0
                grouped.map { (letter, list) ->
                    val sec = AlphabeticalSection(
                        letter = letter,
                        apps = list,
                        headerLazyIndex = runningLazyIdx
                    )
                    runningLazyIdx += 1 + list.size
                    sec
                }
            }

            val azListState = rememberLazyListState()
            var activeScrubLetter by remember { mutableStateOf<String?>(null) }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = azListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = if (sections.size > 1) 28.dp else 0.dp)
                        .nestedScroll(hideImeConnection),
                    contentPadding = PaddingValues(bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEach { section ->
                        item(key = "hdr_${section.letter}") {
                            Text(
                                text = section.letter,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    shadow = WallpaperLabelShadow,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.onWallpaperPrimary,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp, start = 4.dp)
                            )
                        }
                        items(section.apps, key = { it.key.stableId }) { app ->
                            AppLibraryListRow(
                                app = app,
                                cachedIconProvider = cachedIconProvider,
                                loadIconSuspend = loadIconSuspend,
                                onLaunchApp = onLaunchApp,
                                onAppLongPress = onAppLongPress
                            )
                        }
                    }
                }

                // Sağ kenar iOS A–Z hızlı kaydırma rayı
                if (sections.size > 1) {
                    var railHeightPx by remember { mutableFloatStateOf(1f) }
                    val latestSections by rememberUpdatedState(sections)

                    GlassSurface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(24.dp)
                            .padding(vertical = 12.dp)
                            .onSizeChanged {
                                railHeightPx = it.height.toFloat().coerceAtLeast(1f)
                            }
                            .pointerInput(sections.size) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    down.consume()

                                    fun scrubToY(yPx: Float) {
                                        val list = latestSections
                                        if (list.isEmpty()) return
                                        val ratio = (yPx / railHeightPx.coerceAtLeast(1f))
                                            .coerceIn(0f, 0.999f)
                                        val idx = (ratio * list.size).toInt()
                                            .coerceIn(0, list.lastIndex)
                                        val target = list[idx]
                                        if (activeScrubLetter != target.letter) {
                                            activeScrubLetter = target.letter
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            coroutineScope.launch {
                                                azListState.scrollToItem(target.headerLazyIndex)
                                            }
                                        }
                                    }

                                    scrubToY(down.position.y)
                                    while (true) {
                                        val ev = awaitPointerEvent()
                                        val change = ev.changes.firstOrNull { it.id == down.id }
                                        if (change == null || !change.pressed) {
                                            change?.consume()
                                            activeScrubLetter = null
                                            break
                                        }
                                        change.consume()
                                        scrubToY(change.position.y)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        tone = GlassTone.PILL
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            sections.forEach { sec ->
                                val isHighlighted = activeScrubLetter == sec.letter
                                Text(
                                    text = sec.letter,
                                    fontSize = 9.sp,
                                    lineHeight = 11.sp,
                                    fontWeight = if (isHighlighted) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isHighlighted) colors.accentCyan else colors.textSecondary
                                )
                            }
                        }
                    }

                    // Parmağın yanında beliren büyük A–Z harf balonu
                    androidx.compose.animation.AnimatedVisibility(
                        visible = activeScrubLetter != null,
                        enter = fadeIn(animationSpec = HnmMotion.iconPressSpring()) +
                            scaleIn(initialScale = 0.8f, animationSpec = HnmMotion.iconPressSpring()),
                        exit = fadeOut(animationSpec = HnmMotion.iconPressSpring()) +
                            scaleOut(targetScale = 0.8f, animationSpec = HnmMotion.iconPressSpring()),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 42.dp)
                    ) {
                        GlassSurface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            tone = GlassTone.ELEVATED_MODAL
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeScrubLetter.orEmpty(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentCyan
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // 2 sütunlu otomatik kategori klasörleri
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(hideImeConnection),
                contentPadding = PaddingValues(bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(groups, key = { it.category.name }) { group ->
                    AppLibraryCategoryPod(
                        group = group,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend,
                        onLaunchApp = onLaunchApp,
                        onAppLongPress = onAppLongPress,
                        onExpandCategory = { onExpandCategory(group) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppLibraryListRow(
    app: AppItem,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onLaunchApp(app) },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAppLongPress(app)
                }
            ),
        shape = RoundedCornerShape(18.dp),
        tone = GlassTone.CARD
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box {
                MiniAppIconPreview(
                    app = app,
                    size = 44.dp,
                    cachedIconProvider = cachedIconProvider,
                    loadIconSuspend = loadIconSuspend
                )
                NotificationBadgePill(
                    count = app.badgeCount,
                    visible = app.badgeCount > 0,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = app.category.titleTr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HnmPalette.TextMuted
                )
            }
        }
    }
}

@Composable
private fun AppLibraryCategoryPod(
    group: AppLibraryGroup,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onExpandCategory: () -> Unit
) {
    val colors = HnmTheme.colors
    val hasOverflowCluster = group.apps.size > 4
    val totalBadges = remember(group.apps) { group.apps.sumOf { it.badgeCount } }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = SquircleShape.Card,
                tone = GlassTone.DOCK,
                shadowElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (row in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (col in 0..1) {
                                val idx = row * 2 + col
                                if (idx == 3 && hasOverflowCluster) {
                                    // Sağ alt 2x2 mini küme: Dokununca tüm kategoriyi büyütür
                                    val overflowApps = group.apps.drop(3).take(4)
                                    Column(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .clickable(onClick = onExpandCategory)
                                            .padding(2.dp),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        for (mRow in 0..1) {
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                for (mCol in 0..1) {
                                                    val miniApp = overflowApps.getOrNull(mRow * 2 + mCol)
                                                    if (miniApp != null) {
                                                        MiniAppIconPreview(
                                                            app = miniApp,
                                                            size = 24.dp,
                                                            cachedIconProvider = cachedIconProvider,
                                                            loadIconSuspend = loadIconSuspend
                                                        )
                                                    } else {
                                                        Spacer(modifier = Modifier.size(24.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    val app = group.apps.getOrNull(idx)
                                    if (app != null) {
                                        AppIconTile(
                                            app = app,
                                            iconSize = 54.dp,
                                            showLabel = false,
                                            onGlassSurface = true,
                                            cachedIconProvider = cachedIconProvider,
                                            loadIconSuspend = loadIconSuspend,
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = { onAppLongPress(app) }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(58.dp)
                                                .clickable(onClick = onExpandCategory)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            NotificationBadgePill(
                count = totalBadges,
                visible = totalBadges > 0,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = (-4).dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${group.category.titleTr} (${group.apps.size})",
            style = MaterialTheme.typography.labelMedium.copy(shadow = WallpaperLabelShadow),
            color = colors.onWallpaperPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onExpandCategory)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
