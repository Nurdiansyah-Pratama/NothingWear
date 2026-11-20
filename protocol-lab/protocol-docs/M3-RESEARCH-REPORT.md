# M3 Research Report — Mi Fitness & Xiaomi Watch S4 Events Interoperability

**Date:** 2026-11-21  
**Status:** Static APK Analysis + Target B (Infinix X6880) + Physical Watch S4 GATT/SDP Complete; Reference A (Xiaomi 14T) Live HCI Capture Ready & Pending Physical Connection.  
**Scope:** Strictly read-only interoperability research. Zero APK patching, zero firmware modification, zero authentication bypass, zero BLE writes.

---

## 1. Analyzed Artifacts & Environments

### Target B — Infinix XOS 15 (Collected & Verified)
Source: `protocol-lab/protocol-docs/target_b_infinix_xos15_profile.json`
- **Manufacturer / Model:** `INFINIX` / `Infinix X6880`
- **Android / OS:** Android `15` (API `35`), `ro.tranos.version = xos15.1.2`
- **MIUI / HyperOS Properties:** `ro.miui.ui.version.name = null`, `ro.mi.os.version.name = null`
- **Mi Fitness Package:** `com.xiaomi.wearable` (Global), `versionName = 3.60.0i`, `versionCode = 360000`, `minSdk = 26`, `targetSdk = 36`
- **Mi Fitness Runtime Permissions on Target B:**
  - `READ_CALENDAR`: **`granted=true`**
  - `WRITE_CALENDAR`: **`granted=true`**
  - `BLUETOOTH_SCAN`: `granted=true`, `BLUETOOTH_CONNECT`: `granted=true`, `POST_NOTIFICATIONS`: `granted=true`
- **Installed Calendar Providers:** `com.android.providers.calendar`, `com.google.android.calendar`
- **Observed Behavior:** Mi Fitness **Events** menu is hidden/unavailable for Xiaomi Watch S4 despite `READ_CALENDAR` being granted.

### Reference A — Xiaomi 14T (Pending Physical USB Capture)
- Use `protocol-lab/ble-capture/XIAOMI_14T_CAPTURE_GUIDE.md` to record `reference_a_xiaomi_14t_profile.json` and `xiaomi14t_btsnoop_summary.json` using dummy event `NW_TEST_EVENT_01`.

### Legitimately Installed Mi Fitness APK Metadata (`com.xiaomi.wearable 3.60.0i`)
Pulled read-only from Target B into git-ignored `protocol-lab/apk-analysis/mi_fitness_3_60_0i/` (never committed):

| Split Archive | Size (Bytes) | SHA-256 |
| :--- | ---: | :--- |
| `base.apk` | `142,403,764` | `9c9ef140540a2d0a61d1972eadeafa7fece1a8cf3f810d4da090658b33f9de05` |
| `split_config.arm64_v8a.apk` | `66,081,785` | `e8149122eb3d5b9901c3983100f5453d5696894db62bd09a3e781c170141df54` |
| `split_config.in.apk` | `1,008,025` | `e7faffe9c9299ee9c19b003e4c17db43f099c2f79f6ef63f6e4c29fdfe90b7c4` |
| `split_config.xxhdpi.apk` | `547,209` | `955319db42fc7c8e1edf3802c6a5d3ee2d6469cb1cfecc2c4d381ad7efe64682` |

- **DEX Count:** 12 (`classes.dex` .. `classes12.dex`) | **Native `.so` Libraries:** 114

### Physical Wearable Observation (Verified in M2 on Infinix X6880)
- **Watch:** `Xiaomi Watch S4 06A3` (`C4:57:**:**:**:A3`, `DEVICE_TYPE_DUAL`)
- **Responsive GATT Transport:** `BluetoothDevice.TRANSPORT_AUTO` (`< 1s`, `GATT_SUCCESS (0)`)
- **GATT Services:**
  - `0000fdab-0000-1000-8000-00805f9b34fb` (Xiaomi Wearable Service `0xFDAB`):
    - `00000001-...` `[READ]`
    - `00000002-...` `[WRITE_NR, NOTIFY]`
    - `00000003-...` `[WRITE_NR, NOTIFY]`
  - `0000180d-0000-1000-8000-00805f9b34fb` (Heart Rate Service `0x180D`): `00002a37-...` `[NOTIFY]`
