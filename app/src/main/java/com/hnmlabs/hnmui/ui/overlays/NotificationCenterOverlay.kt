package com.hnmlabs.hnmui.ui.overlays

import android.media.AudioManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.LiveNotificationCard
import com.hnmlabs.hnmui.model.QuickNoteItem
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Sol üstten aşağı çekildiğinde One UI bildirim paneli yerine açılan iOS Cover Sheet
 * tarzı HNM Bildirim & Özet Merkezi.
 * Play Protect güvenli mimariyle çalışır; canlı saat/tarih, ses modu, pil özeti,
 * aktif bildirim kartları, hızlı hatırlatıcılar ve Fener/Kamera kısayollarını sunar.
 */
@Composable
fun NotificationCenterOverlay(
    visible: Boolean,
    liveNotifications: List<LiveNotificationCard>,
    quickNotes: List<QuickNoteItem>,
    suggestedApps: List<AppItem>,
    allApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onAddQuickNote: (String) -> Unit,
    onToggleQuickNote: (String) -> Unit,
    onDeleteQuickNote: (String) -> Unit,
    onDismissNotificationCard: (String) -> Unit,
    onClearAllNotificationCards: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenSystemShadeFallback: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val trLocale = remember { Locale("tr", "TR") }
    val clockFmt = remember { DateTimeFormatter.ofPattern("HH:mm", trLocale) }
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMMM EEEE", trLocale) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    var battery by remember {
        mutableStateOf(
            com.hnmlabs.hnmui.core.system.BatterySnapshot(
                levelPercent = 100,
                isCharging = false,
                sourceLabel = "Pil Kullanımda",
                healthLabel = "İyi (%100 Normal)",
                temperatureCelsius = 28.0f,
                voltageVolts = 4.1f,
                currentMilliAmps = 0,
                technology = "Li-ion",
                isPowerSaveMode = false
            )
        )
    }
    var ringerMode by remember { mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL) }
    var isTorchOn by remember { mutableStateOf(false) }
    var draftNote by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            now = LocalDateTime.now()
            val (bat, ringer) = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                DeviceSystemController.readBatterySnapshot(context) to
                    DeviceSystemController.readAudioSnapshot(context).ringerMode
            }
            battery = bat
            ringerMode = ringer
        }
    }

    val appByPackage = remember(allApps) {
        allApps.associateBy { it.key.packageName }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleIn(initialScale = 0.97f, animationSpec = HnmMotion.sheetSpring()),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleOut(targetScale = 0.97f, animationSpec = HnmMotion.sheetSpring())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC070C17),
                            Color(0xDB090F1D),
                            Color(0xEB070B14)
                        )
                    )
                )
                .pointerInput(Unit) {
                    var upwardDragPx = 0f
                    var dismissedInGesture = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            upwardDragPx = 0f
                            dismissedInGesture = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!change.isConsumed && !dismissedInGesture) {
                                upwardDragPx += dragAmount
                                if (upwardDragPx < -90f) {
                                    dismissedInGesture = true
                                    change.consume()
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .hnmSafeTopPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Üst sağ/sol geçiş hapları (Denetim Merkezi'ne hızlı geçiş & isteğe bağlı sistem çekmecesi)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable {
                            onDismiss()
                            onOpenSystemShadeFallback()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = null,
                                tint = HnmPalette.TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Sistem Bildirimleri",
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.TextSecondary
                            )
                        }
                    }

                    GlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable {
                            onOpenControlCenter()
                        }
                    ) {
                        Text(
                            text = "Denetim Merkezi →",
                            style = MaterialTheme.typography.labelSmall,
                            color = HnmPalette.AccentCyan,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // iOS Kilit Ekranı / Cover Sheet Tipografik Saat ve Tarih Başlığı
                Text(
                    text = now.format(dateFmt).replaceFirstChar { it.titlecase(trLocale) },
                    style = MaterialTheme.typography.titleMedium,
                    color = HnmPalette.TextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = now.format(clockFmt),
                    color = Color.White,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = (-1.5).sp,
                    lineHeight = 68.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Minimalist canlı durum kapsülleri (Pil & Ses Modu)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        tone = GlassTone.PILL
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (battery.isCharging) {
                                    Icons.Rounded.BatteryChargingFull
                                } else {
                                    Icons.Rounded.BatteryStd
                                },
                                contentDescription = null,
                                tint = if (battery.isCharging) HnmPalette.SuccessGreen else Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "%${battery.levelPercent} • ${battery.temperatureCelsius}°C",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    val ringerLabel = when (ringerMode) {
                        AudioManager.RINGER_MODE_VIBRATE -> "Titreşim"
                        AudioManager.RINGER_MODE_SILENT -> "Sessiz"
                        else -> "Sesli"
                    }
                    val ringerIcon = when (ringerMode) {
                        AudioManager.RINGER_MODE_VIBRATE -> Icons.Rounded.Vibration
                        AudioManager.RINGER_MODE_SILENT -> Icons.AutoMirrored.Rounded.VolumeOff
                        else -> Icons.AutoMirrored.Rounded.VolumeUp
                    }

                    GlassSurface(
                        shape = RoundedCornerShape(18.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val next = if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                                AudioManager.RINGER_MODE_VIBRATE
                            } else {
                                AudioManager.RINGER_MODE_NORMAL
                            }
                            DeviceSystemController.setRingerMode(context, next)
                            ringerMode = DeviceSystemController.readAudioSnapshot(context).ringerMode
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = ringerIcon,
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = ringerLabel,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Akış Listesi: Canlı Bildirim Kartları + Hatırlatıcılar + Hızlı Devam Et
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (liveNotifications.isNotEmpty()) {
                        item(key = "notif_header") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "BİLDİRİMLER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HnmPalette.TextMuted
                                )
                                Text(
                                    text = "Tümünü Temizle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HnmPalette.AccentCyan,
                                    modifier = Modifier.clickable { onClearAllNotificationCards() }
                                )
                            }
                        }

                        items(liveNotifications, key = { it.key }) { card ->
                            val matchedApp = appByPackage[card.packageName]
                            GlassSurface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (matchedApp != null) {
                                            onDismiss()
                                            onLaunchApp(matchedApp)
                                        }
                                    },
                                shape = RoundedCornerShape(22.dp),
                                tone = GlassTone.ELEVATED_MODAL
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (matchedApp != null) {
                                        MiniAppIconPreview(
                                            app = matchedApp,
                                            size = 40.dp,
                                            cachedIconProvider = cachedIconProvider,
                                            loadIconSuspend = loadIconSuspend
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(SquircleShape.Icon)
                                                .background(Color(0x2EFFFFFF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.NotificationsNone,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = card.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (card.message.isNotEmpty()) {
                                            Text(
                                                text = card.message,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = HnmPalette.TextSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onDismissNotificationCard(card.key) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Kapat",
                                            tint = HnmPalette.TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Hızlı Hatırlatıcı & Not Yığını (iOS Bildirim Merkezi Odak Kartı)
                    item(key = "quick_reminders_card") {
                        GlassSurface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = SquircleShape.Card,
                            tone = GlassTone.ELEVATED_MODAL
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = " Anlık Hatırlatıcılar",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${quickNotes.count { !it.isDone }} aktif",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HnmPalette.AccentCyan
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x1FFFFFFF))
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (draftNote.isEmpty()) {
                                            Text(
                                                text = "Buraya hızlı not veya hatırlatıcı bırak...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = HnmPalette.TextMuted
                                            )
                                        }
                                        BasicTextField(
                                            value = draftNote,
                                            onValueChange = { draftNote = it },
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.White
                                            ),
                                            cursorBrush = SolidColor(HnmPalette.AccentCyan),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(
                                                onDone = {
                                                    if (draftNote.isNotBlank()) {
                                                        onAddQuickNote(draftNote)
                                                        draftNote = ""
                                                        focusManager.clearFocus()
                                                    }
                                                }
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (draftNote.isNotBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(HnmPalette.AccentBlue)
                                                .clickable {
                                                    onAddQuickNote(draftNote)
                                                    draftNote = ""
                                                    focusManager.clearFocus()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Add,
                                                contentDescription = "Ekle",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                }

                                quickNotes.take(4).forEach { note ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onToggleQuickNote(note.id) }
                                            .padding(vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (note.isDone) {
                                                Icons.Rounded.CheckCircle
                                            } else {
                                                Icons.Rounded.RadioButtonUnchecked
                                            },
                                            contentDescription = null,
                                            tint = if (note.isDone) {
                                                HnmPalette.SuccessGreen
                                            } else {
                                                HnmPalette.AccentCyan
                                            },
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = note.text,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                textDecoration = if (note.isDone) {
                                                    TextDecoration.LineThrough
                                                } else {
                                                    TextDecoration.None
                                                }
                                            ),
                                            color = if (note.isDone) {
                                                HnmPalette.TextMuted
                                            } else {
                                                Color.White
                                            },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Sil",
                                            tint = HnmPalette.TextMuted,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable { onDeleteQuickNote(note.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Son Kullanılan / Önerilen Uygulamalar Kartı
                    if (suggestedApps.isNotEmpty()) {
                        item(key = "suggested_apps_card") {
                            GlassSurface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = SquircleShape.Card,
                                tone = GlassTone.CARD
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Hızlı Geçiş",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        suggestedApps.take(4).forEach { app ->
                                            AppIconTile(
                                                app = app,
                                                iconSize = 52.dp,
                                                showLabel = true,
                                                cachedIconProvider = cachedIconProvider,
                                                loadIconSuspend = loadIconSuspend,
                                                onClick = {
                                                    onDismiss()
                                                    onLaunchApp(app)
                                                },
                                                onLongClick = {}
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Alt iOS Cover Sheet Yuvarlak Fener + Kapatma Çubuğu + Kamera Düğmeleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CoverSheetCornerCircle(
                        icon = Icons.Rounded.FlashlightOn,
                        active = isTorchOn,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val next = !isTorchOn
                            if (DeviceSystemController.toggleFlashlight(context, next)) {
                                isTorchOn = next
                            }
                        }
                    )

                    Column(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss
                            )
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Ana Ekrana Dön",
                            style = MaterialTheme.typography.labelSmall,
                            color = HnmPalette.TextMuted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(118.dp)
                                .height(4.5.dp)
                                .clip(CircleShape)
                                .background(Color(0x99FFFFFF))
                        )
                    }

                    CoverSheetCornerCircle(
                        icon = Icons.Rounded.CameraAlt,
                        active = false,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            val cam = allApps.firstOrNull {
                                it.key.packageName.lowercase().contains("camera") ||
                                    it.label.lowercase().contains("kamera")
                            }
                            onDismiss()
                            if (cam != null) onLaunchApp(cam)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverSheetCornerCircle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    val bg by animateColorAsState(
        targetValue = if (active) Color(0xEAFFFFFF) else Color(0x28FFFFFF),
        animationSpec = HnmMotion.slotHighlightSpring(),
        label = "coverCornerBg"
    )
    val scale by animateFloatAsState(
        targetValue = if (active) 1.06f else 1f,
        animationSpec = HnmMotion.iconPressSpring(),
        label = "coverCornerScale"
    )

    Box(
        modifier = Modifier
            .size(50.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color(0xFF0F172A) else Color.White,
            modifier = Modifier.size(23.dp)
        )
    }
}
