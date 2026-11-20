# AGENTS.md — NothingWear Permanent Agent Instructions

This repository uses the **NothingWear Master Project Plan** as its authoritative engineering specification.
All coding agents working in this workspace MUST follow these rules strictly.

## 1. Project Identity

- **Project:** NothingWear
- **Package:** `com.nothingwear.app`
- **Platform:** Android (Kotlin, Jetpack Compose, Material 3)
- **Architecture:** MVVM + Clean Architecture (offline-first, modular, milestone-based)
- **Primary Target Phone:** Infinix XOS 15
- **Target Wearable:** Xiaomi Watch S4
- **Reference Comparison Phone:** Xiaomi 14T (used in M3 comparative research)
- **IDE:** Android Studio (shared workspace with Antigravity; never run two agents editing the same files concurrently)

## 2. Two Separated Tracks

1. **NothingWear Android Application (`app/`):** Calendar management, device diagnostics, Watch Face Studio, and Nothing-inspired Material 3 UI. Must remain useful even if native Watch S4 event sync cannot be reverse-engineered.
2. **NothingWear Reverse Engineering Lab (`protocol-lab/`, `protocol-adapter/`):** Independent research into Mi Fitness compatibility checks and Xiaomi Watch S4 BLE protocol. Must never pollute or break the Android application build.

## 3. Mandatory Working Rules

Before changing code:
1. Inspect the repository and existing project structure.
2. Identify existing code, dependencies, and current build status.
3. Formulate a concise implementation plan and list files to modify.

During implementation:
- Work on **one milestone at a time** (M0 → M1 → M2 → M3 → M4 → M5 → M6 → M7).
- Never advance to the next milestone without explicit user approval.
- Do not overwrite existing user work or rewrite unrelated features.
- Keep all dependency versions centralized in `gradle/libs.versions.toml` and verify compatibility with AGP 9.x (built-in Kotlin, no `org.jetbrains.kotlin.android` plugin, pure KSP instead of KAPT), Kotlin, and `minSdk`.
- Never fabricate events, battery levels, health metrics, or protocol documentation.
- Mark unimplemented areas with explicit `TODO(Mx)` comments and honest UI states.

After implementation:
1. Run Gradle build (`.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug`).
2. Fix any compile, lint, or test errors introduced by the changes.
3. Update relevant files under `docs/` and `README.md`.
4. Commit verified changes and push to `origin main` (`https://github.com/Nurdiansyah-Pratama/NothingWear.git`), ensuring no APKs, raw BLE captures, `local.properties`, or credentials are staged.
5. Report changed files, dependency versions, build/test results, git commit/push status, and known limitations.
6. Stop and wait for explicit approval before starting the next milestone.

## 4. Permissions Policy

Request permissions only when the corresponding milestone feature is active:
- **M0:** No runtime permissions requested.
- **M1:** `READ_CALENDAR` only (no `WRITE_CALENDAR` unless explicitly approved).
- **M2:** Android-version-aware `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` (no `BLUETOOTH_ADVERTISE` unless required).
- Do not request `POST_NOTIFICATIONS` unless the app actually posts notifications.
- Never assume calendar read permission implies native Watch S4 Events synchronization.

## 5. Security & Safety Constraints

- Do NOT modify Xiaomi firmware or tamper with the watch bootloader.
- Do NOT send unknown BLE commands or write to unverified GATT characteristics.
- Do NOT bypass authentication or interfere with official Mi Fitness pairing.
- Do NOT commit APKs, raw BLE HCI captures (`.btsnoop`, `.cfa`, `.pcap`), credentials, or personal calendar dumps.
- Redact personal identifiers and MAC addresses from diagnostic logs and docs.
- Never claim native Watch S4 event synchronization succeeded without physical verification on the watch.

## 6. Current Milestone Status

- **M0 — Project Foundation:** Completed & approved.
- **M1 — Calendar Engine:** Completed & approved.
- **M2 — Device & BLE Discovery:** Completed & physically verified on Infinix X6880 + Xiaomi Watch S4.
- **M3 — Mi Fitness Reverse Engineering:** Completed & reviewed.
- **M4 — Native Events Synchronization:** Completed (isolated `:protocol-adapter:xiaomi-watch-s4` documenting `WearAuthV2` limitation + verified WorkManager Watch Reminder Bridge fallback via Mi Fitness `NotifySyncService` — pending user physical verification & approval for M5).
- **M5 — Watch Face Studio:** Not started.
- **M6 — Health & Device Dashboard:** Not started.
- **M7 — Integration & Release:** Not started.
