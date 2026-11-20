# BLE Discovery & Protocol Research (`M2` / `M3`)

**M2 Status:** Implemented in `0.3.0-m2` (strict read-only BLE discovery & GATT diagnostics).  
**M3 Status:** Pending Approval (Not started).

## M2 Implemented Capabilities (Read-Only BLE Discovery)
- **Android-Version-Aware Permissions (`BluetoothPermissionChecker`):**
  - Android 12+ (API 31+, including Infinix XOS 15): `BLUETOOTH_SCAN` (`usesPermissionFlags="neverForLocation"`) and `BLUETOOTH_CONNECT`.
  - Android 8.0–11 (API 26–30): legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION` (`maxSdkVersion="30"`).
  - Never requests `BLUETOOTH_ADVERTISE` or `POST_NOTIFICATIONS`.
- **Dual-Source Device Discovery (`AndroidBleDiagnosticsRepository`):**
  - Lists **System GATT / GATT_SERVER Connected** (`BluetoothProfile.GATT` & `GATT_SERVER`), **Paired / Bonded** devices (with cached read-only **Classic SDP UUIDs**), and **Nearby BLE Scan** results (15s auto-timeout).
- **Evidence-Based Heuristic Identification (`WatchS4Identifier`):**
  - Classifies peripherals into `Likely Watch S4` (name match), `Xiaomi Device` (Xiaomi SIG Manufacturer ID `0x038F`, Huami `0x0157`, or advertised Mi Service `0xFE95` / `0xFDAB`), or `BLE Peripheral`.
- **Strict Read-Only GATT Inspector (`GattUuidLookup` & `GattStatusDecoder`):**
  - Zero GATT characteristic writes, zero descriptor writes, zero unknown commands.
  - Guarded with session IDs and a 450ms reconnect cooldown so stale disconnect callbacks from an earlier GATT handle never corrupt a re-inspect attempt.
  - Detects Classic BR/EDR-only entries vs Dual/BLE devices, inspects cached SDP UUIDs (`device.uuids`) read-only, and tries `Direct LE`, `Dual/Auto Transport (TRANSPORT_AUTO)` (for Dual-mode watches), and `Passive Auto-Attach (autoConnect=true)` cleanly without disrupting Mi Fitness pairing.
  - Sequential read-only reads are whitelisted **exclusively** to standard Bluetooth SIG Battery Level (`0x2A19`) and Device Information strings (`0x2A24`, `0x2A26`, `0x2A27`, `0x2A28`, `0x2A29`) when exposed with `PROPERTY_READ`.
  - Always cleanly disconnects & closes `BluetoothGatt` to preserve official Mi Fitness pairing.
- **Redacted Logging & Exportable Diagnostic Report (`LogRedactor`):**
  - Masks all MAC addresses (`AA:BB:CC:DD:EE:FF` → `AA:BB:**:**:**:FF`) and embedded email strings in logs and clipboard-exported diagnostic reports.

## M2 Physical Hardware Observation (Verified 2026-11-21)
- **Test Phone:** Infinix X6880 (Android 15, API 35)
- **Target Wearable:** Xiaomi Watch S4 (`06A3`, redacted MAC `C4:57:**:**:**:A3`, `DEVICE_TYPE_DUAL`, bonded via official Mi Fitness)
- **Successful Transport:** `BluetoothDevice.TRANSPORT_AUTO` (GATT over BR/EDR+LE) connected in `< 1s` (`GATT_SUCCESS (0)`) without disrupting Mi Fitness pairing; `TRANSPORT_LE` direct connect timed out while already linked to Mi Fitness.
- **Discovered GATT Services (Strict Read-Only, 0 Writes):**
  1. `0000fdab-0000-1000-8000-00805f9b34fb` — **Xiaomi Wearable Service (`0xFDAB`)**
     - `00000001-0000-1000-8000-00805f9b34fb` (`[READ]`)
     - `00000002-0000-1000-8000-00805f9b34fb` (`[WRITE_NR, NOTIFY]`)
     - `00000003-0000-1000-8000-00805f9b34fb` (`[WRITE_NR, NOTIFY]`)
  2. `0000180d-0000-1000-8000-00805f9b34fb` — **Heart Rate Service (`0x180D`)**
     - `00002a37-0000-1000-8000-00805f9b34fb` (`[NOTIFY]` — Heart Rate Measurement)
- **Standard Battery (`0x180F`) & Device Info (`0x180A`):** Not exposed on Watch S4 GATT (honestly reported as `Unavailable`).
- **Cached Classic / SDP UUIDs (`device.uuids`):**
  - `00001101-0000-1000-8000-00805f9b34fb` — Serial Port Profile / SPP (`0x1101`)
  - `0000110a-0000-1000-8000-00805f9b34fb` — A2DP Audio Source (`0x110A`)
  - `0000111e-0000-1000-8000-00805f9b34fb` — Hands-Free Profile / HFP (`0x111E`)
  - `0000111f-0000-1000-8000-00805f9b34fb` — HFP Audio Gateway (`0x111F`)
  - `00001124-0000-1000-8000-00805f9b34fb` — Human Interface Device / HID (`0x1124`)

## M3 Scope (Comparative Research in `protocol-lab/` — Pending Approval)
- Reference A: Xiaomi 14T (Mi Fitness Events menu available).
- Target B: Infinix XOS 15 (same Mi Fitness version, Events menu unavailable).
- Keep watch firmware and Mi Fitness versions constant and record exact build/version numbers in every experiment.
- Never fabricate undocumented GATT UUIDs, framing headers, checksums, or encryption details.
