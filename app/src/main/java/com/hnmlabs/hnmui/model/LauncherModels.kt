package com.hnmlabs.hnmui.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Bir Android aktivitesini çoklu kullanıcı profiliyle birlikte benzersiz tanımlayan anahtar.
 */
@Serializable
data class AppComponentKey(
    val packageName: String,
    val className: String,
    val userSerial: Long = 0L
) {
    val stableId: String get() = "$packageName/$className#$userSerial"
}

/**
 * App Library (Uygulama Arşivi) otomatik kategori sınıfları.
 */
enum class AppCategory(val titleTr: String) {
    SUGGESTED("Önerilenler"),
    RECENTLY_ADDED("Son Eklenenler"),
    SOCIAL("Sosyal"),
    PRODUCTIVITY("Üretkenlik ve Finans"),
    MEDIA("Eğlence ve Medya"),
    UTILITIES("Araçlar ve Sistem"),
    GAMES("Oyunlar"),
    OTHER("Diğer")
}

/**
 * Çalışma zamanında Launcher içinde gösterilen uygulama modeli.
 */
data class AppItem(
    val key: AppComponentKey,
    val label: String,
    val category: AppCategory,
    val firstInstallTimeMillis: Long = 0L,
    val isSystemApp: Boolean = false,
    val badgeCount: Int = 0
)

/**
 * İkona uzun basıldığında (Haptic Touch) veya Spotlight aramasında gösterilen kısayol modeli.
 */
data class AppShortcutItem(
    val id: String,
    val packageName: String,
    val userSerial: Long,
    val shortLabel: String,
    val longLabel: String? = null
)

/**
 * Ana ekran sayfalarında yer alabilen öğeler: Tekil uygulama veya buzlu cam klasör.
 */
@Serializable
sealed interface HomeGridItem {
    val itemId: String

    @Serializable
    @SerialName("app")
    data class AppTile(
        val key: AppComponentKey
    ) : HomeGridItem {
        override val itemId: String get() = "app:${key.stableId}"
    }

    @Serializable
    @SerialName("folder")
    data class FolderTile(
        val id: String,
        val title: String,
        val apps: List<AppComponentKey>
    ) : HomeGridItem {
        override val itemId: String get() = "folder:$id"
    }
}

@Serializable
data class HomePageModel(
    val id: String,
    val items: List<HomeGridItem>
)

@Serializable
data class QuickNoteItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false
)

/**
 * DataStore üzerinde JSON olarak kalıcı saklanan kullanıcı ana ekran düzeni.
 */
@Serializable
data class PersistedHomeState(
    val dock: List<AppComponentKey> = emptyList(),
    val pages: List<HomePageModel> = emptyList(),
    val quickNotes: List<QuickNoteItem> = emptyList(),
    val boundWidgetIds: List<Int> = emptyList(),
    val launchCounts: Map<String, Int> = emptyMap()
)

/**
 * Çözümlenmiş (AppItem nesneleriyle eşleştirilmiş) UI katmanı ana ekran öğeleri.
 */
sealed interface ResolvedHomeItem {
    val itemId: String

    data class App(val app: AppItem) : ResolvedHomeItem {
        override val itemId: String get() = "app:${app.key.stableId}"
    }

    data class Folder(
        val id: String,
        val title: String,
        val apps: List<AppItem>,
        val totalBadges: Int
    ) : ResolvedHomeItem {
        override val itemId: String get() = "folder:$id"
    }
}

data class ResolvedHomePage(
    val id: String,
    val items: List<ResolvedHomeItem>
)

data class AppLibraryGroup(
    val category: AppCategory,
    val apps: List<AppItem>
)
