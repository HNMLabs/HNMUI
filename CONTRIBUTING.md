# Contributing to HNMUI / HNMUI Katkı Rehberi

[**🇬🇧 English**](#english) · [**🇹🇷 Türkçe**](#türkçe)

---

<a id="english"></a>

## 🇬🇧 Contributing Guide

Thank you for helping make **HNMUI** faster, calmer, and cleaner!

### Core Engineering Principles
1. **120 FPS Frame Budget (`< 8.3ms`):**
   - Never run synchronous Binder IPC (`PackageManager`, `Settings`, `BatteryManager`, `AudioManager`, icon decoding) on the Main UI Thread during animations or composition. Use `Dispatchers.IO` / `Dispatchers.Default` and in-memory caches.
   - Drive continuous animations (`scale`, `alpha`, `translation`, `rotationZ`) inside `Modifier.graphicsLayer { ... }` to avoid layout thrashing.
   - Inside `awaitEachGesture`, **always** invoke `awaitFirstDown(...)` as the unconditional first statement so pointer loops always suspend cleanly.
2. **Play Protect & Privacy First:**
   - Do **not** add `android.permission.INTERNET`, analytics SDKs, or high-risk permissions (`SMS`, `Accessibility`, `QUERY_ALL_PACKAGES`) to `app/src/main/AndroidManifest.xml`.
3. **Zero Secrets in Git:**
   - Never commit `.jks`, `.keystore`, `.p12`, `.pem`, `keystore.properties`, `local.properties`, or tokens. CI automatically scans every PR for secrets.

### Development Workflow
```bash
# 1. Run unit tests
./gradlew testSideloadDebugUnitTest

# 2. Run release lint checks
./gradlew lintVitalSideloadRelease

# 3. Assemble debug APK
./gradlew assembleSideloadDebug
```

1. Fork the repository and create a feature branch (`git checkout -b feat/my-improvement`).
2. Add or update unit tests in `app/src/test/java/com/hnmlabs/hnmui/LauncherCoreTest.kt`.
3. Ensure `./gradlew testSideloadDebugUnitTest lintVitalSideloadRelease` passes cleanly.
4. Open a Pull Request describing the UX/performance impact.

---

<a id="türkçe"></a>

## 🇹🇷 Katkıda Bulunma Rehberi

**HNMUI** projesini daha akıcı, sade ve bağımsız hale getirmek için yapacağınız katkılara teşekkür ederiz!

### Temel Mühendislik İlkeleri
1. **120 FPS Kare Bütçesi (`< 8.3ms`):**
   - Animasyon veya ilk çizim (composition) sırasında Main Thread üzerinde senkron sistem çağrısı (`PackageManager`, `Settings`, `AudioManager`, ikon çözme) yapmayın; `Dispatchers.IO` ve bellek önbelleği kullanın.
   - Sürekli animasyonları yeniden yerleşim (re-layout) tetiklememek için `Modifier.graphicsLayer { ... }` katmanında çalıştırın.
   - `awaitEachGesture` bloklarında her zaman ilk satırda koşulsuz `awaitFirstDown(...)` çağırarak döngünün güvenli biçimde askıda (`suspend`) kalmasını sağlayın.
2. **Play Protect ve Gizlilik Önceliği:**
   - Ana `AndroidManifest.xml` dosyasına `INTERNET`, takip/analitik kütüphaneleri veya yüksek riskli izinler (`SMS`, `Accessibility`, `QUERY_ALL_PACKAGES`) eklemeyin.
3. **Sıfır Gizli Anahtar Kuralı:**
   - `.jks`, `.keystore`, `keystore.properties`, `local.properties` veya herhangi bir anahtar/parola dosyasını asla git'e eklemeyin.

### Geliştirme ve Test Komutları
```bash
# 1. Birim testlerini çalıştırın
./gradlew testSideloadDebugUnitTest

# 2. Sürüm Lint denetimini çalıştırın
./gradlew lintVitalSideloadRelease

# 3. Debug APK derleyin
./gradlew assembleSideloadDebug
```
