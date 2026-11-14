# Changelog / Sürüm Geçmişi

All notable changes to **HNMUI** are documented in this file.  
**HNMUI** projesindeki tüm önemli değişiklikler bu dosyada kayıt altına alınır.

---

## [0.3.4.1-alpha] — 2026-11-14 (Deep Bug Hunt, Theme Zero-Storm & Gesture Precision Hotfix)

### 🐛 Fixed / Düzeltilen Hatalar
- **EN:** **Eliminated 45× Full-Tree Theme Recomposition Storm (`HnmTheme`):** Replaced `staticCompositionLocalOf` on `LocalHnmColors` with tracked `compositionLocalOf`, keyed `MaterialTheme` `colorScheme` and `typography` strictly on `isDark` (1 invalidation instead of ~45 per transition), and drove live default text animation through tracked `LocalContentColor`.
- **EN:** **Fixed Broken "Create New Folder" Quick Menu Action (`HomeLayoutRepository` & `LauncherViewModel`):** Fixed `pruneUninstalledAndNormalize` and `stripAppFromPages` prematurely collapsing intentional 1-app folders created via *"Yeni Klasör Oluştur"* back into `AppTile`s; newly created folders now persist cleanly, survive unrelated Dock/grid drags, and open immediately for inline renaming.
- **EN:** **Protected Physical/Gesture Home Button Signal Against Mid-Scroll Touch Interruption (`LauncherScreen`):** Guarded `pagerState.animateScrollToPage` inside `scrollToPrimaryHomeSignal.collect` with `runCatching` + `ensureActive()`, preventing `MutationInterruptedException` from permanently terminating the Home button listener.
- **EN:** **Eliminated `8dp–14dp` Top Status Bar Flick Dead-Zone & Corner Cutout Overlap (`LauncherScreen` & `HnmIosStatusBar`):** Aligned `topPullThresholdPx` with `touchSlop` (`min(8.dp, touchSlop * 0.9f)`) so short downward flicks on `HnmIosStatusBar` open Control Center / Notification Center 100% of the time, and applied dynamic `displayCutout` top/left/right safe padding.
- **EN:** **Fixed Upward Swipe-to-Dismiss on Scrollable Control Center & Notification Center (`ControlCenterOverlay` & `NotificationCenterOverlay`):** Added `NestedScrollConnection` post-scroll upward drag capture so swiping up anywhere over the scrollable Control Center sheet or Notification `LazyColumn` dismisses the overlay smoothly, alongside `ON_RESUME` battery/ringer sync and outside-tap focus clearing.
- **EN:** **Overlay, System Bar & R8 Hardening (`FolderModalOverlay`, `QuickActionMenuOverlay`, `SpotlightOverlay`, `LauncherActivity`, `proguard-rules.pro`):** Auto-saved edited folder names on Android Back press / app launch; added multi-folder move support (up to 4 folders) + vertical scroll + frozen exit snapshot in `QuickActionMenuOverlay`; retracted soft keyboard immediately on Spotlight close; synced Light/Dark status & navigation bar icon contrast across `onResume` and transient swipes; hardened R8 keep rules for `@Serializable` sealed interfaces/enums; and expanded `LauncherCoreTest` to **21 automated unit tests**.
- **TR:** **Açık ↔ Koyu Geçişindeki 45× Tam Ağaç Recomposition Fırtınası Giderildi (`HnmTheme`):** `LocalHnmColors` `compositionLocalOf` yapısına geçirildi, `MaterialTheme` statik sağlayıcıları yalnızca `isDark` değişimine kilitlendi ve metin geçişleri izlenebilir `LocalContentColor` ile sıfır takılmalı **120 FPS** akışa alındı.
- **TR:** **"Yeni Klasör Oluştur" Klasör Dağılma Hatası Çözüldü (`HomeLayoutRepository` & `LauncherViewModel`):** Hızlı menüden oluşturulan 1 uygulamalı klasörlerin `pruneUninstalledAndNormalize` ve `stripAppFromPages` tarafından anında tekil ikona dönüştürülmesi hatası düzeltildi; yeni klasör artık korunur ve anında yeniden adlandırma penceresiyle açılır.
- **TR:** **Home Tuşu Dinleyicisi & Üst Çubuk Kısa Çekme Ölü Bölgesi Giderildi (`LauncherScreen` & `HnmIosStatusBar`):** Kaydırma sırasında ekrana dokunulduğunda Home tuşu sinyal dinleyicisinin kalıcı olarak durması engellendi; üst çubukta tıklama ile çekme arasındaki `8dp–14dp` ölü bölge sıfırlandı ve köşe kamera delikleri (`displayCutout`) için yatay/dikey güvenli boşluk eklendi.
- **TR:** **Denetim Merkezi & Bildirim Merkezi Yukarı Kaydırarak Kapatma Düzeltildi:** Kaydırılabilir kart listelerinin yukarı kapatma hareketini yutması `NestedScrollConnection` ile çözüldü; klasör adı geri tuşunda otomatik kaydedilir hale getirildi, hızlı menüye çoklu klasöre taşıma + dikey kaydırma eklendi, Spotlight kapanışında klavye anında gizlendi ve birim test paketi **21 kapsamlı teste** çıkarıldı.

