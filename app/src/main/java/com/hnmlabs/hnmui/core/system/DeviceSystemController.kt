package com.hnmlabs.hnmui.core.system

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.view.KeyEvent
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

@Immutable
data class BatterySnapshot(
    val levelPercent: Int,
    val isCharging: Boolean,
    val sourceLabel: String,
    val healthLabel: String,
    val temperatureCelsius: Float,
    val voltageVolts: Float,
    val currentMilliAmps: Int,
    val technology: String,
    val isPowerSaveMode: Boolean
)

@Immutable
data class DisplaySnapshot(
    val brightnessFraction: Float,
    val isAutoBrightness: Boolean,
    val isRotationLocked: Boolean,
    val screenTimeoutMs: Int,
    val canWriteSystemSettings: Boolean
)

@Immutable
data class AudioSnapshot(
    val mediaFraction: Float,
    val ringFraction: Float,
    val alarmFraction: Float,
    val callFraction: Float,
    val ringerMode: Int,
    val isMusicActive: Boolean
)

@Immutable
data class StorageAndMemorySnapshot(
    val totalStorageGb: Float,
    val usedStorageGb: Float,
    val freeStorageGb: Float,
    val storageUsedFraction: Float,
    val totalRamGb: Float,
    val usedRamGb: Float,
    val freeRamGb: Float,
    val ramUsedFraction: Float
)

@Immutable
data class ConnectivitySnapshot(
    val isWifiConnected: Boolean,
    val isCellularConnected: Boolean,
    val isAirplaneMode: Boolean,
    val isBluetoothEnabled: Boolean
)

@Immutable
data class DeviceAboutSnapshot(
    val deviceName: String,
    val modelName: String,
    val manufacturer: String,
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val processorLabel: String,
    val cpuCores: Int,
    val resolutionLabel: String,
    val uptimeLabel: String
)

/**
 * Samsung One UI veya stok Ayarlar ekranına çıkmadan doğrudan HNMUI içerisinde
 * pil, parlaklık, ekran süresi, ses, depolama, RAM, el feneri ve ağ durumunu yöneten köprü.
 */
object DeviceSystemController {

    private data class BrightnessWriteRequest(
        val appContext: Context,
        val activityRef: WeakReference<Activity>?,
        val fraction: Float
    )

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val brightnessWriteFlow = MutableSharedFlow<BrightnessWriteRequest>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    @Volatile
    private var lastPersistedBrightnessInt: Int = -1

    @Volatile
    private var windowOverrideResetJob: Job? = null

    private val streamMaxVolumeCache = ConcurrentHashMap<Int, Int>()
    private val lastDispatchedVolumeStep = ConcurrentHashMap<Int, Int>()

    private val _isTorchEnabled = MutableStateFlow(false)
    val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    @Volatile
    private var torchCallbackRegistered = false

    @Volatile
    private var cachedTorchCameraId: String? = null

    init {
        ioScope.launch {
            brightnessWriteFlow.collect { req ->
                val canWrite = runCatching { Settings.System.canWrite(req.appContext) }.getOrDefault(false)
                if (canWrite) {
                    val targetInt = (req.fraction * 255f).roundToInt().coerceIn(5, 255)
                    if (targetInt != lastPersistedBrightnessInt) {
                        runCatching {
                            val resolver = req.appContext.contentResolver
                            Settings.System.putInt(
                                resolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                            )
                            Settings.System.putInt(
                                resolver,
                                Settings.System.SCREEN_BRIGHTNESS,
                                targetInt
                            )
                            lastPersistedBrightnessInt = targetInt
                        }
                    }
                    // Parmağın kaydırmayı bitirmesinden kısa süre sonra pencere geçersiz kılmasını sıfırla
                    windowOverrideResetJob?.cancel()
                    windowOverrideResetJob = ioScope.launch {
                        delay(260L)
                        mainHandler.post {
                            runCatching {
                                req.activityRef?.get()?.window?.let { win ->
                                    val attrs = win.attributes
                                    if (attrs.screenBrightness != -1f) {
                                        attrs.screenBrightness = -1f
                                        win.attributes = attrs
                                    }
                                }
                            }
                        }
                    }
                }
                // 120Hz dokunuş akışını sistem veritabanı üzerinde saniyede ~28 birleşik yazmaya yumuşat
                delay(34L)
            }
        }
    }

