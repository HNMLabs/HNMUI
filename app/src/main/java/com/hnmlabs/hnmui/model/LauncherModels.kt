package com.hnmlabs.hnmui.model

import androidx.compose.runtime.Immutable
import com.hnmlabs.hnmui.core.text.PreparedSearchQuery
import com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Bir Android aktivitesini çoklu kullanıcı profiliyle birlikte benzersiz tanımlayan anahtar.
 */
@Immutable
@Serializable
data class AppComponentKey(
    val packageName: String,
    val className: String,
    val userSerial: Long = 0L
) {
    @Transient
    val stableId: String = "$packageName/$className#$userSerial"
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
 * Arama indeksleri (foldedLabel, asciiLabel, asciiPackage) oluşturma anında tek sefer hesaplanır;
 * böylece Spotlight, Uygulama Arşivi ve HNM Ayarlar aramalarında tuş başına sıfır tahsis yapılır.
 */
@Immutable
data class AppItem(
    val key: AppComponentKey,
    val label: String,
    val category: AppCategory,
    val firstInstallTimeMillis: Long = 0L,
    val isSystemApp: Boolean = false,
    val badgeCount: Int = 0,
    val foldedLabel: String = TurkishSearchNormalizer.foldTurkishI(label),
    val asciiLabel: String = TurkishSearchNormalizer.foldAscii(label),
    val foldedPackage: String = TurkishSearchNormalizer.foldTurkishI(key.packageName),
    val asciiPackage: String = TurkishSearchNormalizer.foldAscii(key.packageName)
) {
    fun matchesPreparedQuery(prepared: PreparedSearchQuery): Boolean {
        if (prepared.isEmpty) return true
        return TurkishSearchNormalizer.containsPreFolded(foldedLabel, asciiLabel, prepared) ||
            TurkishSearchNormalizer.containsPreFolded(foldedPackage, asciiPackage, prepared)
    }

    fun labelStartsWithPreparedQuery(prepared: PreparedSearchQuery): Boolean {
        return TurkishSearchNormalizer.startsWithPreFolded(foldedLabel, asciiLabel, prepared)
    }
}

/**
 * İkona uzun basıldığında (Haptic Touch) veya Spotlight aramasında gösterilen kısayol modeli.
 */
@Immutable
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
@Immutable
@Serializable
sealed interface HomeGridItem {
    val itemId: String

    @Immutable
    @Serializable
    @SerialName("app")
    data class AppTile(
        val key: AppComponentKey
    ) : HomeGridItem {
        override val itemId: String get() = "app:${key.stableId}"
    }

    @Immutable
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

@Immutable
@Serializable
data class HomePageModel(
    val id: String,
    val items: List<HomeGridItem>
)

@Immutable
@Serializable
data class QuickNoteItem(
    val id: String,
    val text: String,
    val isDone: Boolean = false
)

/**
 * Kullanıcının HNM Ayarlar içerisinden değiştirebildiği minimalist arayüz,
 * ızgara (7x7'ye kadar), klasör ve Dock özelleştirme tercihleri.
 */
@Immutable
@Serializable
data class LauncherCustomizationSettings(
    val gridColumns: Int = 4,
    val gridRows: Int = 6,
    val iconScalePercent: Int = 100,
    val showHomeLabels: Boolean = true,
    val folderColumns: Int = 3,
    val folderPreviewColumns: Int = 2,
    val dockVisible: Boolean = true,
    val dockMaxItems: Int = 4,
    val dockScalePercent: Int = 100,
    val glassOpacityPercent: Int = 100,
    val interceptSystemSettingsApp: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val useIosStatusBar: Boolean = true
)

/**
 * DataStore üzerinde JSON olarak kalıcı saklanan kullanıcı ana ekran düzeni ve ayarları.
 */
@Immutable
@Serializable
data class PersistedHomeState(
    val dock: List<AppComponentKey> = emptyList(),
    val pages: List<HomePageModel> = emptyList(),
    val quickNotes: List<QuickNoteItem> = emptyList(),
    val boundWidgetIds: List<Int> = emptyList(),
    val launchCounts: Map<String, Int> = emptyMap(),
    val customization: LauncherCustomizationSettings = LauncherCustomizationSettings(),
    val hasInjectedHnmSettingsTile: Boolean = false
)

/**
 * Bildirim Merkezi katmanında gösterilen canlı bildirim kartı modeli.
 */
@Immutable
data class LiveNotificationCard(
    val key: String,
    val packageName: String,
    val title: String,
    val message: String,
    val postTimeMillis: Long
)

/**
 * Çözümlenmiş (AppItem nesneleriyle eşleştirilmiş) UI katmanı ana ekran öğeleri.
 */
@Immutable
sealed interface ResolvedHomeItem {
    val itemId: String

    @Immutable
    data class App(val app: AppItem) : ResolvedHomeItem {
        override val itemId: String = "app:${app.key.stableId}"
    }

    @Immutable
    data class Folder(
        val id: String,
        val title: String,
        val apps: List<AppItem>,
        val totalBadges: Int
    ) : ResolvedHomeItem {
        override val itemId: String = "folder:$id"
    }
}

@Immutable
data class ResolvedHomePage(
    val id: String,
    val items: List<ResolvedHomeItem>
)

@Immutable
data class AppLibraryGroup(
    val category: AppCategory,
    val apps: List<AppItem>
)
