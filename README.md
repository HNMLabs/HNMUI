<div align="center">

# HNMUI

**iOS Simplicity Meets Android Freedom — Frosted-Glass 120 FPS Home Launcher**  
**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Latest Release](https://img.shields.io/badge/release-v0.1.2--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.1.2-alpha)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-120%20FPS-38BDF8?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

[**🇬🇧 English**](#english) · [**🇹🇷 Türkçe**](#türkçe) · [**📄 Türkçe Dosya (`README.tr.md`)**](README.tr.md) · [**⬇️ Download APK (`v0.1.2-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.1.2-alpha.apk)

</div>

---

<a id="english"></a>

## 🇬🇧 English

**HNMUI** is a lightweight, battery-friendly, native **Android Home Launcher** (`CATEGORY_HOME`) crafted by **HNMLabs**. Inspired by the calm spatial ergonomics of iOS and powered by Android's open ecosystem, HNMUI unifies your apps under continuous-curvature **Squircle** icons, translucent **Frosted Glass** surfaces, tactile spring physics, and locked **120 FPS** fluidity.

### ✨ Key Features

- **120 FPS High Refresh Rate Engine:** Automatically locks onto the highest native display refresh rate (`90Hz` / `120Hz` / `144Hz`) via `HighRefreshRateHelper`, disables Android 15 (`API 35`) 60Hz adaptive power throttling, and pre-uploads app icon textures to the GPU (`Bitmap.prepareToDraw`) on background threads.
- **Frosted Glass & Mathematical Squircle System:** Every app icon is rendered cleanly into a continuous superellipse (`|x/a|^n + |y/b|^n = 1`) without artificial dark backing boxes or hardware inner-umbra shadow artifacts.
- **Zero-Copy Live System Wallpaper:** Uses `windowShowWallpaper="true"` so static and live wallpapers shine directly behind translucent glass surfaces with zero extra RAM overhead.
- **3-Plane Spatial Navigation (`HorizontalPager`):**
  1. **Leftmost — Today View (`Page 0`):** Live greeting & date, battery + active Hz indicator, smart app suggestions, persistent **Quick Notes**, and native **Android Widgets (`AppWidgetHost`)**.
  2. **Center — Paged Home Screen (`4x6`):** Pinned 4-icon Frosted Glass **Dock**, iOS-style `"Search"` pill, inline-renamable `3x3` glass folders, and long-press **Jiggle / Edit Mode**.
  3. **Rightmost — App Library (`Page N+1`):** Automatically categorizes all installed apps into *Suggested*, *Recently Added*, *Social*, *Productivity & Finance*, *Entertainment & Media*, *Utilities & System*, and *Games* with 3 direct-launch icons + `2x2` expandable cluster and instant A–Z search.
- **Pull-Down Spotlight Search:** Swipe down anywhere on the Home Screen to launch apps, inspect suggestions, jump to Android System Settings, or run a web search.
- **Haptic Touch Quick Actions:** Long-press any icon for tactile feedback, native Android dynamic/static `LauncherApps` shortcuts, folder organization, App Info, or uninstallation.
- **Seamless Default Home Role (`ROLE_HOME`):** Prompts for `RoleManager.ROLE_HOME` and falls back to `Settings.ACTION_HOME_SETTINGS` (including Samsung One UI compatibility) so pressing the system Home button always stays inside HNMUI.
- **Phone-First, Tablet-Reserved Architecture:** Optimized for portrait phone ergonomics (`4x6` grid) out of the box, while keeping a `6x5` tablet layout profile ready in reserve behind `LauncherFeatureFlags(enableTabletLayout = false)`.

### 📦 Download & Installation

Grab the Play Protect-compliant, release-signed APK directly:
- **[Download HNMUI v0.1.2-alpha APK (2.6 MB)](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.1.2-alpha.apk)**
- After installing, tap **"Set as Default" / "Ayarla"** at the top of the Home Screen and select **HNMUI** as your Default Home App.

### 🏗️ Architecture & Build Variants

```text
app/src/main/java/com/hnmlabs/hnmui/
├── core/
│   ├── formfactor/     # Phone 4x6 active profile + Tablet 6x5 reserved policy
│   └── system/         # HighRefreshRateHelper (120Hz) & DefaultLauncherHelper (ROLE_HOME)
├── data/               # InstalledAppsRepository (LRU + GPU icon cache), HomeLayoutRepository (DataStore),
│                       # AppCategoryClassifier, NotificationBadgeStore
├── model/              # @Immutable Launcher domain & UI state models
├── ui/
│   ├── components/     # GlassSurface, SquircleShape, AppIconTile, FolderIconTile
│   ├── surfaces/       # TodayViewPage, HomeGridPage + Dock, AppLibraryPage
│   ├── overlays/       # SpotlightOverlay, FolderModalOverlay, QuickActionMenuOverlay
│   └── theme/          # HnmPalette, HnmTypography, Spring Physics (HnmMotion)
└── widget/             # LauncherWidgetHostManager (AppWidgetHost)
```

| Product Flavor | Purpose | Permissions |
| :--- | :--- | :--- |
| **`sideload`** *(default)* | Direct APK distribution compatible with Google Play Protect Enhanced Fraud Protection | `VIBRATE` + `<queries>` package visibility |
| **`playstore`** | Google Play Store distribution with live unread notification count badges | Adds `HnmNotificationListenerService` |

### 🛠️ Building from Source

Requires **JDK 17+** and **Android SDK 35**.

```bash
# Build Play Protect-safe Sideload Release APK
./gradlew assembleSideloadRelease

# Build Play Store Debug APK (with live notification badges)
./gradlew assemblePlaystoreDebug

# Run unit tests (FormFactorPolicy, AppCategoryClassifier, etc.)
./gradlew testSideloadDebugUnitTest
```

---

<a id="türkçe"></a>

## 🇹🇷 Türkçe

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, buzlu cam (*frosted glass*) estetiğini ve yay fiziğine (*spring physics*) dayalı akıcılığını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

### ✨ Öne Çıkan Özellikler

- **Gerçek 120 FPS / Yüksek Yenileme Hızı Motoru:** `HighRefreshRateHelper` ile ekranın doğal çözünürlüğünü koruyarak desteklenen en yüksek yenileme hızını (`90Hz` / `120Hz` / `144Hz`) kilitler, Android 15 (`API 35`) 60Hz güç kısıtlamasını devre dışı bırakır ve uygulama ikonlarını arka planda GPU belleğine hazırlar (`Bitmap.prepareToDraw`).
- **Buzlu Cam & Matematiksel Squircle Tasarım Sistemi:** Tüm uygulama ikonları arkada yapay koyu kutu veya gölge izi olmadan pürüzsüz süper-elips (`SquircleShape`) formunda çizilir; Dock, klasörler ve kartlar optik kenar ışıltılı yarı saydam cam katmanları üzerinde yüzer.
- **Sıfır Kopya Sistem Duvar Kağıdı:** `windowShowWallpaper="true"` mimarisi sayesinde canlı veya statik duvar kağıdınız ekstra bellek tüketmeden doğrudan arayüzün arkasında çalışır.
- **3 Düzlemli Uzamsal Yatay Akış (`HorizontalPager`):**
  1. **En Sol — Today View (`Sayfa 0`):** Tarih ve karşılama başlığı, pil ve aktif Hz göstergesi, akıllı uygulama önerileri, kalıcı **Hızlı Notlar** ve yerel **Android Widget (`AppWidgetHost`)** alanı.
  2. **Orta — Sayfalı Ana Ekran (`4x6`):** Sabit 4 ikonlu Buzlu Cam **Dock**, iOS tarzı *"Ara"* hapı, satır içi yeniden adlandırılabilir `3x3` cam klasörler ve uzun basınca açılan **Düzenleme (Jiggle) Modu**.
  3. **En Sağ — Uygulama Arşivi (`App Library`):** Yüklü uygulamaları *Önerilenler*, *Son Eklenenler*, *Sosyal*, *Üretkenlik ve Finans*, *Eğlence ve Medya*, *Araçlar ve Sistem* ve *Oyunlar* kategorilerine otomatik ayırır; ilk 3 ikonu tek dokunuşla açar, sağ alt `2x2` kümesiyle tüm kategoriyi büyütür ve anlık A–Z arama sunar.
- **Aşağı Kaydırınca Açılan Spotlight Arama:** Ana ekranda herhangi bir yerden aşağı kaydırdığınızda uygulama, öneri, sistem ayarları ve web araması anında açılır.
- **Haptic Touch Kısayol Menüsü:** İkonlara uzun basıldığında titreşim geri bildirimiyle birlikte Android dinamik uygulama kısayolları (`LauncherApps`), klasöre taşıma, uygulama bilgisi ve kaldırma seçenekleri sunulur.
- **Tam Varsayılan Ana Ekran Kilidi (`ROLE_HOME`):** İlk açılışta ve ana ekran üst çubuğundan `RoleManager.ROLE_HOME` / Samsung One UI *Varsayılan Ana Ekran Ayarları* akışını çalıştırarak alt Ana Sayfa tuşunun her zaman HNMUI içinde kalmasını sağlar.
- **Telefon Odaklı, Tablet Rezerve Mimari:** İlk aşamada telefon dikey ergonomisi (`4x6` ızgara) aktiftir; `FormFactorPolicy` içinde `6x5` tablet ızgara düzeni `LauncherFeatureFlags(enableTabletLayout = false)` bayrağı arkasında hazır tutulur.

### 📦 İndirme ve Kurulum

Play Protect uyumlu, özel sürüm anahtarıyla imzalanmış APK dosyasını doğrudan indirebilirsiniz:
- **[HNMUI v0.1.2-alpha APK İndir (2.6 MB)](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.1.2-alpha.apk)**
- Kurulumdan sonra ana ekranın üstündeki **"Ayarla"** çubuğuna dokunup varsayılan ana ekran uygulaması olarak **HNMUI**'yi seçmeniz yeterlidir.

### 🛠️ Kaynak Koddan Derleme

Derleme için **JDK 17+** ve **Android SDK 35** gereklidir:

```bash
# Play Protect uyumlu Sideload Release APK üretimi
./gradlew assembleSideloadRelease

# Canlı bildirim rozeti destekli Play Store Debug APK üretimi
./gradlew assemblePlaystoreDebug

# Birim testlerini çalıştırma
./gradlew testSideloadDebugUnitTest
```

---

<div align="center">
  <sub>Built with ❤️ by <b>HNMLabs</b></sub>
</div>