---

## [0.3.4-alpha] — 2026-11-13 (System Adaptive Glass & Zero-Miss Control Center)

### 🌗 System Adaptive Appearance Engine / Sistem Uyumlu Açık & Koyu Cam Motoru
- **EN:** Introduced flagship **`HnmThemeMode` (`SYSTEM` / `LIGHT` / `DARK`)** tracking Android's live system Dark/Light mode (`isSystemInDarkTheme()` via zero-restart `uiMode` handling) alongside instant manual overrides in **Control Center** (`Otomatik` / `Açık` / `Koyu`) and **HNM Settings** (*Home Screen, Folder & Dock* + *Display & Brightness*).
- **EN:** Engineered smooth **`380ms` cubic-bezier (`0.22, 1.0, 0.36, 1.0`) color & glass transitions** across **Midnight Dark Glass** and **Daylight Pearlescent Glass** (`GlassSurface` `DOCK` / `CARD` / `ELEVATED_MODAL` / `PILL`, overlays, typography, inputs, sliders, switches, and `WindowInsetsControllerCompat` status/navigation bar contrast) with **0% idle CPU cost** once settled.
- **EN:** Separated `AppIconTile` / `FolderIconTile` optical contrast (`onGlassSurface = true` crisp ink inside frosted sheets vs shadowed `onWallpaperPrimary` over live wallpapers) for full WCAG readability on both bright and dark wallpapers.
- **TR:** Android sisteminin Açık/Koyu modunu yeniden başlatma gerektirmeden canlı takip eden ve **Denetim Merkezi** ile **HNM Ayarlar** (*Ana Ekran, Klasör & Dock* ve *Ekran ve Parlaklık*) üzerinden anında değiştirilebilen **`HnmThemeMode` (`Otomatik` / `Açık` / `Koyu`)** motoru eklendi.
- **TR:** **Gece Koyu Cam** (*Midnight Dark Glass*) ile **Sedefli Açık Cam** (*Daylight Pearlescent Glass*) arasında tüm cam yüzeyler, kartlar, sürgüler, yazılar ve sistem çubukları boyunca **`380 ms` akıcı geçiş animasyonu** (boşta **%0 CPU** yükü) ve tam kontrast uyumu sağlandı.

### 🎯 Zero-Miss Control Center & Performance Polish / Kesintisiz Denetim Merkezi ve Performans
- **EN:** Rebuilt top/right pull-down classification (`classifyDownwardPull`) with a camera-cutout-aware **`86dp+` top strip across all pages & Edit Mode**, **`14dp` instant trigger threshold**, natural diagonal right-thumb arc tolerance (`dy >= dx * 0.72f`), expanded mid-right Home one-handed pull zone (`68%` height), full-half-width `HnmIosStatusBar` tap/pull targets, post-trigger pointer drain loop, auto-rehide of transient OEM status bars (`1.6s`), and non-blocking hit-testing on exiting overlays.
- **EN:** Eliminated idle per-slot `animateFloatAsState` + `graphicsLayer` allocations in `LauncherBottomDockBar` when not dragging and memoized `cameraApp` lookup in `NotificationCenterOverlay`.
- **TR:** Denetim Merkezi'nin bazen açılıp bazen açılmama sorunu kökten çözüldü: kamera deliği duyarlı **`86dp+` üst şerit** (tüm sayfalar + Düzenleme Modu), **`14dp` hızlı tetikleme**, sağ başparmak çapraz kavis toleransı, sağ kenar tek el çekme alanı (`%68` yükseklik), tam yarım ekran `HnmIosStatusBar` dokunma/çekme hedefleri, kapanan panellerde dokunma kilitlememe ve geçici sistem çubuğunu otomatik gizleme (`1.6 sn`) eklendi.
- **TR:** Sürükleme yokken alt Dock yuvalarındaki boşta animatör ve GPU katmanı tahsisatları sıfırlandı; Bildirim Merkezi kamera kısayolu araması önbelleğe alındı.

