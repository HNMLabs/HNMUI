package com.hnmlabs.hnmui.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hnmlabs.hnmui.core.formfactor.FormFactorPolicy
import com.hnmlabs.hnmui.core.formfactor.LauncherFeatureFlags
import com.hnmlabs.hnmui.core.formfactor.LauncherGridSpec
import com.hnmlabs.hnmui.data.AppCategoryClassifier
import com.hnmlabs.hnmui.data.HomeLayoutRepository
import com.hnmlabs.hnmui.data.InstalledAppsRepository
import com.hnmlabs.hnmui.model.AppComponentKey
import com.hnmlabs.hnmui.model.AppItem
import com.hnmlabs.hnmui.model.AppLibraryGroup
import com.hnmlabs.hnmui.model.AppShortcutItem
import com.hnmlabs.hnmui.model.HomeGridItem
import com.hnmlabs.hnmui.model.PersistedHomeState
import com.hnmlabs.hnmui.model.QuickNoteItem
import com.hnmlabs.hnmui.model.ResolvedHomeItem
import com.hnmlabs.hnmui.model.ResolvedHomePage
import com.hnmlabs.hnmui.service.HnmNotificationListenerService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

data class ActiveFolderModal(
    val folderId: String?,
    val title: String,
    val isEditableTitle: Boolean,
    val apps: List<AppItem>
)

data class ActiveQuickMenu(
    val app: AppItem,
    val shortcuts: List<AppShortcutItem>,
    val containingFolderId: String? = null
)

data class LauncherOverlayState(
    val isSpotlightOpen: Boolean = false,
    val spotlightQuery: String = "",
    val isEditMode: Boolean = false,
    val openFolderModal: ActiveFolderModal? = null,
    val quickMenu: ActiveQuickMenu? = null,
    val smallestScreenWidthDp: Int = 392,
    val isDefaultLauncher: Boolean = false,
    val isNotificationAccessEnabled: Boolean = false
)

data class LauncherUiState(
    val gridSpec: LauncherGridSpec,
    val allApps: List<AppItem>,
    val dockApps: List<AppItem>,
    val homePages: List<ResolvedHomePage>,
    val existingFolders: List<ResolvedHomeItem.Folder>,
    val appLibraryGroups: List<AppLibraryGroup>,
    val suggestedApps: List<AppItem>,
    val spotlightMatches: List<AppItem>,
    val quickNotes: List<QuickNoteItem>,
    val boundWidgetIds: List<Int>,
    val totalUnreadBadges: Int,
    val isSpotlightOpen: Boolean,
    val spotlightQuery: String,
    val isEditMode: Boolean,
    val openFolderModal: ActiveFolderModal?,
    val quickMenu: ActiveQuickMenu?,
    val isDefaultLauncher: Boolean,
    val isNotificationAccessEnabled: Boolean
)

/**
 * HNMUI tek yönlü durum yönetimi (UDF) ViewModel sınıfı.
 * Yüklü uygulamaları, canlı bildirim rozetlerini, kalıcı DataStore düzenini ve
 * aktif katman durumlarını tek bir reaktif LauncherUiState akışında birleştirir.
 */
