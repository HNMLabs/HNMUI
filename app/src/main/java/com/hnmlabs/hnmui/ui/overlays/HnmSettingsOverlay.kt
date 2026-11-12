package com.hnmlabs.hnmui.ui.overlays

import android.app.Activity
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hnmlabs.hnmui.core.system.DefaultLauncherHelper
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.data.InstalledAppsRepository
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.LauncherCustomizationSettings
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.MiniAppIconPreview
import com.hnmlabs.hnmui.ui.components.SquircleShape
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmMotion
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import java.util.Locale
import kotlin.math.roundToInt

enum class SettingsRoute(val titleTr: String) {
    ROOT("Ayarlar"),
    CUSTOMIZATION("Ana Ekran, Klasör & Dock"),
    CONNECTIVITY("Wi-Fi ve Bağlantılar"),
    DISPLAY_BRIGHTNESS("Ekran ve Parlaklık"),
    SOUND_HAPTICS("Ses ve Dokunuş"),
    BATTERY("Pil ve Sağlık"),
    STORAGE("Saklama Alanı"),
    APPS("Uygulamalar"),
    GENERAL_ABOUT("Genel ve Hakkında")
}

/**
 * Samsung One UI'dan tamamen bağımsız, iOS sadeliğinde ve buzlu cam dokusunda çalışan
 * tam ekran HNM Ayarlar deneyimi.
 */
