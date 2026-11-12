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
import com.hnmlabs.hnmui.ui.theme.HnmPalette

/**
 * Hem Ana Ekran klasörlerini (yeniden adlandırılabilir) hem de App Library genişletilmiş
 * kategori klasörlerini yay fiziğiyle açan buzlu cam pencere katmanı.
 */
@Composable
fun FolderModalOverlay(
    visible: Boolean,
    title: String,
    isEditableTitle: Boolean,
    columns: Int,
    apps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onRenameTitle: (String) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.folderSpring()) +
            scaleIn(initialScale = 0.84f, animationSpec = HnmMotion.folderSpring()),
        exit = fadeOut(animationSpec = HnmMotion.folderSpring()) +
            scaleOut(targetScale = 0.84f, animationSpec = HnmMotion.folderSpring())
    ) {
        val focusManager = LocalFocusManager.current
        var draftTitle by remember(title) { mutableStateOf(title) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HnmPalette.ScrimOverlay)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (isEditableTitle && draftTitle != title) {
                            onRenameTitle(draftTitle)
                        }
                        onDismiss()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isEditableTitle) {
                    BasicTextField(
                        value = draftTitle,
                        onValueChange = { draftTitle = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineLarge.copy(
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = SolidColor(HnmPalette.AccentCyan),
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
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 240.dp, max = 420.dp),
                    shape = SquircleShape.Card,
                    tone = GlassTone.ELEVATED_MODAL,
                    shadowElevation = 20.dp
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns.coerceAtLeast(3)),
                        contentPadding = PaddingValues(22.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(apps, key = { it.key.stableId }) { app ->
                            AppIconTile(
                                app = app,
                                iconSize = 58.dp,
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