---

## [0.3.3-alpha] — 2026-11-13 (Zero-Freeze & 120 FPS Fluidity Engine)

### ⚡ Performance & Fluidity / Performans ve Sıfır Donma Motoru
- **EN:** Eliminated synchronous Main-Thread Binder/SQLite IPC stalls in `DeviceSystemController` (`setBrightnessFraction`, `setStreamVolumeFraction`, `toggleFlashlight`, `dispatchMediaKey`, `setAutoBrightness`, `setRotationLocked`, `setScreenTimeoutMs`) via conflated `Dispatchers.IO` pipelines and cached stream bounds.
- **EN:** Upgraded `LauncherScreen`, `HomeGridPage`, `AppIconTile`, and `LauncherDragDropController` to a pure **Draw-Phase 120Hz Pipeline**: `HorizontalPager` offset math, overlay background depth recession (`94.5%`), drag ghost scale, and shared `LocalEditJigglePhase` icon jiggle all execute inside `Modifier.graphicsLayer { ... }` with **zero composition/layout invalidations** and zero idle per-cell `Animatable` allocations.
- **EN:** Precomputed `SquircleShape` unit superellipse vertices with a thread-safe LRU `Outline` cache (zero `sin`/`cos`/`pow` or `Path` allocations while scrolling), hoisted `GlassSurface` border/fill brushes, and annotated all UI/domain state models `@Immutable`.
- **EN:** Decoupled `InstalledAppsRepository` cold-start app emission from cooperative background icon pre-warming (`512`-entry LRU cache + `inFlightIconLoads` deduplication + `45s` `onResume` scan throttle), moved `LauncherActivity` permission/role checks off the UI thread, cached decoded `HomeLayoutRepository` JSON on `Dispatchers.IO`, deferred `recordAppLaunch` by `450ms` after app start, and added `HnmSteppedCustomizationSlider` + pre-folded `PreparedSearchQuery` matching.
- **TR:** Denetim Merkezi ve HNM Ayarlar'daki parlaklık, ses, el feneri, medya ve döndürme kilidi işlemlerinin Main Thread üzerindeki senkron Binder/SQLite IPC blokajları tamamen kaldırıldı (`Dispatchers.IO` conflated akış mimarisi + yerel sürgü takibi).
- **TR:** `LauncherScreen`, `HomeGridPage`, `AppIconTile` ve `LauncherDragDropController` tamamen **Draw-Phase (GPU Çizim Katmanı)** mimarisine geçirildi: sayfa kaydırma (`HorizontalPager`), arka plan derinlik çekilmesi, sürükle-bırak ve tek saatli (`LocalEditJigglePhase`) jiggle animasyonları **sıfır recomposition** ve sıfır boşta hücre animatörü ile sabit **120 FPS** çalışır.
- **TR:** `SquircleShape` süper-elips trigonometrisi önbelleğe alındı (`LRU Outline Cache`), `GlassSurface` fırçaları sabitlendi, tüm veri/arayüz modelleri `@Immutable` yapıldı ve Türkçe arama (`PreparedSearchQuery`) önden katlanmış alanlarla sıfır tahsisatlı hale getirildi.
- **TR:** Açılışta uygulama listesi ikon yüklemesini beklemeden anında ekrana gelir (`512` kapasiteli LRU önbellek + tekrarlı yükleme birleştirme + `45 sn` `onResume` tarama koruması), `LauncherActivity` izin kontrolleri arka plana taşındı ve uygulama başlatma animasyonunun takılmaması için sayaç yazımı `450 ms` ertelendi.

