# BLE Discovery & Protocol Research (`M2` / `M3`)

**Status:** Not Started (Intentionally inactive in M0).

## M2 Scope (Read-Only BLE Discovery)
- Request Android-version-aware `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` only when entering device diagnostics.
- Scan nearby BLE peripherals and identify Xiaomi Watch S4 where possible.
- Inspect GATT services and characteristics in **strict read-only mode** (no writes to unknown characteristics).
- Preserve official Mi Fitness pairing.
- Store redacted diagnostic logs only (no raw MAC addresses or personal data committed to Git).

## M3 Scope (Comparative Research in `protocol-lab/`)
- Reference A: Xiaomi 14T (Mi Fitness Events menu available).
- Target B: Infinix XOS 15 (same Mi Fitness version, Events menu unavailable).
- Keep watch firmware and Mi Fitness versions constant and record exact build/version numbers in every experiment.
- Never fabricate undocumented GATT UUIDs, framing headers, checksums, or encryption details.
