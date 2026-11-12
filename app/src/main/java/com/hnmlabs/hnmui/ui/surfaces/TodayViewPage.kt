package com.hnmlabs.hnmui.ui.surfaces

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.hnmlabs.hnmui.BuildConfig
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.QuickNoteItem
import com.hnmlabs.hnmui.ui.components.AppIconTile
import com.hnmlabs.hnmui.ui.components.GlassSurface
import com.hnmlabs.hnmui.ui.components.GlassTone
import com.hnmlabs.hnmui.ui.components.hnmSafeTopPadding
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.widget.LauncherWidgetHostManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * En soldaki sayfa (Sayfa 0): Today View / Özet & Widget Ekranı.
 * Tarih özeti, sistem kurulum kısayolları, sık kullanılan öneriler, hızlı notlar
 * ve yerel Android Widget barındırma kartlarını içerir.
 */
@Composable
fun TodayViewPage(
    isDefaultLauncher: Boolean,
    isNotificationAccessEnabled: Boolean,
    isTabletHardwareDetected: Boolean,
    targetRefreshRateHz: Int,
    suggestedApps: List<AppItem>,
    installedAppCount: Int,
    totalUnreadBadges: Int,
    quickNotes: List<QuickNoteItem>,
    boundWidgetIds: List<Int>,
    widgetHostManager: LauncherWidgetHostManager,
    cachedIconProvider: (AppComponentKey) -> ImageBitmap?,
    loadIconSuspend: suspend (AppComponentKey) -> ImageBitmap?,
    onRequestDefaultLauncher: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onAddQuickNote: (String) -> Unit,
    onToggleQuickNote: (String) -> Unit,
    onDeleteQuickNote: (String) -> Unit,
    onPickWidget: () -> Unit,
    onRemoveWidget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    val trLocale = remember { Locale("tr", "TR") }
    val dateLabel = remember(now) {
        now.format(DateTimeFormatter.ofPattern("d MMMM EEEE", trLocale))
            .replaceFirstChar { it.titlecase(trLocale) }
    }
    val greeting = remember(now) {
        when (now.hour) {
            in 5..11 -> "Günaydın"
            in 12..17 -> "İyi Günler"
            in 18..22 -> "İyi Akşamlar"
            else -> "İyi Geceler"
        }
    }

    val batteryInfo = remember { readBatteryStatus(context) }
    var noteDraft by remember { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .hnmSafeTopPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = dateLabel.uppercase(trLocale),
                    style = MaterialTheme.typography.labelMedium,
                    color = HnmPalette.TextSecondary
                )
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineLarge
                )
                if (isTabletHardwareDetected) {
                    Text(
                        text = "Telefon uyumluluk profili aktif • Tablet ızgarası rezerve",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HnmPalette.AccentCyan
                    )
                }
            }
        }

        val showNotificationBanner =
            BuildConfig.SUPPORTS_NOTIFICATION_LISTENER && !isNotificationAccessEnabled

        if (!isDefaultLauncher || showNotificationBanner) {
            item(key = "setup_banners") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!isDefaultLauncher) {
                        SetupActionCard(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Home,
                                    contentDescription = null,
                                    tint = HnmPalette.AccentCyan
                                )
                            },
                            title = "HNMUI'yi Varsayılan Yap",
                            subtitle = "Ana ekran tuşu ve uygulama kısayolları için etkinleştirin",
                            actionLabel = "Seç",
                            onClick = onRequestDefaultLauncher
                        )
                    }
                    if (showNotificationBanner) {
                        SetupActionCard(
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsActive,
                                    contentDescription = null,
                                    tint = HnmPalette.BadgeRed
                                )
                            },
                            title = "Canlı Bildirim Rozetleri",
                            subtitle = "İkon üzerinde okunmamış bildirim sayısını görmek için izin verin",
                            actionLabel = "İzin Ver",
                            onClick = onRequestNotificationAccess
                        )
                    }
                }
            }
        }

        item(key = "glance_metrics") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GlassSurface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    tone = GlassTone.CARD
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (batteryInfo.second) {
                                    Icons.Rounded.BatteryChargingFull
                                } else {
                                    Icons.Rounded.BatteryStd
                                },
                                contentDescription = null,
                                tint = HnmPalette.SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Pil Durumu",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "%${batteryInfo.first}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (batteryInfo.second) {
                                "Şarj oluyor • ${targetRefreshRateHz} Hz"
                            } else {
                                "${targetRefreshRateHz} Hz akıcı mod"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = HnmPalette.TextMuted
                        )
                    }
                }

                GlassSurface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    tone = GlassTone.CARD
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (BuildConfig.SUPPORTS_NOTIFICATION_LISTENER) {
                                    Icons.Rounded.NotificationsActive
                                } else {
                                    Icons.Rounded.Widgets
                                },
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (BuildConfig.SUPPORTS_NOTIFICATION_LISTENER) {
                                    "Bildirimler"
                                } else {
                                    "Uygulamalar"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = if (BuildConfig.SUPPORTS_NOTIFICATION_LISTENER) {
                                totalUnreadBadges.toString()
                            } else {
                                installedAppCount.toString()
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (BuildConfig.SUPPORTS_NOTIFICATION_LISTENER) {
                                if (totalUnreadBadges == 0) "Her şey güncel" else "Okunmamış bildirim"
                            } else {
                                "Arşivde hazır"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = HnmPalette.TextMuted
                        )
                    }
                }
            }
        }

        if (suggestedApps.isNotEmpty()) {
            item(key = "suggested_card") {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    tone = GlassTone.CARD
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "HNM Önerileri",
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

        item(key = "quick_notes") {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                tone = GlassTone.CARD
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Hızlı Notlar",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0x1FFFFFFF))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (noteDraft.isEmpty()) {
                                Text(
                                    text = "Yeni not veya hatırlatma ekle...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = HnmPalette.TextMuted
                                )
                            }
                            BasicTextField(
                                value = noteDraft,
                                onValueChange = { noteDraft = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White
                                ),
                                cursorBrush = SolidColor(HnmPalette.AccentCyan),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (noteDraft.isNotBlank()) {
                                            onAddQuickNote(noteDraft)
                                            noteDraft = ""
                                        }
                                    }
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(HnmPalette.AccentBlue)
                                .clickable {
                                    if (noteDraft.isNotBlank()) {
                                        onAddQuickNote(noteDraft)
                                        noteDraft = ""
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Not ekle",
                                tint = Color.White
                            )
                        }
                    }

                    quickNotes.forEach { note ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleQuickNote(note.id) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (note.isDone) {
                                    Icons.Rounded.CheckCircle
                                } else {
                                    Icons.Rounded.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (note.isDone) HnmPalette.SuccessGreen else HnmPalette.TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = note.text,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (note.isDone) {
                                        TextDecoration.LineThrough
                                    } else {
                                        TextDecoration.None
                                    },
                                    color = if (note.isDone) {
                                        HnmPalette.TextMuted
                                    } else {
                                        HnmPalette.TextPrimary
                                    }
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteQuickNote(note.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Sil",
                                    tint = HnmPalette.TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "widgets_section") {
            GlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                tone = GlassTone.CARD
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                imageVector = Icons.Rounded.Widgets,
                                contentDescription = null,
                                tint = HnmPalette.AccentCyan
                            )
                            Text(
                                text = "Android Widget'ları",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0x2EFFFFFF))
                                .clickable(onClick = onPickWidget)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+ Widget Ekle",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (boundWidgetIds.isEmpty()) {
                        Text(
                            text = "Hava durumu, takvim veya müzik gibi favori Android widget'larınızı buraya sabitleyebilirsiniz.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HnmPalette.TextMuted
                        )
                    } else {
                        boundWidgetIds.forEach { widgetId ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .padding(vertical = 4.dp)
                            ) {
                                AndroidView(
                                    factory = { ctx ->
                                        widgetHostManager.createHostView(ctx, widgetId)
                                            ?: android.widget.FrameLayout(ctx)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(140.dp)
                                )
                                IconButton(
                                    onClick = { onRemoveWidget(widgetId) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x99000000))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Widget kaldır",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
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
private fun SetupActionCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        tone = GlassTone.CARD
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x24FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = HnmPalette.TextMuted
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(HnmPalette.AccentBlue)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(text = actionLabel, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private fun readBatteryStatus(context: Context): Pair<Int, Boolean> {
    val bm = context.getSystemService(BatteryManager::class.java)
    val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)?.coerceIn(0, 100) ?: 100
    val charging = bm?.isCharging ?: false
    return level to charging
}
