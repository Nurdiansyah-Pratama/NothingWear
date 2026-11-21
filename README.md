# NothingWear

Independent, offline-first Android companion application and research workspace for **Xiaomi Watch S4**, focused on personal calendar management, BLE device diagnostics, Watch Face Studio, and ethical protocol research for enabling native Watch S4 Events synchronization on non-Xiaomi devices (primary target: **Infinix XOS 15**).

> **Current Status:** `M6 — Health & Device Dashboard` complete (verified wearable telemetry & GATT capability matrix, live standard Bluetooth SIG Heart Rate `0x180D / 0x2A37` monitor, read-only Android Health Connect `1.1.0` integration for Steps/HR/Sleep with source attribution, separated host phone telemetry, and honest `UNAVAILABLE` states). Final milestone `M7 — Integration & Release` awaits approval.

## Tech Stack (Verified Baseline)

- **Package:** `com.nothingwear.app`
- **Language:** Kotlin (`2.2.10`) with AGP `9.4.1` built-in Kotlin & Gradle `9.6.0`
- **UI:** Jetpack Compose + Material 3 (Compose BOM `2026.02.01`, dark-mode default, light mode, 6 custom accent colors)
- **Architecture:** MVVM + Clean Architecture (`:app` + isolated `:protocol-adapter:xiaomi-watch-s4`)
- **Database:** Room (`2.8.5`) via KSP with exported schemas (`app/schemas/`)
- **Background Scheduling:** WorkManager (`2.12.0`) for Watch Reminder Bridge alerts
- **Health Integration:** Android Health Connect (`1.1.0`, strictly read-only) + standard Bluetooth SIG BLE Heart Rate (`0x180D / 0x2A37`)
- **DI:** Hilt (`2.60.1`) via KSP (`2.3.14`)
- **Navigation:** Navigation Compose (`2.10.2`) + Hilt Navigation Compose (`1.4.0`)
- **Preferences:** Jetpack DataStore Preferences (`1.3.0`)
- **Async:** Kotlin Coroutines & Flow (`1.11.0`)
- **SDK:** `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`
- **Testing:** JUnit (`4.13.2`), MockK (`1.14.12`), Coroutines Test (`1.11.0`)

## Repository Structure

```text
NothingWear/
├── app/                         # Main Android application (com.nothingwear.app)
├── protocol-lab/                # Isolated reverse engineering lab (non-Gradle)
│   ├── apk-analysis/
│   ├── ble-capture/
│   ├── packet-parser/
│   ├── protocol-docs/
│   └── test-vectors/
├── protocol-adapter/
│   └── xiaomi-watch-s4/         # Isolated Watch S4 protocol adapter workspace (M4)
├── watchface-workspace/         # Watch Face Studio themes, assets, exports (M5)
│   ├── themes/
│   ├── assets/
│   └── exports/
├── docs/                        # Architecture, roadmap, ADRs, testing & protocol docs
├── gradle/                      # Gradle Wrapper & libs.versions.toml
├── AGENTS.md                    # Permanent instructions for AI coding agents
└── README.md
```

## Running in Android Studio

1. Open `D:\Project Android Studio\NothingWear` directly via **File → Open** in Android Studio.
2. Wait for **Gradle Sync** to finish (uses Gradle `9.6.0` and JDK toolchain configured in `gradle/gradle-daemon-jvm.properties`).
3. Connect your physical **Infinix XOS 15** phone via USB with **USB Debugging** enabled (recommended over heavy emulators for RAM efficiency and realistic BLE testing in later milestones).
4. Select the connected device in the toolbar and click **Run (`app`)**.

### Command-Line Verification

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

## Documentation Index

- [AGENTS.md](AGENTS.md) — Master agent rules & constraints
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — App architecture & isolation boundaries
- [docs/ROADMAP.md](docs/ROADMAP.md) — Milestone plan (M0–M7)
- [docs/DECISIONS.md](docs/DECISIONS.md) — Architecture Decision Records (ADRs)
- [docs/CALENDAR.md](docs/CALENDAR.md) — Calendar engine specification (M1)
- [docs/BLE-PROTOCOL.md](docs/BLE-PROTOCOL.md) — BLE discovery & protocol research notes (M2/M3)
- [docs/EVENT-SYNC.md](docs/EVENT-SYNC.md) — Native Events sync adapter spec (M4)
- [docs/WATCHFACE-SPEC.md](docs/WATCHFACE-SPEC.md) — Watch Face Studio & 466×466 target spec (M5)
- [docs/HEALTH-DASHBOARD.md](docs/HEALTH-DASHBOARD.md) — Verified Health & Device Dashboard spec (M6)
- [docs/TESTING.md](docs/TESTING.md) — Unit, instrumented & physical device test plan
- [docs/CHANGELOG.md](docs/CHANGELOG.md) — Milestone changelog
