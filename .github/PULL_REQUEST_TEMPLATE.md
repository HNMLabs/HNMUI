## Summary / Özet
<!-- Briefly describe what changed and why / Neyi ve neden değiştirdiğinizi kısaca açıklayın -->

## Quality & Safety Checklist / Kalite ve Güvenlik Kontrol Listesi
- [ ] **120 FPS Safe:** No synchronous Binder IPC (`PackageManager`, `Settings`, `AudioManager`) on the Main UI Thread during composition or animations.
- [ ] **Gesture Safe:** Every `awaitEachGesture` calls `awaitFirstDown` as its unconditional first statement.
- [ ] **Play Protect Safe:** Zero `INTERNET` or high-risk permissions added to `app/src/main/AndroidManifest.xml`.
- [ ] **Zero Secrets:** No `.jks`, `.keystore`, `keystore.properties`, `local.properties`, tokens, or private keys included.
- [ ] **Verified:** `./gradlew testSideloadDebugUnitTest lintVitalSideloadRelease` passes cleanly.
