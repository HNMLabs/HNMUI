package com.hnmlabs.hnmui.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppShortcutItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Yüklü tüm başlatılabilir uygulamaları, ikon önbelleğini ve uygulama kısayollarını yönetir.
 * LauncherApps.Callback sayesinde paket kurulum/silme/güncelleme olaylarını anlık yakalar.
 */
class InstalledAppsRepository(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshMutex = Mutex()
    private val refreshRequested = AtomicBoolean(false)

    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val packageManager = context.packageManager

    private val iconCache = object : LruCache<String, ImageBitmap>(200) {}

    private val _apps = MutableStateFlow<List<AppItem>>(emptyList())
    val apps: StateFlow<List<AppItem>> = _apps.asStateFlow()

    private val appsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) {
            evictIconsForPackage(packageName)
            refreshAppsAsync()
        }

        override fun onPackageAdded(packageName: String?, user: UserHandle?) {
            refreshAppsAsync()
        }

        override fun onPackageChanged(packageName: String?, user: UserHandle?) {
            evictIconsForPackage(packageName)
            refreshAppsAsync()
        }

        override fun onPackagesAvailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            if (replacing) {
                packageNames?.forEach { evictIconsForPackage(it) }
            }
            refreshAppsAsync()
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            packageNames?.forEach { evictIconsForPackage(it) }
            refreshAppsAsync()
        }
    }

    init {
        runCatching { launcherApps?.registerCallback(appsCallback) }
        refreshAppsAsync()
    }

    fun refreshAppsAsync() {
        refreshRequested.set(true)
        scope.launch {
            if (!refreshMutex.tryLock()) return@launch
            try {
                while (refreshRequested.getAndSet(false)) {
                    _apps.value = loadInstalledApps()
                }
            } finally {
                refreshMutex.unlock()
                if (refreshRequested.get() && refreshMutex.tryLock()) {
                    try {
                        while (refreshRequested.getAndSet(false)) {
                            _apps.value = loadInstalledApps()
                        }
                    } finally {
                        refreshMutex.unlock()
                    }
                }
            }
        }
    }

    private fun loadInstalledApps(): List<AppItem> {
        val la = launcherApps ?: return emptyList()
        val um = userManager
        val profiles = um?.userProfiles?.ifEmpty { listOf(Process.myUserHandle()) }
            ?: listOf(Process.myUserHandle())

        val selfPkg = context.packageName
        val collator = Collator.getInstance(Locale("tr", "TR"))
        val loaded = ArrayList<AppItem>()

        for (profile in profiles) {
            val serial = um?.getSerialNumberForUser(profile) ?: 0L
            val activities: List<LauncherActivityInfo> =
                runCatching { la.getActivityList(null, profile) }.getOrDefault(emptyList())

            for (info in activities) {
                val component = info.componentName ?: continue
                if (component.packageName == selfPkg) continue

                val label = info.label?.toString()?.trim().orEmpty().ifEmpty { component.packageName }
                val appInfo = info.applicationInfo
                val isSystem = appInfo != null && (
                    (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                    )
                val firstInstall = info.firstInstallTime
                val category = AppCategoryClassifier.classify(
                    packageName = component.packageName,
                    label = label,
                    androidCategory = appInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED
                )

                val key = AppComponentKey(
                    packageName = component.packageName,
                    className = component.className,
                    userSerial = serial
                )

                // İkonu arka planda önbelleğe ısıt
                if (iconCache.get(key.stableId) == null) {
                    runCatching {
                        val density = context.resources.displayMetrics.densityDpi
                        val drawable = info.getIcon(density) ?: info.getBadgedIcon(density)
                        iconCache.put(key.stableId, drawableToImageBitmap(drawable))
                    }
                }

                loaded += AppItem(
                    key = key,
                    label = label,
                    category = category,
                    firstInstallTimeMillis = firstInstall,
                    isSystemApp = isSystem
                )
            }
        }

        // HNMUI dahili iOS tarzı "HNM Ayarlar" uygulamasını listeye ve önbelleğe ekle
        if (iconCache.get(HNM_SETTINGS_KEY.stableId) == null) {
            iconCache.put(HNM_SETTINGS_KEY.stableId, createHnmSettingsIconBitmap())
        }
        loaded += AppItem(
            key = HNM_SETTINGS_KEY,
            label = "HNM Ayarlar",
            category = com.hnmlabs.hnmui.model.AppCategory.UTILITIES,
            firstInstallTimeMillis = Long.MAX_VALUE / 2,
            isSystemApp = true
        )

        return loaded.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun getCachedIcon(key: AppComponentKey): ImageBitmap? {
        if (key.stableId == HNM_SETTINGS_KEY.stableId && iconCache.get(key.stableId) == null) {
            iconCache.put(key.stableId, createHnmSettingsIconBitmap())
        }
        return iconCache.get(key.stableId)
    }

    suspend fun loadIconIfNeeded(key: AppComponentKey): ImageBitmap? {
        if (key.stableId == HNM_SETTINGS_KEY.stableId) {
            return iconCache.get(key.stableId)
                ?: createHnmSettingsIconBitmap().also { iconCache.put(key.stableId, it) }
        }
        iconCache.get(key.stableId)?.let { return it }
        return withContext(Dispatchers.IO) {
            val la = launcherApps ?: return@withContext null
            val handle = resolveUserHandle(key.userSerial)
            val activities = runCatching { la.getActivityList(key.packageName, handle) }
                .getOrDefault(emptyList())
            val match = activities.firstOrNull { it.componentName.className == key.className }
                ?: activities.firstOrNull()
                ?: return@withContext null

            runCatching {
                val density = context.resources.displayMetrics.densityDpi
                val drawable = match.getIcon(density) ?: match.getBadgedIcon(density)
                drawableToImageBitmap(drawable).also { iconCache.put(key.stableId, it) }
            }.getOrNull()
        }
    }

    /**
     * Uygulamaya uzun basıldığında (Haptic Touch) veya Spotlight aramasında dinamik ve statik
     * kısayolları döndürür. Not: Yalnızca varsayılan Home Launcher olunduğunda sistem izin verir.
     */
    suspend fun queryShortcutsForPackage(
        packageName: String,
        userSerial: Long
    ): List<AppShortcutItem> = withContext(Dispatchers.IO) {
        val la = launcherApps ?: return@withContext emptyList()
        if (!runCatching { la.hasShortcutHostPermission() }.getOrDefault(false)) {
            return@withContext emptyList()
        }

        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        val handle = resolveUserHandle(userSerial)
        val shortcuts = runCatching { la.getShortcuts(query, handle) }.getOrNull().orEmpty()

        shortcuts
            .filter { it.isEnabled && !it.shortLabel.isNullOrBlank() }
            .sortedBy { it.rank }
            .take(4)
            .map { info ->
                AppShortcutItem(
                    id = info.id,
                    packageName = info.`package`,
                    userSerial = userSerial,
                    shortLabel = info.shortLabel?.toString().orEmpty(),
                    longLabel = info.longLabel?.toString()
                )
            }
    }

    fun launchApp(key: AppComponentKey): Boolean {
        val la = launcherApps ?: return false
        val handle = resolveUserHandle(key.userSerial)
        val component = ComponentName(key.packageName, key.className)
        return runCatching {
            la.startMainActivity(component, handle, null, null)
            true
        }.getOrElse {
            val launchIntent = packageManager.getLaunchIntentForPackage(key.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                runCatching {
                    context.startActivity(launchIntent)
                    true
                }.getOrDefault(false)
            } else {
                false
            }
        }
    }

    fun launchShortcut(shortcut: AppShortcutItem): Boolean {
        val la = launcherApps ?: return false
        val handle = resolveUserHandle(shortcut.userSerial)
        return runCatching {
            la.startShortcut(shortcut.packageName, shortcut.id, null, null, handle)
            true
        }.getOrDefault(false)
    }

    private fun resolveUserHandle(serial: Long): UserHandle {
        return userManager?.getUserForSerialNumber(serial) ?: Process.myUserHandle()
    }

    private fun evictIconsForPackage(packageName: String?) {
        if (packageName.isNullOrBlank()) return
        val snapshot = iconCache.snapshot().keys
        for (cachedKey in snapshot) {
            if (cachedKey.startsWith("$packageName/")) {
                iconCache.remove(cachedKey)
            }
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable, targetPx: Int = 180): ImageBitmap {
        val safePx = targetPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(safePx, safePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        if (drawable is android.graphics.drawable.AdaptiveIconDrawable) {
            val bleed = (safePx * android.graphics.drawable.AdaptiveIconDrawable.getExtraInsetFraction() * 0.65f).toInt()
            drawable.background?.let { bg ->
                bg.setBounds(-bleed, -bleed, safePx + bleed, safePx + bleed)
                bg.draw(canvas)
            }
            drawable.foreground?.let { fg ->
                fg.setBounds(-bleed, -bleed, safePx + bleed, safePx + bleed)
                fg.draw(canvas)
            }
        } else {
            drawable.setBounds(0, 0, safePx, safePx)
            drawable.draw(canvas)
        }

        // 120 FPS akıcılığı için dokuyu arka plan iş parçacığında GPU belleğine hazırla
        bitmap.prepareToDraw()
        return bitmap.asImageBitmap()
    }

    companion object {
        val HNM_SETTINGS_KEY = AppComponentKey(
            packageName = "com.hnmlabs.hnmui",
            className = "com.hnmlabs.hnmui.InternalSettings",
            userSerial = 0L
        )

        fun isSystemSettingsApp(key: AppComponentKey): Boolean {
            return key.stableId == HNM_SETTINGS_KEY.stableId ||
                key.packageName == "com.android.settings"
        }

        /**
         * iOS Ayarlar estetiğinde fırçalanmış titanyum zemin ve hassas dişli çarklı
         * HNM Ayarlar ikonunu vektörel hassasiyetle üretir.
         */
        fun createHnmSettingsIconBitmap(sizePx: Int = 180): ImageBitmap {
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val cx = sizePx / 2f
            val cy = sizePx / 2f

            val bgPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                shader = android.graphics.LinearGradient(
                    0f,
                    0f,
                    0f,
                    sizePx.toFloat(),
                    intArrayOf(0xFF475569.toInt(), 0xFF1E293B.toInt()),
                    null,
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

            // Dış kadran halkası
            val dialPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = sizePx * 0.025f
                color = 0x44CBD5E1
            }
            canvas.drawCircle(cx, cy, sizePx * 0.39f, dialPaint)

            // 8 dişli metalik çark kolları
            val toothPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeCap = android.graphics.Paint.Cap.ROUND
                strokeWidth = sizePx * 0.095f
                color = 0xFFE2E8F0.toInt()
            }
            for (i in 0 until 8) {
                val angle = Math.toRadians((i * 45.0))
                val rInner = sizePx * 0.16f
                val rOuter = sizePx * 0.31f
                val x1 = cx + (Math.cos(angle) * rInner).toFloat()
                val y1 = cy + (Math.sin(angle) * rInner).toFloat()
                val x2 = cx + (Math.cos(angle) * rOuter).toFloat()
                val y2 = cy + (Math.sin(angle) * rOuter).toFloat()
                canvas.drawLine(x1, y1, x2, y2, toothPaint)
            }

            // Ana çark gövdesi
            val ringPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = sizePx * 0.105f
                color = 0xFFF8FAFC.toInt()
            }
            canvas.drawCircle(cx, cy, sizePx * 0.205f, ringPaint)

            // İç HNMUI cam göbeği
            val coreFill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
                color = 0xFF0F172A.toInt()
            }
            canvas.drawCircle(cx, cy, sizePx * 0.115f, coreFill)

            val accentRing = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = sizePx * 0.024f
                color = 0xFF38BDF8.toInt()
            }
            canvas.drawCircle(cx, cy, sizePx * 0.085f, accentRing)

            bitmap.prepareToDraw()
            return bitmap.asImageBitmap()
        }
    }
}
