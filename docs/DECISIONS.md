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
