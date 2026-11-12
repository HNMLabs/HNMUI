package com.hnmlabs.hnmui.data

import android.content.pm.ApplicationInfo
import com.hnmlabs.hnmui.model.AppCategory
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppLibraryGroup

/**
 * Uygulamaları hem Android sistem kategori bayrağına hem de paket/etiket sezgilerine göre
 * iOS App Library tarzında otomatik sınıflandıran saf Kotlin motoru.
 */
object AppCategoryClassifier {

    private val socialKeywords = listOf(
        "whatsapp", "instagram", "telegram", "twitter", "zhiliaoapp", "tiktok",
        "facebook", "messenger", "discord", "snapchat", "linkedin", "threads",
        "messages", "mms", "messaging", "dialer", "contacts"
    )

    private val productivityKeywords = listOf(
        "gmail", "mail", "outlook", "calendar", "docs", "sheets", "slides",
        "notion", "slack", "teams", "zoom", "drive", "keep", "notes",
        "bank", "garanti", "isbank", "yapikredi", "akbank", "ziraat", "papara", "wallet", "finance"
    )

    private val mediaKeywords = listOf(
        "youtube", "spotify", "netflix", "music", "gallery", "photos",
        "camera", "vlc", "twitch", "disney", "primevideo", "podcast", "player"
    )

    private val utilityKeywords = listOf(
        "settings", "calculator", "clock", "deskclock", "files", "filemanager",
        "weather", "maps", "chrome", "browser", "firefox", "vending", "edevlet", "turkiye"
    )

    fun classify(
        packageName: String,
        label: String,
        androidCategory: Int
    ): AppCategory {
        when (androidCategory) {
            ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.MEDIA
            ApplicationInfo.CATEGORY_MAPS,
            ApplicationInfo.CATEGORY_NEWS -> return AppCategory.UTILITIES
        }

        val haystack = "${packageName.lowercase()} ${label.lowercase()}"
        return when {
            socialKeywords.any { haystack.contains(it) } -> AppCategory.SOCIAL
            productivityKeywords.any { haystack.contains(it) } -> AppCategory.PRODUCTIVITY
            mediaKeywords.any { haystack.contains(it) } -> AppCategory.MEDIA
            utilityKeywords.any { haystack.contains(it) } -> AppCategory.UTILITIES
            else -> AppCategory.OTHER
        }
    }

    /**
     * App Library (Uygulama Arşivi) sayfasındaki kategori klasörlerini oluşturur:
     * 1. Önerilenler (en sık açılanlar + temel iletişim araçları)
     * 2. Son Eklenenler (en yeni yüklenen kullanıcı uygulamaları)
     * 3. Konu bazlı kategoriler
     */
    fun buildAppLibraryGroups(
        apps: List<AppItem>,
        launchCounts: Map<String, Int>
    ): List<AppLibraryGroup> {
        if (apps.isEmpty()) return emptyList()

        val result = ArrayList<AppLibraryGroup>()

        val suggested = apps
            .sortedWith(
                compareByDescending<AppItem> { launchCounts[it.key.stableId] ?: 0 }
                    .thenByDescending { it.badgeCount }
                    .thenBy { it.label.lowercase() }
            )
            .take(8)
        if (suggested.isNotEmpty()) {
            result += AppLibraryGroup(AppCategory.SUGGESTED, suggested)
        }

        val recent = apps
            .filter { !it.isSystemApp && it.firstInstallTimeMillis > 0L }
            .sortedByDescending { it.firstInstallTimeMillis }
            .take(8)
            .ifEmpty { apps.take(4) }
        if (recent.isNotEmpty()) {
            result += AppLibraryGroup(AppCategory.RECENTLY_ADDED, recent)
        }

        val orderedCategories = listOf(
            AppCategory.SOCIAL,
            AppCategory.PRODUCTIVITY,
            AppCategory.MEDIA,
            AppCategory.UTILITIES,
            AppCategory.GAMES,
            AppCategory.OTHER
        )

        for (category in orderedCategories) {
            val matching = apps
                .filter { it.category == category }
                .sortedBy { it.label.lowercase() }
            if (matching.isNotEmpty()) {
                result += AppLibraryGroup(category, matching)
            }
        }

        return result
    }
}
