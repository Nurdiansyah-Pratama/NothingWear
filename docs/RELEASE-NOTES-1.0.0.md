# NothingWear v1.0.0 — Release Notes & Integration Verification (`M7`)

**Version:** `1.0.0` (`versionCode = 7`)  
**Package:** `com.nothingwear.app`  
**Minimum / Target SDK:** Android 8.0 (`API 26`) → Android 16/17 (`compileSdk = 37`, `targetSdk = 37`)  
**Primary Verified Hardware:** Infinix X6880 (Infinix XOS 15.1.2, Android 15 / API 35) + Xiaomi Watch S4 (`06A3`, `DEVICE_TYPE_DUAL`, Mi Fitness `3.60.0i`)

---

## 1. Delivered Capabilities Across Milestones (M0–M7)

- **M0 — Foundation & Design System:** Clean Architecture + MVVM (`:app`), Nothing-inspired Material 3 UI (Dark default, Light, System, 6 custom accents persisted in DataStore), 5-tab navigation, zero fabricated data.
- **M1 — Offline-First Calendar Engine:** Strictly read-only `READ_CALENDAR` reading Android Calendar Provider & synced Google Calendars into Room (`v1`→`v2`), UTC all-day normalization, Today / 7 Days / 30 Days views, live search, per-calendar filters, and Event Detail screen.
- **M2 — Device & Read-Only BLE Discovery:** Android-version-aware `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, bonded/system GATT & 15s BLE scan, Watch S4 identification, `<1s` `TRANSPORT_AUTO` read-only GATT inspector (`0xFDAB` & `0x180D`), cached SDP UUID inspection (`0x1101` SPP, HFP, A2DP, HID), MAC/email redaction, and clipboard diagnostic report.
- **M3 — Isolated Reverse Engineering Lab (`protocol-lab/`):** Pure-Python ADB device profiler, read-only APK symbol scanner, BTSnoop v1 HCI parser with unit tests, Target B (Infinix X6880) redacted profile, Xiaomi 14T capture guide, and evidence-backed `M3-RESEARCH-REPORT.md`.
- **M4 — Protocol Adapter & Watch Reminder Bridge:** Isolated JVM Gradle module `:protocol-adapter:xiaomi-watch-s4` honestly reporting `UNSUPPORTED` (`WearAuthV2`), paired with a physically verified WorkManager (`2.12.0`) Watch Reminder Bridge delivering unique `VISIBILITY_PUBLIC` calendar notifications via Mi Fitness `NotifySyncService` to Xiaomi Watch S4 (plus 1-tap instant & 10s lockscreen tests).
- **M5 — Watch Face Studio (466×466 px):** Interactive circular canvas preview, 6 categories (`Nothing`, `Minimal`, `Analog`, `Cyberpunk`, `Tactical`, `Custom`), full color/dial/ring/font/complication customization, Room v2 local project library (`MIGRATION_1_2`), **5 Mi Fitness Kustom safe-zone alignment templates** (`Custom analog`, `Custom elegance`, `Custom digital`, `Custom postcard`, `Custom diamond`), preview-only safe-zone guides, and clean **Background-Only 466×466 PNG** export directly to `Pictures/NothingWear` (zero hand/clock overlap).
- **M6 — Verified Health & Device Dashboard:** Live Bluetooth SIG Heart Rate (`0x180D / 0x2A37`) monitor with pure-Kotlin SIG payload parser & mini sparkline bars, read-only Android Health Connect (`1.1.0`) integration (`Steps`, `Heart Rate`, `Sleep`) with origin attribution, Unified Auto-Switch Heart Rate (`Live BLE` → `Health Connect Auto` → persisted `Last Verified` cache), `ON_RESUME` auto-refresh, and separated Host Phone telemetry.
- **M7 — Integration, Room Migration & Release Hardening:** Real Android SQLite Room `MIGRATION_1_2` instrumented test (`RoomMigrationTest`) + JVM schema snapshot check (`RoomSchemaIntegrityTest`), Infinix XOS 15 live battery optimization status & settings shortcuts in `SettingsScreen`, R8/ProGuard rules (`app/proguard-rules.pro`), verified Release APK build (`:app:assembleRelease`), and complete limitation documentation.

---

## 2. Physical Hardware Verification Matrix (Infinix X6880 + Xiaomi Watch S4)

| Area | Verification Method | Result |
| :--- | :--- | :--- |
| Calendar Provider + Offline Room Cache | Live Android/Google calendar sync + offline read | **Verified** |
| Read-Only BLE & GATT Inspection (`0xFDAB`, `0x180D`) | `TRANSPORT_AUTO` on Watch S4 `06A3` (`<1s`, 0 writes) | **Verified** |
| Watch Reminder Bridge → Watch S4 | WorkManager + Mi Fitness `NotifySyncService` alert received on Watch S4 | **Verified** |
| Watch Face Studio Background-Only PNG | 466×466 PNG exported to `Pictures/NothingWear` & applied in Mi Fitness **Kustom** | **Verified** |
| Live Standard BLE Heart Rate (`0x2A37`) | Real-time BPM streaming from Watch S4 Heart Rate Broadcast (`GATT_SUCCESS`) | **Verified** |
| Health Connect (Steps & Sleep) | Read-only Android Health Connect `1.1.0` synced from Mi Fitness (`com.xiaomi.wearable`) | **Verified** |
| Room `MIGRATION_1_2` (SQLite) | Instrumented `RoomMigrationTest` on Infinix X6880 | **Verified** |
| Debug & R8 Release Builds | `:app:assembleDebug` + `:app:assembleRelease` + all unit & Python tests | **Verified** |

---

## 3. Infinix XOS 15 Background Reliability Checklist

To prevent XOS 15 (Phone Master / Power Marathon) from delaying WorkManager calendar reminders to Xiaomi Watch S4:
1. Open **NothingWear → Settings → Background & XOS 15 Battery Reliability → Battery Settings** and set both **NothingWear** and **Mi Fitness** to **Unrestricted / Don't optimize**.
2. In **XOS Phone Master → Auto-start Management**, allow **NothingWear** and **Mi Fitness** to run in background.
3. In **Mi Fitness → Device → App Notifications**, enable **NothingWear** and disable *"Only receive when phone is locked"* if you also want alerts while the phone screen is awake.
4. For automatic background Steps, Sleep, and periodic Heart Rate in NothingWear: enable **Mi Fitness → Profile → Third-party data → Health Connect** (note: Mi Fitness `3.60.0i` pushes Steps quickly and batches Vitals/Heart Rate periodically when Continuous HR is enabled).

---

## 4. Documented Technical Limitations (Honesty Policy)

1. **Xiaomi Watch S4 Native Events App Sync (`UNSUPPORTED`):** Mi Fitness `3.60.0i` gates native Events syncing behind an OEM whitelist (`Xiaomi/Vivo/Oppo/Honor`) and encrypts `0xFDAB` / RFCOMM channel payloads via `WearAuthV1`/`WearAuthV2`. NothingWear never bypasses authentication or writes to `0xFDAB`; it delivers reliable watch calendar alerts through the official notification bridge.
2. **Watch Face Binary Flashing (`.bin`/`.face`):** Direct Vela binary flashing (`FaceInstallBleV2Impl`) requires Xiaomi's proprietary compiler and `WearAuthV2`. NothingWear exports pixel-exact 466×466 clean Background-Only PNGs tailored to Mi Fitness's 5 official **Kustom** watch-face templates.
3. **Watch S4 Battery & Device Info (`UNAVAILABLE`):** Xiaomi Watch S4 does not expose Bluetooth SIG Battery (`0x180F / 0x2A19`) or Device Information (`0x180A`) over standard GATT. NothingWear displays `UNAVAILABLE` unless an inspected device actually exposes those standard characteristics, and keeps Host Phone battery strictly separate.
4. **Live BLE Heart Rate Outside Broadcast:** Watch S4 Vela firmware only transmits standard BLE `0x2A37` notifications while *Heart Rate Broadcast* is turned ON. Outside broadcast, NothingWear automatically falls back to Health Connect (when Mi Fitness pushes periodic Vitals) or the locally persisted Last Verified reading with its original timestamp.
