package com.hnmlabs.hnmui.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hnmlabs.hnmui.core.text.TurkishSearchNormalizer
import com.hnmlabs.hnmui.model.AppCategory
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.HomeGridItem
import com.hnmlabs.hnmui.model.HomePageModel
import com.hnmlabs.hnmui.model.LauncherCustomizationSettings
import com.hnmlabs.hnmui.model.PersistedHomeState
import com.hnmlabs.hnmui.model.QuickNoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.UUID

private val Context.homeLayoutDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "hnmui_home_layout",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
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
    private val decodeCacheLock = Any()
    private var cachedRawJson: String? = null
    private var cachedDecodedState: PersistedHomeState? = null

    private fun decodeCached(raw: String): PersistedHomeState? {
        synchronized(decodeCacheLock) {
            if (raw == cachedRawJson && cachedDecodedState != null) {
                return cachedDecodedState
            }
        }
        val decoded = runCatching { json.decodeFromString<PersistedHomeState>(raw) }.getOrNull()
        if (decoded != null) {
            synchronized(decodeCacheLock) {
                cachedRawJson = raw
                cachedDecodedState = decoded
            }
        }
        return decoded
    }

    private fun encodeAndPrimeCache(state: PersistedHomeState): String {
        val encoded = json.encodeToString(state)
        synchronized(decodeCacheLock) {
            cachedRawJson = encoded
            cachedDecodedState = state
        }
        return encoded
    }

    val persistedState: Flow<PersistedHomeState?> = context.homeLayoutDataStore.data
        .catch { err ->
            if (err is IOException) emit(emptyPreferences()) else throw err
        }
        .map { prefs ->
            val raw = prefs[stateKey] ?: return@map null
            decodeCached(raw)
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    /**
     * İlk tohumlamayı (initial seed), HNM Ayarlar ikonu göçünü ve kaldırılmış (ghost)
     * uygulamaların temizliğini tek bir atomik DataStore işleminde senkronize eder.
     */
    suspend fun syncWithInstalledApps(
        installedApps: List<AppItem>,
        maxPerPage: Int,
        dockMaxItems: Int
    ) = withContext(Dispatchers.IO) {
        if (installedApps.isEmpty()) return@withContext
        val installedIds = installedApps.mapTo(HashSet(installedApps.size)) { it.key.stableId }

        context.homeLayoutDataStore.edit { prefs ->
            val existing = prefs[stateKey]?.let { decodeCached(it) }

            if (existing == null) {
                val seeded = buildInitialSeedState(installedApps, maxPerPage, dockMaxItems)
                prefs[stateKey] = encodeAndPrimeCache(seeded)
                return@edit
            }

            var candidate = existing
            if (!candidate.hasInjectedHnmSettingsTile) {
                val settingsKey = InstalledAppsRepository.HNM_SETTINGS_KEY
                val alreadyPresent = candidate.dock.any { it.stableId == settingsKey.stableId } ||
                    candidate.pages.any { page ->
                        page.items.any { item ->
                            when (item) {
                                is HomeGridItem.AppTile -> item.key.stableId == settingsKey.stableId
                                is HomeGridItem.FolderTile ->
                                    item.apps.any { it.stableId == settingsKey.stableId }
                            }
                        }
                    }

                val updatedPages = if (alreadyPresent) {
                    candidate.pages
                } else {
                    val pages = candidate.pages.toMutableList()
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
                        maxPerPage = (candidate.customization.gridColumns * candidate.customization.gridRows)
                            .coerceAtLeast(16)
                    )
                }

                candidate = candidate.copy(
                    pages = updatedPages,
                    hasInjectedHnmSettingsTile = true
                )
            }

            val normalized = pruneUninstalledAndNormalize(
                state = candidate,
                installedIds = installedIds,
                maxPerPage = maxPerPage
            )
            if (normalized != existing) {
                prefs[stateKey] = json.encodeToString(normalized)
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
                val strippedPages = stripAppFromPages(state.pages, key.stableId)
                state.copy(
                    dock = state.dock + key,
                    pages = pruneEmptyExtraPages(strippedPages)
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

            val safeMax = maxPerPage.coerceAtLeast(8)
            val pages = normalizePages(state.pages, safeMax).toMutableList()
            val targetIdx = pages.indexOfFirst { it.items.size < safeMax }
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
            val cleanedPages = pruneEmptyExtraPages(stripAppFromPages(state.pages, key.stableId))
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
            // Uygulamayı hem Dock'tan hem de mevcut sayfalardan/diğer klasörlerden çıkar ki kopyalanmasın
            val nextDock = state.dock.filterNot { it.stableId == appKey.stableId }
            val strippedPages = stripAppFromPages(
                pages = state.pages,
                appStableId = appKey.stableId,
                preserveFolderId = targetFolderId
            ).toMutableList()

            if (targetFolderId != null) {
                var addedToTarget = false
                val updatedPages = strippedPages.map { page ->
                    page.copy(
                        items = page.items.map { item ->
                            if (item is HomeGridItem.FolderTile && item.id == targetFolderId) {
                                addedToTarget = true
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
                if (!addedToTarget) return@mutateState state
                state.copy(
                    dock = nextDock,
                    pages = pruneEmptyExtraPages(updatedPages)
                )
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
                state.copy(
                    dock = nextDock,
                    pages = rebalancePages(strippedPages, maxPerPage)
                )
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

    suspend fun applyDragDrop(
        source: LayoutDragSource,
        destination: LayoutDropDestination,
        maxPerPage: Int,
        maxDockItems: Int
    ) {
        mutateState { state ->
            applyDragDropToState(
                state = state,
                source = source,
                destination = destination,
                maxPerPage = maxPerPage,
                maxDockItems = maxDockItems
            )
        }
    }

    private suspend fun mutateState(
        transform: (PersistedHomeState) -> PersistedHomeState
    ) = withContext(Dispatchers.IO) {
        context.homeLayoutDataStore.edit { prefs ->
            val current = prefs[stateKey]?.let { decodeCached(it) } ?: PersistedHomeState()
            val updated = transform(current)
            if (updated != current || prefs[stateKey] == null) {
                prefs[stateKey] = encodeAndPrimeCache(updated)
            }
        }
    }

    companion object {
        sealed interface LayoutDragSource {
            data class HomeSlot(
                val pageIndex: Int,
                val slotIndex: Int,
                val expectedItemId: String? = null
            ) : LayoutDragSource

            data class DockSlot(
                val dockIndex: Int,
                val expectedAppKey: AppComponentKey? = null
            ) : LayoutDragSource
        }

        sealed interface LayoutDropDestination {
            data class HomeCell(val pageIndex: Int, val slotIndex: Int) : LayoutDropDestination
            data class MergeIntoFolder(val folderId: String) : LayoutDropDestination
            data class CreateFolderWithApp(
                val targetAppKey: AppComponentKey,
                val folderTitle: String = "Klasör"
            ) : LayoutDropDestination
            data class DockCell(val insertIndex: Int) : LayoutDropDestination
        }

        /**
         * Sayfa listesini hem HomeLayoutRepository hem LauncherViewModel için birebir aynı
         * taşma/budama kurallarıyla dengeler; böylece UI sayfa/slot indeksleri DataStore ile %100 eşleşir.
         */
        fun normalizePages(
            pages: List<HomePageModel>,
            maxPerPage: Int
        ): List<HomePageModel> {
            val safeMax = maxPerPage.coerceAtLeast(8)
            val base = pages.ifEmpty { listOf(HomePageModel(id = "page_1", items = emptyList())) }
            return spillOverflowPages(pruneEmptyExtraPages(base), safeMax)
        }

        /**
         * Cihazdan silinmiş (ghost) uygulamaları Dock, sayfalar ve klasörlerden temizler;
         * tek uygulamaya düşen klasörleri tekil ikona, boşalan klasörleri ise silinmişe dönüştürür.
         */
        fun pruneUninstalledAndNormalize(
            state: PersistedHomeState,
            installedIds: Set<String>,
            maxPerPage: Int
        ): PersistedHomeState {
            if (installedIds.isEmpty()) return state
            val safeMax = maxPerPage.coerceAtLeast(8)
            val cleanDock = state.dock
                .filter { it.stableId in installedIds }
                .distinctBy { it.stableId }

            val dockIdSet = cleanDock.mapTo(HashSet()) { it.stableId }
            val seenAppIds = HashSet<String>(dockIdSet)

            val cleanedPages = state.pages.map { page ->
                page.copy(
                    items = page.items.mapNotNull { item ->
                        when (item) {
                            is HomeGridItem.AppTile -> {
                                val id = item.key.stableId
                                if (id in installedIds && seenAppIds.add(id)) item else null
                            }
                            is HomeGridItem.FolderTile -> {
                                val validApps = item.apps.filter { appKey ->
                                    val id = appKey.stableId
                                    id in installedIds && seenAppIds.add(id)
                                }
                                when (validApps.size) {
                                    0 -> null
                                    1 -> HomeGridItem.AppTile(validApps.first())
                                    else -> item.copy(apps = validApps)
                                }
                            }
                        }
                    }
                )
            }

            return state.copy(
                dock = cleanDock,
                pages = normalizePages(cleanedPages, safeMax)
            )
        }

        /**
         * Ana ekran ızgarası, klasörler ve alt Dock arasındaki tüm sürükle-bırak
         * geçişlerini (Dock'a ekleme, Dock doluysa takas etme, Dock'tan çıkarma,
         * başka uygulamanın üstüne bırakıp yeni klasör kurma, klasöre ekleme ve
         * sayfa içi/arası taşıma) atomik ve kimlik-doğrulamalı olarak uygular.
         */
        fun applyDragDropToState(
            state: PersistedHomeState,
            source: LayoutDragSource,
            destination: LayoutDropDestination,
            maxPerPage: Int,
            maxDockItems: Int
        ): PersistedHomeState {
            val safeMaxPerPage = maxPerPage.coerceAtLeast(8)
            val safeMaxDock = maxDockItems.coerceIn(3, 7)

            val initialPages = normalizePages(state.pages, safeMaxPerPage).toMutableList()
            val dock = state.dock.toMutableList()

            when (source) {
                is LayoutDragSource.HomeSlot -> {
                    val located = locateHomeSlot(initialPages, source) ?: return state
                    val actualPageIdx = located.first
                    val actualSlotIdx = located.second
                    val fromPage = initialPages[actualPageIdx]
                    val movingItem = fromPage.items[actualSlotIdx]

                    when (destination) {
                        is LayoutDropDestination.HomeCell -> {
                            if (actualPageIdx == destination.pageIndex) {
                                val list = fromPage.items.toMutableList()
                                list.removeAt(actualSlotIdx)
                                val insertIdx = destination.slotIndex.coerceIn(0, list.size)
                                list.add(insertIdx, movingItem)
                                initialPages[actualPageIdx] = fromPage.copy(items = list)
                            } else {
                                val srcList = fromPage.items.toMutableList()
                                srcList.removeAt(actualSlotIdx)
                                initialPages[actualPageIdx] = fromPage.copy(items = srcList)

                                while (initialPages.size <= destination.pageIndex) {
                                    initialPages += HomePageModel(
                                        id = "page_${UUID.randomUUID()}",
                                        items = emptyList()
                                    )
                                }
                                val dstPage = initialPages[destination.pageIndex]
                                val dstList = dstPage.items.toMutableList()
                                dstList.add(destination.slotIndex.coerceIn(0, dstList.size), movingItem)
                                initialPages[destination.pageIndex] = dstPage.copy(items = dstList)
                            }
                            return state.copy(
                                pages = normalizePages(initialPages, safeMaxPerPage)
                            )
                        }

                        is LayoutDropDestination.MergeIntoFolder -> {
                            val appTile = movingItem as? HomeGridItem.AppTile ?: return state
                            val folderExists = initialPages.any { p ->
                                p.items.any { it is HomeGridItem.FolderTile && it.id == destination.folderId }
                            }
                            if (!folderExists) return state

                            val srcList = fromPage.items.toMutableList()
                            srcList.removeAt(actualSlotIdx)
                            initialPages[actualPageIdx] = fromPage.copy(items = srcList)

                            val mergedPages = initialPages.map { page ->
                                page.copy(
                                    items = page.items.map { item ->
                                        if (item is HomeGridItem.FolderTile && item.id == destination.folderId) {
                                            if (item.apps.any { it.stableId == appTile.key.stableId }) {
                                                item
                                            } else {
                                                item.copy(apps = item.apps + appTile.key)
                                            }
                                        } else {
                                            item
                                        }
                                    }
                                )
                            }
                            return state.copy(pages = normalizePages(mergedPages, safeMaxPerPage))
                        }

                        is LayoutDropDestination.CreateFolderWithApp -> {
                            val appTile = movingItem as? HomeGridItem.AppTile ?: return state
                            if (appTile.key.stableId == destination.targetAppKey.stableId) return state
                            val targetExists = initialPages.any { p ->
                                p.items.any {
                                    it is HomeGridItem.AppTile &&
                                        it.key.stableId == destination.targetAppKey.stableId
                                }
                            }
                            if (!targetExists) return state

                            val srcList = fromPage.items.toMutableList()
                            srcList.removeAt(actualSlotIdx)
                            initialPages[actualPageIdx] = fromPage.copy(items = srcList)

                            val newFolder = HomeGridItem.FolderTile(
                                id = UUID.randomUUID().toString(),
                                title = destination.folderTitle.trim().ifEmpty { "Klasör" },
                                apps = listOf(destination.targetAppKey, appTile.key)
                            )
                            val updatedPages = initialPages.map { page ->
                                page.copy(
                                    items = page.items.map { item ->
                                        if (item is HomeGridItem.AppTile &&
                                            item.key.stableId == destination.targetAppKey.stableId
                                        ) {
                                            newFolder
                                        } else {
                                            item
                                        }
                                    }
                                )
                            }
                            return state.copy(pages = normalizePages(updatedPages, safeMaxPerPage))
                        }

                        is LayoutDropDestination.DockCell -> {
                            val appTile = movingItem as? HomeGridItem.AppTile ?: return state
                            if (dock.any { it.stableId == appTile.key.stableId }) return state

                            val srcList = fromPage.items.toMutableList()
                            srcList.removeAt(actualSlotIdx)

                            if (dock.size < safeMaxDock) {
                                dock.add(destination.insertIndex.coerceIn(0, dock.size), appTile.key)
                            } else if (dock.isNotEmpty()) {
                                val swapIdx = destination.insertIndex.coerceIn(0, dock.lastIndex)
                                val displacedKey = dock[swapIdx]
                                dock[swapIdx] = appTile.key
                                srcList.add(actualSlotIdx.coerceIn(0, srcList.size), HomeGridItem.AppTile(displacedKey))
                            }
                            initialPages[actualPageIdx] = fromPage.copy(items = srcList)
                            return state.copy(
                                dock = dock,
                                pages = normalizePages(initialPages, safeMaxPerPage)
                            )
                        }
                    }
                }

                is LayoutDragSource.DockSlot -> {
                    val actualDockIdx = locateDockSlot(dock, source) ?: return state
                    val movingAppKey = dock[actualDockIdx]

                    when (destination) {
                        is LayoutDropDestination.DockCell -> {
                            dock.removeAt(actualDockIdx)
                            dock.add(destination.insertIndex.coerceIn(0, dock.size), movingAppKey)
                            return state.copy(dock = dock)
                        }

                        is LayoutDropDestination.HomeCell -> {
                            dock.removeAt(actualDockIdx)
                            while (initialPages.size <= destination.pageIndex) {
                                initialPages += HomePageModel(
                                    id = "page_${UUID.randomUUID()}",
                                    items = emptyList()
                                )
                            }
                            val dstPage = initialPages[destination.pageIndex]
                            val dstList = dstPage.items
                                .filterNot { it is HomeGridItem.AppTile && it.key.stableId == movingAppKey.stableId }
                                .toMutableList()
                            dstList.add(
                                destination.slotIndex.coerceIn(0, dstList.size),
                                HomeGridItem.AppTile(movingAppKey)
                            )
                            initialPages[destination.pageIndex] = dstPage.copy(items = dstList)
                            return state.copy(
                                dock = dock,
                                pages = normalizePages(initialPages, safeMaxPerPage)
                            )
                        }

                        is LayoutDropDestination.MergeIntoFolder -> {
                            val folderExists = initialPages.any { p ->
                                p.items.any { it is HomeGridItem.FolderTile && it.id == destination.folderId }
                            }
                            if (!folderExists) return state

                            dock.removeAt(actualDockIdx)
                            val mergedPages = initialPages.map { page ->
                                page.copy(
                                    items = page.items.map { item ->
                                        if (item is HomeGridItem.FolderTile && item.id == destination.folderId) {
                                            if (item.apps.any { it.stableId == movingAppKey.stableId }) {
                                                item
                                            } else {
                                                item.copy(apps = item.apps + movingAppKey)
                                            }
                                        } else {
                                            item
                                        }
                                    }
                                )
                            }
                            return state.copy(
                                dock = dock,
                                pages = normalizePages(mergedPages, safeMaxPerPage)
                            )
                        }

                        is LayoutDropDestination.CreateFolderWithApp -> {
                            if (movingAppKey.stableId == destination.targetAppKey.stableId) return state
                            val targetExists = initialPages.any { p ->
                                p.items.any {
                                    it is HomeGridItem.AppTile &&
                                        it.key.stableId == destination.targetAppKey.stableId
                                }
                            }
                            if (!targetExists) return state

                            dock.removeAt(actualDockIdx)
                            val newFolder = HomeGridItem.FolderTile(
                                id = UUID.randomUUID().toString(),
                                title = destination.folderTitle.trim().ifEmpty { "Klasör" },
                                apps = listOf(destination.targetAppKey, movingAppKey)
                            )
                            val updatedPages = initialPages.map { page ->
                                page.copy(
                                    items = page.items.map { item ->
                                        if (item is HomeGridItem.AppTile &&
                                            item.key.stableId == destination.targetAppKey.stableId
                                        ) {
                                            newFolder
                                        } else {
                                            item
                                        }
                                    }
                                )
                            }
                            return state.copy(
                                dock = dock,
                                pages = normalizePages(updatedPages, safeMaxPerPage)
                            )
                        }
                    }
                }
            }
        }

        private fun locateHomeSlot(
            pages: List<HomePageModel>,
            source: LayoutDragSource.HomeSlot
        ): Pair<Int, Int>? {
            val expectedId = source.expectedItemId
            val directItem = pages.getOrNull(source.pageIndex)?.items?.getOrNull(source.slotIndex)
            if (expectedId == null) {
                return if (directItem != null) source.pageIndex to source.slotIndex else null
            }
            if (directItem?.itemId == expectedId) {
                return source.pageIndex to source.slotIndex
            }
            for ((pIdx, page) in pages.withIndex()) {
                val sIdx = page.items.indexOfFirst { it.itemId == expectedId }
                if (sIdx >= 0) return pIdx to sIdx
            }
            return null
        }

        private fun locateDockSlot(
            dock: List<AppComponentKey>,
            source: LayoutDragSource.DockSlot
        ): Int? {
            val expected = source.expectedAppKey
            val direct = dock.getOrNull(source.dockIndex)
            if (expected == null) {
                return if (direct != null) source.dockIndex else null
            }
            if (direct?.stableId == expected.stableId) {
                return source.dockIndex
            }
            val found = dock.indexOfFirst { it.stableId == expected.stableId }
            return if (found >= 0) found else null
        }

        private fun stripAppFromPages(
            pages: List<HomePageModel>,
            appStableId: String,
            preserveFolderId: String? = null
        ): List<HomePageModel> {
            return pages.map { page ->
                page.copy(
                    items = page.items.mapNotNull { item ->
                        when (item) {
                            is HomeGridItem.AppTile ->
                                if (item.key.stableId == appStableId) null else item
                            is HomeGridItem.FolderTile -> {
                                if (preserveFolderId != null && item.id == preserveFolderId) {
                                    item
                                } else {
                                    val rem = item.apps.filterNot { it.stableId == appStableId }
                                    when (rem.size) {
                                        0 -> null
                                        1 -> HomeGridItem.AppTile(rem.first())
                                        else -> item.copy(apps = rem)
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }

        private fun pruneEmptyExtraPages(pages: List<HomePageModel>): List<HomePageModel> {
            val nonEmpty = pages.filter { it.items.isNotEmpty() }
            return nonEmpty.ifEmpty {
                listOf(pages.firstOrNull()?.copy(items = emptyList()) ?: HomePageModel(id = "page_1", items = emptyList()))
            }
        }

        private fun spillOverflowPages(
            pages: List<HomePageModel>,
            maxPerPage: Int
        ): List<HomePageModel> {
            val result = ArrayList<HomePageModel>()
            var carryOver = emptyList<HomeGridItem>()
            for (page in pages) {
                val combined = carryOver + page.items
                if (combined.size <= maxPerPage) {
                    result += page.copy(items = combined)
                    carryOver = emptyList()
                } else {
                    result += page.copy(items = combined.take(maxPerPage))
                    carryOver = combined.drop(maxPerPage)
                }
            }
            while (carryOver.isNotEmpty()) {
                val chunk = carryOver.take(maxPerPage)
                carryOver = carryOver.drop(maxPerPage)
                result += HomePageModel(id = "page_${result.size + 1}", items = chunk)
            }
            return result.ifEmpty { listOf(HomePageModel(id = "page_1", items = emptyList())) }
        }

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
                            TurkishSearchNormalizer.foldAscii(app.key.packageName).contains(kw) ||
                                TurkishSearchNormalizer.foldAscii(app.label).contains(kw)
                        }
                }
                if (match != null && dockCandidates.size < dockMaxItems) {
                    dockCandidates += match
                }
            }

            pickDockCandidate(listOf("dialer", "phone", "telefon"))
            pickDockCandidate(listOf("messages", "messaging", "mms", "whatsapp", "mesaj"))
            pickDockCandidate(listOf("chrome", "browser", "safari", "firefox", "tarayici"))
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