class LauncherViewModel(
    private val appsRepository: InstalledAppsRepository,
    private val layoutRepository: HomeLayoutRepository,
    private val featureFlags: LauncherFeatureFlags
) : ViewModel() {

    private val trLocale = Locale("tr", "TR")
    private val overlayState = MutableStateFlow(LauncherOverlayState())

    private val _scrollToPrimaryHomeSignal = Channel<Unit>(Channel.CONFLATED)
    val scrollToPrimaryHomeSignal = _scrollToPrimaryHomeSignal.receiveAsFlow()

    private val defaultGridSpec = FormFactorPolicy.resolve(392, featureFlags)

    val uiState: StateFlow<LauncherUiState> = combine(
        appsRepository.apps,
        HnmNotificationListenerService.badgeCounts,
        layoutRepository.persistedState,
        overlayState
    ) { rawApps, badges, persisted, overlays ->
        val gridSpec = FormFactorPolicy.resolve(
            smallestScreenWidthDp = overlays.smallestScreenWidthDp,
            flags = featureFlags
        )

        val badgedApps = rawApps.map { app ->
            app.copy(badgeCount = badges[app.key.packageName] ?: 0)
        }
        val appById = badgedApps.associateBy { it.key.stableId }

        val effectivePersisted = persisted ?: HomeLayoutRepository.buildInitialSeedState(
            installedApps = badgedApps,
            maxPerPage = gridSpec.maxItemsPerPage,
            dockMaxItems = gridSpec.dockMaxItems
        )

        if (persisted == null && badgedApps.isNotEmpty()) {
            viewModelScope.launch {
                layoutRepository.ensureInitialSeed(
                    installedApps = badgedApps,
                    maxPerPage = gridSpec.maxItemsPerPage,
                    dockMaxItems = gridSpec.dockMaxItems
                )
            }
        }

        val dockApps = effectivePersisted.dock.mapNotNull { appById[it.stableId] }
        val resolvedFolders = ArrayList<ResolvedHomeItem.Folder>()

        val resolvedPages = effectivePersisted.pages.map { page ->
            val items = page.items.mapNotNull { gridItem ->
                when (gridItem) {
                    is HomeGridItem.AppTile -> {
                        appById[gridItem.key.stableId]?.let { ResolvedHomeItem.App(it) }
                    }
                    is HomeGridItem.FolderTile -> {
                        val folderApps = gridItem.apps.mapNotNull { appById[it.stableId] }
                        if (folderApps.isEmpty()) {
                            null
                        } else {
                            ResolvedHomeItem.Folder(
                                id = gridItem.id,
                                title = gridItem.title,
                                apps = folderApps,
                                totalBadges = folderApps.sumOf { it.badgeCount }
                            ).also { resolvedFolders += it }
                        }
                    }
                }
            }
            ResolvedHomePage(id = page.id, items = items)
        }.ifEmpty { listOf(ResolvedHomePage(id = "page_1", items = emptyList())) }

        val libraryGroups = AppCategoryClassifier.buildAppLibraryGroups(
            apps = badgedApps,
            launchCounts = effectivePersisted.launchCounts
        )

        val suggestedApps = libraryGroups
            .firstOrNull()
            ?.apps
            ?.take(4)
            ?: badgedApps.take(4)

        val queryTrimmed = overlays.spotlightQuery.trim().lowercase(trLocale)
        val spotlightMatches = if (queryTrimmed.isEmpty()) {
            emptyList()
        } else {
            badgedApps
                .filter {
                    it.label.lowercase(trLocale).contains(queryTrimmed) ||
                        it.key.packageName.lowercase().contains(queryTrimmed)
                }
                .sortedByDescending {
                    it.label.lowercase(trLocale).startsWith(queryTrimmed)
                }
        }

        // Açık klasör penceresi varsa içeriğini güncel rozet ve isimle senkronize tut
        val syncedFolderModal = overlays.openFolderModal?.let { modal ->
            if (modal.folderId != null) {
                val liveFolder = resolvedFolders.firstOrNull { it.id == modal.folderId }
                if (liveFolder != null) {
                    modal.copy(title = liveFolder.title, apps = liveFolder.apps)
                } else {
                    null
                }
            } else {
                modal.copy(apps = modal.apps.mapNotNull { appById[it.key.stableId] })
            }
        }

        LauncherUiState(
            gridSpec = gridSpec,
            allApps = badgedApps,
            dockApps = dockApps,
            homePages = resolvedPages,
            existingFolders = resolvedFolders,
            appLibraryGroups = libraryGroups,
            suggestedApps = suggestedApps,
            spotlightMatches = spotlightMatches,
            quickNotes = effectivePersisted.quickNotes,
            boundWidgetIds = effectivePersisted.boundWidgetIds,
            totalUnreadBadges = badges.values.sum(),
            isSpotlightOpen = overlays.isSpotlightOpen,
            spotlightQuery = overlays.spotlightQuery,
            isEditMode = overlays.isEditMode,
            openFolderModal = syncedFolderModal,
            quickMenu = overlays.quickMenu,
            isDefaultLauncher = overlays.isDefaultLauncher,
            isNotificationAccessEnabled = overlays.isNotificationAccessEnabled
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LauncherUiState(
            gridSpec = defaultGridSpec,
            allApps = emptyList(),
            dockApps = emptyList(),
            homePages = listOf(ResolvedHomePage(id = "page_1", items = emptyList())),
            existingFolders = emptyList(),
            appLibraryGroups = emptyList(),
            suggestedApps = emptyList(),
            spotlightMatches = emptyList(),
            quickNotes = emptyList(),
            boundWidgetIds = emptyList(),
            totalUnreadBadges = 0,
            isSpotlightOpen = false,
            spotlightQuery = "",
            isEditMode = false,
            openFolderModal = null,
            quickMenu = null,
            isDefaultLauncher = false,
            isNotificationAccessEnabled = false
        )
    )

    fun updateDeviceAndPermissionStatus(
        smallestScreenWidthDp: Int,
        isDefaultLauncher: Boolean,
        isNotificationAccessEnabled: Boolean
    ) {
        overlayState.update {
            it.copy(
                smallestScreenWidthDp = smallestScreenWidthDp,
                isDefaultLauncher = isDefaultLauncher,
                isNotificationAccessEnabled = isNotificationAccessEnabled
            )
        }
    }

    fun getCachedIcon(key: AppComponentKey): ImageBitmap? = appsRepository.getCachedIcon(key)

    suspend fun loadIconIfNeeded(key: AppComponentKey): ImageBitmap? =
        appsRepository.loadIconIfNeeded(key)

    fun launchApp(app: AppItem) {
        dismissAllOverlays()
        if (appsRepository.launchApp(app.key)) {
            viewModelScope.launch {
                layoutRepository.recordAppLaunch(app.key)
            }
        }
    }

    fun launchShortcut(shortcut: AppShortcutItem) {
        dismissQuickMenu()
        appsRepository.launchShortcut(shortcut)
    }

    fun openSpotlight() {
        overlayState.update {
            it.copy(
                isSpotlightOpen = true,
                spotlightQuery = "",
                isEditMode = false,
                openFolderModal = null,
                quickMenu = null
            )
        }
    }

    fun updateSpotlightQuery(query: String) {
        overlayState.update { it.copy(spotlightQuery = query) }
    }

    fun closeSpotlight() {
        overlayState.update { it.copy(isSpotlightOpen = false, spotlightQuery = "") }
    }

    fun toggleEditMode() {
        overlayState.update { it.copy(isEditMode = !it.isEditMode, quickMenu = null) }
    }

    fun openHomeFolder(folder: ResolvedHomeItem.Folder) {
        overlayState.update {
            it.copy(
                openFolderModal = ActiveFolderModal(
                    folderId = folder.id,
                    title = folder.title,
                    isEditableTitle = true,
                    apps = folder.apps
                ),
                quickMenu = null
            )
        }
    }

    fun openAppLibraryCategory(group: AppLibraryGroup) {
        overlayState.update {
            it.copy(
                openFolderModal = ActiveFolderModal(
                    folderId = null,
                    title = group.category.titleTr,
                    isEditableTitle = false,
                    apps = group.apps
                ),
                quickMenu = null
            )
        }
    }

    fun closeFolderModal() {
        overlayState.update { it.copy(openFolderModal = null) }
    }

    fun renameOpenFolder(newTitle: String) {
        val folderId = overlayState.value.openFolderModal?.folderId ?: return
        viewModelScope.launch {
            layoutRepository.renameFolder(folderId, newTitle)
        }
    }

    fun openQuickMenu(app: AppItem, containingFolderId: String? = null) {
        overlayState.update {
            it.copy(
                quickMenu = ActiveQuickMenu(
                    app = app,
                    shortcuts = emptyList(),
                    containingFolderId = containingFolderId
                )
            )
        }
        viewModelScope.launch {
            val shortcuts = appsRepository.queryShortcutsForPackage(
                packageName = app.key.packageName,
                userSerial = app.key.userSerial
            )
            overlayState.update { current ->
                val active = current.quickMenu
                if (active != null && active.app.key.stableId == app.key.stableId) {
                    current.copy(quickMenu = active.copy(shortcuts = shortcuts))
                } else {
                    current
                }
            }
        }
    }

    fun dismissQuickMenu() {
        overlayState.update { it.copy(quickMenu = null) }
    }

    fun addAppToHomeScreen(app: AppItem) {
        val maxPerPage = uiState.value.gridSpec.maxItemsPerPage
        viewModelScope.launch {
            layoutRepository.addAppToHomeScreen(app.key, maxPerPage)
        }
    }

    fun removeAppFromHomeScreen(key: AppComponentKey) {
        viewModelScope.launch {
            layoutRepository.removeFromHomeScreen(key)
        }
    }

    fun createNewFolderWithApp(app: AppItem) {
        viewModelScope.launch {
            layoutRepository.createOrAddToFolder(
                appKey = app.key,
                targetFolderId = null,
                folderTitle = app.category.titleTr
            )
        }
    }

    fun moveAppToFolder(app: AppItem, folderId: String) {
        viewModelScope.launch {
            layoutRepository.createOrAddToFolder(
                appKey = app.key,
                targetFolderId = folderId
            )
        }
    }

    fun extractAppFromFolderToHome(folderId: String, app: AppItem) {
        val maxPerPage = uiState.value.gridSpec.maxItemsPerPage
        viewModelScope.launch {
            layoutRepository.removeAppFromFolderToHome(folderId, app.key, maxPerPage)
        }
    }

    fun addQuickNote(text: String) {
        viewModelScope.launch { layoutRepository.addQuickNote(text) }
    }

    fun toggleQuickNote(noteId: String) {
        viewModelScope.launch { layoutRepository.toggleQuickNote(noteId) }
    }

    fun deleteQuickNote(noteId: String) {
        viewModelScope.launch { layoutRepository.deleteQuickNote(noteId) }
    }

    fun onWidgetBound(appWidgetId: Int) {
        viewModelScope.launch { layoutRepository.bindWidgetId(appWidgetId) }
    }

    fun onWidgetRemoved(appWidgetId: Int) {
        viewModelScope.launch { layoutRepository.removeWidgetId(appWidgetId) }
    }

    fun onHomeIntentReceived() {
        dismissAllOverlays()
        _scrollToPrimaryHomeSignal.trySend(Unit)
    }

    fun dismissAllOverlays() {
        overlayState.update {
            it.copy(
                isSpotlightOpen = false,
                spotlightQuery = "",
                isEditMode = false,
                openFolderModal = null,
                quickMenu = null
            )
        }
    }

    companion object {
        fun factory(
            appsRepository: InstalledAppsRepository,
            layoutRepository: HomeLayoutRepository,
            featureFlags: LauncherFeatureFlags
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LauncherViewModel(
                    appsRepository = appsRepository,
                    layoutRepository = layoutRepository,
                    featureFlags = featureFlags
                ) as T
            }
        }
    }
}
