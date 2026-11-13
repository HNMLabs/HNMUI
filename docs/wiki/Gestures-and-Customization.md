# Gestures & Customization / Hareketler ve Özelleştirme

## 🧭 Gesture Map / Hareket Haritası

| Gesture / Hareket | Action (EN) | İşlev (TR) |
| :--- | :--- | :--- |
| **Pull down from Top-Right / Right Edge** / **Sağ Üstten veya Sağ Kenardan Çek** | Opens frosted-glass **Control Center** (`86dp+` cutout-aware zone + diagonal thumb-arc recognition; Brightness, Volume, Live Appearance Mode, Connectivity, Media, Torch, Rotation) | Buzlu cam **Denetim Merkezi** kesintisiz açılır (`86dp+` kamera deliği duyarlı alan + çapraz başparmak desteği; Parlaklık, Ses, Görünüm Modu, Bağlantılar, Medya, Fener, Yön Kilidi) |
| **Pull down from Top-Left** / **Sol Üstten Aşağı Çek** | Opens **HNM Notification & Summary Center** instead of One UI shade | One UI bildirim paneli yerine **HNM Bildirim & Özet Merkezi** açılır |
| **Swipe down on Middle Home** / **Orta Alanda Aşağı Çek** | Opens **Spotlight Quick Search** | **Spotlight Hızlı Arama** açılır |
| **Swipe Right (Page 0)** / **En Sola Kaydır** | **Today View** (Clock, Quick Notes, Suggested Apps, Android AppWidgets) | **Today View** (Saat, Hızlı Notlar, Önerilenler, Android Widget'lar) |
| **Swipe Left (Last Page)** / **En Sağa Kaydır** | **App Library** (Auto-categorized 2x2 frosted pods + A–Z search) | **Uygulama Arşivi** (Otomatik 2x2 kategori kutuları + arama) |
| **Long-Press Icon (Hold)** / **İkona Uzun Basıp Bekle** | Opens **Haptic Quick Action Menu** (App Shortcuts, Folder, Pin to Dock) | **Haptic Kısayol Menüsü** açılır |
| **Long-Press Icon + Move Finger** / **Uzun Basıp Parmağı Kaydır** | Enters **120 FPS Drag-and-Drop** across Home Grid, Folders, Pages, and Dock! | **120 FPS Sürükle-Bırak** başlar (Izgara, Klasör, Sayfalar ve Dock dahil!) |
| **Long-Press Empty Space** / **Boş Alana Uzun Bas** | Enters **Edit (Jiggle) Mode** (Wallpaper, `7x7` Grid & Dock Studio) | **Düzenleme (Jiggle) Modu** açılır |

---

## 🧲 120 FPS Drag-and-Drop & Dock / Sürükle-Bırak ve Dock

- **Reorder & Cross-Page Move / Sıralama ve Sayfa Geçişi:** Hold any app or folder and slide your finger. Hold near the left or right screen edge for `480ms` to glide smoothly to the adjacent Home Screen page.
- **Create or Merge Folders / Klasör Oluşturma ve Birleştirme:** Hover an app directly over another app or folder (or use the Haptic Quick Menu) to create or merge folders cleanly.
- **Drag Into Dock / Dock'a Uygulama Sürükleme:** Drag any Home Screen app down into the floating frosted-glass Dock to pin it. If the Dock is already at maximum capacity, HNMUI performs a **smart swap** with the hovered Dock app so nothing is lost.
- **Drag Out of Dock / Dock'tan Uygulama Çıkarma:** Long-press and drag any Dock app upward onto the Home Screen grid to unpin it from the Dock and place it on that page.

---

## 🎛️ Appearance Mode, `7x7` Grids & Resizable Dock Studio / Özelleştirme Stüdyosu

Open **HNM Settings → Home Screen, Folder & Dock** (or **Display & Brightness**, **Control Center**, or tap **"Izgara & Dock"** in Edit Mode):
- **Appearance Mode (`Otomatik` / `Açık` / `Koyu`):** Follow Android's system Dark/Light mode automatically with live `380ms` frosted-glass transitions, or lock **Daylight Pearlescent Glass (`Açık`)** / **Midnight Dark Glass (`Koyu`)** manually.
- **Home Grid:** `4` to `7` columns × `4` to `7` rows (up to **`7x7` = 49 slots per page**), icon scale (`80%–120%`), label toggle.
- **Folder Grid:** `3x3` up to **`7x7`** expanded folder columns + `2x2` or `3x3` mini-grid folder cover preview.
- **Dock Studio:** Scale the Dock (`75%–125%`), adjust Dock capacity (`3–7` pinned apps), hide/show the Dock completely, and tune frosted-glass opacity (`65%–135%`).
- **iOS Status Bar Toggle:** Hide One UI's top status bar icons in favor of HNMUI's calm top bar (`useIosStatusBar`).