---

## [0.3.2-alpha] — 2026-11-13 (Bug Hunt & Stability Sweep)

### 🐛 Fixed / Düzeltilenler
- **EN:** Unified UI ↔ DataStore page/slot normalization (`normalizePages`, `pruneUninstalledAndNormalize`) with identity-verified drag sources (`expectedItemId`, `expectedAppKey`), eliminating shifted-slot drops and ghost uninstalled apps.
- **EN:** Added iOS-style **App-on-App Drag Folder Creation** (`HoverDropTarget.CreateFolderWithApp` + frosted squircle halo) and prevented Dock + Folder app duplication.
- **EN:** Introduced `TurkishSearchNormalizer` fixing Turkish `'I' / 'İ' / 'ı' / 'i'` search mismatches (`"InShot"` ↔ `"inshot"`, `"Instagram"` ↔ `"insta"`, `"İşCep"` ↔ `"iscep"`) across Spotlight, App Library, Settings, and `AppCategoryClassifier`.
- **EN:** Removed impure `viewModelScope.launch` side-effects from `LauncherViewModel.uiState` `combine`, added DataStore `ReplaceFileCorruptionHandler` + `IOException` recovery, coalesced `InstalledAppsRepository` scans, added `FLAG_ACTIVITY_NEW_TASK` fallback, and evicted updated/uninstalled icons from cache.
- **EN:** Wired `customization.hapticFeedbackEnabled` globally via `CompositionLocalProvider(LocalHapticFeedback)`, synced Flashlight state across Control Center & Notification Center via `CameraManager.TorchCallback`, fixed Today View orphan widgets / `key(widgetId)` / `%0` battery fallback, bound `BuildConfig.VERSION_NAME` in Settings with `Dispatchers.IO` + `ON_RESUME` snapshots, deduplicated `LazyColumn` notification keys, guarded multi-touch dual-drag, and grouped Kotlin/AndroidX Dependabot PRs.
- **TR:** UI ile DataStore arasındaki sayfa/slot senkronizasyonu (`normalizePages` + kimlik doğrulamalı sürükleme) birleştirildi; silinen uygulamaların hayalet ikonları ve 1 uygulamalı klasör kalıntıları otomatik temizlendi.
- **TR:** Bir uygulamayı başka bir uygulamanın üstüne sürükleyip bırakarak **yeni klasör oluşturma** (buzlu squircle önizleme halkası ile) eklendi; Dock ve Klasör kopyalanma hatası giderildi.
- **TR:** `TurkishSearchNormalizer` ile Türkçe/İngilizce `'I' / 'İ' / 'ı' / 'i'` arama hatası (`"InShot"`, `"Instagram"`, `"İşCep"`, `"7×7 Izgara"`) Spotlight, Uygulama Arşivi ve Ayarlar genelinde çözüldü; arama sonuçlarına uzun basma menüsü eklendi.
- **TR:** Haptic Touch kapatma ayarı tüm yüzeylere bağlandı, El Feneri durumu `TorchCallback` ile senkronize edildi, Today View widget/pil hataları ve HNM Ayarlar sürüm (`BuildConfig.VERSION_NAME`) / arka plan yenileme sorunları giderildi.

---

## [0.3.1-alpha] — 2026-11-12 (Hotfix)

### 🛠️ Fixed / Düzeltilenler
- **EN:** Fixed a critical Main-Thread infinite loop in `LauncherScreen` (`awaitEachGesture`) that caused a freeze and crash when lifting the finger after pulling down Control Center.
- **EN:** Hardened `HnmIosStatusBar` broadcast registration with `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` and moved status/overlay Binder IPC reads to `Dispatchers.IO` with `IntOffset.VisibilityThreshold` slide springs.
- **TR:** Sağ üstten Denetim Merkezi indirilirken parmak kalktığı anda `LauncherScreen` `awaitEachGesture` bloğunda oluşan Main Thread sonsuz döngüsü (takılma ve çökme) giderildi.
- **TR:** Android 14/15/One UI 9 `BroadcastReceiver` `SecurityException` koruması eklendi; Denetim ve Bildirim Merkezi sistem sorguları `Dispatchers.IO`'ya taşındı.

---