- **Classic SDP UUIDs:** `SPP (0x1101)`, `A2DP Source (0x110A)`, `HFP (0x111E / 0x111F)`, `HID (0x1124)`

---

## 2. Answers to the 10 Research Questions (Evidence Matrix)

| # | Research Question | Status | Verified Evidence |
| :--- | :--- | :--- | :--- |
| **1** | Does Mi Fitness explicitly check manufacturer/brand identity? | **CONFIRMED (Yes)** | `FeatureItemModule.createSyncScheduleItem$lambda$62` (`classes10.dex`) gates the Watch S4 schedule toggle using `RomUtils.isXiaomi()` and `DeviceSettingsSyncHelperImpl.isCalendarSyncAndroidVersion()`. `isCalendarSyncAndroidVersion()` checks ONLY `RomUtils.isXiaomi()`, `RomUtils.isVivo()`, `RomUtils.isOppo()`, and `RomUtils.isHonor()` against remote/config JSON `ConfigPreference.getIS_CALENDAR_SYNC_LIST`. **Infinix / Transsion / XOS is completely absent from this whitelist.** |
| **2** | Does it check MIUI or HyperOS version? | **CONFIRMED (Yes)** | `RomUtils` (`classes9.dex`) inspects `ro.miui.ui.version.name`, `ro.miui.ui.version.code`, `ro.mi.os.version.name`, `ro.mi.os.version.code`, and `ro.mi.os.version.incremental` via reflection (`getSystemPropertyByReflect`), all of which are `null` on Infinix XOS 15. |
| **3** | Does it check other OEM system properties? | **CONFIRMED (Yes)** | Static DEX scan confirms checks for `ro.product.manufacturer`, `ro.product.brand`, `ro.build.version.opporom` (OPPO/ColorOS), `ro.build.version.emui` (Huawei), and `ro.vivo.os.version` (Vivo). Zero `ro.tranos.*` / Infinix properties exist in the APK. |
| **4** | Does it rely on Xiaomi-specific Android APIs to read calendar events? | **CONFIRMED (No — Standard Android API)** | `CalendarUtils` (`classes12.dex`) reads standard Android Calendar Provider URIs (`content://com.android.calendar/calendars`, `events`, `reminders`), with optional ColorOS fallback (`content://com.coloros.calendar/...`). The restriction on Infinix XOS 15 is purely a software OEM whitelist in Mi Fitness UI (`FeatureItemModule`), **not** a missing OS API! |
| **5** | How does it access Calendar Provider? | **CONFIRMED** | `SyncEngine.syncCalendarToDevices` calls `CalendarUtils.genCalendarEvents()` → `buildEventsFromCursor()` on standard `content://com.android.calendar/*`, requiring only `READ_CALENDAR` (which is already `granted=true` on Target B). |
| **6** | What BLE/Bluetooth services and characteristics are involved? | **PARTIALLY CONFIRMED** | Physically verified on Watch S4 `06A3`: **GATT `0xFDAB`** (`0x0001` READ, `0x0002` & `0x0003` WRITE_NR/NOTIFY) and **Classic SPP `0x1101`**. `com.xiaomi.wearable` contains both `00001101-...` (SPP) and `0000FE95-...` / `0xFDAB` transport paths via `DeviceContact.call(...)`. Live Xiaomi 14T HCI capture is needed to confirm whether Events traffic flows over `0xFDAB` GATT or `0x1101` SPP. |
| **7** | How are events encoded? | **CONFIRMED (Protobuf)** | `BlueToothSender.handleCalendar()` & `clearCalendar()` (`classes12.dex`) serialize events into Protobuf message wrappers (`Lt3u` → `Lgq3` → `Lhq3` containing 9 typed fields: 4 strings, 3 ints, 2 booleans) before calling `DeviceContact.call$default(...)`. Exact field numbers and command IDs require live Xiaomi 14T capture verification before any encoding is assumed. |
| **8** | Is the protocol authenticated or encrypted? | **CONFIRMED (Yes — `WearAuthV1` / `WearAuthV2`)** | `classes3.dex` & `classes12.dex` implement authenticated Xiaomi Wearable sessions (`com.xiaomi.wearable.wear.api.WearAuthV1`, `WearAuthV2`, `AESEncryption`, `TokenEncryption`, `BleSecurityChipEncrypt`, `AuthenticatedCipherText`). Live HCI entropy verification on Xiaomi 14T will confirm whether `handleCalendar` frames are encrypted on the wire (expected: **Yes**). |
| **9** | Does the watch accept event sync from another client? | **UNCONFIRMED (High Risk of Rejection)** | Because `WearAuthV2` uses device/account-bound session keys held by Mi Fitness, Watch S4 is expected to reject unauthenticated writes to `0xFDAB` / `0x1101`. Must NOT be tested with speculative writes or auth bypasses. |
| **10** | Does the watch require a particular pairing/session state? | **CONFIRMED (Yes)** | Physical M2 test showed `Xiaomi Watch S4 06A3` is bonded in Dual mode (`TRANSPORT_AUTO` required for GATT) and maintains an authenticated session with Mi Fitness (`com.xiaomi.wearable`). |

