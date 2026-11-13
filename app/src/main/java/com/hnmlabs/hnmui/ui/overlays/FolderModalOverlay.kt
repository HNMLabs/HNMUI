package com.hnmlabs.hnmui.ui.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmTheme

/**
 * Hem Ana Ekran klasörlerini (yeniden adlandırılabilir) hem de App Library genişletilmiş
 * kategori klasörlerini 3x3'ten 7x7'ye kadar dinamik ızgarayla açan buzlu cam pencere katmanı.
 */
@Composable
fun FolderModalOverlay(
    visible: Boolean,
    title: String,
    isEditableTitle: Boolean,
    columns: Int,
    showLabels: Boolean = true,
    apps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onRenameTitle: (String) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = HnmTheme.colors

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.folderSpring()) +
            scaleIn(initialScale = 0.84f, animationSpec = HnmMotion.folderSpring()),
        exit = fadeOut(animationSpec = HnmMotion.folderSpring()) +
            scaleOut(targetScale = 0.84f, animationSpec = HnmMotion.folderSpring())
    ) {
        val focusManager = LocalFocusManager.current
        var draftTitle by remember(title) { mutableStateOf(title) }
        val safeCols = columns.coerceIn(3, 7)

        val outerHorizontalPad = when (safeCols) {
            3 -> 28.dp
            4 -> 20.dp
            5 -> 14.dp
            6 -> 10.dp
            else -> 8.dp
        }
        val innerGridPad = when (safeCols) {
            3 -> 22.dp
            4 -> 16.dp
            5 -> 12.dp
            6 -> 10.dp
            else -> 8.dp
        }
        val hSpacing = when (safeCols) {
            3 -> 14.dp
            4 -> 10.dp
            5 -> 8.dp
            6 -> 6.dp
            else -> 4.dp
        }
        val vSpacing = when (safeCols) {
            3 -> 16.dp
            4 -> 14.dp
            5 -> 12.dp
            6 -> 10.dp
            else -> 8.dp
        }
        val folderIconDp = when (safeCols) {
            3 -> 58.dp
            4 -> 48.dp
            5 -> 40.dp
            6 -> 34.dp
            else -> 29.dp
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrimOverlay)
                .then(
                    if (visible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (isEditableTitle && draftTitle != title) {
                                    onRenameTitle(draftTitle)
                                }
                                onDismiss()
                            }
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = outerHorizontalPad)
                    .then(
                        if (visible) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditableTitle) {
                    BasicTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(colors.accentCyan),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onRenameTitle(draftTitle)
                                focusManager.clearFocus()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = if (safeCols >= 6) 540.dp else 440.dp),
                    shape = SquircleShape.Card,
                    tone = GlassTone.ELEVATED_MODAL
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(safeCols),
                        contentPadding = PaddingValues(innerGridPad),
                        horizontalArrangement = Arrangement.spacedBy(hSpacing),
                        verticalArrangement = Arrangement.spacedBy(vSpacing)
                    ) {
                        items(apps, key = { it.key.stableId }) { app ->
                            AppIconTile(
                                app = app,
                                iconSize = folderIconDp,
                                showLabel = if (safeCols >= 7) false else showLabels,
                                onGlassSurface = true,
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
}
