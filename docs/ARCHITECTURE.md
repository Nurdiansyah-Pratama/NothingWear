# NothingWear Architecture

## 1. Guiding Principles

- **Offline-First:** Core user preferences, cached calendars (M1), and watch-face projects (M5) work without a backend server.
- **Strict Research Isolation:** Reverse engineering scripts, APK analysis notes, BLE capture parsers, and protocol experiments live exclusively in `protocol-lab/` and `protocol-adapter/` outside Android's `:app` build graph.
- **No Fabricated Data:** Unimplemented capabilities (calendar, BLE, battery, health, native watch sync) must surface explicit honest states (`DISCONNECTED`, `NOT_CONFIGURED`, `PROTOCOL_UNKNOWN`, `UNAVAILABLE`) rather than fake numbers.
- **Single-Responsibility Layers:** UI screens depend only on ViewModels/UI state and `core/designsystem`; `domain` is pure Kotlin with zero Android or Compose imports.

## 2. Package Architecture (`com.nothingwear.app`)

In M0, Android layers are structured as clean package boundaries inside `:app` to keep Android Studio memory consumption low and Gradle sync fast on constrained laptops, while preserving 1-to-1 extraction boundaries for multi-module growth in later milestones (see ADR-002 in `docs/DECISIONS.md`).

```text
app/src/main/java/com/nothingwear/app/
├── NothingWearApplication.kt        # @HiltAndroidApp entry point
├── MainActivity.kt                  # Edge-to-edge host observing theme preferences
├── MainViewModel.kt                 # Exposes reactive AppThemePreferences
├── core/
│   ├── common/                      # Coroutine dispatcher qualifiers, TimeProvider & Hilt modules
│   ├── database/                    # Room NothingWearDatabase (calendar_accounts, calendar_events)
│   ├── permissions/                 # CalendarPermissionChecker (READ_CALENDAR)
│   └── designsystem/
│       ├── theme/                   # Color, AccentSpec, Spacing, Radius, Type, Theme
│       └── component/               # NothingSurfaceCard, NothingStatusBadge, Headers, Placeholders
├── domain/                          # Pure Kotlin — zero Android/Compose dependencies
│   ├── model/                       # ThemePreferences, CalendarModels, DeviceStatus, DashboardOverview
│   ├── repository/                  # ThemePreferencesRepository, CalendarRepository, DashboardRepository
│   ├── usecase/                     # Theme preferences, Calendar sync/observe/filter, Dashboard overview
│   └── util/                        # AgendaTimeWindows (window math & UTC all-day normalization)
├── data/
│   ├── calendar/                    # AndroidCalendarDataSource (CalendarContract.Calendars & Instances)
│   ├── preferences/                 # DataStoreThemePreferencesRepository
│   ├── repository/                  # OfflineFirstCalendarRepository, FoundationDashboardRepository
│   └── di/                          # Hilt bindings & DataStore provider
├── features/
│   ├── dashboard/                   # Home screen (live Today/Upcoming agenda + honest M2/M4 status)
│   ├── calendar/                    # M1 Calendar Engine (agenda, search, calendar filter, EventDetailScreen)
│   ├── watchface/                   # M5 Watch Face Studio placeholder
│   ├── device/                      # M2/M4/M6 Device, BLE Diagnostics & Health placeholder
│   └── settings/                    # Theme Mode & Accent customization + App metadata
└── navigation/
    ├── NothingWearDestination.kt    # 5 top-level tabs: Home, Calendar, Studio, Devices, Settings
    └── NothingWearApp.kt            # Scaffold + Bottom NavigationBar + NavHost (+ calendar/event/{instanceKey})
```

## 3. Data Flow (MVVM + Unidirectional Data Flow)

```text
Compose UI (Screen)
   │  collectAsStateWithLifecycle()
   ▼
ViewModel (@HiltViewModel, exposes StateFlow<UiState>)
   │  invokes UseCase
   ▼
Domain UseCase (pure Kotlin)
   │  depends on interface
   ▼
Domain Repository Interface
   ▲  implemented by
   │
Data Repository (DataStore / Room [M1] / BLE [M2] / Protocol Adapter [M4])
```

## 4. Design System Summary

- **Default Theme:** Dark mode (`#000000` OLED canvas, `#0D0D10` / `#141418` cards, 1dp hairline borders).
- **Light Theme:** Warm editorial light surface (`#F4F4F0` background, `#FFFFFF` cards).
- **Accent Customization:** 6 accessible accents (`Signal Red`, `Monochrome`, `Amber Pulse`, `Cyber Cyan`, `Emerald`, `Cobalt`) persisted offline via Jetpack DataStore Preferences.
- **Typography:** Geometric sans-serif hierarchy paired with uppercase monospace technical labels.
