# Security & Privacy Policy / Güvenlik ve Gizlilik Politikası

[**🇬🇧 English**](#english) · [**🇹🇷 Türkçe**](#türkçe)

---

<a id="english"></a>

## 🇬🇧 Security & Privacy Policy

### 1. Supported Versions

Active security and stability patches are provided for the latest minor release line:

| Version | Supported | Notes |
| :--- | :---: | :--- |
| **`0.3.x`** (`v0.3.3-alpha`+) | ✅ | Current supported release branch |
| `0.2.x` | ❌ | Upgrade to `v0.3.3-alpha` or newer |
| `0.1.x` | ❌ | Early alpha — deprecated |

### 2. Zero-Network / Offline-by-Design Guarantee

**HNMUI does NOT request `android.permission.INTERNET`.**

- **Zero Telemetry & Zero Analytics:** No crash reporters, ad SDKs, tracking pixels, or external servers exist in the codebase.
- **100% On-Device Storage:** Home Screen layout, folders, Dock pins, quick reminders, and customization settings are stored strictly locally inside Android's sandboxed **Jetpack DataStore** (`files/datastore/`).
- **Zero Private Keys in Repository:** Signing keystores (`*.jks`, `*.keystore`, `keystore.properties`) are strictly excluded via `.gitignore` and enforced by CI secret scanning. Published APKs contain only the standard public X.509 verification certificate (`META-INF/`), never any private key material.

### 3. Permission Transparency

The default `sideload` release APK requests **only** low-risk, transparent Android permissions and passes **Google Play Protect** without warnings:

| Permission | Why It Is Used |
| :--- | :--- |
| `android.permission.VIBRATE` | Subtle tactile haptic ticks during icon drag-and-drop, Control Center sliders, and Edit Mode. |
| `android.permission.ACCESS_NETWORK_STATE` | Displays live Wi-Fi / Cellular status indicators in the iOS Status Bar and Control Center. |
| `android.permission.EXPAND_STATUS_BAR` | Optional manual fallback button (*"System Notifications"*) inside Notification Center. |
| `android.permission.WRITE_SETTINGS` | Optional user-granted toggle to persist system-wide brightness, auto-rotate, and screen timeout from HNM Settings / Control Center. |
| `<queries>` (`MAIN` / `LAUNCHER` / `HOME`) | Android 11+ scoped package visibility to list launchable apps without requesting `QUERY_ALL_PACKAGES`. |

### 4. Verifying Official APK Signatures

Every official HNMUI release APK is signed with the **HNMLabs Release Key**. You can verify an APK before installing using `apksigner` or `keytool`:

```bash
keytool -printcert -jarfile HNMUI-v0.3.3-alpha.apk
```

Expected certificate identity:
- **Subject / Issuer:** `CN=HNMUI, OU=Android, O=HNMLabs, L=Istanbul, ST=Istanbul, C=TR`
- **SHA-256 Fingerprint:** `94:A4:BA:C6:70:06:61:E3:6B:BD:16:77:FA:07:74:B0:A6:01:F4:94:F9:28:69:C1:B6:6C:8D:10:DB:16:B7:FF`
- **SHA-1 Fingerprint:** `D2:93:2B:4F:88:E2:EB:16:30:13:93:B2:14:5E:59:29:B2:CA:5A:86`

### 5. Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.  
Instead, report vulnerabilities privately through **GitHub Security Advisories**:

👉 **[Report a Private Security Vulnerability](https://github.com/HNMLabs/HNMUI/security/advisories/new)**

We aim to acknowledge reports within **48 hours** and publish a verified fix release as quickly as possible.

---

<a id="türkçe"></a>

## 🇹🇷 Güvenlik ve Gizlilik Politikası

### 1. Desteklenen Sürümler

Güvenlik ve kararlılık güncellemeleri en güncel sürüm serisi üzerinden sunulmaktadır:

| Sürüm | Destek Durumu | Açıklama |
| :--- | :---: | :--- |
| **`0.3.x`** (`v0.3.3-alpha`+) | ✅ | Aktif olarak desteklenen güncel sürüm |
| `0.2.x` | ❌ | Lütfen `v0.3.3-alpha` veya üstüne güncelleyin |
| `0.1.x` | ❌ | İlk alfa sürümleri — kullanım dışı |

### 2. Sıfır İnternet İzni & %100 Çevrimdışı Tasarım

**HNMUI, `android.permission.INTERNET` izni İSTEMEZ.**

- **Sıfır Telemetri ve Takip:** Kod tabanında hiçbir analitik servisi, reklam SDK'sı, çökme izleyici veya dış sunucu bağlantısı bulunmaz.
- **Tamamen Cihaz İçi Depolama:** Ana ekran düzeni, klasörler, Dock kısayolları, hızlı hatırlatıcılar ve özelleştirme ayarları yalnızca cihaz içindeki izole **Jetpack DataStore** alanında saklanır.
- **Depoda Sıfır Özel Anahtar:** İmzalama anahtarları (`*.jks`, `*.keystore`, `keystore.properties`) `.gitignore` ve otomatik CI güvenlik taraması ile korunur; GitHub deposunda hiçbir özel anahtar veya parola yer almaz. Yayınlanan APK dosyaları yalnızca Android'in güncelleme doğrulaması için zorunlu tuttuğu **genel (public)** X.509 sertifikasını içerir.

### 3. İzin Şeffaflığı Tablosu

Varsayılan `sideload` APK'sı **Google Play Protect** uyumludur ve hiçbir yüksek riskli izin (`SMS`, `Accessibility`, `QUERY_ALL_PACKAGES`) içermez:

| İzin | Kullanım Amacı |
| :--- | :--- |
| `android.permission.VIBRATE` | Sürükle-bırak, Denetim Merkezi sürgüleri ve Düzenleme Modu için hafif dokunsal geri bildirim. |
| `android.permission.ACCESS_NETWORK_STATE` | iOS Durum Çubuğu ve Denetim Merkezi'nde canlı Wi-Fi / Hücresel durum gösterimi. |
| `android.permission.EXPAND_STATUS_BAR` | Bildirim Merkezi içindeki isteğe bağlı *"Sistem Bildirimleri"* yedek kısayolu. |
| `android.permission.WRITE_SETTINGS` | İsteğe bağlı olarak HNM Ayarlar / Denetim Merkezi üzerinden sistem parlaklığı ve döndürme kilidini kalıcı yazma. |
| `<queries>` (`MAIN` / `LAUNCHER` / `HOME`) | Android 11+ kurallarına uygun güvenli başlatılabilir uygulama sorgusu. |

### 4. Resmi APK İmza Doğrulaması

Resmi HNMUI sürümlerinin bütünlüğünü kurulum öncesinde aşağıdaki komutla doğrulayabilirsiniz:

```bash
keytool -printcert -jarfile HNMUI-v0.3.3-alpha.apk
```

Beklenen resmi sertifika bilgileri:
- **Sahip / İmzalayan:** `CN=HNMUI, OU=Android, O=HNMLabs, L=Istanbul, ST=Istanbul, C=TR`
- **SHA-256 Parmak İzi:** `94:A4:BA:C6:70:06:61:E3:6B:BD:16:77:FA:07:74:B0:A6:01:F4:94:F9:28:69:C1:B6:6C:8D:10:DB:16:B7:FF`
- **SHA-1 Parmak İzi:** `D2:93:2B:4F:88:E2:EB:16:30:13:93:B2:14:5E:59:29:B2:CA:5A:86`

### 5. Güvenlik Açığı Bildirimi

Güvenlik açıklarını lütfen herkese açık Issue olarak paylaşmayın. Bunun yerine doğrudan **GitHub Özel Güvenlik Bildirimi** üzerinden iletin:

👉 **[Özel Güvenlik Açığı Bildir](https://github.com/HNMLabs/HNMUI/security/advisories/new)**

Bildirimler **48 saat** içerisinde incelenir ve doğrulanan bulgular için hızla yama sürümü yayınlanır.
