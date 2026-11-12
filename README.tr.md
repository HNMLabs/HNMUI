<div align="center">

# HNMUI (Türkçe Dokümantasyon)

**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Son Sürüm](https://img.shields.io/badge/s%C3%BCr%C3%BCm-v0.2.0--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.2.0-alpha)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-120%20FPS-38BDF8?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

[**🇬🇧 English (`README.md`)**](README.md) · [**⬇️ APK İndir (`v0.2.0-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.2.0-alpha.apk)

</div>

---

## Genel Bakış

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, buzlu cam (*frosted glass*) dokusunu ve yay fiziğine (*spring physics*) dayalı akıcılığını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

## ✨ Öne Çıkan Özellikler

- **Gerçek 120 FPS / Yüksek Yenileme Hızı Motoru:** `HighRefreshRateHelper` ile ekranın doğal çözünürlüğünü koruyarak desteklenen en yüksek yenileme hızını (`90Hz` / `120Hz` / `144Hz`) kilitler, Android 15 (`API 35`) 60Hz güç kısıtlamasını kapatır ve ikon dokularını arka planda GPU belleğine hazırlar (`Bitmap.prepareToDraw`).
- **One UI'dan Bağımsız Dahili "HNM Ayarlar" Uygulaması:** Pil sağlığı ve sıcaklığı (`°C`), ekran parlaklığı ve kilit süresi, ses ve Haptic profilleri, renkli depolama çubuğu, canlı RAM kullanımı ve uygulama yöneticisi tamamen kendi buzlu cam tasarımımızla çalışır; *Ayarlar* ikonuna basıldığında One UI yerine doğrudan **HNM Ayarlar** açılır.
- **Sağ Üstten İnen Buzlu Cam Denetim Merkezi (`Control Center`):** Dikey iOS Parlaklık ve Ses barları, El Feneri, Yön Kilidi, Medya Oynatıcı kontrolleri ve hızlı bağlantı kartları Play Protect engeline takılmadan çalışır.
- **Minimalist Özelleştirme Stüdyosu (`7×7` Izgara, Klasör & Dock):** Ana ekran ızgarasını `4×4`'ten **`7×7`**'ye, klasör içi dizilimi `3×3`'ten **`7×7`**'ye kadar ayarlayabilir; alt **Dock barını büyütüp küçültebilir (`%75–%125`)** ve Dock ikon sayısını (`3–7` ikon) özelleştirebilirsiniz.
- **3 Düzlemli Uzamsal Yatay Akış (`HorizontalPager`):** Today View (`Sayfa 0`), Dinamik Sayfalı Ana Ekran + Buzlu Cam Dock ve Otomatik Kategorili Uygulama Arşivi (`App Library`).
- **Telefon Odaklı, Tablet Rezerve Mimari:** İlk aşamada telefon dikey ergonomisi aktiftir; `FormFactorPolicy` içinde `6×5` tablet ızgara düzeni rezerve olarak hazır tutulur.

## 📦 İndirme ve Kurulum

- **[HNMUI v0.2.0-alpha APK İndir](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.2.0-alpha.apk)**
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