## [0.3.0-alpha] — 2026-11-12

### ✨ Added / Eklenenler
- **EN:** Added calm **iOS Status Bar** (`HnmIosStatusBar`) hiding One UI's top status bar icons, plus root `PointerEventPass.Initial` pull-down gesture interception.
- **EN:** Added **HNM Notification & Summary Center** (`NotificationCenterOverlay`) on top-left pull-down with Cover Sheet clock/date, live battery/ringer capsules, inline Quick Reminders, Suggested Apps, and Flashlight/Camera buttons.
- **EN:** Added **120 FPS App & Folder Drag-and-Drop** (`LauncherDragDropController`) supporting grid reordering, cross-page edge paging, folder merging, **dragging apps into the Dock**, and **dragging apps out of the Dock**.
- **EN:** Added minimalist depth recession (`94.5%` GPU scale), subtle page scale/fade transitions, animated active page capsule, and phase-staggered organic Edit Mode jiggle.
- **TR:** One UI üst durum çubuğunu gizleyen sade **iOS Durum Çubuğu** ve `PointerEventPass.Initial` tabanlı güvenilir sağ üst / sol üst çekme hareketleri eklendi.
- **TR:** Sol üstten çekince One UI yerine açılan **HNM Bildirim & Özet Merkezi** eklendi.
- **TR:** **Dock'a uygulama sürükleyip ekleme** ve **Dock'tan ana ekrana uygulama çıkarma** dahil tam 120 FPS uygulama/klasör sürükle-bırak mekanizması getirildi.

---

## [0.2.0-alpha] — 2026-11-12

### ✨ Added / Eklenenler
- **EN:** Built-in One UI-independent **HNM Settings** (`HnmSettingsOverlay` & `DeviceSystemController`) covering Battery (`%`, health, `°C`, voltage), Display & 120 FPS live Brightness, Sound & Haptics, Storage, Apps, Connectivity mini-panels, and RAM/Device Info.
- **EN:** Initial **Control Center** (`ControlCenterOverlay`) with vertical touch Brightness & Volume sliders, Flashlight, Rotation Lock, Media player controls, and 2x2 Connectivity pod.
- **EN:** Minimalist **Customization Studio** supporting up to **`7x7`** Home Screen & Folder grids (`2x2`/`3x3` folder preview) and **Resizable Dock** (`75%–125%` scale, `3–7` capacity, show/hide toggle, glass opacity).
- **TR:** One UI 9 görmeden çalışan tam ekran **HNM Ayarlar**, **Denetim Merkezi** ve **`7x7`** Ana Ekran / Klasör ızgarası ile **boyutlandırılabilir Dock** stüdyosu eklendi.

---

## [0.1.2-alpha] — 2026-11-12

### ✨ Added & Fixed / Eklenenler ve Düzeltilenler
- **EN:** Added native **120 FPS (`120Hz`)** display mode locking (`HighRefreshRateHelper`), `RoleManager.ROLE_HOME` default Home Launcher flow, and removed hardware shadow inner-rectangle artifacts on translucent frosted-glass cards.
- **TR:** **120 FPS** yüksek yenileme hızı kilidi, varsayılan Ana Ekran (`ROLE_HOME`) akışı ve Today View cam kartlarındaki iç dikdörtgen gölge izi düzeltmesi yapıldı.

---

## [0.1.1-alpha] — 2026-11-12

### 🛡️ Security / Güvenlik
- **EN:** Introduced `sideload` vs `playstore` product flavors and custom `CN=HNMUI` release signing so sideloaded APKs install cleanly with zero **Google Play Protect** warnings.
- **TR:** Google Play Protect uyarısını tamamen ortadan kaldıran `sideload` / `playstore` mimarisi ve özel sürüm imzalama yapılandırması eklendi.

---

## [0.1.0-alpha] — 2026-11-12

### 🎉 Initial MVP / İlk Sürüm
- **EN:** Initial Jetpack Compose launcher with Today View, Squircle Home Grid, Frosted Glass Dock, Spotlight Search, and automatic Turkish App Library categorization.
- **TR:** Today View, Squircle Ana Ekran, Buzlu Cam Dock, Spotlight Arama ve otomatik Türkçe Uygulama Arşivi içeren ilk MVP sürümü.
