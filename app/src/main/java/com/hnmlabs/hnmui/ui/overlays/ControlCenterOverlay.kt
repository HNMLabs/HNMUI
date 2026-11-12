package com.hnmlabs.hnmui.ui.overlays

import android.app.Activity
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.ScreenLockPortrait
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette

/**
 * Ana ekranda sağ üstten aşağı çekildiğinde açılan iOS tarzı buzlu cam Denetim Merkezi.
 * One UI bildirim/kontrol ekranına ihtiyaç duymadan parlaklık, ses, el feneri,
 * döndürme kilidi, medya oynatıcı ve bağlantı kısayollarını yönetir.
 */
@Composable
fun ControlCenterOverlay(
    visible: Boolean,
    allApps: List<AppItem>,
    onOpenHnmSettings: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptics = LocalHapticFeedback.current

    var connectivity by remember { mutableStateOf(DeviceSystemController.readConnectivitySnapshot(context)) }
    var brightness by remember { mutableFloatStateOf(0.6f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var isRotationLocked by remember { mutableStateOf(false) }
    var isTorchOn by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            connectivity = DeviceSystemController.readConnectivitySnapshot(context)
            val disp = DeviceSystemController.readDisplaySnapshot(context, activity)
            brightness = disp.brightnessFraction
            isRotationLocked = disp.isRotationLocked
            val aud = DeviceSystemController.readAudioSnapshot(context)
            volume = aud.mediaFraction
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { -it / 3 },
                animationSpec = HnmMotion.sheetSpring()
            ),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { -it / 3 },
                animationSpec = HnmMotion.sheetSpring()
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xAD080D18))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Denetim Merkezi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    GlassSurface(
                        shape = RoundedCornerShape(16.dp),
                        tone = GlassTone.PILL,
                        modifier = Modifier.clickable {
                            onDismiss()
                            onOpenHnmSettings()
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "Ayarlar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(text = "Ayarlar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Üst Satır: 2x2 Bağlantı Kartı + Medya Oynatıcı Kartı
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GlassSurface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = SquircleShape.Card,
                        tone = GlassTone.ELEVATED_MODAL
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ControlCircleButton(
                                    icon = Icons.Rounded.AirplanemodeActive,
                                    active = connectivity.isAirplaneMode,
                                    activeColor = Color(0xFFF59E0B),
                                    onClick = {
                                        DeviceSystemController.openInternetMiniPanel(context)
                                    }
                                )
                                ControlCircleButton(
                                    icon = Icons.Rounded.CellTower,
                                    active = connectivity.isCellularConnected,
                                    activeColor = HnmPalette.SuccessGreen,
                                    onClick = {
                                        DeviceSystemController.openInternetMiniPanel(context)
                                    }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                ControlCircleButton(
                                    icon = Icons.Rounded.Wifi,
                                    active = connectivity.isWifiConnected,
                                    activeColor = HnmPalette.AccentBlue,
                                    onClick = {
                                        DeviceSystemController.openInternetMiniPanel(context)
                                    }
                                )
                                ControlCircleButton(
                                    icon = Icons.Rounded.Bluetooth,
                                    active = connectivity.isBluetoothEnabled,
                                    activeColor = HnmPalette.AccentBlue,
                                    onClick = {
                                        DeviceSystemController.openBluetoothPanel(context)
                                    }
                                )
                            }
                        }
                    }

                    GlassSurface(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        shape = SquircleShape.Card,
                        tone = GlassTone.ELEVATED_MODAL
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Medya Kontrolü",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Anında Oynat / Değiştir",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HnmPalette.TextMuted
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        DeviceSystemController.dispatchMediaKey(
                                            context,
                                            KeyEvent.KEYCODE_MEDIA_PREVIOUS
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipPrevious,
                                        contentDescription = "Önceki",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x2EFFFFFF))
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            DeviceSystemController.dispatchMediaKey(
                                                context,
                                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Oynat / Duraklat",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        DeviceSystemController.dispatchMediaKey(
                                            context,
                                            KeyEvent.KEYCODE_MEDIA_NEXT
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SkipNext,
                                        contentDescription = "Sonraki",
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Orta Satır: 2x2 Hızlı Araçlar + Dikey iOS Parlaklık ve Ses Sürgüleri
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickToggleTile(
                                icon = Icons.Rounded.FlashlightOn,
                                label = "Fener",
                                active = isTorchOn,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val next = !isTorchOn
                                    if (DeviceSystemController.toggleFlashlight(context, next)) {
                                        isTorchOn = next
                                    }
                                }
                            )
                            QuickToggleTile(
                                icon = Icons.Rounded.ScreenLockPortrait,
                                label = "Yön Kilidi",
                                active = isRotationLocked,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val next = !isRotationLocked
                                    if (DeviceSystemController.setRotationLocked(context, next)) {
                                        isRotationLocked = next
                                    } else {
                                        DeviceSystemController.openWriteSettingsGrant(context)
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            QuickToggleTile(
                                icon = Icons.Rounded.Calculate,
                                label = "Hesapla",
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val calc = allApps.firstOrNull {
                                        it.key.packageName.lowercase().contains("calc") ||
                                            it.label.lowercase().contains("hesap")
                                    }
                                    if (calc != null) onLaunchApp(calc) else onOpenHnmSettings()
                                }
                            )
                            QuickToggleTile(
                                icon = Icons.Rounded.CameraAlt,
                                label = "Kamera",
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val cam = allApps.firstOrNull {
                                        it.key.packageName.lowercase().contains("camera") ||
                                            it.label.lowercase().contains("kamera")
                                    }
                                    if (cam != null) onLaunchApp(cam) else onDismiss()
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        VerticalGlassSlider(
                            fraction = brightness,
                            icon = Icons.Rounded.BrightnessHigh,
                            onFractionChange = { next ->
                                brightness = next
                                DeviceSystemController.setBrightnessFraction(context, activity, next)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        VerticalGlassSlider(
                            fraction = volume,
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            onFractionChange = { next ->
                                volume = next
                                DeviceSystemController.setStreamVolumeFraction(
                                    context,
                                    AudioManager.STREAM_MUSIC,
                                    next
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlCircleButton(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (active) activeColor else Color(0x26FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun QuickToggleTile(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tone = if (active) GlassTone.PILL else GlassTone.ELEVATED_MODAL
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (active) Color(0x5538BDF8) else Color.Transparent)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color.White else HnmPalette.TextPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun VerticalGlassSlider(
    fraction: Float,
    icon: ImageVector,
    onFractionChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableFloatStateOf(1f) }
    val clamped = fraction.coerceIn(0f, 1f)

    GlassSurface(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val next = 1f - (offset.y / heightPx)
                        onFractionChange(next.coerceIn(0f, 1f))
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val next = 1f - (change.position.y / heightPx)
                        onFractionChange(next.coerceIn(0f, 1f))
                    }
                )
            },
        shape = RoundedCornerShape(28.dp),
        tone = GlassTone.ELEVATED_MODAL
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped)
                    .align(Alignment.BottomCenter)
                    .background(Color(0xCCFFFFFF))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (clamped > 0.24f) Color(0xFF0F172A) else Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(24.dp)
            )
        }
    }
}
