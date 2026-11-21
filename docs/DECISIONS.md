# Architecture Decision Records (ADRs)

## ADR-001: Package Migration from Wizard Template (`lat.pam.nothingwear` → `com.nothingwear.app`)
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** The existing workspace contained an untouched Android Studio New Project template generated under `lat.pam.nothingwear`. The Master Project Plan mandates `com.nothingwear.app`.
- **Decision:** Preserved the Android Studio Gradle wrapper, AGP `9.4.1`, Compose BOM, resource mipmaps, keep rules, and `local.properties`, while migrating `namespace`, `applicationId`, source packages, and test packages to `com.nothingwear.app`.
- **Consequences:** Single clean namespace matching the Master Project Plan with zero duplicate template activities.

## ADR-002: Clean Architecture Package Boundaries in `:app` for M0 + Top-Level Research Isolation
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** Primary development hardware has constrained RAM; adding 15+ empty Android library Gradle subprojects in M0 would slow Gradle sync and increase Android Studio memory overhead.
- **Decision:** Keep the Android app in `:app` with strict Clean Architecture package boundaries (`core`, `domain`, `data`, `features`, `navigation`) in M0, while isolating non-Android research and workspace directories (`protocol-lab/`, `protocol-adapter/xiaomi-watch-s4/`, `watchface-workspace/`) completely outside `settings.gradle.kts`.
- **Consequences:** Fast Android Studio sync and low RAM footprint today, with clean boundaries ready to extract `:domain`, `:core:*`, or `:protocol-adapter:xiaomi-watch-s4` into standalone Gradle modules when M1–M4 complexity warrants it.

## ADR-003: AGP 9.4.1 Built-In Kotlin + Pure KSP (`2.3.14`) + Hilt (`2.60.1`)
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** Android Studio initialized the project on AGP `9.4.1` and Gradle `9.6.0`, where Kotlin Android support is built into AGP and legacy `kapt` / `org.jetbrains.kotlin.android` are incompatible.
- **Decision:** Keep AGP `9.4.1` built-in Kotlin without applying `org.jetbrains.kotlin.android`, use `com.google.devtools.ksp:2.3.14` (unbundled 2.3.x schema), and `com.google.dagger.hilt.android:2.60.1` (full AGP 9 + KSP support).
- **Consequences:** Clean modern toolchain compatible with Android Studio and Configuration Cache.

## ADR-004: Minimum SDK Set to API 26
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** The Master Project Plan baseline specifies API 26 after verifying library compatibility (template defaulted to 27).
- **Decision:** Set `minSdk = 26` and verified all M0 dependencies pass `checkDebugAarMetadata` and compilation.
- **Consequences:** Broad Android 8.0+ compatibility while staying aligned with the project baseline.

## ADR-005: Defer Room & WorkManager Dependencies Until M1
- **Date:** 2026-11-20
- **Status:** Accepted (Room added in M1; WorkManager deferred until periodic background sync is needed)
- **Context:** Master Project Plan Section 3 advises avoiding unnecessary dependencies.
- **Decision:** Add only dependencies actively used per milestone. Room (`2.8.5`) was introduced in M1 for offline calendar storage; WorkManager remains deferred until background sync is explicitly needed and tested against XOS battery restrictions.
- **Consequences:** Leaner APK and faster incremental builds.

## ADR-006: `CalendarContract.Instances` + UTC All-Day Normalization + Room `2.8.5`
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** Recurring events in Google Calendar are stored as RRULEs in `CalendarContract.Events` and require expansion; all-day events are stored as UTC midnight timestamps, which can bleed across adjacent days in non-UTC timezones (e.g., WIB `UTC+7`).
- **Decision:** Query `CalendarContract.Instances` across a padded window (`[today - 1d .. today + 31d]`), normalize `isAllDay` events in `ZoneOffset.UTC` before projecting to the device `ZoneId`, cache selected calendars and instances in Room `2.8.5` with exported schemas (`app/schemas/`), and preserve per-calendar user toggles across re-syncs.
- **Consequences:** Accurate recurring and all-day agenda rendering offline on Infinix XOS 15 without manual RRULE parsing.

## ADR-007: Native Android BLE APIs + Strict Read-Only GATT Whitelist + MAC Redaction (M2)
- **Date:** 2026-11-20
- **Status:** Accepted
- **Context:** M2 requires BLE discovery, Xiaomi Watch S4 identification, and GATT inspection without interfering with official Mi Fitness pairing or adding unnecessary third-party BLE wrappers.
- **Decision:** Use native Android `BluetoothManager` / `BluetoothLeScanner` / `BluetoothGatt` with `neverForLocation` on Android 12+, list both bonded/system-GATT-connected devices and active 15s BLE scan results, whitelist auto-reads strictly to standard Bluetooth SIG Battery (`0x2A19`) and Device Information (`0x180A`) characteristics with zero GATT writes, and redact all MAC addresses via `LogRedactor`.
- **Consequences:** Zero extra BLE library bloat, safe coexistence with Mi Fitness, and privacy-safe diagnostic reports ready to assist M3 research.

## ADR-008: Isolated `:protocol-adapter:xiaomi-watch-s4` JVM Module + WorkManager Reminder Bridge Fallback (M4)
- **Date:** 2026-11-21
- **Status:** Accepted
- **Context:** M3 confirmed that Xiaomi Watch S4 native Events injection is gated by `WearAuthV1`/`WearAuthV2` authenticated encryption held exclusively by Mi Fitness, and NothingWear's security rules forbid bypassing authentication or tampering with Mi Fitness/firmware.
- **Decision:** Extract `:protocol-adapter:xiaomi-watch-s4` as an isolated pure-Kotlin JVM Gradle module reporting honest `UNSUPPORTED` capability with sanitized event payload validation, and deliver a reliable user-approved fallback (`WorkManager 2.12.0` + `NotificationChannel` + on-demand `POST_NOTIFICATIONS`) that forwards calendar alerts through Mi Fitness's official `NotifySyncService` to Xiaomi Watch S4.
- **Consequences:** Zero unauthorized BLE writes, Mi Fitness pairing remains 100% stable, and users on Infinix XOS 15 receive timely calendar alerts directly on Xiaomi Watch S4.

## ADR-009: Watch Face Studio Decoupled Export + Non-Destructive Room Migration v1 → v2 (M5)
- **Date:** 2026-11-21
- **Status:** Accepted
- **Context:** Static analysis of `com.xiaomi.wearable 3.60.0i` confirmed native dial installation uses `FaceInstallBleV2Impl` over `WearAuthV2` with proprietary Xiaomi Vela dial binaries, whereas custom photo dials accept standard images (`WatchFaceImage`). Existing users already have M1 calendar data in Room v1.
- **Decision:** Add non-destructive `MIGRATION_1_2` creating `watch_face_projects` (exporting Room schema `2.json`), build a pixel-exact 466×466 px Canvas/Bitmap renderer exporting PNG (`Pictures/NothingWear` via permissionless Android 10+ MediaStore) + portable JSON spec, and explicitly mark native binary `.bin`/`.face` installation as unsupported rather than misrepresenting previews as installable watch faces.
- **Consequences:** Preserves existing user calendar data cleanly, provides a real interactive Watch Face Studio with usable 466×466 artwork export, and maintains strict engineering honesty.
