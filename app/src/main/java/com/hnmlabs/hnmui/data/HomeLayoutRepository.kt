package com.hnmlabs.hnmui.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hnmlabs.hnmui.model.AppCategory
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.HomeGridItem
import com.hnmlabs.hnmui.model.HomePageModel
import com.hnmlabs.hnmui.model.LauncherCustomizationSettings
import com.hnmlabs.hnmui.model.PersistedHomeState
import com.hnmlabs.hnmui.model.QuickNoteItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.homeLayoutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hnmui_home_layout"
)

/**
 * Ana ekran sayfalarını, buzlu cam Dock öğelerini, klasörleri, Today View hızlı notlarını
 * ve minimalist ızgara/Dock özelleştirmelerini DataStore üzerinde kalıcı olarak yönetir.
 */
class HomeLayoutRepository(
    private val context: Context
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val stateKey = stringPreferencesKey("persisted_home_state_v1")

    val persistedState: Flow<PersistedHomeState?> = context.homeLayoutDataStore.data.map { prefs ->
        val raw = prefs[stateKey] ?: return@map null
        runCatching { json.decodeFromString<PersistedHomeState>(raw) }.getOrNull()
    }

    suspend fun ensureInitialSeed(
        installedApps: List<AppItem>,
        maxPerPage: Int,
        dockMaxItems: Int
    ) {
        if (installedApps.isEmpty()) return
        context.homeLayoutDataStore.edit { prefs ->
            val existing = prefs[stateKey]
                ?.let { runCatching { json.decodeFromString<PersistedHomeState>(it) }.getOrNull() }

            if (existing == null) {
                val seeded = buildInitialSeedState(installedApps, maxPerPage, dockMaxItems)
                prefs[stateKey] = json.encodeToString(seeded)
            } else if (!existing.hasInjectedHnmSettingsTile) {
                // Önceki sürümden güncellenen kullanıcının ana ekranına HNM Ayarlar ikonunu ekle
                val settingsKey = InstalledAppsRepository.HNM_SETTINGS_KEY
                val alreadyPresent = existing.dock.any { it.stableId == settingsKey.stableId } ||
                    existing.pages.any { page ->
                        page.items.any { item ->
                            when (item) {
                                is HomeGridItem.AppTile -> item.key.stableId == settingsKey.stableId
                                is HomeGridItem.FolderTile ->
                                    item.apps.any { it.stableId == settingsKey.stableId }
                            }
                        }
                    }

                val updatedPages = if (alreadyPresent) {
                    existing.pages
                } else {
                    val pages = existing.pages.toMutableList()
                    val first = pages.firstOrNull()
                        ?: HomePageModel(id = "page_1", items = emptyList())
                    if (pages.isEmpty()) {
                        pages += first.copy(items = listOf(HomeGridItem.AppTile(settingsKey)))
                    } else {
                        pages[0] = first.copy(
                            items = listOf(HomeGridItem.AppTile(settingsKey)) + first.items
                        )
                    }
                    rebalancePages(
                        pages = pages,
                        maxPerPage = (existing.customization.gridColumns * existing.customization.gridRows)
                            .coerceAtLeast(16)
                    )
                }

                prefs[stateKey] = json.encodeToString(
                    existing.copy(
                        pages = updatedPages,
                        hasInjectedHnmSettingsTile = true
                    )
                )
            }
        }
    }

    suspend fun updateCustomization(
        transform: (LauncherCustomizationSettings) -> LauncherCustomizationSettings
    ) {
        mutateState { state ->
            val oldCustom = state.customization
            val nextCustom = transform(oldCustom)
            val oldMaxPerPage = oldCustom.gridColumns * oldCustom.gridRows
            val nextMaxPerPage = (nextCustom.gridColumns.coerceIn(4, 7)) *
                (nextCustom.gridRows.coerceIn(4, 7))

            val nextPages = if (oldMaxPerPage != nextMaxPerPage) {
                rebalancePages(state.pages, nextMaxPerPage)
            } else {
                state.pages
            }

            // Dock kapasitesi küçültüldüyse taşan öğeleri ana ekrana geri taşı
            val maxDock = nextCustom.dockMaxItems.coerceIn(3, 7)
            if (state.dock.size > maxDock) {
                val keptDock = state.dock.take(maxDock)
                val overflowDock = state.dock.drop(maxDock).map { HomeGridItem.AppTile(it) }
                val mergedFirst = nextPages.firstOrNull()
                    ?: HomePageModel(id = "page_1", items = emptyList())
                val withOverflow = if (nextPages.isEmpty()) {
                    listOf(mergedFirst.copy(items = overflowDock))
                } else {
                    listOf(mergedFirst.copy(items = mergedFirst.items + overflowDock)) +
                        nextPages.drop(1)
                }
                state.copy(
                    dock = keptDock,
                    pages = rebalancePages(withOverflow, nextMaxPerPage),
                    customization = nextCustom
                )
            } else {
                state.copy(
                    pages = nextPages,
                    customization = nextCustom
                )
            }
        }
    }

    suspend fun toggleDockPin(key: AppComponentKey, maxDockItems: Int) {
        mutateState { state ->
            val maxPerPage = (state.customization.gridColumns * state.customization.gridRows)
                .coerceAtLeast(16)
            val isCurrentlyInDock = state.dock.any { it.stableId == key.stableId }

            if (isCurrentlyInDock) {
                val nextDock = state.dock.filterNot { it.stableId == key.stableId }
                val pages = state.pages.toMutableList()
                val first = pages.firstOrNull() ?: HomePageModel(id = "page_1", items = emptyList())
                if (pages.isEmpty()) {
                    pages += first.copy(items = listOf(HomeGridItem.AppTile(key)))
                } else {
                    pages[0] = first.copy(items = first.items + HomeGridItem.AppTile(key))
                }
                state.copy(
                    dock = nextDock,
                    pages = rebalancePages(pages, maxPerPage)
                )
            } else {
                if (state.dock.size >= maxDockItems.coerceIn(3, 7)) return@mutateState state
                // Ana ekran sayfalarından çıkarıp Dock'a sabitle
                val strippedPages = state.pages.map { page ->
                    page.copy(
                        items = page.items.mapNotNull { item ->
                            when (item) {
                                is HomeGridItem.AppTile ->
                                    if (item.key.stableId == key.stableId) null else item
                                is HomeGridItem.FolderTile -> {
                                    val rem = item.apps.filterNot { it.stableId == key.stableId }
                                    when (rem.size) {
                                        0 -> null
                                        1 -> HomeGridItem.AppTile(rem.first())
                                        else -> item.copy(apps = rem)
                                    }
                                }
                            }
                        }
                    )
                }
                state.copy(
                    dock = state.dock + key,
                    pages = strippedPages.ifEmpty {
                        listOf(HomePageModel(id = "page_1", items = emptyList()))
                    }
                )
            }
        }
    }

    suspend fun recordAppLaunch(key: AppComponentKey) {
        mutateState { state ->
            val updated = state.launchCounts.toMutableMap()
            updated[key.stableId] = (updated[key.stableId] ?: 0) + 1
            state.copy(launchCounts = updated)
        }
    }

    suspend fun addAppToHomeScreen(key: AppComponentKey, maxPerPage: Int) {
        mutateState { state ->
            val alreadyInDock = state.dock.any { it.stableId == key.stableId }
            val alreadyInPages = state.pages.any { page ->
                page.items.any { item ->
                    when (item) {
                        is HomeGridItem.AppTile -> item.key.stableId == key.stableId
                        is HomeGridItem.FolderTile -> item.apps.any { it.stableId == key.stableId }
                    }
                }
            }
            if (alreadyInDock || alreadyInPages) return@mutateState state

            val pages = state.pages.toMutableList()
            val targetIdx = pages.indexOfFirst { it.items.size < maxPerPage }
            if (targetIdx >= 0) {
                val target = pages[targetIdx]
                pages[targetIdx] = target.copy(items = target.items + HomeGridItem.AppTile(key))
            } else {
                pages += HomePageModel(
                    id = "page_${UUID.randomUUID()}",
                    items = listOf(HomeGridItem.AppTile(key))
                )
            }
            state.copy(pages = pages)
        }
    }

    suspend fun removeFromHomeScreen(key: AppComponentKey) {
        mutateState { state ->
            val cleanedPages = state.pages.mapNotNull { page ->
                val items = page.items.mapNotNull { item ->
                    when (item) {
                        is HomeGridItem.AppTile ->
                            if (item.key.stableId == key.stableId) null else item
                        is HomeGridItem.FolderTile -> {
                            val remaining = item.apps.filterNot { it.stableId == key.stableId }
                            when (remaining.size) {
                                0 -> null
                                1 -> HomeGridItem.AppTile(remaining.first())
                                else -> item.copy(apps = remaining)
                            }
                        }
                    }
                }
                if (items.isEmpty() && state.pages.size > 1) null else page.copy(items = items)
            }.ifEmpty { listOf(HomePageModel(id = "page_main", items = emptyList())) }

            val cleanedDock = state.dock.filterNot { it.stableId == key.stableId }
            state.copy(dock = cleanedDock, pages = cleanedPages)
        }
    }

    suspend fun createOrAddToFolder(
        appKey: AppComponentKey,
        targetFolderId: String?,
        folderTitle: String = "Klasör"
    ) {
        mutateState { state ->
            val strippedPages = state.pages.map { page ->
                page.copy(
                    items = page.items.filterNot {
                        it is HomeGridItem.AppTile && it.key.stableId == appKey.stableId
                    }
                )
            }.toMutableList()

            if (targetFolderId != null) {
                val updatedPages = strippedPages.map { page ->
                    page.copy(
                        items = page.items.map { item ->
                            if (item is HomeGridItem.FolderTile && item.id == targetFolderId) {
                                if (item.apps.any { it.stableId == appKey.stableId }) {
                                    item
                                } else {
                                    item.copy(apps = item.apps + appKey)
                                }
                            } else {
                                item
                            }
                        }
                    )
                }
                state.copy(pages = updatedPages)
            } else {
                val newFolder = HomeGridItem.FolderTile(
                    id = UUID.randomUUID().toString(),
                    title = folderTitle,
                    apps = listOf(appKey)
                )
                val firstPage = strippedPages.firstOrNull()
                    ?: HomePageModel(id = "page_main", items = emptyList())
                if (strippedPages.isEmpty()) {
                    strippedPages += firstPage.copy(items = listOf(newFolder))
                } else {
                    strippedPages[0] = firstPage.copy(items = listOf(newFolder) + firstPage.items)
                }
                val maxPerPage = (state.customization.gridColumns * state.customization.gridRows)
                    .coerceAtLeast(16)
                state.copy(pages = rebalancePages(strippedPages, maxPerPage))
            }
        }
    }

    suspend fun renameFolder(folderId: String, newTitle: String) {
        val sanitized = newTitle.trim().ifEmpty { "Klasör" }
        mutateState { state ->
            state.copy(
                pages = state.pages.map { page ->
                    page.copy(
                        items = page.items.map { item ->
                            if (item is HomeGridItem.FolderTile && item.id == folderId) {
                                item.copy(title = sanitized)
                            } else {
                                item
                            }
                        }
                    )
                }
            )
        }
    }

    suspend fun removeAppFromFolderToHome(
        folderId: String,
        appKey: AppComponentKey,
        maxPerPage: Int
    ) {
        mutateState { state ->
            val updatedPages = state.pages.map { page ->
                val nextItems = ArrayList<HomeGridItem>()
                for (item in page.items) {
                    if (item is HomeGridItem.FolderTile && item.id == folderId) {
                        val remaining = item.apps.filterNot { it.stableId == appKey.stableId }
                        when (remaining.size) {
                            0 -> Unit
                            1 -> nextItems += HomeGridItem.AppTile(remaining.first())
                            else -> nextItems += item.copy(apps = remaining)
                        }
                        nextItems += HomeGridItem.AppTile(appKey)
                    } else {
                        nextItems += item
                    }
                }
                page.copy(items = nextItems)
            }
            state.copy(pages = rebalancePages(updatedPages, maxPerPage.coerceAtLeast(16)))
        }
    }

    suspend fun addQuickNote(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        mutateState { state ->
            val note = QuickNoteItem(id = UUID.randomUUID().toString(), text = clean, isDone = false)
            state.copy(quickNotes = listOf(note) + state.quickNotes)
        }
    }

    suspend fun toggleQuickNote(noteId: String) {
        mutateState { state ->
            state.copy(
                quickNotes = state.quickNotes.map {
                    if (it.id == noteId) it.copy(isDone = !it.isDone) else it
                }
            )
        }
    }

    suspend fun deleteQuickNote(noteId: String) {
        mutateState { state ->
            state.copy(quickNotes = state.quickNotes.filterNot { it.id == noteId })
        }
    }

    suspend fun bindWidgetId(appWidgetId: Int) {
        mutateState { state ->
            if (state.boundWidgetIds.contains(appWidgetId)) state
            else state.copy(boundWidgetIds = state.boundWidgetIds + appWidgetId)
        }
    }

    suspend fun removeWidgetId(appWidgetId: Int) {
        mutateState { state ->
            state.copy(boundWidgetIds = state.boundWidgetIds.filterNot { it == appWidgetId })
        }
    }

    private suspend fun mutateState(transform: (PersistedHomeState) -> PersistedHomeState) {
        context.homeLayoutDataStore.edit { prefs ->
            val current = prefs[stateKey]
                ?.let { runCatching { json.decodeFromString<PersistedHomeState>(it) }.getOrNull() }
                ?: PersistedHomeState()
            val updated = transform(current)
            prefs[stateKey] = json.encodeToString(updated)
        }
    }

    companion object {
        fun rebalancePages(
            pages: List<HomePageModel>,
            maxPerPage: Int
        ): List<HomePageModel> {
            val safeChunk = maxPerPage.coerceAtLeast(8)
            val allItems = pages.flatMap { it.items }
            if (allItems.isEmpty()) {
                return listOf(HomePageModel(id = "page_1", items = emptyList()))
            }
            return allItems.chunked(safeChunk).mapIndexed { index, chunk ->
                val existingId = pages.getOrNull(index)?.id ?: "page_${index + 1}"
                HomePageModel(id = existingId, items = chunk)
            }
        }

        /**
         * İlk açılışta kullanıcının temel iletişim ve kamera uygulamalarını Dock'a yerleştirir,
         * HNM Ayarlar ve geri kalan uygulamaları temiz sayfalara dağıtır.
         */
        fun buildInitialSeedState(
            installedApps: List<AppItem>,
            maxPerPage: Int,
            dockMaxItems: Int
        ): PersistedHomeState {
            val dockCandidates = ArrayList<AppItem>()

            fun pickDockCandidate(keywords: List<String>) {
                val match = installedApps.firstOrNull { app ->
                    app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId &&
                        app !in dockCandidates &&
                        keywords.any { kw ->
                            app.key.packageName.lowercase().contains(kw) ||
                                app.label.lowercase().contains(kw)
                        }
                }
                if (match != null && dockCandidates.size < dockMaxItems) {
                    dockCandidates += match
                }
            }

            pickDockCandidate(listOf("dialer", "phone", "telefon"))
            pickDockCandidate(listOf("messages", "messaging", "mms", "whatsapp", "mesaj"))
            pickDockCandidate(listOf("chrome", "browser", "safari", "firefox", "tarayıcı"))
            pickDockCandidate(listOf("camera", "kamera"))

            for (app in installedApps) {
                if (dockCandidates.size >= dockMaxItems) break
                if (app !in dockCandidates &&
                    app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId
                ) {
                    dockCandidates += app
                }
            }

            val dockKeys = dockCandidates.map { it.key }
            val dockIds = dockKeys.map { it.stableId }.toSet()

            val remaining = installedApps.filterNot { it.key.stableId in dockIds }
            val utilityFolderApps = remaining
                .filter {
                    it.category == AppCategory.UTILITIES &&
                        it.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId
                }
                .take(4)

            val folderAppIds = if (utilityFolderApps.size >= 2) {
                utilityFolderApps.map { it.key.stableId }.toSet()
            } else {
                emptySet()
            }

            val gridItems = ArrayList<HomeGridItem>()
            // İlk sıraya HNM Ayarlar ikonunu koy
            gridItems += HomeGridItem.AppTile(InstalledAppsRepository.HNM_SETTINGS_KEY)

            if (utilityFolderApps.size >= 2) {
                gridItems += HomeGridItem.FolderTile(
                    id = "folder_utilities_seed",
                    title = "Araçlar",
                    apps = utilityFolderApps.map { it.key }
                )
            }

            for (app in remaining) {
                if (app.key.stableId !in folderAppIds &&
                    app.key.stableId != InstalledAppsRepository.HNM_SETTINGS_KEY.stableId
                ) {
                    gridItems += HomeGridItem.AppTile(app.key)
                }
            }

            val safePageSize = maxPerPage.coerceAtLeast(8)
            val pages = gridItems
                .take(safePageSize * 2)
                .chunked(safePageSize)
                .mapIndexed { idx, chunk ->
                    HomePageModel(id = "page_${idx + 1}", items = chunk)
                }
                .ifEmpty { listOf(HomePageModel(id = "page_1", items = emptyList())) }

            val defaultNotes = listOf(
                QuickNoteItem(
                    id = "welcome_1",
                    text = "Sağ üstten aşağı çek: Denetim Merkezi • Ortadan çek: Spotlight",
                    isDone = false
                ),
                QuickNoteItem(
                    id = "welcome_2",
                    text = "HNM Ayarlar ile 7x7 ızgara, klasör ve Dock boyutunu özelleştir",
                    isDone = false
                )
            )

            return PersistedHomeState(
                dock = dockKeys,
                pages = pages,
                quickNotes = defaultNotes,
                hasInjectedHnmSettingsTile = true
            )
        }
    }
}
