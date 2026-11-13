package com.hnmlabs.hnmui.ui.overlays

import android.app.Activity
import android.media.AudioManager
import android.view.KeyEvent
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CellTower
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LightMode
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnmlabs.hnmui.core.system.ConnectivitySnapshot
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.HnmThemeMode
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Ana ekranda sağ üstten aşağı çekildiğinde açılan iOS tarzı buzlu cam Denetim Merkezi.
 * One UI bildirim/kontrol ekranına ihtiyaç duymadan parlaklık, ses, el feneri,
 * döndürme kilidi, canlı Koyu/Açık görünüm geçişi, medya oynatıcı ve bağlantı kısayollarını yönetir.
 */
@Composable
fun ControlCenterOverlay(
    visible: Boolean,
    themeMode: HnmThemeMode,
    allApps: List<AppItem>,
    onSetThemeMode: (HnmThemeMode) -> Unit,
    onOpenNotificationCenter: () -> Unit,
    onOpenHnmSettings: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val haptics = LocalHapticFeedback.current
    val colors = HnmTheme.colors
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)

    var connectivity by remember {
        mutableStateOf(
            ConnectivitySnapshot(
                isWifiConnected = true,
                isCellularConnected = false,
                isAirplaneMode = false,
                isBluetoothEnabled = false
            )
        )
    }
    var brightness by remember { mutableFloatStateOf(0.6f) }
    var volume by remember { mutableFloatStateOf(0.5f) }
    var isRotationLocked by remember { mutableStateOf(false) }
    val isTorchOn by DeviceSystemController.isTorchEnabled.collectAsStateWithLifecycle()

    val calculatorApp = remember(allApps) {
        allApps.firstOrNull {
            it.asciiPackage.contains("calc") || it.foldedLabel.contains("hesap")
        }
    }
    val cameraApp = remember(allApps) {
        allApps.firstOrNull {
            it.asciiPackage.contains("camera") || it.foldedLabel.contains("kamera")
        }
    }

    suspend fun refreshSystemState() {
        DeviceSystemController.ensureTorchObserver(context)
        val winBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val (net, disp, aud) = withContext(Dispatchers.IO) {
            Triple(
                DeviceSystemController.readConnectivitySnapshot(context),
                DeviceSystemController.readDisplaySnapshot(context, null),
                DeviceSystemController.readAudioSnapshot(context)
            )
        }
        connectivity = net
        brightness = if (winBrightness in 0.01f..1.0f && !disp.canWriteSystemSettings) {
            winBrightness.coerceIn(0.02f, 1f)
        } else {
            disp.brightnessFraction
        }
        isRotationLocked = disp.isRotationLocked
        volume = aud.mediaFraction
    }

    LaunchedEffect(visible) {
        if (visible) {
            refreshSystemState()
        }
    }

    DisposableEffect(lifecycleOwner, visible) {
        val observer = LifecycleEventObserver { _, event ->
            if (visible && event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch { refreshSystemState() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleIn(initialScale = 0.96f, animationSpec = HnmMotion.sheetSpring()),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleOut(targetScale = 0.96f, animationSpec = HnmMotion.sheetSpring())
    ) {
        // Kapanış animasyonu sürerken dokunma olaylarını anında serbest bırak (tekrar açılışta 0 takılma!)
        val backdropTouchModifier = if (visible) {
            Modifier
                .pointerInput(Unit) {
                    var upwardSwipePx = 0f
                    var dismissedInGesture = false
                    detectVerticalDragGestures(
                        onDragStart = {
                            upwardSwipePx = 0f
                            dismissedInGesture = false
                        },
                        onVerticalDrag = { change, dragAmount ->
                            if (!change.isConsumed && !dismissedInGesture) {
                                upwardSwipePx += dragAmount
                                if (upwardSwipePx < -85f) {
                                    dismissedInGesture = true
                                    change.consume()
                                    currentOnDismiss()
                                }
                            }
                        }
                    )
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
        } else {
            Modifier
        }

        val sheetAbsorbClickModifier = if (visible) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.controlCenterBackdrop)
                .then(backdropTouchModifier)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .hnmSafeTopPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .then(sheetAbsorbClickModifier),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Denetim Merkezi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "← Bildirim Merkezi",
                            style = MaterialTheme.typography.labelSmall,
                            color = HnmPalette.AccentCyan,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { onOpenNotificationCenter() }
                        )
                    }
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
                                tint = colors.textPrimary,
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
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(CircleShape)
                                        .background(colors.controlInactiveFill)
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
                                        tint = colors.textPrimary,
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
                                        tint = colors.textPrimary,
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
                                    DeviceSystemController.toggleFlashlight(context, !isTorchOn)
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
                                    if (calculatorApp != null) onLaunchApp(calculatorApp) else onOpenHnmSettings()
                                }
                            )
                            QuickToggleTile(
                                icon = Icons.Rounded.CameraAlt,
                                label = "Kamera",
                                active = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (cameraApp != null) onLaunchApp(cameraApp) else onDismiss()
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

                // Alt Satır: Profesyonel Canlı Görünüm Geçiş Çubuğu (Sistem / Açık / Koyu)
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    tone = GlassTone.ELEVATED_MODAL
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Görünüm Modu",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (colors.isDark) "Gece Camı Aktif" else "Gün Işığı Camı Aktif",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.accentCyan
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modeItems = listOf(
                                Triple(HnmThemeMode.SYSTEM, "Otomatik", Icons.Rounded.BrightnessAuto),
                                Triple(HnmThemeMode.LIGHT, "Açık", Icons.Rounded.LightMode),
                                Triple(HnmThemeMode.DARK, "Koyu", Icons.Rounded.DarkMode)
                            )
                            modeItems.forEach { (mode, label, iconVec) ->
                                val selected = themeMode == mode
                                val chipBg by animateColorAsState(
                                    targetValue = if (selected) colors.accentBlue else colors.surfaceSubtleFill,
                                    animationSpec = HnmMotion.slotHighlightSpring(),
                                    label = "ccThemeChipBg_${mode.name}"
                                )
                                val chipFg = if (selected) Color.White else colors.textSecondary
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(chipBg)
                                        .clickable {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSetThemeMode(mode)
                                        }
                                        .padding(vertical = 9.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = iconVec,
                                        contentDescription = label,
                                        tint = chipFg,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.size(5.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = chipFg,
                                        maxLines = 1
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

@Composable
private fun ControlCircleButton(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    val colors = HnmTheme.colors
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (active) activeColor else colors.controlInactiveFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (active) Color.White else colors.textPrimary,
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
    val colors = HnmTheme.colors
    val activeTintBg = if (colors.isDark) Color(0x5538BDF8) else Color(0x24007AFF)
    val activeIconTint = if (colors.isDark) Color.White else colors.accentBlue
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
                .background(if (active) activeTintBg else Color.Transparent)
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) activeIconTint else colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (active && !colors.isDark) colors.accentBlue else colors.textPrimary,
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
    val colors = HnmTheme.colors
    var heightPx by remember { mutableFloatStateOf(1f) }
    val currentOnFractionChange by rememberUpdatedState(onFractionChange)
    val safeFraction = if (fraction.isFinite()) fraction else 0.5f
    val clamped = safeFraction.coerceIn(0f, 1f)

    GlassSurface(
        modifier = modifier
            .onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        val next = 1f - (offset.y / heightPx.coerceAtLeast(1f))
                        if (next.isFinite()) currentOnFractionChange(next.coerceIn(0f, 1f))
                    },
                    onVerticalDrag = { change, _ ->
                        change.consume()
                        val next = 1f - (change.position.y / heightPx.coerceAtLeast(1f))
                        if (next.isFinite()) currentOnFractionChange(next.coerceIn(0f, 1f))
                    }
                )
            },
        shape = RoundedCornerShape(28.dp),
        tone = GlassTone.ELEVATED_MODAL
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surfaceSubtleFill)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(clamped)
                    .align(Alignment.BottomCenter)
                    .background(colors.verticalSliderFill)
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (clamped > 0.24f) colors.verticalSliderIconActive else colors.verticalSliderIconIdle,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(24.dp)
            )
        }
    }
}
