package com.hnmlabs.hnmui.ui.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette

/**
 * Ana ekranda aşağı kaydırıldığında veya "Ara" hapına dokunulduğunda açılan
 * iOS Spotlight tarzı hızlı uygulama, öneri ve web arama katmanı.
 */
@Composable
fun SpotlightOverlay(
    visible: Boolean,
    query: String,
    suggestedApps: List<AppItem>,
    matchedApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onSearchWeb: (String) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(visible) {
        if (visible) {
            runCatching { focusRequester.requestFocus() }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { -it / 7 },
                animationSpec = HnmMotion.sheetSpring()
            ) +
            scaleIn(initialScale = 0.97f, animationSpec = HnmMotion.sheetSpring()),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { -it / 7 },
                animationSpec = HnmMotion.sheetSpring()
            ) +
            scaleOut(targetScale = 0.97f, animationSpec = HnmMotion.sheetSpring())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HnmPalette.ScrimOverlay)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hnmSafeTopPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                // Üst Spotlight Arama Çubuğu
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    shape = RoundedCornerShape(22.dp),
                    tone = GlassTone.ELEVATED_MODAL,
                    shadowElevation = 14.dp
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = "Uygulama, kısayol veya web'de ara",
                                color = HnmPalette.TextSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    if (query.isNotEmpty()) onQueryChange("") else onDismiss()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Kapat",
                                    tint = HnmPalette.TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val topMatch = matchedApps.firstOrNull()
                                if (topMatch != null) {
                                    onLaunchApp(topMatch)
                                } else if (query.isNotBlank()) {
                                    onSearchWeb(query)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (query.isBlank()) {
                        if (suggestedApps.isNotEmpty()) {
                            item(key = "spotlight_suggestions") {
                                GlassSurface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    tone = GlassTone.ELEVATED_MODAL
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "Sık Kullanılan Öneriler",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            suggestedApps.take(4).forEach { app ->
                                                AppIconTile(
                                                    app = app,
                                                    iconSize = 54.dp,
                                                    cachedIconProvider = cachedIconProvider,
                                                    loadIconSuspend = loadIconSuspend,
                                                    onClick = { onLaunchApp(app) },
                                                    onLongClick = { onAppLongPress(app) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        item(key = "system_settings_shortcut") {
                            GlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onOpenSystemSettings),
                                shape = RoundedCornerShape(20.dp),
                                tone = GlassTone.CARD
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = null,
                                        tint = HnmPalette.AccentCyan
                                    )
                                    Text(
                                        text = "HNM Ayarlar & 7×7 Özelleştirme",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    } else {
                        if (matchedApps.isNotEmpty()) {
                            item(key = "matched_header") {
                                Text(
                                    text = "UYGULAMALAR (${matchedApps.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HnmPalette.TextSecondary
                                )
                            }

                            items(matchedApps.take(12), key = { it.key.stableId }) { app ->
                                GlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onLaunchApp(app) },
                                    shape = RoundedCornerShape(18.dp),
                                    tone = GlassTone.ELEVATED_MODAL
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
                                            size = 42.dp,
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

                        item(key = "web_search_fallback") {
                            GlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSearchWeb(query) },
                                shape = RoundedCornerShape(18.dp),
                                tone = GlassTone.CARD
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Language,
                                        contentDescription = null,
                                        tint = HnmPalette.AccentCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "\"$query\" için Web'de Ara",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
