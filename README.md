<div align="center">

# HNMUI

**iOS Simplicity Meets Android Freedom — Frosted-Glass 120 FPS Home Launcher**  
**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Latest Release](https://img.shields.io/badge/release-v0.3.3--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.3.3-alpha)
[![CI & Secret Audit](https://img.shields.io/github/actions/workflow/status/HNMLabs/HNMUI/android-ci.yml?branch=main&style=for-the-badge&label=CI%20%26%20Security)](https://github.com/HNMLabs/HNMUI/actions/workflows/android-ci.yml)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Offline First](https://img.shields.io/badge/Privacy-Zero%20Internet-10B981?style=for-the-badge)](SECURITY.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B?style=for-the-badge)](LICENSE)

[**🇬🇧 English**](#english) · [**🇹🇷 Türkçe**](#türkçe) · [**📄 `README.tr.md`**](README.tr.md) · [**📚 Wiki**](docs/wiki/Home.md) · [**🔒 Security**](SECURITY.md) · [**📋 Changelog**](CHANGELOG.md) · [**🤝 Contributing**](CONTRIBUTING.md) · [**⬇️ APK (`v0.3.3-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.3-alpha.apk)

</div>

---

<a id="english"></a>

## 🇬🇧 English

**HNMUI** is a lightweight, battery-friendly, native **Android Home Launcher** (`CATEGORY_HOME`) crafted by **HNMLabs**. Inspired by the calm spatial ergonomics of iOS and powered by Android's open ecosystem, HNMUI unifies your apps under continuous-curvature **Squircle** icons, translucent **Frosted Glass** surfaces, minimalist depth-recession spring physics, **HNM Settings**, **Control Center**, **Notification Cover Sheet**, and **120 FPS Drag & Drop**.

### ✨ Key Features

- **120 FPS High Refresh Rate & Calm Minimalist Motion:** Locks onto the highest native display refresh rate (`90Hz` / `120Hz` / `144Hz`) via `HighRefreshRateHelper`, applies critically-damped iOS spring physics, subtle page depth transitions, organic staggered Edit Mode jiggle, and GPU background depth recession (`94.5%`) when overlays open.
- **iOS Status Bar + Dual Top Pull-Down Shades (Zero One UI Interruption):** Hides OEM status bars on the Home Screen in favor of `HnmIosStatusBar`:
  - **Top-Right Pull-Down → Control Center:** Vertical Brightness & Volume bars, Flashlight, Rotation Lock, Media controls, and Connectivity pods.
  - **Top-Left Pull-Down → HNM Notification & Cover Sheet:** Large iOS typography clock & date, live battery/ringer pills, notification cards, instant reminders, and Flashlight/Camera quick buttons.
- **120 FPS SpringBoard Drag & Drop (Home Grid + Folders + Dock In/Out):** Long-press and move any icon to seamlessly transition into live 120 FPS dragging — reorder across pages (with edge auto-pagination), drop onto folders, **drag apps into the Dock** (with smart full-Dock swap), and **drag apps out of the Dock** back onto the Home Screen.
- **Built-in iOS-Style HNM Settings:** Complete frosted-glass **HNM Settings** suite (*Battery & Health*, *Display & Brightness*, *Sounds & Haptics*, *Storage*, *Wi-Fi & Connectivity*, *Apps*, *General & RAM*), automatically intercepting the stock Settings icon.
- **Minimalist Customization Studio (`4×4` up to `7×7`):** Customize Home Screen grids up to **`7×7`**, expanded Folder grids from `3×3` up to **`7×7`**, folder cover mini-previews (`2×2` / `3×3`), icon size, label visibility, glass opacity, and **Dock size (`75%–125%`) & icon capacity (`3–7` icons)**.
- **Zero-Copy Live System Wallpaper & 3-Plane Navigation:** Today View (`Page 0` with Quick Notes & Android `AppWidgetHost`), Paged Home Screen (`4×4`–`7×7`), and automatic **App Library (`Page N+1`)**.
- **Phone-First, Tablet-Reserved Architecture:** Optimized for portrait phone ergonomics out of the box, while keeping a `6×5` tablet layout profile ready in reserve behind `LauncherFeatureFlags(enableTabletLayout = false)`.

### 📦 Download & Installation

Grab the Play Protect-compliant, release-signed APK directly:
- **[Download HNMUI v0.3.3-alpha APK](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.3-alpha.apk)**
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
│   ├── components/     # HnmIosStatusBar, GlassSurface, SquircleShape, AppIconTile, FolderIconTile
│   ├── drag/           # LauncherDragDropController (120 FPS Home, Folder & Dock drag-in/drag-out)
│   ├── surfaces/       # TodayViewPage, HomeGridPage + Resizable Dock, AppLibraryPage
│   ├── overlays/       # HnmSettingsOverlay, ControlCenterOverlay, NotificationCenterOverlay,
│   │                   # SpotlightOverlay, FolderModalOverlay (3x3..7x7), QuickActionMenuOverlay
│   └── theme/          # HnmPalette, HnmTypography, Minimalist Spring Physics (HnmMotion)
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

# Run unit tests (FormFactorPolicy, AppCategoryClassifier, Drag & Drop, etc.)
./gradlew testSideloadDebugUnitTest
```

---

<a id="türkçe"></a>

## 🇹🇷 Türkçe

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, buzlu cam (*frosted glass*) estetiğini ve yay fiziğine (*spring physics*) dayalı sade animasyonlarını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

### ✨ Öne Çıkan Özellikler

- **Gerçek 120 FPS & Minimalist iOS Animasyon Fiziği:** `HighRefreshRateHelper` ile en yüksek ekran yenileme hızını (`90Hz` / `120Hz` / `144Hz`) kilitler; paneller açıldığında arka plan derinlik çekilmesi (`%94.5`), yumuşak sayfa geçişleri ve doğal faz farkıyla çalışan ikon düzenleme (jiggle) animasyonu sunar.
- **iOS Durum Çubuğu + Sağ Üst Denetim Merkezi & Sol Üst HNM Bildirim Merkezi:** Ana ekranda One UI üst çubuğunu gizleyerek kendi sade iOS durum çubuğunu sunar:
  - **Sağ Üstten Aşağı Çekme → Denetim Merkezi:** Dikey Parlaklık ve Ses çubukları, El Feneri, Yön Kilidi, Medya kontrolleri ve Bağlantı kartı.
  - **Sol Üstten Aşağı Çekme → HNM Bildirim & Özet Merkezi:** One UI bildirim paneli yerine büyük iOS saat/tarih başlığı, canlı pil/ses modu kapsülleri, anlık hatırlatıcılar ve Fener/Kamera kısayolları açılır.
- **120 FPS Uygulama Sürükle-Bırak (Izgara + Klasör + Dock'a Ekle / Dock'tan Çıkar):** Herhangi bir ikona basılı tutup parmağınızı kaydırarak uygulamaları sayfalar arasında taşıyabilir, klasöre bırakabilir, **alt Dock'a sürükleyip sabitleyebilir** (doluysa otomatik takas eder) veya **Dock'tan sürükleyip ana ekrana çıkarabilirsiniz**.
- **One UI'dan Bağımsız Dahili "HNM Ayarlar" Uygulaması:** Pil sağlığı ve sıcaklığı (`°C`), ekran parlaklığı, ses profilleri, depolama, RAM ve `7×7`'ye kadar ızgara/klasör ile Dock boyutu (`%75–%125`) özelleştirmelerini tek çatı altında sunar.

### 📦 İndirme ve Kurulum

Play Protect uyumlu, özel sürüm anahtarıyla imzalanmış APK dosyasını doğrudan indirebilirsiniz:
- **[HNMUI v0.3.3-alpha APK İndir](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.3-alpha.apk)**
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
