package com.hnmlabs.hnmui.data

import android.content.ComponentName
import android.content.Context
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
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

/**
 * Yüklü tüm başlatılabilir uygulamaları, ikon önbelleğini ve uygulama kısayollarını yönetir.
 * LauncherApps.Callback sayesinde paket kurulum/silme/güncelleme olaylarını anlık yakalar.
 */
class InstalledAppsRepository(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)
    private val packageManager = context.packageManager

    private val iconCache = object : LruCache<String, ImageBitmap>(180) {}

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
            refreshAppsAsync()
        }

        override fun onPackagesUnavailable(
            packageNames: Array<out String>?,
            user: UserHandle?,
            replacing: Boolean
        ) {
            refreshAppsAsync()
        }
    }

    init {
        runCatching { launcherApps?.registerCallback(appsCallback) }
        refreshAppsAsync()
    }

    fun refreshAppsAsync() {
        scope.launch {
            _apps.value = loadInstalledApps()
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
                        val drawable = info.getBadgedIcon(density)
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

        return loaded.sortedWith { a, b -> collator.compare(a.label, b.label) }
    }

    fun getCachedIcon(key: AppComponentKey): ImageBitmap? = iconCache.get(key.stableId)

    suspend fun loadIconIfNeeded(key: AppComponentKey): ImageBitmap? {
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
                val drawable = match.getBadgedIcon(context.resources.displayMetrics.densityDpi)
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
            val launchIntent = packageManager.getLaunchIntentForPackage(key.packageName)
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

    private fun drawableToImageBitmap(drawable: Drawable, targetPx: Int = 192): ImageBitmap {
        val width = drawable.intrinsicWidth.takeIf { it > 0 }?.coerceAtMost(targetPx) ?: targetPx
        val height = drawable.intrinsicHeight.takeIf { it > 0 }?.coerceAtMost(targetPx) ?: targetPx
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
