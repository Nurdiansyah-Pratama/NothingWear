# Testing Strategy & Verification Log

## Testing Layers
1. **Unit Tests (`app/src/test`):** Pure Kotlin domain logic, UseCases, ViewModels (`JUnit 4`, `MockK`, `kotlinx-coroutines-test`).
2. **Instrumented Tests (`app/src/androidTest`):** Package verification, Room migrations (M1+), Compose UI flows on physical devices.
3. **Physical Device Testing:**
   - Primary target: **Infinix XOS 15** via USB Debugging.
   - Reference device (M3/M7): **Xiaomi 14T**.
   - Wearable: **Xiaomi Watch S4** (official Mi Fitness pairing preserved).

## M0 & M1 Test Coverage
- `AgendaTimeWindowsTest`: Verifies `Today`/`7 Days`/`30 Days` window filtering, WIB (`UTC+7`) all-day UTC normalization, and case-insensitive keyword search across title/location/description/calendar name.
- `OfflineFirstCalendarRepositoryTest`: Verifies `PermissionDenied` preserves existing Room cache, re-sync preserves user's calendar selection flags, and duplicate instances are deduplicated.
- `CalendarViewModelTest`: Verifies offline cache visibility when permission is denied, range switching, search filtering, and auto-sync when permission is granted.
- `DashboardViewModelTest`: Verifies live cached Today/Upcoming agendas surface on Home while BLE (`DISCONNECTED`, `batteryPercent == null`) and Native Watch S4 Sync (`PROTOCOL_UNKNOWN`) remain honest.
- `SettingsViewModelTest`: Verifies Dark mode + Signal Red defaults and reactive updates for Theme Mode and Accent Color.
- `AccentColorOptionTest`: Verifies ID resolution, fallback, and uniqueness.
- `AppPackageInstrumentedTest`: Verifies `com.nothingwear.app` application package ID.

## Commands
```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```