@Composable
fun HnmSettingsOverlay(
    visible: Boolean,
    customization: LauncherCustomizationSettings,
    isDefaultLauncher: Boolean,
    targetRefreshRateHz: Int,
    allApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onUpdateCustomization: ((LauncherCustomizationSettings) -> LauncherCustomizationSettings) -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onDismiss: () -> Unit
) {
    var currentRoute by remember { mutableStateOf(SettingsRoute.ROOT) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (!visible) {
            currentRoute = SettingsRoute.ROOT
            searchQuery = ""
        }
    }

    BackHandler(enabled = visible) {
        if (currentRoute != SettingsRoute.ROOT) {
            currentRoute = SettingsRoute.ROOT
        } else {
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = HnmMotion.sheetSpring()) +
            slideInVertically(
                initialOffsetY = { it / 10 },
                animationSpec = HnmMotion.sheetSpring()
            ),
        exit = fadeOut(animationSpec = HnmMotion.sheetSpring()) +
            slideOutVertically(
                targetOffsetY = { it / 10 },
                animationSpec = HnmMotion.sheetSpring()
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xEB0B1220), Color(0xF5070C16))
                    )
                )
                .hnmSafeTopPadding()
                .navigationBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentRoute,
                transitionSpec = {
                    if (targetState != SettingsRoute.ROOT) {
                        (slideInHorizontally(animationSpec = HnmMotion.sheetSpring()) { it / 3 } +
                            fadeIn(animationSpec = HnmMotion.sheetSpring())) togetherWith
                            (slideOutHorizontally(animationSpec = HnmMotion.sheetSpring()) { -it / 5 } +
                                fadeOut(animationSpec = HnmMotion.sheetSpring()))
                    } else {
                        (slideInHorizontally(animationSpec = HnmMotion.sheetSpring()) { -it / 5 } +
                            fadeIn(animationSpec = HnmMotion.sheetSpring())) togetherWith
                            (slideOutHorizontally(animationSpec = HnmMotion.sheetSpring()) { it / 3 } +
                                fadeOut(animationSpec = HnmMotion.sheetSpring()))
                    }
                },
                label = "settingsRouteTransition"
            ) { route ->
                when (route) {
                    SettingsRoute.ROOT -> SettingsRootPage(
                        customization = customization,
                        isDefaultLauncher = isDefaultLauncher,
                        targetRefreshRateHz = targetRefreshRateHz,
                        installedAppCount = allApps.size,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigate = { currentRoute = it },
                        onClose = onDismiss
                    )

                    SettingsRoute.CUSTOMIZATION -> CustomizationSubPage(
                        customization = customization,
                        isDefaultLauncher = isDefaultLauncher,
                        onUpdateCustomization = onUpdateCustomization,
                        onRequestDefaultLauncher = onRequestDefaultLauncher,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.CONNECTIVITY -> ConnectivitySubPage(
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.DISPLAY_BRIGHTNESS -> DisplayBrightnessSubPage(
                        targetRefreshRateHz = targetRefreshRateHz,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.SOUND_HAPTICS -> SoundHapticsSubPage(
                        customization = customization,
                        onUpdateCustomization = onUpdateCustomization,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.BATTERY -> BatterySubPage(
                        targetRefreshRateHz = targetRefreshRateHz,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.STORAGE -> StorageSubPage(
                        allApps = allApps,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend,
                        onLaunchApp = onLaunchApp,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.APPS -> AppsManagerSubPage(
                        allApps = allApps,
                        cachedIconProvider = cachedIconProvider,
                        loadIconSuspend = loadIconSuspend,
                        onLaunchApp = onLaunchApp,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )

                    SettingsRoute.GENERAL_ABOUT -> GeneralAboutSubPage(
                        customization = customization,
                        targetRefreshRateHz = targetRefreshRateHz,
                        onUpdateCustomization = onUpdateCustomization,
                        onBack = { currentRoute = SettingsRoute.ROOT }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsRootPage(
    customization: LauncherCustomizationSettings,
    isDefaultLauncher: Boolean,
    targetRefreshRateHz: Int,
    installedAppCount: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigate: (SettingsRoute) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val trLocale = remember { Locale("tr", "TR") }
    val battery = remember { DeviceSystemController.readBatterySnapshot(context) }
    val connectivity = remember { DeviceSystemController.readConnectivitySnapshot(context) }
    val storage = remember { DeviceSystemController.readStorageAndMemorySnapshot(context) }
    val about = remember { DeviceSystemController.readDeviceAboutSnapshot(context, targetRefreshRateHz) }

    val q = searchQuery.trim().lowercase(trLocale)
    fun matches(vararg terms: String): Boolean {
        if (q.isEmpty()) return true
        return terms.any { it.lowercase(trLocale).contains(q) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "root_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ayarlar",
                    style = MaterialTheme.typography.headlineLarge
                )
                GlassSurface(
                    shape = RoundedCornerShape(18.dp),
                    tone = GlassTone.PILL,
                    modifier = Modifier.clickable(onClick = onClose)
                ) {
                    Text(
                        text = "Ana Ekran",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        item(key = "root_search") {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tone = GlassTone.CARD
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = HnmPalette.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Pil, 7x7 ızgara, Dock, ses, depolama ara...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HnmPalette.TextMuted
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                            cursorBrush = SolidColor(HnmPalette.AccentCyan),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Temizle",
                            tint = HnmPalette.TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }
            }
        }

        if (q.isEmpty()) {
            item(key = "device_profile_card") {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(SettingsRoute.GENERAL_ABOUT) },
                    shape = RoundedCornerShape(24.dp),
                    tone = GlassTone.CARD
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(HnmPalette.AccentBlue, HnmPalette.AccentCyan)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = about.deviceName.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = about.deviceName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${targetRefreshRateHz} Hz • ${customization.gridColumns}×${customization.gridRows} Izgara • " +
                                    if (isDefaultLauncher) "Varsayılan Launcher" else "Kurulum Bekliyor",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HnmPalette.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = HnmPalette.TextMuted
                        )
                    }
                }
            }
        }

        if (matches("ana ekran", "ızgara", "7x7", "klasör", "dock", "ikon", "boyut", "cam", "duvar kağıdı")) {
            item(key = "group_customization") {
                SettingsGroupCard(title = "ARAYÜZ VE MİNİMALİST ÖZELLEŞTİRME") {
                    SettingsNavRow(
                        icon = Icons.Rounded.GridView,
                        badgeColor = HnmPalette.AccentBlue,
                        title = "Ana Ekran, Klasör & Dock",
                        subtitle = "7×7 ızgara, klasör dizilimi ve Dock boyutu",
                        valueLabel = "${customization.gridColumns}×${customization.gridRows}",
                        onClick = { onNavigate(SettingsRoute.CUSTOMIZATION) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Rounded.Wallpaper,
                        badgeColor = Color(0xFF8B5CF6),
                        title = "Duvar Kağıdını Değiştir",
                        subtitle = "Sistem canlı ve statik duvar kağıdı",
                        valueLabel = null,
                        onClick = { DefaultLauncherHelper.openWallpaperPicker(context) }
                    )
                }
            }
        }

        if (matches("wifi", "wi-fi", "bluetooth", "internet", "hücresel", "ağ", "uçak")) {
            item(key = "group_connectivity") {
                SettingsGroupCard(title = "BAĞLANTILAR") {
                    SettingsNavRow(
                        icon = Icons.Rounded.Wifi,
                        badgeColor = HnmPalette.AccentBlue,
                        title = "Wi-Fi ve Hücresel Ağ",
                        subtitle = "Canlı bağlantı durumu ve hızlı ağ paneli",
                        valueLabel = when {
                            connectivity.isWifiConnected -> "Wi-Fi"
                            connectivity.isCellularConnected -> "Hücresel"
                            else -> "Kapalı"
                        },
                        onClick = { onNavigate(SettingsRoute.CONNECTIVITY) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Rounded.Bluetooth,
                        badgeColor = Color(0xFF2563EB),
                        title = "Bluetooth",
                        subtitle = "Kablosuz kulaklık ve aksesuar durumu",
                        valueLabel = if (connectivity.isBluetoothEnabled) "Açık" else "Kapalı",
                        onClick = { onNavigate(SettingsRoute.CONNECTIVITY) }
                    )
                }
            }
        }

        if (matches("ekran", "parlaklık", "120", "ses", "titreşim", "pil", "batarya", "şarj", "sağlık")) {
            item(key = "group_hardware") {
                SettingsGroupCard(title = "EKRAN, SES VE PİL") {
                    SettingsNavRow(
                        icon = Icons.Rounded.BrightnessHigh,
                        badgeColor = Color(0xFFF59E0B),
                        title = "Ekran ve Parlaklık",
                        subtitle = "Parlaklık, ekran süresi ve yön kilidi",
                        valueLabel = "${targetRefreshRateHz} Hz",
                        onClick = { onNavigate(SettingsRoute.DISPLAY_BRIGHTNESS) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.AutoMirrored.Rounded.VolumeUp,
                        badgeColor = Color(0xFFEC4899),
                        title = "Ses ve Dokunuş",
                        subtitle = "Zil, medya, alarm sesleri ve Haptic",
                        valueLabel = null,
                        onClick = { onNavigate(SettingsRoute.SOUND_HAPTICS) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = if (battery.isCharging) {
                            Icons.Rounded.BatteryChargingFull
                        } else {
                            Icons.Rounded.BatteryFull
                        },
                        badgeColor = HnmPalette.SuccessGreen,
                        title = "Pil ve Sağlık",
                        subtitle = "${battery.sourceLabel} • ${battery.temperatureCelsius} °C",
                        valueLabel = "%${battery.levelPercent}",
                        onClick = { onNavigate(SettingsRoute.BATTERY) }
                    )
                }
            }
        }

        if (matches("saklama", "depolama", "hafıza", "uygulamalar", "genel", "hakkında", "ram", "model")) {
            item(key = "group_system") {
                SettingsGroupCard(title = "SİSTEM VE DEPOLAMA") {
                    SettingsNavRow(
                        icon = Icons.Rounded.Storage,
                        badgeColor = Color(0xFF06B6D4),
                        title = "Saklama Alanı",
                        subtitle = "Kullanılan ve boş dahili depolama",
                        valueLabel = "${storage.usedStorageGb.roundToInt()} / ${storage.totalStorageGb.roundToInt()} GB",
                        onClick = { onNavigate(SettingsRoute.STORAGE) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Rounded.Apps,
                        badgeColor = Color(0xFF6366F1),
                        title = "Uygulamalar",
                        subtitle = "Tüm yüklü uygulamaları ara ve yönet",
                        valueLabel = installedAppCount.toString(),
                        onClick = { onNavigate(SettingsRoute.APPS) }
                    )
                    SettingsDivider()
                    SettingsNavRow(
                        icon = Icons.Rounded.Info,
                        badgeColor = Color(0xFF64748B),
                        title = "Genel ve Hakkında",
                        subtitle = "${about.modelName} • Android ${about.androidVersion} • RAM",
                        valueLabel = null,
                        onClick = { onNavigate(SettingsRoute.GENERAL_ABOUT) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomizationSubPage(
    customization: LauncherCustomizationSettings,
    isDefaultLauncher: Boolean,
    onUpdateCustomization: ((LauncherCustomizationSettings) -> LauncherCustomizationSettings) -> Unit,
    onRequestDefaultLauncher: () -> Unit,
    onBack: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Ana Ekran, Klasör & Dock", onBack = onBack) }

        item {
            SettingsGroupCard(title = "HIZLI IZGARA PROFİLLERİ") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Tek dokunuşla hem ana ekran hem klasör yoğunluğunu seçin",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HnmPalette.TextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val presets = listOf(
                            Triple("4×6 Klasik", 4, 6),
                            Triple("5×6 Dengeli", 5, 6),
                            Triple("6×6 Yoğun", 6, 6),
                            Triple("7×7 Maks", 7, 7)
                        )
                        presets.forEach { (label, c, r) ->
                            val selected = customization.gridColumns == c && customization.gridRows == r
                            SegmentedChoicePill(
                                label = label,
                                selected = selected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onUpdateCustomization {
                                        it.copy(gridColumns = c, gridRows = r)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "ANA EKRAN UYGULAMA IZGARASI (4×4 – 7×7)") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Yatay Sütun Sayısı", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (col in 4..7) {
                                NumberPill(
                                    numberText = "$col",
                                    selected = customization.gridColumns == col,
                                    onClick = {
                                        onUpdateCustomization { it.copy(gridColumns = col) }
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dikey Satır Sayısı", style = MaterialTheme.typography.bodyMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (row in 4..7) {
                                NumberPill(
                                    numberText = "$row",
                                    selected = customization.gridRows == row,
                                    onClick = {
                                        onUpdateCustomization { it.copy(gridRows = row) }
                                    }
                                )
                            }
                        }
                    }

                    SettingsDivider()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Uygulama İkon Boyutu", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "%${customization.iconScalePercent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.AccentCyan
                            )
                        }
                        HnmMinimalSlider(
                            value = customization.iconScalePercent.toFloat(),
                            valueRange = 80f..120f,
                            onValueChange = { next ->
                                onUpdateCustomization {
                                    it.copy(iconScalePercent = next.roundToInt())
                                }
                            }
                        )
                    }

                    SettingsToggleRow(
                        title = "Uygulama İsimlerini Göster",
                        subtitle = "7×7 ızgarada kapatarak saf minimalist görünüm elde edebilirsiniz",
                        checked = customization.showHomeLabels,
                        onCheckedChange = { checked ->
                            onUpdateCustomization { it.copy(showHomeLabels = checked) }
                        }
                    )
                }
            }
        }

        item {
            SettingsGroupCard(title = "KLASÖR GÖRÜNÜMÜ (3×3 – 7×7)") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Açık Klasör İkon Dizilimi",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (fCol in 3..7) {
                            SegmentedChoicePill(
                                label = "${fCol}×${fCol}",
                                selected = customization.folderColumns == fCol,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    onUpdateCustomization { it.copy(folderColumns = fCol) }
                                }
                            )
                        }
                    }

                    SettingsDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Klasör Kapağı Mini Önizleme",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Ana ekrandaki kapalı klasör kutusu içi",
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.TextMuted
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberPill(
                                numberText = "2×2",
                                selected = customization.folderPreviewColumns == 2,
                                onClick = {
                                    onUpdateCustomization { it.copy(folderPreviewColumns = 2) }
                                }
                            )
                            NumberPill(
                                numberText = "3×3",
                                selected = customization.folderPreviewColumns == 3,
                                onClick = {
                                    onUpdateCustomization { it.copy(folderPreviewColumns = 3) }
                                }
                            )
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "ALT DOCK ÖZELLEŞTİRME (KÜÇÜLT / BÜYÜT)") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SettingsToggleRow(
                        title = "Buzlu Cam Dock Çubuğunu Göster",
                        subtitle = "Ana ekranın altındaki sabit uygulama barı",
                        checked = customization.dockVisible,
                        onCheckedChange = { visible ->
                            onUpdateCustomization { it.copy(dockVisible = visible) }
                        }
                    )

                    if (customization.dockVisible) {
                        SettingsDivider()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Dock Boyutu (Küçült / Büyüt)", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "%${customization.dockScalePercent}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = HnmPalette.AccentCyan
                                )
                            }
                            HnmMinimalSlider(
                                value = customization.dockScalePercent.toFloat(),
                                valueRange = 75f..125f,
                                onValueChange = { next ->
                                    onUpdateCustomization {
                                        it.copy(dockScalePercent = next.roundToInt())
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Maksimum Dock İkonu", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (cnt in 3..7) {
                                    NumberPill(
                                        numberText = "$cnt",
                                        selected = customization.dockMaxItems == cnt,
                                        onClick = {
                                            onUpdateCustomization { it.copy(dockMaxItems = cnt) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "BUZLU CAM DOKUSU VE VARSAYILAN LAUNCHER") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Buzlu Cam Yoğunluğu", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "%${customization.glassOpacityPercent}",
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.AccentCyan
                            )
                        }
                        HnmMinimalSlider(
                            value = customization.glassOpacityPercent.toFloat(),
                            valueRange = 70f..135f,
                            onValueChange = { next ->
                                onUpdateCustomization {
                                    it.copy(glassOpacityPercent = next.roundToInt())
                                }
                            }
                        )
                    }

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "iOS Durum Çubuğu (One UI Üst Çubuğu Gizle)",
                        subtitle = "Sağ üstten Denetim Merkezi, sol üstten HNM Bildirim Merkezi iner",
                        checked = customization.useIosStatusBar,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(useIosStatusBar = enabled) }
                        }
                    )

                    SettingsDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onRequestDefaultLauncher)
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Home,
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan
                            )
                            Column {
                                Text("Varsayılan Ana Ekran", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = if (isDefaultLauncher) "HNMUI Aktif ve Kilitli" else "Etkinleştirmek için dokunun",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDefaultLauncher) HnmPalette.SuccessGreen else HnmPalette.AccentCyan
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = HnmPalette.TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatterySubPage(
    targetRefreshRateHz: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val battery = remember { DeviceSystemController.readBatterySnapshot(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Pil ve Sağlık", onBack = onBack) }

        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                tone = GlassTone.CARD
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "Mevcut Pil Düzeyi",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HnmPalette.TextMuted
                            )
                            Text(
                                text = "%${battery.levelPercent}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2830D158))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = battery.sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.SuccessGreen
                            )
                        }
                    }

                    // iOS tarzı yuvarlatılmış doluluk barı
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(22.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Color(0x26FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((battery.levelPercent / 100f).coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (battery.levelPercent <= 20 && !battery.isCharging) {
                                            listOf(HnmPalette.BadgeRed, Color(0xFFF59E0B))
                                        } else {
                                            listOf(HnmPalette.SuccessGreen, HnmPalette.AccentCyan)
                                        }
                                    )
                                )
                        )
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "CANLI PİL SAĞLIĞI VE TELEMETRİ") {
                InfoKeyValueRow("Pil Sağlığı Durumu", battery.healthLabel)
                SettingsDivider()
                InfoKeyValueRow("Pil Sıcaklığı", "${battery.temperatureCelsius} °C")
                SettingsDivider()
                InfoKeyValueRow("Hücre Voltajı", String.format(Locale.US, "%.2f V", battery.voltageVolts))
                SettingsDivider()
                InfoKeyValueRow("Pil Teknolojisi", battery.technology)
                SettingsDivider()
                InfoKeyValueRow(
                    "Sistem Güç Tasarrufu",
                    if (battery.isPowerSaveMode) "Açık" else "Kapalı (${targetRefreshRateHz} Hz Tam Akıcılık)"
                )
            }
        }
    }
}

@Composable
private fun DisplayBrightnessSubPage(
    targetRefreshRateHz: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var snapshot by remember {
        mutableStateOf(DeviceSystemController.readDisplaySnapshot(context, activity))
    }
    var brightness by remember { mutableFloatStateOf(snapshot.brightnessFraction) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Ekran ve Parlaklık", onBack = onBack) }

        if (!snapshot.canWriteSystemSettings) {
            item {
                GlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { DeviceSystemController.openWriteSettingsGrant(context) },
                    shape = RoundedCornerShape(22.dp),
                    tone = GlassTone.CARD
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = null,
                            tint = HnmPalette.AccentCyan
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sistem Geneli Parlaklık İzni", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Anlık parlaklık çalışıyor; tüm uygulamalarda kalıcı olması için tek seferlik onay verin",
                                style = MaterialTheme.typography.labelSmall,
                                color = HnmPalette.TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = HnmPalette.TextMuted
                        )
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "PARLAKLIK VE KARE HIZI") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Ekran Parlaklığı", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "%${(brightness * 100).roundToInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HnmPalette.AccentCyan
                        )
                    }
                    HnmMinimalSlider(
                        value = brightness,
                        valueRange = 0.03f..1f,
                        onValueChange = { next ->
                            brightness = next
                            DeviceSystemController.setBrightnessFraction(context, activity, next)
                        }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Otomatik Parlaklık",
                        subtitle = "Ortam ışığına göre parlaklığı dengeler",
                        checked = snapshot.isAutoBrightness,
                        onCheckedChange = { enabled ->
                            if (DeviceSystemController.setAutoBrightness(context, enabled)) {
                                snapshot = snapshot.copy(isAutoBrightness = enabled)
                            } else {
                                DeviceSystemController.openWriteSettingsGrant(context)
                            }
                        }
                    )

                    SettingsDivider()

                    SettingsToggleRow(
                        title = "Dikey Yön Kilidi",
                        subtitle = "Ekran döndürmeyi sabitler",
                        checked = snapshot.isRotationLocked,
                        onCheckedChange = { locked ->
                            if (DeviceSystemController.setRotationLocked(context, locked)) {
                                snapshot = snapshot.copy(isRotationLocked = locked)
                            } else {
                                DeviceSystemController.openWriteSettingsGrant(context)
                            }
                        }
                    )
                }
                SettingsDivider()
                InfoKeyValueRow("Aktif Ekran Akıcılığı", "${targetRefreshRateHz} Hz Yüksek Yenileme Hızı")
            }
        }

        item {
            SettingsGroupCard(title = "OTOMATİK KİLİT SÜRESİ") {
                val options = listOf(
                    15_000 to "15 sn",
                    30_000 to "30 sn",
                    60_000 to "1 dk",
                    120_000 to "2 dk",
                    300_000 to "5 dk",
                    600_000 to "10 dk"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    options.forEach { (ms, label) ->
                        SegmentedChoicePill(
                            label = label,
                            selected = snapshot.screenTimeoutMs == ms,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (DeviceSystemController.setScreenTimeoutMs(context, ms)) {
                                    snapshot = snapshot.copy(screenTimeoutMs = ms)
                                } else {
                                    DeviceSystemController.openWriteSettingsGrant(context)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundHapticsSubPage(
    customization: LauncherCustomizationSettings,
    onUpdateCustomization: ((LauncherCustomizationSettings) -> LauncherCustomizationSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val initial = remember { DeviceSystemController.readAudioSnapshot(context) }
    var ringerMode by remember { mutableIntStateOf(initial.ringerMode) }
    var mediaVol by remember { mutableFloatStateOf(initial.mediaFraction) }
    var ringVol by remember { mutableFloatStateOf(initial.ringFraction) }
    var alarmVol by remember { mutableFloatStateOf(initial.alarmFraction) }
    var callVol by remember { mutableFloatStateOf(initial.callFraction) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Ses ve Dokunuş", onBack = onBack) }

        item {
            SettingsGroupCard(title = "SES PROFİLİ") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        AudioManager.RINGER_MODE_NORMAL to "Sesli",
                        AudioManager.RINGER_MODE_VIBRATE to "Titreşim",
                        AudioManager.RINGER_MODE_SILENT to "Sessiz"
                    )
                    modes.forEach { (mode, label) ->
                        SegmentedChoicePill(
                            label = label,
                            selected = ringerMode == mode,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                DeviceSystemController.setRingerMode(context, mode)
                                ringerMode = DeviceSystemController.readAudioSnapshot(context).ringerMode
                            }
                        )
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "SES DÜZEYLERİ") {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VolumeSliderItem("Medya ve Müzik", mediaVol) { next ->
                        mediaVol = next
                        DeviceSystemController.setStreamVolumeFraction(context, AudioManager.STREAM_MUSIC, next)
                    }
                    VolumeSliderItem("Zil ve Bildirimler", ringVol) { next ->
                        ringVol = next
                        DeviceSystemController.setStreamVolumeFraction(context, AudioManager.STREAM_RING, next)
                    }
                    VolumeSliderItem("Alarmlar", alarmVol) { next ->
                        alarmVol = next
                        DeviceSystemController.setStreamVolumeFraction(context, AudioManager.STREAM_ALARM, next)
                    }
                    VolumeSliderItem("Telefon Görüşmesi", callVol) { next ->
                        callVol = next
                        DeviceSystemController.setStreamVolumeFraction(context, AudioManager.STREAM_VOICE_CALL, next)
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "HAPTIC TOUCH") {
                Box(modifier = Modifier.padding(14.dp)) {
                    SettingsToggleRow(
                        title = "Sistem Dokunuş Titreşimi",
                        subtitle = "İkonlara uzun basıldığında ve geçişlerde dokunsal tepki",
                        checked = customization.hapticFeedbackEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(hapticFeedbackEnabled = enabled) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StorageSubPage(
    allApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val storage = remember { DeviceSystemController.readStorageAndMemorySnapshot(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Saklama Alanı", onBack = onBack) }

        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                tone = GlassTone.CARD
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Dahili Depolama", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.1f GB / %.0f GB Kullanılan",
                                storage.usedStorageGb,
                                storage.totalStorageGb
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = HnmPalette.TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Color(0x26FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(storage.storageUsedFraction.coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(HnmPalette.AccentBlue, Color(0xFF8B5CF6), HnmPalette.AccentCyan)
                                    )
                                )
                        )
                    }

                    Text(
                        text = String.format(Locale.US, "Kullanılabilir Boş Alan: %.1f GB", storage.freeStorageGb),
                        style = MaterialTheme.typography.labelSmall,
                        color = HnmPalette.SuccessGreen
                    )
                }
            }
        }

        item {
            Text(
                text = "YÜKLÜ UYGULAMALAR (${allApps.size})",
                style = MaterialTheme.typography.labelSmall,
                color = HnmPalette.TextMuted
            )
        }

        items(allApps.take(40), key = { it.key.stableId }) { app ->
            AppRowCard(
                app = app,
                cachedIconProvider = cachedIconProvider,
                loadIconSuspend = loadIconSuspend,
                onOpen = { onLaunchApp(app) },
                onUninstall = {
                    if (!app.isSystemApp && app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId) {
                        DefaultLauncherHelper.requestUninstall(context, app.key.packageName)
                    }
                }
            )
        }
    }
}

@Composable
private fun ConnectivitySubPage(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snap = remember { DeviceSystemController.readConnectivitySnapshot(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Wi-Fi ve Bağlantılar", onBack = onBack) }

        item {
            SettingsGroupCard(title = "CANLI AĞ DURUMU") {
                InfoKeyValueRow("Wi-Fi Bağlantısı", if (snap.isWifiConnected) "Bağlı" else "Bağlı Değil")
                SettingsDivider()
                InfoKeyValueRow("Hücresel Veri", if (snap.isCellularConnected) "Aktif" else "Beklemede")
                SettingsDivider()
                InfoKeyValueRow("Bluetooth", if (snap.isBluetoothEnabled) "Açık" else "Kapalı")
                SettingsDivider()
                InfoKeyValueRow("Uçak Modu", if (snap.isAirplaneMode) "Açık" else "Kapalı")
            }
        }

        item {
            SettingsGroupCard(title = "HIZLI AĞ SEÇİMİ (ONE UI'SIZ ALT PANEL)") {
                SettingsNavRow(
                    icon = Icons.Rounded.Wifi,
                    badgeColor = HnmPalette.AccentBlue,
                    title = "Wi-Fi ve İnternet Panelini Aç",
                    subtitle = "HNMUI'den çıkmadan ağ değiştir",
                    valueLabel = null,
                    onClick = { DeviceSystemController.openInternetMiniPanel(context) }
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Bluetooth,
                    badgeColor = Color(0xFF2563EB),
                    title = "Bluetooth Cihaz Eşleştir",
                    subtitle = "Kulaklık ve aksesuar bağlantısı",
                    valueLabel = null,
                    onClick = { DeviceSystemController.openBluetoothPanel(context) }
                )
            }
        }
    }
}

@Composable
private fun AppsManagerSubPage(
    allApps: List<AppItem>,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onLaunchApp: (AppItem) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val trLocale = remember { Locale("tr", "TR") }
    var filter by remember { mutableStateOf("") }

    val filtered = remember(allApps, filter) {
        val q = filter.trim().lowercase(trLocale)
        if (q.isEmpty()) allApps
        else allApps.filter {
            it.label.lowercase(trLocale).contains(q) ||
                it.key.packageName.lowercase(trLocale).contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SubPageHeader(title = "Uygulamalar (${filtered.size})", onBack = onBack) }

        item {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                tone = GlassTone.CARD
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    if (filter.isEmpty()) {
                        Text(
                            text = "Uygulama ara...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HnmPalette.TextMuted
                        )
                    }
                    BasicTextField(
                        value = filter,
                        onValueChange = { filter = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White),
                        cursorBrush = SolidColor(HnmPalette.AccentCyan),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        items(filtered, key = { it.key.stableId }) { app ->
            AppRowCard(
                app = app,
                cachedIconProvider = cachedIconProvider,
                loadIconSuspend = loadIconSuspend,
                onOpen = { onLaunchApp(app) },
                onUninstall = {
                    if (!app.isSystemApp && app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId) {
                        DefaultLauncherHelper.requestUninstall(context, app.key.packageName)
                    }
                }
            )
        }
    }
}

@Composable
private fun GeneralAboutSubPage(
    customization: LauncherCustomizationSettings,
    targetRefreshRateHz: Int,
    onUpdateCustomization: ((LauncherCustomizationSettings) -> LauncherCustomizationSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val about = remember { DeviceSystemController.readDeviceAboutSnapshot(context, targetRefreshRateHz) }
    val mem = remember { DeviceSystemController.readStorageAndMemorySnapshot(context) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SubPageHeader(title = "Genel ve Hakkında", onBack = onBack) }

        item {
            SettingsGroupCard(title = "CANLI BELLEK (RAM) DURUMU") {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Memory,
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan
                            )
                            Text("Fiziksel RAM Kullanımı", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = String.format(Locale.US, "%.1f / %.1f GB", mem.usedRamGb, mem.totalRamGb),
                            style = MaterialTheme.typography.labelSmall,
                            color = HnmPalette.AccentCyan
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x26FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(mem.ramUsedFraction.coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .background(HnmPalette.AccentCyan)
                        )
                    }
                }
            }
        }

        item {
            SettingsGroupCard(title = "CİHAZ BİLGİLERİ") {
                InfoKeyValueRow("Cihaz Adı", about.deviceName)
                SettingsDivider()
                InfoKeyValueRow("Üretici / Model", "${about.manufacturer} ${about.modelName}")
                SettingsDivider()
                InfoKeyValueRow("Android Sürümü", "Android ${about.androidVersion} (API ${about.apiLevel})")
                SettingsDivider()
                InfoKeyValueRow("Güvenlik Yaması", about.securityPatch)
                SettingsDivider()
                InfoKeyValueRow("İşlemci", "${about.processorLabel} (${about.cpuCores} Çekirdek)")
                SettingsDivider()
                InfoKeyValueRow("Ekran & Akıcılık", about.resolutionLabel)
                SettingsDivider()
                InfoKeyValueRow("Çalışma Süresi", about.uptimeLabel)
                SettingsDivider()
                InfoKeyValueRow("HNMUI Sürümü", "0.2.0-alpha")
            }
        }

        item {
            SettingsGroupCard(title = "AYARLAR YÖNLENDİRME VE ÇEKİRDEK KÖPRÜSÜ") {
                Box(modifier = Modifier.padding(14.dp)) {
                    SettingsToggleRow(
                        title = "Ayarlar İkonunu HNM Ayarlar Olarak Aç",
                        subtitle = "Telefonun Ayarlar ikonuna basıldığında One UI yerine bu ekran açılır",
                        checked = customization.interceptSystemSettingsApp,
                        onCheckedChange = { enabled ->
                            onUpdateCustomization { it.copy(interceptSystemSettingsApp = enabled) }
                        }
                    )
                }
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Rounded.Settings,
                    badgeColor = Color(0xFF475569),
                    title = "Gelişmiş Android Çekirdek Ayarları",
                    subtitle = "Yalnızca SIM, Parmak İzi veya Geliştirici Seçenekleri gerektiğinde",
                    valueLabel = null,
                    onClick = { DefaultLauncherHelper.openSystemSettings(context) }
                )
            }
        }
    }
}

@Composable
private fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlassSurface(
            shape = RoundedCornerShape(14.dp),
            tone = GlassTone.PILL,
            modifier = Modifier.clickable(onClick = onBack)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                    contentDescription = "Geri",
                    tint = HnmPalette.AccentCyan,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "Ayarlar",
                    style = MaterialTheme.typography.labelSmall,
                    color = HnmPalette.AccentCyan
                )
            }
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = HnmPalette.TextMuted,
            modifier = Modifier.padding(start = 8.dp)
        )
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            tone = GlassTone.CARD
        ) {
            Column(modifier = Modifier.fillMaxWidth(), content = content)
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    badgeColor: Color,
    title: String,
    subtitle: String,
    valueLabel: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(SquircleShape.Icon)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = HnmPalette.TextMuted)
        }
        if (valueLabel != null) {
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = HnmPalette.TextMuted
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = HnmPalette.TextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun InfoKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = HnmPalette.TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = HnmPalette.TextMuted)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = HnmPalette.SuccessGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0x38FFFFFF)
            )
        )
    }
}

@Composable
private fun SegmentedChoicePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) HnmPalette.AccentBlue else Color(0x1FFFFFFF))
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) Color.White else HnmPalette.TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
private fun NumberPill(
    numberText: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) HnmPalette.AccentBlue else Color(0x22FFFFFF))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = numberText,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
private fun HnmMinimalSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = HnmPalette.AccentBlue,
            inactiveTrackColor = Color(0x30FFFFFF)
        )
    )
}

@Composable
private fun VolumeSliderItem(
    label: String,
    fraction: Float,
    onChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "%${(fraction * 100).roundToInt()}",
                style = MaterialTheme.typography.labelSmall,
                color = HnmPalette.TextMuted
            )
        }
        HnmMinimalSlider(value = fraction, valueRange = 0f..1f, onValueChange = onChange)
    }
}

@Composable
private fun AppRowCard(
    app: AppItem,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onOpen: () -> Unit,
    onUninstall: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        tone = GlassTone.CARD
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniAppIconPreview(
                app = app,
                size = 40.dp,
                cachedIconProvider = cachedIconProvider,
                loadIconSuspend = loadIconSuspend
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.label, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(text = app.category.titleTr, style = MaterialTheme.typography.labelSmall, color = HnmPalette.TextMuted)
            }
            if (!app.isSystemApp && app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId) {
                IconButton(onClick = onUninstall, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Kaldır",
                        tint = HnmPalette.BadgeRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 58.dp),
        thickness = 0.6.dp,
        color = Color(0x1FFFFFFF)
    )
}