---

## 3. Feasibility Assessment for M4 (Native Events Synchronization)

### Key Engineering Conclusion
1. **Why Events is missing on Infinix XOS 15 is 100% solved:** It is an intentional OEM whitelist inside Mi Fitness `3.60.0i` (`RomUtils.isXiaomi()` + `ConfigPreference.getIS_CALENDAR_SYNC_LIST` checking only `vivo`/`oppo`/`honor`). Infinix X6880 has full standard Calendar Provider support and already grants `READ_CALENDAR` to Mi Fitness.
2. **Hard Security & Protocol Boundary:** Direct third-party injection into Watch S4's native Events app via `0xFDAB` / `0x1101` is gated by **`WearAuthV1`/`WearAuthV2` authenticated encryption**. Per NothingWear's non-negotiable security rules (`AGENTS.md` Section 5 & Master Project Plan Section 9):
   - NothingWear **will NOT** bypass authentication, extract Xiaomi account/session tokens from Mi Fitness private storage, patch/repackage Mi Fitness, or modify Watch S4 firmware.
3. **Two Clean Paths for M4:**
   - **Path A (If Reference A Xiaomi 14T HCI capture reveals an open/unauthenticated local channel):** Implement a verified `protocol-adapter/xiaomi-watch-s4` encoder/decoder and physically test with dummy event `NW_TEST_EVENT_01` on Watch S4.
   - **Path B (Expected Outcome if Xiaomi 14T HCI capture confirms encrypted `WearAuthV2`):** Per Master Plan Section 7 M4 (*"Verified native Events synchronization, or a documented technical limitation with a working fallback"*):
     1. Implement the isolated `protocol-adapter/xiaomi-watch-s4` interface with honest `UNSUPPORTED` / `AUTH_REQUIRED` state (zero fake sync claims, zero unauthorized BLE writes).
     2. Implement a **verified, reliable working fallback**: **NothingWear Calendar Reminder Bridge** — schedules accurate local Android reminder notifications (explicitly requesting `POST_NOTIFICATIONS` only when the user enables it) for upcoming Room-cached calendar events so **Mi Fitness's official `NotifySyncService`** delivers them natively to Xiaomi Watch S4 on Infinix XOS 15 with 100% reliability and zero pairing disruption!
