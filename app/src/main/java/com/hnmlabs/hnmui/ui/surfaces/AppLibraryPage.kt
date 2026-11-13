package com.hnmlabs.hnmui.ui.surfaces

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppLibraryGroup
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmPalette

/**
 * En sağ sayfa (Sayfa N+1): Otomatik kategorilenen Uygulama Arşivi (App Library).
 * iOS ergonomisine sadık olarak kategori kutularında ilk 3 ikonu doğrudan başlatır,
 * sağ alt 2x2 mini kümesine dokunulduğunda tüm kategoriyi açar.
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
    val haptics = LocalHapticFeedback.current
    var filterQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .hnmSafeTopPadding()
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

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
                        text = "Uygulama Arşivi'nde Ara",
                        color = HnmPalette.TextSecondary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = HnmPalette.TextSecondary
                    )
                },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Temizle",
                                tint = HnmPalette.TextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (filterQuery.isNotBlank()) {
            // Alfabetik filtrelenmiş A-Z görünümü (Türkçe I/İ/ı/i duyarlı)
            val q = filterQuery.trim()
            val filtered = remember(allApps, q) {
                allApps.filter { app ->
                    TurkishSearchNormalizer.containsMatch(app.label, q) ||
                        TurkishSearchNormalizer.containsMatch(app.key.packageName, q)
                }.sortedWith(
                    compareByDescending<AppItem> { TurkishSearchNormalizer.startsWithMatch(it.label, q) }
                        .thenBy { TurkishSearchNormalizer.foldTurkishI(it.label) }
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.key.stableId }) { app ->
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
                            MiniAppIconPreview(
                                app = app,
                                size = 44.dp,
                                cachedIconProvider = cachedIconProvider,
                                loadIconSuspend = loadIconSuspend
                            )
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
            }
        } else {
            // 2 sütunlu otomatik kategori klasörleri
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
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

@Composable
private fun AppLibraryCategoryPod(
    group: AppLibraryGroup,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onExpandCategory: () -> Unit
) {
    val hasOverflowCluster = group.apps.size > 4

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
                                        cachedIconProvider = cachedIconProvider,
                                        loadIconSuspend = loadIconSuspend,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onAppLongPress(app) }
                                    )
                                } else {
                                    Box(modifier = Modifier.size(58.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = group.category.titleTr,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
