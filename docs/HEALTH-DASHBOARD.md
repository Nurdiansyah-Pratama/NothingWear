# Health & Device Dashboard Specification (`M6`)

**Status:** Implemented in `0.6.0-m6`.

## 1. Confirmed Data Sources (Zero Fabricated Metrics)

NothingWear M6 displays wearable and health telemetry **strictly from verified, user-authorized sources**, with explicit `PERMISSION NEEDED`, `NO DATA`, or `UNAVAILABLE` states whenever a metric cannot be verified:

1. **Standard Bluetooth SIG Heart Rate Service (`0x180D` → `0x2A37`):**
   - Physically verified on `Xiaomi Watch S4 06A3` (`C4:57:**:**:**:A3`) during M2 GATT discovery.
   - Decoded with `HeartRateMeasurementParser` per official Bluetooth SIG GATT specification (UINT8 & UINT16 BPM, sensor contact status, energy expended, RR intervals).
   - Controlled, explicit user-triggered subscription strictly whitelisted to standard CCCD `0x2902` on `0x2A37` only (**zero writes** to `0xFDAB` or any proprietary vendor characteristic; official Mi Fitness pairing preserved).
2. **Android Health Connect (`androidx.health.connect:connect-client:1.1.0`, Read-Only):**
   - On-demand read permissions declared in `AndroidManifest.xml` (`READ_STEPS`, `READ_HEART_RATE`, `READ_SLEEP`) alongside Android 13 (`ACTION_SHOW_PERMISSIONS_RATIONALE`) and Android 14/15 (`VIEW_PERMISSION_USAGE` / `HEALTH_PERMISSIONS`) handlers.
   - Reads today's verified aggregate `StepsRecord`, today's `HeartRateRecord` samples (latest + min–max), and recent 30h `SleepSessionRecord` duration, attributing every verified reading to its source `dataOrigin.packageName` (e.g., `com.xiaomi.wearable` when Mi Fitness Health Connect sync is enabled).
3. **Standard BLE Battery (`0x180F / 0x2A19`) & Device Info (`0x180A`):**
   - Displayed only when exposed by the inspected GATT peripheral.
   - Explicitly marked `UNAVAILABLE` on Xiaomi Watch S4 (which does not expose `0x180F` or `0x180A` over standard GATT).
4. **Host Companion Phone Environment:**
   - Displays real host phone model, Android release / API level, Bluetooth adapter status, and phone battery % / charging status — clearly labeled **Host Phone Environment** and never substituted for watch battery.

## 2. Xiaomi Watch S4 + Mi Fitness Workflow (Infinix XOS 15)
- **Unified Auto-Switch Heart Rate (Live & Outside Broadcast):**
  1. When Watch S4 **Heart Rate Broadcast** is ON and NothingWear is listening on `0x2A37`, the card streams **Live BLE `0x2A37`** BPM in real time with mini sparkline bars.
  2. Outside Heart Rate Broadcast (when Watch S4 firmware disables `0x2A37`), NothingWear automatically switches to **Android Health Connect (last 24h)** readings synced in the background from **Mi Fitness → Profile → Third-party data → Health Connect**, refreshing automatically on `ON_RESUME`.
  3. If neither live broadcast nor a new Health Connect sample is available yet, NothingWear shows the persisted **Last Verified Reading** (stored in DataStore with its exact original timestamp and source label — never fabricated).
- **Steps, Heart Rate History & Sleep:** Open **Mi Fitness → Profile → Third-party data → Health Connect** and enable sync, then tap **Grant Health Connect Read Access** (or **Open Android Health Connect Settings**) in **NothingWear → Health & Device Dashboard**.
- **Live BLE Heart Rate (`0x2A37`):** Inspect Watch S4 (`Devices` tab or 1-tap `Inspect Watch S4`), enable Heart Rate Broadcast on Watch S4, and tap **Listen Live BLE Broadcast (0x2A37)**.
