# NothingWear Roadmap

NothingWear is built strictly one milestone at a time. Each milestone requires a passing Gradle build, relevant tests, updated documentation, and explicit user approval before advancing.

| Milestone | Name | Status | Scope Summary |
| :--- | :--- | :--- | :--- |
| **M0** | Project Foundation | **Completed** | Project setup (`com.nothingwear.app`), Gradle Version Catalog, Hilt + KSP, Compose Material 3 Nothing-inspired design system (Dark/Light/Custom Accents), 5-tab navigation, honest Dashboard & feature placeholders, isolated lab folders, documentation, unit tests, passing debug build. |
| **M1** | Calendar Engine | **Completed** | `READ_CALENDAR` permission flow, Android Calendar Provider + Google Calendar sync reading, per-calendar selection sheet, daily/weekly/30-day agendas, live search, event detail screen, Room offline cache (`2.8.5`), Dashboard integration, unit tests. |
| **M2** | Device & BLE Discovery | **Completed** | Android-version-aware `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, bonded/system-connected & nearby BLE scan (15s timeout), Xiaomi Watch S4 heuristic identification, strict read-only GATT inspector, redacted logs & clipboard report, Mi Fitness pairing preservation, unit tests. |
| **M3** | Mi Fitness Reverse Engineering | Pending Approval | Legitimate Mi Fitness APK metadata & JADX analysis, Xiaomi 14T vs Infinix XOS 15 comparative study, controlled BLE HCI log captures with dummy events, packet structure & feasibility report inside `protocol-lab/`. |
| **M4** | Native Events Synchronization | Not Started | Evidence-backed payload models, desktop + Kotlin parser, isolated `protocol-adapter/xiaomi-watch-s4`, controlled testing only after protocol understanding, physical Watch S4 verification (or documented technical limitation + fallback). |
| **M5** | Watch Face Studio | Not Started | 6 theme categories (`Nothing`, `Minimal`, `Analog`, `Cyberpunk`, `Tactical`, `Custom`), interactive 466×466 visual editor, color/typography/layout customization, project save/load, and verified export pipeline research. |
| **M6** | Health & Device Dashboard | Not Started | Verified device info, connection state, battery and health metrics strictly from confirmed data sources; explicit unavailable states for unsupported metrics. |
| **M7** | Integration & Release | Not Started | End-to-end permission/lifecycle/background testing on Infinix XOS 15 & Xiaomi 14T, Room migration checks, XOS battery optimization review, release build & limitation docs. |
