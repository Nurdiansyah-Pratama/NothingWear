# Changelog

## [0.3.0-m2] — M2 Device & BLE Discovery (2026-11-20)

### Added
- Declared Android-version-aware BLE permissions in `AndroidManifest.xml` (`BLUETOOTH_SCAN` with `neverForLocation` & `BLUETOOTH_CONNECT` on Android 12+, legacy `BLUETOOTH`/`BLUETOOTH_ADMIN`/`ACCESS_FINE_LOCATION` capped at `maxSdkVersion="30"`, optional BLE feature flag; no `BLUETOOTH_ADVERTISE` or `POST_NOTIFICATIONS`).
- Implemented `BluetoothPermissionChecker`, `LogRedactor`, `WatchS4Identifier`, and `GattUuidLookup` (with strict read-only standard Bluetooth SIG whitelist).
- Implemented `AndroidBleDiagnosticsRepository` supporting system-connected GATT & bonded device listing alongside 15-second auto-timeout BLE scanning, strict read-only GATT service/characteristic discovery, sequential standard Battery/DIS reads when exposed, clean GATT disconnect/close, redacted diagnostic logs, and copyable redacted diagnostic reports.
- Built full `DevicesScreen` with permission rationale, Bluetooth enable prompt, Xiaomi/Paired filter, expandable GATT service/characteristic tree, live redacted diagnostic console, and Mi Fitness coexistence troubleshooting notes.
- Wired live read-only BLE device status and verified standard Battery % (only when exposed) into Home Dashboard while preserving honest `PROTOCOL_UNKNOWN` Watch S4 Native Events status.
- Added unit tests for `LogRedactor`, `WatchS4Identifier`, `GattUuidLookup`, `DevicesViewModel`, and updated `DashboardViewModelTest`.

### Fixed (M2 Re-Inspect & Already-Connected Watch S4)
- Added GATT session ID guards so stale disconnect callbacks from a closed GATT handle cannot overwrite a newly started Re-Inspect session.
- Added a 450ms Bluetooth stack cooldown between closing an active GATT handle and re-opening a new connection (prevents status `133` on MediaTek/Infinix XOS stacks).
- Added device transport detection (`LE` / `Dual` / `Classic BR/EDR`), `GATT_SERVER` active link inspection, read-only cached Classic SDP UUID inspection (`device.uuids`), multi-step GATT transport probing (`Direct LE` → `TRANSPORT_AUTO` → `Passive Auto-Attach`), and decoded GATT status diagnostics (`GattStatusDecoder`).

## [0.2.0-m1] — M1 Calendar Engine (2026-11-20)

### Added
- Added Room (`2.8.5`) with KSP compiler and official `androidx.room` Gradle plugin exporting schemas to `app/schemas/`.
- Declared `android.permission.READ_CALENDAR` (strictly read-only; no `WRITE_CALENDAR`) with runtime permission request, rationale, denial handling, and App Settings fallback.
- Implemented `AndroidCalendarDataSource` reading `CalendarContract.Calendars` (including synced Google Calendar accounts) and `CalendarContract.Instances` across a 30-day window.
- Implemented `NothingWearDatabase`, `CalendarDao`, and `OfflineFirstCalendarRepository` preserving per-calendar user selection toggles across refreshes and supporting offline access.
- Implemented `AgendaTimeWindows` with accurate UTC all-day event normalization for non-UTC timezones.
- Built full Calendar screen with Today / 7 Days / 30 Days views, live search, per-calendar selection bottom sheet, manual Sync, and `EventDetailScreen` (`calendar/event/{instanceKey}`).
- Wired live cached Today's Agenda, Upcoming Events, and honest `LOCAL_ONLY` / `PERMISSION_REQUIRED` status onto the Home Dashboard while keeping Watch S4 Native Events honestly `PROTOCOL_UNKNOWN`.
- Added unit tests for `AgendaTimeWindows`, `OfflineFirstCalendarRepository`, `CalendarViewModel`, and updated `DashboardViewModelTest`.

## [0.1.0-m0] — M0 Project Foundation (2026-11-20)

### Added
- Migrated Android Studio baseline project cleanly to package `com.nothingwear.app` (`minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`).
- Centralized verified stable dependencies in `gradle/libs.versions.toml` (AGP `9.4.1`, Kotlin `2.2.10`, KSP `2.3.14`, Hilt `2.60.1`, Compose BOM `2026.02.01`, Navigation Compose `2.10.2`, Lifecycle `2.11.1`, DataStore Preferences `1.3.0`, Coroutines `1.11.0`, MockK `1.14.12`).
- Implemented MVVM + Clean Architecture layers (`core`, `domain`, `data`, `features`, `navigation`) with Hilt DI.
- Built Nothing-inspired Material 3 design system with Dark mode default, Light mode, System mode, and 6 customizable accent colors persisted via DataStore Preferences.
- Implemented 5-tab top-level navigation: Home, Calendar, Watch Face Studio, Devices, Settings.
- Implemented M0 Dashboard with Device Status, Calendar & Watch Sync status, Today's Agenda empty state, Upcoming Events empty state, Watch Face Studio shortcut, and Device Diagnostics shortcut (zero fabricated data).
- Added explicit milestone placeholders for Calendar (M1), Devices/BLE/Health (M2/M6), and Watch Face Studio (M5).
- Created isolated non-Android directories: `protocol-lab/`, `protocol-adapter/xiaomi-watch-s4/`, and `watchface-workspace/`, with `.gitignore` protections against APKs, BLE captures, and secrets.
- Added complete M0 documentation (`AGENTS.md`, `README.md`, and `docs/*`) and unit tests.
