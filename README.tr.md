<div align="center">

# HNMUI (Türkçe Dokümantasyon)

**iOS Sadeliği, Android Özgürlüğüyle Buluştu — Buzlu Cam Dokulu 120 FPS Ana Ekran**

[![Son Sürüm](https://img.shields.io/badge/s%C3%BCr%C3%BCm-v0.3.4.2--alpha-0A84FF?style=for-the-badge)](https://github.com/HNMLabs/HNMUI/releases/tag/v0.3.4.2-alpha)
[![CI & Güvenlik](https://img.shields.io/github/actions/workflow/status/HNMLabs/HNMUI/android-ci.yml?branch=main&style=for-the-badge&label=CI%20%26%20G%C3%BCvenlik)](https://github.com/HNMLabs/HNMUI/actions/workflows/android-ci.yml)
[![Gizlilik](https://img.shields.io/badge/Gizlilik-S%C4%B1f%C4%B1r%20%C4%B0nternet-10B981?style=for-the-badge)](SECURITY.md#türkçe)
[![Lisans: MIT](https://img.shields.io/badge/Lisans-MIT-F59E0B?style=for-the-badge)](LICENSE)

[**🇬🇧 English (`README.md`)**](README.md) · [**📚 Wiki**](docs/wiki/Home.md) · [**🔒 Güvenlik (`SECURITY.md`)**](SECURITY.md#türkçe) · [**📋 Sürüm Geçmişi**](CHANGELOG.md) · [**🤝 Katkı Rehberi**](CONTRIBUTING.md#türkçe) · [**⬇️ APK İndir (`v0.3.4.2-alpha`)**](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.4.2-alpha.apk)

</div>

---

## Genel Bakış

**HNMUI**, **HNMLabs** tarafından geliştirilen; iOS işletim sisteminin dinginliğini, sistem uyumlu **Açık ve Koyu Buzlu Cam (*Adaptive Frosted Glass*)** dokusunu ve yay fiziğine (*spring physics*) dayalı 120 FPS animasyonlarını Android'in açık sistem gücüyle birleştiren saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

## ✨ Öne Çıkan Özellikler

- **Sistem Uyumlu Açık & Koyu Cam Motoru (`Otomatik` / `Açık` / `Koyu`):** Android sisteminin Koyu/Açık modunu yeniden başlatma gerektirmeden canlı takip eder; **Sedefli Açık Cam** ile **Gece Koyu Cam** arasında `380 ms` profesyonel geçiş animasyonu sunar.
- **Spotlight 2.0 (Anlık Matematik & KDV Hesaplayıcı + Canlı Sistem Komutları):** Çevrimdışı dört işlem, üs alma (`^`), parantez ve Türkçe/uluslararası yüzde hesaplayıcı (`1250 + %20`, `800 - 15%`), tek dokunuşla kopyalama, canlı Fener/Tema/Ayarlar derin bağlantıları ve kullanım sıklığı ağırlıklı `"En İyi ↵"` sıralaması sunar.
- **Uygulama Arşivi 2.0 (`Kategoriler` | `A–Z Liste` & Dokunsal Harf Rayı):** Okunmamış rozet toplamlı 2×2 akıllı kategori kutuları ile sağ kenarda yüzen harf balonlu **iOS A–Z Hızlı Kaydırma Rayı** arasında anında geçiş sağlar.
- **Denetim Merkezi Pro & 120 FPS Esnek Kapatma Fiziği:** Denetim Merkezi ve Bildirim Merkezi yukarı kaydırılırken GPU `graphicsLayer` esneme fiziğiyle parmağı takip eder; canlı `%` sürgü göstergesi, Ses Modu (`Sesli`/`Titreşim`/`Sessiz`) hapı, Fener, Yön Kilidi ve Medya kontrolü sunar.
- **Ana Ekran Ergonomisi & 120 FPS Sürükle-Bırak:** Boş alana çift dokunuşla açılan hızlı eylem, dokunmatik/kaydırmalı sayfa kapsülü, `4×4`–`7×7` ızgara/klasör ve Dock (`%75–%125`) özelleştirmeleri sunar.

## 📦 İndirme ve Kurulum

- **[HNMUI v0.3.4.2-alpha APK İndir](https://github.com/HNMLabs/HNMUI/raw/main/releases/HNMUI-v0.3.4.2-alpha.apk)**
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
