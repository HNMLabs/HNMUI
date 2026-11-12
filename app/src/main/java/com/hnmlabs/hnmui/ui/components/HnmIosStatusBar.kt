package com.hnmlabs.hnmui.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SignalCellular4Bar
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * One UI sistem durum çubuğu gizlendiğinde HNMUI iOS durum çubuğunun ayrıldığı güvenli üst yükseklik.
 */
val LocalHnmTopBarHeight = compositionLocalOf { 0.dp }

/**
 * Hem sistem durum çubuğu açıkken hem de HNMUI iOS durum çubuğu (kamera deliği / cutout dahil)
 * aktifken tüm sayfa ve katmanlara güvenli üst boşluk uygular.
 */
fun Modifier.hnmSafeTopPadding(): Modifier = composed {
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val cutoutTop = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
    val customBarTop = LocalHnmTopBarHeight.current
    val effectiveTop = max(max(statusTop, cutoutTop), customBarTop)
    this.padding(top = effectiveTop)
}

/**
 * Samsung One UI durum çubuğu ikonları yerine ana ekranın en üstünde yer alan minimalist iOS Durum Çubuğu.
 * - Sol kulak: Canlı saat (dokunma veya aşağı çekme -> HNM Bildirim Merkezi)
 * - Sağ kulak: Hücresel + Wi-Fi + iOS Pil kapsülü ve ince Denetim Merkezi çizgisi (dokunma/çekme -> Denetim Merkezi)
 */
@Composable
fun HnmIosStatusBar(
    visible: Boolean,
    onTapLeftNotifications: () -> Unit,
    onTapRightControlCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var timeText by remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var isWifi by remember { mutableStateOf(true) }

    DisposableEffect(context) {
        fun refreshStatus(batteryIntent: Intent? = null) {
            timeText = LocalTime.now().format(timeFormatter)
            val net = DeviceSystemController.readConnectivitySnapshot(context)
            isWifi = net.isWifiConnected

            val sticky = batteryIntent ?: context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) {
                    batteryPercent = ((level * 100f) / scale).toInt().coerceIn(0, 100)
                }
                val status = sticky.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }

        refreshStatus()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshStatus(
                    batteryIntent = intent?.takeIf { it.action == Intent.ACTION_BATTERY_CHANGED }
                )
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter)
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val barHeight: Dp = max(cutoutPadding.calculateTopPadding(), 36.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol kulak: iOS Saat (Bildirim Merkezi tetikleyicisi)
        Row(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapLeftNotifications
                )
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = timeText,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }

        // Sağ kulak: Sinyal + Wi-Fi + iOS Pil kapsülü + Denetim Merkezi mikro çizgisi
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapRightControlCenter
                )
                .padding(vertical = 2.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.SignalCellular4Bar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                if (isWifi) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
                IosBatteryPill(
                    percent = batteryPercent,
                    isCharging = isCharging
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(24.dp)
                    .height(2.2.dp)
                    .clip(CircleShape)
                    .background(Color(0x78FFFFFF))
            )
        }
    }
}

@Composable
private fun IosBatteryPill(
    percent: Int,
    isCharging: Boolean
) {
    val pillBg = when {
        isCharging -> HnmPalette.SuccessGreen
        percent <= 18 -> HnmPalette.BadgeRed
        else -> Color(0xEBFFFFFF)
    }
    val textColor = if (isCharging || percent <= 18) Color.White else Color(0xFF0B1220)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .height(15.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(pillBg)
                .padding(horizontal = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$percent",
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 10.sp
            )
        }
        Spacer(modifier = Modifier.width(1.5.dp))
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp))
                .background(Color(0x99FFFFFF))
        )
    }
}