    fun ensureTorchObserver(context: Context) {
        if (torchCallbackRegistered) return
        synchronized(this) {
            if (torchCallbackRegistered) return
            val appCtx = context.applicationContext
            ioScope.launch {
                val cameraManager = runCatching {
                    appCtx.getSystemService(CameraManager::class.java)
                }.getOrNull() ?: return@launch
                val targetId = resolveTorchCameraId(cameraManager)
                runCatching {
                    cameraManager.registerTorchCallback(
                        object : CameraManager.TorchCallback() {
                            override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                                if (targetId == null || cameraId == targetId) {
                                    _isTorchEnabled.value = enabled
                                }
                            }

                            override fun onTorchModeUnavailable(cameraId: String) {
                                if (targetId == null || cameraId == targetId) {
                                    _isTorchEnabled.value = false
                                }
                            }
                        },
                        mainHandler
                    )
                    torchCallbackRegistered = true
                }
            }
        }
    }

    private fun resolveTorchCameraId(cameraManager: CameraManager): String? {
        cachedTorchCameraId?.let { return it }
        val resolved = runCatching {
            cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val lensFacing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && (lensFacing == null || lensFacing == CameraCharacteristics.LENS_FACING_BACK)
            } ?: cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
        cachedTorchCameraId = resolved
        return resolved
    }

    fun readBatterySnapshot(context: Context): BatterySnapshot {
        val bm = context.getSystemService(BatteryManager::class.java)
        val powerManager = context.getSystemService(PowerManager::class.java)
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()

        val propCap = runCatching {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        }.getOrNull()?.takeIf { it in 0..100 }
        val stickyCap = runCatching {
            val l = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val s = (sticky?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100).coerceAtLeast(1)
            if (l >= 0) ((l * 100f) / s).roundToInt().coerceIn(0, 100) else null
        }.getOrNull()
        val level = propCap ?: stickyCap ?: 100

        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val charging = runCatching { bm?.isCharging }.getOrNull() ?: (
            status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL ||
                plugged != 0
            )

        val sourceLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "Hızlı Adaptör (AC)"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Bağlantısı"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Kablosuz Şarj"
            else -> if (charging) "Şarj Oluyor" else "Pil Kullanımda"
        }

        val healthCode = sticky?.getIntExtra(
            BatteryManager.EXTRA_HEALTH,
            BatteryManager.BATTERY_HEALTH_GOOD
        ) ?: BatteryManager.BATTERY_HEALTH_GOOD

        val healthLabel = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "İyi (%100 Normal)"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Yüksek Sıcaklık"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Servis Önerilir"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Yüksek Voltaj"
            BatteryManager.BATTERY_HEALTH_COLD -> "Soğuk Ortam"
            else -> "Normal"
        }

        val tempTenths = sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 285) ?: 285
        val voltageMv = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 4100) ?: 4100
        val voltageV = if (voltageMv > 100) voltageMv / 1000f else voltageMv.toFloat()
        val currentMicroAmps = runCatching {
            bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        }.getOrNull()?.takeIf { it != Int.MIN_VALUE } ?: 0
        val currentMa = (currentMicroAmps / 1000)
        val tech = sticky?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)
            ?.takeIf { it.isNotBlank() } ?: "Li-ion"

        return BatterySnapshot(
            levelPercent = level,
            isCharging = charging,
            sourceLabel = sourceLabel,
            healthLabel = healthLabel,
            temperatureCelsius = tempTenths / 10f,
            voltageVolts = voltageV,
            currentMilliAmps = currentMa,
            technology = tech,
            isPowerSaveMode = runCatching { powerManager?.isPowerSaveMode }.getOrNull() ?: false
        )
    }

    fun readDisplaySnapshot(context: Context, activity: Activity? = null): DisplaySnapshot {
        val resolver = context.contentResolver
        val canWrite = runCatching { Settings.System.canWrite(context) }.getOrDefault(false)

        val windowBrightness = activity?.window?.attributes?.screenBrightness ?: -1f
        val sysBrightnessInt = runCatching {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(140).coerceIn(1, 255)

        val fraction = if (windowBrightness in 0.01f..1.0f && !canWrite) {
            windowBrightness.coerceIn(0.02f, 1f)
        } else {
            (sysBrightnessInt / 255f).coerceIn(0.02f, 1f)
        }

        val autoBright = runCatching {
            Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        }.getOrDefault(true)

        val autoRotateEnabled = runCatching {
            Settings.System.getInt(resolver, Settings.System.ACCELEROMETER_ROTATION) == 1
        }.getOrDefault(true)

        val timeoutMs = runCatching {
            Settings.System.getInt(resolver, Settings.System.SCREEN_OFF_TIMEOUT)
        }.getOrDefault(60_000)

        return DisplaySnapshot(
            brightnessFraction = fraction,
            isAutoBrightness = autoBright,
            isRotationLocked = !autoRotateEnabled,
            screenTimeoutMs = timeoutMs,
            canWriteSystemSettings = canWrite
        )
    }

    fun setBrightnessFraction(context: Context, activity: Activity?, fraction: Float) {
        if (!fraction.isFinite()) return
        val clamped = fraction.coerceIn(0.02f, 1f)
        // Pencere parlaklığını sıfır Binder maliyetiyle Main Thread üzerinde anında 120 FPS uygula
        runCatching {
            activity?.window?.let { win ->
                val attrs = win.attributes
                if (attrs.screenBrightness < 0f || abs(attrs.screenBrightness - clamped) >= 0.004f) {
                    attrs.screenBrightness = clamped
                    win.attributes = attrs
                }
            }
        }

        // Kalıcı Settings.System SQLite/Binder yazımını arka plan birleştirici kuyruğa (IO) devret
        brightnessWriteFlow.tryEmit(
            BrightnessWriteRequest(
                appContext = context.applicationContext,
                activityRef = activity?.let { WeakReference(it) },
                fraction = clamped
            )
        )
    }

    fun setAutoBrightness(context: Context, enabled: Boolean): Boolean {
        val appCtx = context.applicationContext
        if (!runCatching { Settings.System.canWrite(appCtx) }.getOrDefault(false)) return false
        ioScope.launch {
            runCatching {
                Settings.System.putInt(
                    appCtx.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    if (enabled) {
                        Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                    } else {
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    }
                )
            }
        }
        return true
    }

    fun setRotationLocked(context: Context, locked: Boolean): Boolean {
        val appCtx = context.applicationContext
        if (!runCatching { Settings.System.canWrite(appCtx) }.getOrDefault(false)) return false
        ioScope.launch {
            runCatching {
                Settings.System.putInt(
                    appCtx.contentResolver,
                    Settings.System.ACCELEROMETER_ROTATION,
                    if (locked) 0 else 1
                )
            }
        }
        return true
    }

    fun setScreenTimeoutMs(context: Context, timeoutMs: Int): Boolean {
        val appCtx = context.applicationContext
        if (!runCatching { Settings.System.canWrite(appCtx) }.getOrDefault(false)) return false
        ioScope.launch {
            runCatching {
                Settings.System.putInt(
                    appCtx.contentResolver,
                    Settings.System.SCREEN_OFF_TIMEOUT,
                    timeoutMs
                )
            }
        }
        return true
    }

    fun openWriteSettingsGrant(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun readAudioSnapshot(context: Context): AudioSnapshot {
        val am = runCatching { context.getSystemService(AudioManager::class.java) }.getOrNull()
            ?: return AudioSnapshot(0.5f, 0.5f, 0.7f, 0.7f, AudioManager.RINGER_MODE_NORMAL, false)

        fun streamFraction(stream: Int, fallback: Float = 0.5f): Float = runCatching {
            val max = am.getStreamMaxVolume(stream).coerceAtLeast(1)
            streamMaxVolumeCache[stream] = max
            val cur = am.getStreamVolume(stream).coerceIn(0, max)
            lastDispatchedVolumeStep[stream] = cur
            (cur.toFloat() / max.toFloat()).takeIf { it.isFinite() } ?: fallback
        }.getOrDefault(fallback)

        return AudioSnapshot(
            mediaFraction = streamFraction(AudioManager.STREAM_MUSIC, 0.5f),
            ringFraction = streamFraction(AudioManager.STREAM_RING, 0.5f),
            alarmFraction = streamFraction(AudioManager.STREAM_ALARM, 0.7f),
            callFraction = streamFraction(AudioManager.STREAM_VOICE_CALL, 0.7f),
            ringerMode = runCatching { am.ringerMode }.getOrDefault(AudioManager.RINGER_MODE_NORMAL),
            isMusicActive = runCatching { am.isMusicActive }.getOrDefault(false)
        )
    }

    fun setStreamVolumeFraction(context: Context, streamType: Int, fraction: Float) {
        if (!fraction.isFinite()) return
        val appCtx = context.applicationContext
        val cachedMax = streamMaxVolumeCache[streamType]
        if (cachedMax != null) {
            val target = (fraction.coerceIn(0f, 1f) * cachedMax).roundToInt().coerceIn(0, cachedMax)
            if (lastDispatchedVolumeStep[streamType] == target) return
            lastDispatchedVolumeStep[streamType] = target
        }

        ioScope.launch {
            val am = appCtx.getSystemService(AudioManager::class.java) ?: return@launch
            val max = streamMaxVolumeCache.getOrPut(streamType) {
                runCatching { am.getStreamMaxVolume(streamType).coerceAtLeast(1) }.getOrDefault(15)
            }
            val target = (fraction.coerceIn(0f, 1f) * max).roundToInt().coerceIn(0, max)
            lastDispatchedVolumeStep[streamType] = target
            runCatching {
                am.setStreamVolume(streamType, target, 0)
            }
        }
    }

    fun setRingerMode(context: Context, mode: Int) {
        val am = context.getSystemService(AudioManager::class.java) ?: return
        runCatching {
            am.ringerMode = mode
        }.onFailure {
            // DND izni yoksa sessiz yerine titreşime düş
            if (mode == AudioManager.RINGER_MODE_SILENT) {
                runCatching { am.ringerMode = AudioManager.RINGER_MODE_VIBRATE }
            }
        }
    }

    fun dispatchMediaKey(context: Context, keyCode: Int) {
        val appCtx = context.applicationContext
        ioScope.launch {
            val am = appCtx.getSystemService(AudioManager::class.java) ?: return@launch
            runCatching {
                val now = SystemClock.uptimeMillis()
                am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
                am.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
            }
        }
    }

    fun readStorageAndMemorySnapshot(context: Context): StorageAndMemorySnapshot {
        val bytesPerGb = 1024f * 1024f * 1024f
        val statFs = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val totalStorage = (statFs?.totalBytes ?: 0L) / bytesPerGb
        val freeStorage = (statFs?.availableBytes ?: 0L) / bytesPerGb
        val usedStorage = (totalStorage - freeStorage).coerceAtLeast(0f)
        val storageRatio = if (totalStorage > 0f) (usedStorage / totalStorage).coerceIn(0f, 1f) else 0f

        val activityManager = context.getSystemService(ActivityManager::class.java)
        val memInfo = ActivityManager.MemoryInfo()
        runCatching { activityManager?.getMemoryInfo(memInfo) }
        val totalRam = memInfo.totalMem / bytesPerGb
        val freeRam = memInfo.availMem / bytesPerGb
        val usedRam = (totalRam - freeRam).coerceAtLeast(0f)
        val ramRatio = if (totalRam > 0f) (usedRam / totalRam).coerceIn(0f, 1f) else 0f

        return StorageAndMemorySnapshot(
            totalStorageGb = totalStorage,
            usedStorageGb = usedStorage,
            freeStorageGb = freeStorage,
            storageUsedFraction = storageRatio,
            totalRamGb = totalRam,
            usedRamGb = usedRam,
            freeRamGb = freeRam,
            ramUsedFraction = ramRatio
        )
    }

    fun readConnectivitySnapshot(context: Context): ConnectivitySnapshot {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

        val airplane = runCatching {
            Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1
        }.getOrDefault(false)

        val btEnabled = runCatching {
            val bm = context.getSystemService(BluetoothManager::class.java)
            bm?.adapter?.isEnabled == true
        }.getOrDefault(false)

        return ConnectivitySnapshot(
            isWifiConnected = wifi,
            isCellularConnected = cell,
            isAirplaneMode = airplane,
            isBluetoothEnabled = btEnabled
        )
    }

    fun toggleFlashlight(context: Context, enable: Boolean): Boolean {
        val appCtx = context.applicationContext
        ensureTorchObserver(appCtx)
        _isTorchEnabled.value = enable
        ioScope.launch {
            val cameraManager = appCtx.getSystemService(CameraManager::class.java) ?: run {
                _isTorchEnabled.value = false
                return@launch
            }
            val ok = runCatching {
                val torchCameraId = resolveTorchCameraId(cameraManager) ?: return@runCatching false
                cameraManager.setTorchMode(torchCameraId, enable)
                true
            }.getOrDefault(false)
            if (!ok) {
                _isTorchEnabled.value = !enable
            }
        }
        return true
    }

    /**
     * One UI tam ekran Ayarları'na çıkmadan HNMUI üzerinde Android'in küçük alt bağlantı
     * penceresini (Settings.Panel) açar.
     */
    fun openInternetMiniPanel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val panelIntent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (runCatching { context.startActivity(panelIntent) }.isSuccess) return
        }
        val fallback = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(fallback) }
    }

    fun openBluetoothPanel(context: Context) {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    /**
     * Sol üstten aşağı çekildiğinde sistem bildirimlerini (EXPAND_STATUS_BAR normal izniyle) indirir.
     */
    @SuppressLint("WrongConstant")
    fun expandNotificationShade(context: Context): Boolean {
        return runCatching {
            val statusBarService = context.getSystemService("statusbar") ?: return false
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(statusBarService)
            true
        }.getOrDefault(false)
    }

    fun readDeviceAboutSnapshot(context: Context, refreshRateHz: Int): DeviceAboutSnapshot {
        val deviceName = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: Build.MODEL

        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }

        val dm = context.resources.displayMetrics
        val resolution = "${dm.widthPixels} × ${dm.heightPixels} • ${refreshRateHz} Hz"

        val totalMinutes = (SystemClock.elapsedRealtime() / 60_000L).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        val uptime = if (hours > 0) "${hours} sa ${minutes} dk" else "${minutes} dk"

        return DeviceAboutSnapshot(
            deviceName = deviceName,
            modelName = Build.MODEL,
            manufacturer = Build.MANUFACTURER.replaceFirstChar { it.uppercase() },
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            securityPatch = Build.VERSION.SECURITY_PATCH.ifBlank { "Güncel" },
            processorLabel = soc,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            resolutionLabel = resolution,
            uptimeLabel = uptime
        )
    }
}
