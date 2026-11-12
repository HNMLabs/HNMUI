<div align="center">

# HNMUI (Türkçe Dokümantasyon)

**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Son Sürüm](https://img.shields.io/badge/s%C3%BCr%C3%BCm-v0.1.2--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.1.2-alpha)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-120%20FPS-38BDF8?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

[**🇬🇧 English (`README.md`)**](README.md) · [**⬇️ APK İndir (`v0.1.2-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.1.2-alpha.apk)

</div>

---

## Genel Bakış

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, buzlu cam (*frosted glass*) dokusunu ve yay fiziğine (*spring physics*) dayalı akıcılığını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

## ✨ Öne Çıkan Özellikler

- **Gerçek 120 FPS / Yüksek Yenileme Hızı Motoru:** `HighRefreshRateHelper` ile ekranın doğal çözünürlüğünü koruyarak desteklenen en yüksek yenileme hızını (`90Hz` / `120Hz` / `144Hz`) kilitler, Android 15 (`API 35`) 60Hz güç kısıtlamasını kapatır ve ikon dokularını arka planda GPU belleğine hazırlar (`Bitmap.prepareToDraw`).
- **Buzlu Cam & Matematiksel Squircle Tasarım Sistemi:** Tüm uygulama ikonları arkada yapay koyu kutu veya gölge izi olmadan pürüzsüz süper-elips (`SquircleShape`) formunda çizilir; Dock, klasörler ve kartlar optik kenar ışıltılı yarı saydam cam katmanları üzerinde yüzer.
- **Sıfır Kopya Sistem Duvar Kağıdı:** `windowShowWallpaper="true"` sayesinde canlı veya statik sistem duvar kağıdınız ekstra bellek harcamadan doğrudan arayüzün arkasında görünür.
- **3 Düzlemli Uzamsal Yatay Akış (`HorizontalPager`):**
  1. **En Sol — Today View (`Sayfa 0`):** Tarih ve karşılama başlığı, pil ve aktif Hz göstergesi, akıllı uygulama önerileri, kalıcı **Hızlı Notlar** ve yerel **Android Widget (`AppWidgetHost`)** alanı.
  2. **Orta — Sayfalı Ana Ekran (`4x6`):** Sabit 4 ikonlu Buzlu Cam **Dock**, iOS tarzı *"Ara"* hapı, satır içi yeniden adlandırılabilir `3x3` cam klasörler ve uzun basınca açılan **Düzenleme (Jiggle) Modu**.
  3. **En Sağ — Uygulama Arşivi (`App Library`):** Yüklü uygulamaları *Önerilenler*, *Son Eklenenler*, *Sosyal*, *Üretkenlik ve Finans*, *Eğlence ve Medya*, *Araçlar ve Sistem* ve *Oyunlar* başlıkları altında otomatik gruplar; ilk 3 ikonu doğrudan başlatır, sağ alt `2x2` kümesiyle tüm kategoriyi açar ve A–Z filtreleme sunar.
- **Aşağı Kaydırınca Açılan Spotlight Arama:** Ana ekranda parmağınızı aşağı kaydırdığınızda uygulama, öneri, sistem ayarları ve web araması anında açılır.
- **Haptic Touch Kısayol Menüsü:** İkonlara uzun basıldığında titreşim geri bildirimiyle birlikte Android dinamik uygulama kısayolları (`LauncherApps`), klasörleme, uygulama bilgisi ve kaldırma eylemleri sunulur.
- **Tam Varsayılan Ana Ekran Kilidi (`ROLE_HOME`):** İlk açılışta ve ana ekran üst çubuğundan `RoleManager.ROLE_HOME` / Samsung One UI *Varsayılan Ana Ekran Ayarları* akışını çalıştırarak alt Ana Sayfa tuşunun her zaman HNMUI'ye dönmesini sağlar.
- **Telefon Odaklı, Tablet Rezerve Mimari:** İlk aşamada telefon dikey ergonomisi (`4x6` ızgara) aktiftir; `FormFactorPolicy` içinde `6x5` tablet ızgara düzeni rezerve olarak hazır tutulur.

## 📦 İndirme ve Kurulum

- **[HNMUI v0.1.2-alpha APK İndir (2.6 MB)](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.1.2-alpha.apk)**
- Kurulumdan sonra ana ekranın üstündeki **"Ayarla"** çubuğuna dokunarak varsayılan ana ekran uygulaması olarak **HNMUI**'yi seçebilirsiniz.

## 🏗️ Proje Yapısı ve Dağıtım Profilleri

| Dağıtım Profili (`Flavor`) | Amaç | İzinler |
| :--- | :--- | :--- |
| **`sideload`** *(varsayılan)* | Google Play Protect gelişmiş koruma uyumlu doğrudan APK kurulumu | `VIBRATE` + `<queries>` paket görünürlüğü |
| **`playstore`** | Canlı okunmamış bildirim sayacı destekli Google Play mağaza sürümü | Ek olarak `HnmNotificationListenerService` |

## 🛠️ Derleme ve Test

```bash
# Play Protect uyumlu Sideload Release APK derleme
./gradlew assembleSideloadRelease

# Play Store Debug APK derleme
./gradlew assemblePlaystoreDebug

# Birim testlerini çalıştırma
./gradlew testSideloadDebugUnitTest
```
