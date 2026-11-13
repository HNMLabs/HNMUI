# Changelog / Sürüm Geçmişi

All notable changes to **HNMUI** are documented in this file.  
**HNMUI** projesindeki tüm önemli değişiklikler bu dosyada kayıt altına alınır.

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
