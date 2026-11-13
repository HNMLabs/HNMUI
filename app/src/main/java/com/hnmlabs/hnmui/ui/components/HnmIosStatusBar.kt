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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.TextStyle
import com.hnmlabs.hnmui.core.system.DeviceSystemController
import com.hnmlabs.hnmui.ui.theme.HnmPalette
import com.hnmlabs.hnmui.ui.theme.WallpaperLabelShadow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
 * - Sol yarı (tam genişlik dokunma/çekme alanı): Canlı saat -> HNM Bildirim Merkezi
 * - Sağ yarı (tam genişlik dokunma/çekme alanı): Hücresel + Wi-Fi + iOS Pil + tutamaç -> Denetim Merkezi
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
    val scope = rememberCoroutineScope()
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    var timeText by remember { mutableStateOf(LocalTime.now().format(timeFormatter)) }
    var batteryPercent by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }
    var isWifi by remember { mutableStateOf(true) }

    suspend fun refreshStatusSuspend(batteryIntent: Intent? = null) {
        val formattedTime = LocalTime.now().format(timeFormatter)
        val (wifiState, pct, charging) = withContext(Dispatchers.IO) {
            val net = DeviceSystemController.readConnectivitySnapshot(context)
            val sticky = batteryIntent ?: runCatching {
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            }.getOrNull()

            var resolvedPct = batteryPercent
            var resolvedCharging = isCharging
            if (sticky != null) {
                val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                if (level >= 0) {
                    resolvedPct = ((level * 100f) / scale).toInt().coerceIn(0, 100)
                }
                val status = sticky.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )
                resolvedCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
            Triple(net.isWifiConnected, resolvedPct, resolvedCharging)
        }
        timeText = formattedTime
        isWifi = wifiState
        batteryPercent = pct
        isCharging = charging
    }

    LaunchedEffect(context) {
        refreshStatusSuspend()
    }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val batIntent = intent?.takeIf { it.action == Intent.ACTION_BATTERY_CHANGED }
                scope.launch {
                    refreshStatusSuspend(batteryIntent = batIntent)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        val registered = runCatching {
            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            true
        }.getOrDefault(false)

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
        val networkCallback = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                scope.launch { refreshStatusSuspend() }
            }

            override fun onLost(network: android.net.Network) {
                scope.launch { refreshStatusSuspend() }
            }

            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: android.net.NetworkCapabilities
            ) {
                scope.launch { refreshStatusSuspend() }
            }
        }
        val networkRegistered = if (connectivityManager != null) {
            runCatching {
                connectivityManager.registerDefaultNetworkCallback(networkCallback)
                true
            }.getOrDefault(false)
        } else {
            false
        }

        onDispose {
            if (registered) {
                runCatching { context.unregisterReceiver(receiver) }
            }
            if (networkRegistered && connectivityManager != null) {
                runCatching { connectivityManager.unregisterNetworkCallback(networkCallback) }
            }
        }
    }

    val cutoutPadding = WindowInsets.displayCutout.asPaddingValues()
    val barHeight: Dp = max(cutoutPadding.calculateTopPadding(), 42.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Sol yarı: Tam genişlik Bildirim Merkezi dokunma & çekme hedefi
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapLeftNotifications
                )
                .padding(start = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = timeText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                    shadow = WallpaperLabelShadow
                )
            )
        }

        // Sağ yarı: Tam genişlik Denetim Merkezi dokunma & çekme hedefi
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTapRightControlCenter
                )
                .padding(end = 4.dp),
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
            Spacer(modifier = Modifier.height(2.5.dp))
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(2.6.dp)
                    .clip(CircleShape)
                    .background(Color(0x94FFFFFF))
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
