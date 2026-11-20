# BLE Discovery & Protocol Research (`M2` / `M3`)

**M2 Status:** Implemented in `0.3.0-m2` (strict read-only BLE discovery & GATT diagnostics).  
**M3 Status:** Pending Approval (Not started).

## M2 Implemented Capabilities (Read-Only BLE Discovery)
- **Android-Version-Aware Permissions (`BluetoothPermissionChecker`):**
  - Android 12+ (API 31+, including Infinix XOS 15): `BLUETOOTH_SCAN` (`usesPermissionFlags="neverForLocation"`) and `BLUETOOTH_CONNECT`.
  - Android 8.0–11 (API 26–30): legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, and `ACCESS_FINE_LOCATION` (`maxSdkVersion="30"`).
  - Never requests `BLUETOOTH_ADVERTISE` or `POST_NOTIFICATIONS`.
- **Dual-Source Device Discovery (`AndroidBleDiagnosticsRepository`):**
  - Lists **System GATT Connected** (`BluetoothProfile.GATT`) and **Paired / Bonded** devices alongside **Nearby BLE Scan** results (15s auto-timeout) so Xiaomi Watch S4 paired via Mi Fitness remains visible even when not actively advertising.
- **Evidence-Based Heuristic Identification (`WatchS4Identifier`):**
  - Classifies peripherals into `Likely Watch S4` (name match), `Xiaomi Device` (Xiaomi SIG Manufacturer ID `0x038F`, Huami `0x0157`, or advertised Mi Service `0xFE95` / `0xFDAB`), or `BLE Peripheral`.
- **Strict Read-Only GATT Inspector (`GattUuidLookup`):**
  - Zero GATT characteristic writes, zero descriptor writes, zero unknown commands.
  - Sequential read-only reads are whitelisted **exclusively** to standard Bluetooth SIG Battery Level (`0x2A19`) and Device Information strings (`0x2A24`, `0x2A26`, `0x2A27`, `0x2A28`, `0x2A29`) when exposed with `PROPERTY_READ`.
  - Always cleanly disconnects & closes `BluetoothGatt` to preserve official Mi Fitness pairing.
- **Redacted Logging & Exportable Diagnostic Report (`LogRedactor`):**
  - Masks all MAC addresses (`AA:BB:CC:DD:EE:FF` → `AA:BB:**:**:**:FF`) and embedded email strings in logs and clipboard-exported diagnostic reports.

## M3 Scope (Comparative Research in `protocol-lab/` — Pending Approval)
- Reference A: Xiaomi 14T (Mi Fitness Events menu available).
- Target B: Infinix XOS 15 (same Mi Fitness version, Events menu unavailable).
- Keep watch firmware and Mi Fitness versions constant and record exact build/version numbers in every experiment.
- Never fabricate undocumented GATT UUIDs, framing headers, checksums, or encryption details.
