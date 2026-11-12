<div align="center">

# HNMUI (Türkçe Dokümantasyon)

**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Son Sürüm](https://img.shields.io/badge/s%C3%BCr%C3%BCm-v0.3.1--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.3.1-alpha)
[![CI & Güvenlik](https://img.shields.io/github/actions/workflow/status/HNMLabs/HNMUI/android-ci.yml?branch=main&style=for-the-badge&label=CI%20%26%20G%C3%BCvenlik)](https://github.com/HNMLabs/HNMUI/actions/workflows/android-ci.yml)
[![Gizlilik](https://img.shields.io/badge/Gizlilik-S%C4%B1f%C4%B1r%20%C4%B0nternet-10B981?style=for-the-badge)](SECURITY.md#türkçe)
[![Lisans: MIT](https://img.shields.io/badge/Lisans-MIT-F59E0B?style=for-the-badge)](LICENSE)

[**🇬🇧 English (`README.md`)**](README.md) · [**📚 Wiki**](docs/wiki/Home.md) · [**🔒 Güvenlik (`SECURITY.md`)**](SECURITY.md#türkçe) · [**📋 Sürüm Geçmişi**](CHANGELOG.md) · [**🤝 Katkı Rehberi**](CONTRIBUTING.md#türkçe) · [**⬇️ APK İndir (`v0.3.1-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.1-alpha.apk)

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
