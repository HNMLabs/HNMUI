<div align="center">

# HNMUI

**iOS Simplicity Meets Android Freedom — Frosted-Glass 120 FPS Home Launcher**  
**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Latest Release](https://img.shields.io/badge/release-v0.2.0--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.2.0-alpha)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-120%20FPS-38BDF8?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

[**🇬🇧 English**](#english) · [**🇹🇷 Türkçe**](#türkçe) · [**📄 Türkçe Dosya (`README.tr.md`)**](README.tr.md) · [**⬇️ Download APK (`v0.2.0-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.2.0-alpha.apk)

</div>

---

<a id="english"></a>

## 🇬🇧 English

**HNMUI** is a lightweight, battery-friendly, native **Android Home Launcher** (`CATEGORY_HOME`) crafted by **HNMLabs**. Inspired by the calm spatial ergonomics of iOS and powered by Android's open ecosystem, HNMUI unifies your apps under continuous-curvature **Squircle** icons, translucent **Frosted Glass** surfaces, tactile spring physics, built-in **HNM Settings**, **Control Center**, and locked **120 FPS** fluidity.

### ✨ Key Features

- **120 FPS High Refresh Rate Engine:** Automatically locks onto the highest native display refresh rate (`90Hz` / `120Hz` / `144Hz`) via `HighRefreshRateHelper`, disables Android 15 (`API 35`) 60Hz adaptive power throttling, and pre-uploads app icon textures to the GPU (`Bitmap.prepareToDraw`) on background threads.
- **Built-in iOS-Style HNM Settings (Zero OEM/One UI Clutter):** Includes a complete frosted-glass **HNM Settings** suite (*Battery & Health*, *Display & Brightness*, *Sounds & Haptics*, *Storage*, *Wi-Fi & Connectivity*, *Apps*, *General & RAM*) rendered natively inside HNMUI — even redirecting the stock Settings icon seamlessly.
- **Pull-Down Frosted-Glass Control Center:** Swipe down from the top-right corner for vertical Brightness & Volume bars, Hardware Flashlight, Rotation Lock, Media Playback controls, and Connectivity pods.
- **Minimalist Customization Studio (`4×4` up to `7×7`):** Customize Home Screen grids up to **`7×7`**, expanded Folder grids from `3×3` up to **`7×7`**, folder cover mini-previews (`2×2` / `3×3`), icon size, label visibility, glass opacity, and **Dock size (`75%–125%`) & icon capacity (`3–7` icons)**.
- **Zero-Copy Live System Wallpaper:** Uses `windowShowWallpaper="true"` so static and live wallpapers shine directly behind translucent glass surfaces with zero extra RAM overhead.
- **3-Plane Spatial Navigation (`HorizontalPager`):**
  1. **Leftmost — Today View (`Page 0`):** Live greeting & date, battery + active Hz indicator, smart app suggestions, persistent **Quick Notes**, and native **Android Widgets (`AppWidgetHost`)**.
  2. **Center — Paged Home Screen (`4×4`–`7×7`):** Resizable Frosted Glass **Dock**, iOS-style `"Search"` pill, inline-renamable glass folders, and long-press **Jiggle / Edit Mode**.
  3. **Rightmost — App Library (`Page N+1`):** Automatically categorizes all installed apps into *Suggested*, *Recently Added*, *Social*, *Productivity & Finance*, *Entertainment & Media*, *Utilities & System*, and *Games* with 3 direct-launch icons + `2×2` expandable cluster and instant A–Z search.
- **Pull-Down Spotlight Search & Haptic Touch Quick Actions:** Swipe down from the center to search apps or the web; long-press any icon for dynamic shortcuts, Dock pinning, or folder creation.
- **Phone-First, Tablet-Reserved Architecture:** Optimized for portrait phone ergonomics out of the box, while keeping a `6×5` tablet layout profile ready in reserve behind `LauncherFeatureFlags(enableTabletLayout = false)`.

### 📦 Download & Installation

Grab the Play Protect-compliant, release-signed APK directly:
- **[Download HNMUI v0.2.0-alpha APK](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.2.0-alpha.apk)**
- After installing, tap **"Set as Default" / "Ayarla"** at the top of the Home Screen and select **HNMUI** as your Default Home App.

### 🏗️ Architecture & Build Variants

```text
app/src/main/java/com/hnmlabs/hnmui/
├── core/
│   ├── formfactor/     # Responsive 4x4..7x7 Phone grid, Dock scaling + Tablet 6x5 reserve
│   └── system/         # HighRefreshRateHelper (120Hz), DeviceSystemController & DefaultLauncherHelper
├── data/               # InstalledAppsRepository (LRU + GPU icon cache), HomeLayoutRepository (DataStore),
│                       # AppCategoryClassifier
├── model/              # @Immutable Launcher domain, customization & UI state models
├── ui/
│   ├── components/     # GlassSurface (dynamic opacity), SquircleShape, AppIconTile, FolderIconTile
│   ├── surfaces/       # TodayViewPage, HomeGridPage + Resizable Dock, AppLibraryPage
│   ├── overlays/       # HnmSettingsOverlay, ControlCenterOverlay, SpotlightOverlay,
│   │                   # FolderModalOverlay (3x3..7x7), QuickActionMenuOverlay
│   └── theme/          # HnmPalette, HnmTypography, Spring Physics (HnmMotion)
└── widget/             # LauncherWidgetHostManager (AppWidgetHost)
```

| Product Flavor | Purpose | Permissions |
| :--- | :--- | :--- |
| **`sideload`** *(default)* | Direct APK distribution compatible with Google Play Protect Enhanced Fraud Protection | `VIBRATE`, `EXPAND_STATUS_BAR`, `ACCESS_NETWORK_STATE`, `WRITE_SETTINGS` + `<queries>` |
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
- **One UI'dan Bağımsız Dahili "HNM Ayarlar" Uygulaması:** Pil sağlığı ve sıcaklığı (`°C`), ekran parlaklığı ve kilit süresi, ses ve Haptic profilleri, renkli depolama çubuğu, canlı RAM kullanımı ve uygulama yöneticisi tamamen kendi buzlu cam tasarımımızla çalışır; telefonun *Ayarlar* ikonuna basıldığında bile One UI yerine doğrudan **HNM Ayarlar** açılır.
- **Sağ Üstten İnen Buzlu Cam Denetim Merkezi (`Control Center`):** Dikey iOS Parlaklık ve Ses barları, El Feneri, Yön Kilidi, Medya Oynatıcı kontrolleri ve hızlı ağ kartı Play Protect engeline takılmadan çalışır.
- **Minimalist Özelleştirme Stüdyosu (`7×7` Izgara, Klasör & Dock):** Ana ekran uygulama ızgarasını `4×4`'ten **`7×7`**'ye, klasör içi dizilimi `3×3`'ten **`7×7`**'ye kadar ayarlayabilir; alt **Dock barını büyütüp küçültebilir (`%75–%125`)** ve Dock ikon kapasitesini (`3–7` ikon) dilediğiniz gibi değiştirebilirsiniz.
- **3 Düzlemli Uzamsal Yatay Akış (`HorizontalPager`):** Today View (`Sayfa 0`), Dinamik Sayfalı Ana Ekran + Buzlu Cam Dock ve Otomatik Kategorili Uygulama Arşivi (`App Library`).
- **Telefon Odaklı, Tablet Rezerve Mimari:** Telefon dikey ergonomisi aktiftir; `FormFactorPolicy` içinde `6×5` tablet ızgara düzeni rezerve olarak hazır tutulur.

### 📦 İndirme ve Kurulum

Play Protect uyumlu, özel sürüm anahtarıyla imzalanmış APK dosyasını doğrudan indirebilirsiniz:
- **[HNMUI v0.2.0-alpha APK İndir](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.2.0-alpha.apk)**
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
