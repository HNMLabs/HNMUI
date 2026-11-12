# HNMUI — iOS Sadeliğinde Modern Android Home Launcher

**HNMUI**, Android'in açık sistem gücünü iOS'un dinginliği, buzlu cam (*frosted glass*) dokusu ve yay fiziğine (*spring physics*) dayalı akıcı etkileşimleriyle buluşturan saf, hızlı ve pil dostu bir **Android Ana Ekran (Home Launcher)** uygulamasıdır.

## Öne Çıkan MVP Özellikleri

- **Buzlu Cam (Frosted Glass) & Squircle Tasarım Sistemi:** Tüm uygulama ikonları matematiksel süper-elips (`SquircleShape`) ile homojenleştirilir; Dock, klasörler ve kartlar optik kenar ışıltılı yarı saydam cam katmanları üzerinde yüzer.
- **Gerçek Sistem Duvar Kağıdı Entegrasyonu:** `windowShowWallpaper="true"` sayesinde cihazın canlı veya statik duvar kağıdı ekstra bellek kopyası olmadan doğrudan arayüzün arkasında çalışır.
- **Uzamsal 3 Düzlemli Yatay Akış (`HorizontalPager`):**
  1. **En Sol — Today View (Bugün & Özet):** Pil durumu, bildirim özeti, HNM uygulama önerileri, kalıcı *Hızlı Notlar* ve yerel **Android Widget (`AppWidgetHost`)** alanı.
  2. **Orta — Sayfalı Ana Ekran (`4x6`):** Sabit 4'lü buzlu cam Dock, "Ara" hapı, satır içi yeniden adlandırılabilir `3x3` buzlu cam klasörler ve *Düzenleme (Jiggle) Modu*.
  3. **En Sağ — Uygulama Arşivi (`App Library`):** Yüklü uygulamaları *Önerilenler*, *Son Eklenenler*, *Sosyal*, *Üretkenlik ve Finans*, *Eğlence ve Medya*, *Araçlar ve Sistem* ve *Oyunlar* başlıkları altında otomatik gruplar; 3 doğrudan ikon + `2x2` genişleyen küme ergonomisi ve A–Z filtreleme sunar.
- **Aşağı Kaydırınca Açılan Spotlight Arama:** Ana ekranda parmağınızı aşağı kaydırdığınızda veya *Ara* hapına dokunduğunuzda uygulama, öneri, sistem ayarları ve web araması anında açılır.
- **Canlı Bildirim Rozetleri & Haptic Touch Menüsü:** `NotificationListenerService` ile sıfır-polling bildirim sayaçları ve ikona uzun basıldığında Android `LauncherApps` dinamik uygulama kısayolları + klasörleme eylemleri.
- **Telefon Odaklı, Tablet Rezerve Mimari:** İlk aşamada telefon dikey ergonomisi aktiftir; `FormFactorPolicy` içinde `6x5` tablet ızgara profili rezerve olarak hazır tutulur.

## Teknoloji Yığını

- **Dil & UI:** Kotlin 2.0, Jetpack Compose (Material 3 + özel `GlassSurface` / `SquircleShape` tasarım sistemi)
- **Durum Yönetimi & Kalıcılık:** Coroutines `StateFlow` (UDF), Jetpack DataStore Preferences + `kotlinx.serialization`
- **Android Sistem Köprüleri:** `LauncherApps`, `NotificationListenerService`, `AppWidgetHost`, `RoleManager` (`ROLE_HOME`)
- **Hedef Sürümler:** `minSdk = 28` (Android 9+), `targetSdk = 35` (Android 15)

## Derleme ve Çalıştırma

Projeyi Android Studio (Ladybug veya üzeri) ile doğrudan açabilir ya da komut satırından derleyebilirsiniz:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```
