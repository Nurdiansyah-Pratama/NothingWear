# Testing Strategy & Verification Log

## Testing Layers
1. **Unit Tests (`app/src/test`):** Pure Kotlin domain logic, UseCases, ViewModels (`JUnit 4`, `MockK`, `kotlinx-coroutines-test`).
2. **Instrumented Tests (`app/src/androidTest`):** Package verification, Room migrations (M1+), Compose UI flows on physical devices.
3. **Physical Device Testing:**
   - Primary target: **Infinix XOS 15** via USB Debugging.
   - Reference device (M3/M7): **Xiaomi 14T**.
   - Wearable: **Xiaomi Watch S4** (official Mi Fitness pairing preserved).

## M0–M2 Test Coverage
- `LogRedactorTest`: Verifies MAC address masking (colon/dash/mixed case) and email redaction in diagnostic logs.
- `WatchS4IdentifierTest`: Verifies Xiaomi Watch S4 name matching, Xiaomi SIG Manufacturer ID (`0x038F`), Mi Service UUID (`0xFE95`), and unrelated peripheral classification.
- `GattUuidLookupTest`: Verifies standard Bluetooth SIG service names, property bitmask decoding, and strict read-only whitelist enforcement.
- `DevicesViewModelTest`: Verifies Xiaomi/bonded filter toggle, BLE scan start/stop, read-only GATT inspection state, and clean disconnect.
- `AgendaTimeWindowsTest`: Verifies `Today`/`7 Days`/`30 Days` window filtering, WIB (`UTC+7`) all-day UTC normalization, and case-insensitive keyword search across title/location/description/calendar name.
- `OfflineFirstCalendarRepositoryTest`: Verifies `PermissionDenied` preserves existing Room cache, re-sync preserves user's calendar selection flags, and duplicate instances are deduplicated.
- `CalendarViewModelTest`: Verifies offline cache visibility when permission is denied, range switching, search filtering, and auto-sync when permission is granted.
- `DashboardViewModelTest`: Verifies live cached Today/Upcoming agendas and live read-only BLE status/verified Battery % surface on Home while Native Watch S4 Sync remains honestly `PROTOCOL_UNKNOWN`.
- `SettingsViewModelTest`: Verifies Dark mode + Signal Red defaults and reactive updates for Theme Mode and Accent Color.
- `AccentColorOptionTest`: Verifies ID resolution, fallback, and uniqueness.
- `AppPackageInstrumentedTest`: Verifies `com.nothingwear.app` application package ID.

## Commands
```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```
