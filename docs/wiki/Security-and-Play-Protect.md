# Security & Play Protect / Güvenlik ve Play Protect Uyumu

## 🔒 Offline-by-Design (`0` Network Access)

**HNMUI** is engineered as a 100% local, privacy-preserving system launcher:
- **`android.permission.INTERNET` is NOT declared** in `AndroidManifest.xml` — the Android OS kernel physically blocks the app from opening network sockets.
- All layout data, folders, Dock pins, and Quick Reminders live exclusively inside your device's private **Jetpack DataStore**.

---

## 🛡️ Why Dual Build Flavors (`sideload` vs `playstore`)?

Google Play Protect flags sideloaded (non-Play-Store) APKs from new developer certificates if they bundle `BIND_NOTIFICATION_LISTENER_SERVICE` in the manifest.

| Build Flavor | Target Channel | Manifest Profile |
| :--- | :--- | :--- |
| **`sideload`** *(Default GitHub APK)* | Direct APK installation | **Zero restricted services** — installs cleanly with **no Play Protect warning** |
| **`playstore`** | Google Play Store AAB/APK | Merges optional `HnmNotificationListenerService` for live unread count badges |

---

## 🔑 Verifying Official Releases / İmza Doğrulaması

Private signing keys (`*.jks`, `keystore.properties`) are never stored in this repository. Every GitHub push and Pull Request is verified by the **[Android CI & Secret Audit](../../.github/workflows/android-ci.yml)** workflow.

To verify an APK's public X.509 certificate:
```bash
keytool -printcert -jarfile HNMUI-v0.3.4-alpha.apk
```
- **SHA-256:** `94:A4:BA:C6:70:06:61:E3:6B:BD:16:77:FA:07:74:B0:A6:01:F4:94:F9:28:69:C1:B6:6C:8D:10:DB:16:B7:FF`
