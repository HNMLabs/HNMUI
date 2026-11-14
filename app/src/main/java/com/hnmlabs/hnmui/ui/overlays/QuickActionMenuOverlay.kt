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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.AddToHomeScreen
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppShortcutItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme

/**
 * İkona uzun basıldığında açılan iOS Haptic Touch tarzı buzlu cam bağlam menüsü.
 * Android'in yerel dinamik/statik App Shortcuts öğelerini ve HNMUI düzenleme eylemlerini birleştirir.
 */
@Composable
fun QuickActionMenuOverlay(
    targetApp: AppItem?,
    shortcuts: List<AppShortcutItem>,
    existingFolders: List<ResolvedHomeItem.Folder>,
    containingFolderId: String?,
    isAlreadyOnHome: Boolean,
    isInDock: Boolean,
    canAddMoreToDock: Boolean,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchShortcut: (AppShortcutItem) -> Unit,
    onAddToHome: (AppItem) -> Unit,
    onRemoveFromHome: (AppItem) -> Unit,
    onToggleDockPin: (AppItem) -> Unit,
    onCreateNewFolderWithApp: (AppItem) -> Unit,
    onMoveToFolder: (AppItem, String) -> Unit,
    onExtractFromFolderToHome: (String, AppItem) -> Unit,
    onEnterEditMode: () -> Unit,
    onAppInfo: (AppItem) -> Unit,
    onUninstallApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = HnmTheme.colors
    val isVisible = targetApp != null
    var lastApp by remember { mutableStateOf(targetApp) }
    var lastShortcuts by remember { mutableStateOf(shortcuts) }
    var lastContainingFolderId by remember { mutableStateOf(containingFolderId) }
    var lastAlreadyOnHome by remember { mutableStateOf(isAlreadyOnHome) }
    var lastInDock by remember { mutableStateOf(isInDock) }
    var lastCanAddDock by remember { mutableStateOf(canAddMoreToDock) }

    if (targetApp != null) {
        lastApp = targetApp
        lastShortcuts = shortcuts
        lastContainingFolderId = containingFolderId
        lastAlreadyOnHome = isAlreadyOnHome
        lastInDock = isInDock
        lastCanAddDock = canAddMoreToDock
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            scaleIn(initialScale = 0.90f, animationSpec = HnmMotion.sheetSpring()),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            scaleOut(targetScale = 0.90f, animationSpec = HnmMotion.sheetSpring())
    ) {
        val app = targetApp ?: lastApp ?: return@AnimatedVisibility
        val displayShortcuts = if (isVisible) shortcuts else lastShortcuts
        val displayFolderId = if (isVisible) containingFolderId else lastContainingFolderId
        val displayAlreadyOnHome = if (isVisible) isAlreadyOnHome else lastAlreadyOnHome
        val displayInDock = if (isVisible) isInDock else lastInDock
        val displayCanAddDock = if (isVisible) canAddMoreToDock else lastCanAddDock

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrimOverlay)
                .then(
                    if (isVisible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .fillMaxWidth(0.84f)
                    .padding(vertical = 24.dp)
                    .then(
                        if (isVisible) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {}
                            )
                        } else {
                            Modifier
                        }
                    ),
                horizontalAlignment = Alignment.Start
            ) {
                // Vurgulanan uygulama başlığı ve squircle ikonu
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    MiniAppIconPreview(
                        app = app,
                        size = 62.dp,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend
                    )
                    Column {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                        Text(
                            text = app.category.titleTr,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    shape = RoundedCornerShape(24.dp),
                    tone = GlassTone.ELEVATED_MODAL
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Sistem kısayolları (LauncherApps.ShortcutQuery)
                        displayShortcuts.forEach { shortcut ->
                            ContextActionRow(
                                label = shortcut.longLabel?.takeIf { it.isNotBlank() }
                                    ?: shortcut.shortLabel,
                                icon = Icons.Rounded.Bolt,
                                tint = colors.accentCyan,
                                onClick = {
                                    onLaunchShortcut(shortcut)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        }

                        if (displayInDock) {
                            ContextActionRow(
                                label = "Dock'tan Ana Ekrana Çıkar",
                                icon = Icons.Rounded.RemoveCircleOutline,
                                onClick = {
                                    onToggleDockPin(app)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        } else if (displayCanAddDock) {
                            ContextActionRow(
                                label = "Alt Dock'a Sabitle",
                                icon = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                                tint = colors.accentCyan,
                                onClick = {
                                    onToggleDockPin(app)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        }

                        if (displayFolderId != null) {
                            ContextActionRow(
                                label = "Klasörden Ana Ekrana Çıkar",
                                icon = Icons.Rounded.FolderOpen,
                                onClick = {
                                    onExtractFromFolderToHome(displayFolderId, app)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        } else {
                            if (!displayAlreadyOnHome) {
                                ContextActionRow(
                                    label = "Ana Ekrana Ekle",
                                    icon = Icons.AutoMirrored.Rounded.AddToHomeScreen,
                                    onClick = {
                                        onAddToHome(app)
                                        onDismiss()
                                    }
                                )
                                MenuDivider()
                            } else {
                                ContextActionRow(
                                    label = "Ana Ekrandan Kaldır",
                                    icon = Icons.Rounded.RemoveCircleOutline,
                                    onClick = {
                                        onRemoveFromHome(app)
                                        onDismiss()
                                    }
                                )
                                MenuDivider()
                            }
                        }

                        val candidateFolders = existingFolders
                            .filter { it.id != displayFolderId }
                            .take(4)
                        candidateFolders.forEach { folder ->
                            ContextActionRow(
                                label = "\"${folder.title}\" Klasörüne Taşı",
                                icon = Icons.Rounded.FolderOpen,
                                onClick = {
                                    onMoveToFolder(app, folder.id)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        }

                        if (displayFolderId == null) {
                            ContextActionRow(
                                label = "Yeni Klasör Oluştur",
                                icon = Icons.Rounded.CreateNewFolder,
                                onClick = {
                                    onCreateNewFolderWithApp(app)
                                    onDismiss()
                                }
                            )
                            MenuDivider()
                        }

                        ContextActionRow(
                            label = "Ana Ekranı Düzenle",
                            icon = Icons.Rounded.Edit,
                            onClick = {
                                onDismiss()
                                onEnterEditMode()
                            }
                        )
                        MenuDivider()

                        ContextActionRow(
                            label = "Uygulama Bilgisi",
                            icon = Icons.Rounded.Info,
                            onClick = {
                                onDismiss()
                                onAppInfo(app)
                            }
                        )

                        if (!app.isSystemApp) {
                            MenuDivider()
                            ContextActionRow(
                                label = "Uygulamayı Kaldır",
                                icon = Icons.Rounded.DeleteOutline,
                                tint = HnmPalette.BadgeRed,
                                onClick = {
                                    onDismiss()
                                    onUninstallApp(app)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextActionRow(
    label: String,
    icon: ImageVector,
    tint: Color = HnmTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(color = tint),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        thickness = 0.5.dp,
        color = HnmTheme.colors.divider
    )
}
