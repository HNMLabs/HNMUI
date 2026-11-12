<div align="center">

# HNMUI (Türkçe Dokümantasyon)

**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Son Sürüm](https://img.shields.io/badge/s%C3%BCr%C3%BCm-v0.3.1--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.3.1-alpha)
[![Android](https://img.shields.io/badge/Android-9.0%2B%20(API%2028--35)-30D158?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-120%20FPS-38BDF8?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)

[**🇬🇧 English (`README.md`)**](README.md) · [**⬇️ APK İndir (`v0.3.1-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.1-alpha.apk)

</div>

---

## Genel Bakış

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, buzlu cam (*frosted glass*) dokusunu ve yay fiziğine (*spring physics*) dayalı sade animasyonlarını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

## ✨ Öne Çıkan Özellikler

- **Gerçek 120 FPS & Minimalist Animasyon Fiziği:** `HighRefreshRateHelper` ile ekranın en yüksek yenileme hızını (`90Hz` / `120Hz` / `144Hz`) kilitler; üst katmanlar açıldığında arka plan derinlik çekilmesi (`%94.5`), akıcı sayfa geçişleri ve doğal faz farklı jiggle animasyonları sunar.
- **iOS Durum Çubuğu + Sağ Üst Denetim Merkezi & Sol Üst HNM Bildirim Merkezi:** Ana ekranda One UI üst çubuğunu gizleyerek kendi sade iOS durum çubuğunu sunar; sağ üstten çekince **Denetim Merkezi**, sol üstten çekince One UI yerine doğrudan **HNM Bildirim & Özet Merkezi** iner.
- **120 FPS Sürükle-Bırak (Izgara + Klasör + Dock'a Sürükle / Dock'tan Çıkar):** Uygulamaları sayfalar arasında sürükleyebilir, klasöre bırakabilir, **Dock'a sürükleyip ekleyebilir** ve **Dock'tan sürükleyip ana ekrana çıkarabilirsiniz**.
- **One UI'dan Bağımsız Dahili "HNM Ayarlar" & `7×7` Özelleştirme Stüdyosu:** Pil sağlığı/sıcaklığı, parlaklık, ses, depolama, RAM ve `4×4`–`7×7` ızgara/klasör ile Dock boyutu (`%75–%125`) ayarlarını sunar.

## 📦 İndirme ve Kurulum

- **[HNMUI v0.3.1-alpha APK İndir](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.1-alpha.apk)**
- Kurulumdan sonra ana ekranın üstündeki **"Ayarla"** çubuğuna dokunarak varsayılan ana ekran uygulaması olarak **HNMUI**'yi seçebilirsiniz.

## 🏗️ Proje Yapısı ve Dağıtım Profilleri

| Dağıtım Profili (`Flavor`) | Amaç | İzinler |
| :--- | :--- | :--- |
| **`sideload`** *(varsayılan)* | Google Play Protect gelişmiş koruma uyumlu doğrudan APK kurulumu | `VIBRATE`, `EXPAND_STATUS_BAR`, `ACCESS_NETWORK_STATE`, `WRITE_SETTINGS` + `<queries>` |
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
