package com.hnmlabs.hnmui.ui.overlays

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.core.text.SpotlightMathEngine
import com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.HnmThemeMode
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.HnmTheme
import kotlinx.coroutines.delay
import kotlin.math.abs

@Immutable
private data class SpotlightSystemQuickAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val badgeText: String?,
    val icon: ImageVector,
    val accentColor: Color,
    val onInvoke: () -> Unit
)

/**
 * Ana ekranda aşağı kaydırıldığında, boş alana çift dokunulduğunda veya "Ara" hapına basıldığında
 * açılan akıllı iOS Spotlight 2.0 katmanı:
 * - Anlık matematik & KDV/yüzde hesaplayıcı (`1250 + %20`, `(480+320)/4`, `2^10`) ve tek dokunuş kopyalama
 * - Canlı sistem denetimleri (Fener aç/kapat, Koyu/Açık mod değiştir, Wi-Fi/BT mini panel, Ayarlar alt rotaları)
 * - Kullanım sıklığı ağırlıklı "En İyi Eşleşme" uygulama sıralaması ve kaydırırken otomatik klavye gizleme
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpotlightOverlay(
    visible: Boolean,
    query: String,
    themeMode: HnmThemeMode,
    suggestedApps: List<AppItem>,
    matchedApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onQueryChange: (String) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onSearchWeb: (String) -> Unit,
    onSetThemeMode: (HnmThemeMode) -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenSettingsRoute: (SettingsRoute) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = HnmTheme.colors
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val haptics = LocalHapticFeedback.current
    val isTorchOn by DeviceSystemController.isTorchEnabled.collectAsStateWithLifecycle()

    var copiedExpression by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(copiedExpression) {
        if (copiedExpression != null) {
            delay(1_800L)
            copiedExpression = null
        }
    }

    LaunchedEffect(visible) {
        if (visible) {
            DeviceSystemController.ensureTorchObserver(context)
            delay(85L)
            runCatching { focusRequester.requestFocus() }
        } else {
            copiedExpression = null
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val mathResult = remember(query) {
        SpotlightMathEngine.evaluate(query)
    }

    fun copyMathResultToClipboard(copyText: String, exprKey: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("HNMUI Hesaplama", copyText))
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        copiedExpression = exprKey
    }

    val trimmedQuery = query.trim()
    val systemActions = remember(trimmedQuery, isTorchOn, themeMode, colors.isDark) {
        if (trimmedQuery.length < 2) {
            emptyList()
        } else {
            val prepared = TurkishSearchNormalizer.prepareQuery(trimmedQuery)
            fun matchesAny(vararg keywords: String): Boolean = keywords.any { kw ->
                TurkishSearchNormalizer.containsPreFolded(
                    foldedTarget = TurkishSearchNormalizer.foldTurkishI(kw),
                    asciiTarget = TurkishSearchNormalizer.foldAscii(kw),
                    prepared = prepared
                )
            }

            buildList {
                if (matchesAny("fener", "el feneri", "ışık", "flashlight", "torch")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_torch",
                            title = if (isTorchOn) "El Fenerini Kapat" else "El Fenerini Aç",
                            subtitle = "HNM Denetim Kısayolu",
                            badgeText = if (isTorchOn) "AÇIK" else "KAPALI",
                            icon = Icons.Rounded.FlashlightOn,
                            accentColor = Color(0xFFF59E0B),
                            onInvoke = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                DeviceSystemController.toggleFlashlight(context, !isTorchOn)
                            }
                        )
                    )
                }
                if (matchesAny("koyu", "açık", "gece", "gündüz", "tema", "görünüm", "dark", "light")) {
                    val nextMode = themeMode.nextMode()
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_theme",
                            title = "Görünüm Modu: ${themeMode.shortTitleTr}",
                            subtitle = "Dokununca \"${nextMode.titleTr}\" moduna geçer",
                            badgeText = if (colors.isDark) "GECE CAMI" else "GÜN IŞIĞI",
                            icon = Icons.Rounded.DarkMode,
                            accentColor = colors.accentBlue,
                            onInvoke = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSetThemeMode(nextMode)
                            }
                        )
                    )
                }
                if (matchesAny("denetim", "kontrol merkezi", "parlaklık", "control center")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_cc",
                            title = "Denetim Merkezi'ni Aç",
                            subtitle = "Parlaklık, ses, medya ve hızlı bağlantı kartları",
                            badgeText = null,
                            icon = Icons.Rounded.Tune,
                            accentColor = colors.accentCyan,
                            onInvoke = onOpenControlCenter
                        )
                    )
                }
                if (matchesAny("ızgara", "7x7", "dock", "klasör", "ikon", "çift dokun", "ana ekran")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_grid",
                            title = "Ana Ekran, Klasör & Dock Ayarları",
                            subtitle = "4×4–7×7 ızgara, ikon boyutu, cam yoğunluğu ve çift dokunuş",
                            badgeText = "AYARLAR",
                            icon = Icons.Rounded.GridView,
                            accentColor = colors.accentBlue,
                            onInvoke = { onOpenSettingsRoute(SettingsRoute.CUSTOMIZATION) }
                        )
                    )
                }
                if (matchesAny("wifi", "wi-fi", "internet", "hücresel", "ağ")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_wifi",
                            title = "Wi-Fi ve İnternet Paneli",
                            subtitle = "Launcher'dan çıkmadan hızlı ağ penceresini aç",
                            badgeText = null,
                            icon = Icons.Rounded.Wifi,
                            accentColor = colors.accentBlue,
                            onInvoke = {
                                onDismiss()
                                DeviceSystemController.openInternetMiniPanel(context)
                            }
                        )
                    )
                }
                if (matchesAny("bluetooth", "kulaklık", "aksesuar")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_bt",
                            title = "Bluetooth Ayarları",
                            subtitle = "Kablosuz kulaklık ve cihaz bağlantıları",
                            badgeText = null,
                            icon = Icons.Rounded.Bluetooth,
                            accentColor = Color(0xFF2563EB),
                            onInvoke = {
                                onDismiss()
                                DeviceSystemController.openBluetoothPanel(context)
                            }
                        )
                    )
                }
                if (matchesAny("pil", "batarya", "şarj", "sağlık", "battery")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_battery",
                            title = "Pil ve Sağlık Telemetrisi",
                            subtitle = "Sıcaklık, voltaj ve pil sağlığı durumu",
                            badgeText = "AYARLAR",
                            icon = Icons.Rounded.BatteryChargingFull,
                            accentColor = colors.successGreen,
                            onInvoke = { onOpenSettingsRoute(SettingsRoute.BATTERY) }
                        )
                    )
                }
                if (matchesAny("saklama", "depolama", "hafıza", "ram", "bellek", "storage")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_storage",
                            title = "Saklama Alanı ve RAM",
                            subtitle = "Dahili depolama ve canlı bellek kullanımı",
                            badgeText = "AYARLAR",
                            icon = Icons.Rounded.Storage,
                            accentColor = Color(0xFF06B6D4),
                            onInvoke = { onOpenSettingsRoute(SettingsRoute.STORAGE) }
                        )
                    )
                }
                if (matchesAny("ses", "titreşim", "zil", "alarm", "haptic")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_sound",
                            title = "Ses ve Dokunuş",
                            subtitle = "Medya, zil, alarm düzeyi ve Haptic geri bildirim",
                            badgeText = "AYARLAR",
                            icon = Icons.AutoMirrored.Rounded.VolumeUp,
                            accentColor = Color(0xFFEC4899),
                            onInvoke = { onOpenSettingsRoute(SettingsRoute.SOUND_HAPTICS) }
                        )
                    )
                }
                if (matchesAny("duvar", "wallpaper", "arka plan")) {
                    add(
                        SpotlightSystemQuickAction(
                            id = "sys_wallpaper",
                            title = "Duvar Kağıdını Değiştir",
                            subtitle = "Canlı veya statik sistem duvar kağıdı seç",
                            badgeText = null,
                            icon = Icons.Rounded.Wallpaper,
                            accentColor = Color(0xFF8B5CF6),
                            onInvoke = {
                                onDismiss()
                                DefaultLauncherHelper.openWallpaperPicker(context)
                            }
                        )
                    )
                }
            }.take(3)
        }
    }

    val scrollHideKeyboardConnection = remember(keyboardController) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && abs(available.y) > 6f) {
                    keyboardController?.hide()
                }
                return Offset.Zero
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { -it / 7 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleIn(initialScale = 0.97f, animationSpec = HnmMotion.sheetSpring()),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { -it / 7 },
                animationSpec = HnmMotion.sheetOffsetSpring()
            ) +
            scaleOut(targetScale = 0.97f, animationSpec = HnmMotion.sheetSpring())
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.scrimOverlay)
                .then(
                    if (visible) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss
                        )
                    } else {
                        Modifier
                    }
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
                    shape = RoundedCornerShape(22.dp),
                    tone = GlassTone.ELEVATED_MODAL,
                    shadowElevation = 14.dp
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        placeholder = {
                            Text(
                                text = "Uygulama, işlem (1250+%20) veya ayar ara",
                                color = colors.textSecondary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = colors.accentCyan
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
                                    tint = colors.textSecondary
                                )
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val topMatch = matchedApps.firstOrNull()
                                when {
                                    topMatch != null -> onLaunchApp(topMatch)
                                    mathResult != null -> copyMathResultToClipboard(
                                        copyText = mathResult.copyableValue,
                                        exprKey = mathResult.normalizedExpression
                                    )
                                    systemActions.isNotEmpty() -> systemActions.first().onInvoke()
                                    query.isNotBlank() -> onSearchWeb(query)
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            cursorColor = colors.accentBlue
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollHideKeyboardConnection),
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

                        item(key = "spotlight_quick_hub") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                GlassSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onOpenSettingsRoute(SettingsRoute.ROOT) },
                                    shape = RoundedCornerShape(20.dp),
                                    tone = GlassTone.CARD
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Settings,
                                            contentDescription = null,
                                            tint = HnmPalette.AccentCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "HNM Ayarlar",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "7×7 & Görünüm",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }

                                GlassSurface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable(onClick = onOpenControlCenter),
                                    shape = RoundedCornerShape(20.dp),
                                    tone = GlassTone.CARD
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Tune,
                                            contentDescription = null,
                                            tint = colors.accentBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Denetim",
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "Hızlı Kontroller",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // 1) Anlık Hesap Makinesi Kartı (KDV, yüzde, üs, parantezli dört işlem)
                        if (mathResult != null) {
                            item(key = "instant_math_card") {
                                val isCopied = copiedExpression == mathResult.normalizedExpression
                                GlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.2.dp,
                                            color = colors.accentCyan.copy(alpha = 0.55f),
                                            shape = RoundedCornerShape(22.dp)
                                        )
                                        .clickable {
                                            copyMathResultToClipboard(
                                                copyText = mathResult.copyableValue,
                                                exprKey = mathResult.normalizedExpression
                                            )
                                        },
                                    shape = RoundedCornerShape(22.dp),
                                    tone = GlassTone.ELEVATED_MODAL
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Calculate,
                                                    contentDescription = null,
                                                    tint = colors.accentCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "${mathResult.normalizedExpression} =",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = colors.textSecondary
                                                )
                                            }
                                            Text(
                                                text = mathResult.formattedResult,
                                                style = MaterialTheme.typography.headlineLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        }

                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isCopied) {
                                                        colors.successGreen.copy(alpha = 0.20f)
                                                    } else {
                                                        colors.surfaceSubtleFill
                                                    }
                                                )
                                                .padding(horizontal = 11.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCopied) {
                                                    Icons.Rounded.Check
                                                } else {
                                                    Icons.Rounded.ContentCopy
                                                },
                                                contentDescription = null,
                                                tint = if (isCopied) colors.successGreen else colors.accentCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = if (isCopied) "Kopyalandı" else "Kopyala",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isCopied) colors.successGreen else colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 2) Hızlı Sistem Eylemleri ve Ayar Rotaları
                        if (systemActions.isNotEmpty()) {
                            item(key = "system_actions_header") {
                                Text(
                                    text = "HIZLI DENETİM & AYARLAR",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HnmPalette.TextSecondary
                                )
                            }

                            systemActions.forEach { action ->
                                item(key = action.id) {
                                    GlassSurface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(onClick = action.onInvoke),
                                        shape = RoundedCornerShape(18.dp),
                                        tone = GlassTone.ELEVATED_MODAL
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(action.accentColor.copy(alpha = 0.18f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = action.icon,
                                                    contentDescription = null,
                                                    tint = action.accentColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = action.title,
                                                    style = MaterialTheme.typography.titleSmall
                                                )
                                                Text(
                                                    text = action.subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.textMuted,
                                                    maxLines = 1
                                                )
                                            }
                                            if (action.badgeText != null) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(colors.surfaceSubtleFill)
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = action.badgeText,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colors.accentCyan
                                                    )
                                                }
                                            } else {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                                    contentDescription = null,
                                                    tint = colors.textMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3) Uygulama Eşleşmeleri (En İyi Eşleşme Vurgulu)
                        if (matchedApps.isNotEmpty()) {
                            item(key = "matched_header") {
                                Text(
                                    text = "UYGULAMALAR (${matchedApps.size})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HnmPalette.TextSecondary
                                )
                            }

                            itemsIndexed(
                                items = matchedApps.take(12),
                                key = { _, app -> app.key.stableId }
                            ) { index, app ->
                                val isTopHit = index == 0
                                GlassSurface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isTopHit) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = colors.accentBlue.copy(alpha = 0.45f),
                                                    shape = RoundedCornerShape(18.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .combinedClickable(
                                            onClick = { onLaunchApp(app) },
                                            onLongClick = {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onAppLongPress(app)
                                            }
                                        ),
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
                                        if (isTopHit) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(colors.accentBlue.copy(alpha = 0.16f))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "En İyi ↵",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colors.accentBlue
                                                )
                                            }
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
