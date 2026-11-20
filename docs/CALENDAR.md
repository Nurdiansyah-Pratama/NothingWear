# Calendar Engine Specification (`M1`)

**Status:** Implemented in `0.2.0-m1`.

## Permissions
- Requests **`android.permission.READ_CALENDAR`** only at runtime with explicit user-facing rationale.
- Never requests `WRITE_CALENDAR`.
- Gracefully handles permission denial (offers retry + direct link to Android App Settings if previously denied) while continuing to display previously cached offline Room events.

## Data Flow & Offline-First Storage
1. **Source (`AndroidCalendarDataSource`):**
   - Queries `CalendarContract.Calendars` for all calendars synced to Android (including Google Calendar `com.google` accounts).
   - Queries `CalendarContract.Instances` across `[today - 1 day .. today + 31 days]` so recurring events are properly expanded by Android's Calendar Provider without missing UTC all-day edge cases.
2. **Local Database (`NothingWearDatabase` — Room `2.8.5`):**
   - `calendar_accounts`: stores calendar metadata (`calendar_id`, `display_name`, `account_name`, `account_type`, `color`, `is_selected`, `is_primary`). Re-sync preserves the user's `is_selected` toggles.
   - `calendar_events`: stores expanded event instances keyed by `instance_key = "${eventId}_${startTimeMillis}"` with title, description, location, timestamps, all-day flag, timezone, and `last_synced_at_millis`.
   - Room schemas are exported under `app/schemas/` for migration verification in M7.
3. **All-Day Timezone Normalization (`AgendaTimeWindows`):**
   - Android Calendar Provider stores all-day event timestamps at UTC midnight. `AgendaTimeWindows` resolves all-day start/end dates in `ZoneOffset.UTC` before projecting to the device's local `ZoneId`, preventing multi-day bleed in positive/negative UTC offsets (e.g., WIB `UTC+7`).

## User-Facing Features
- **Calendar Tab:** Live search across title/location/description/calendar name, 3 agenda windows (`Today`, `7 Days`, `30 Days`), per-calendar selection bottom sheet, manual Sync button, and honest local cache status banner.
- **Event Detail Screen:** Full date, time range or `ALL DAY`, timezone, location, description, last cached timestamp, and explicit notice that Watch S4 native sync is not active (`PROTOCOL_UNKNOWN`).
- **Home Dashboard Integration:** Surfaces live cached Today's Agenda and Upcoming Events with tap-through to Event Detail while keeping BLE (`DISCONNECTED`) and Watch S4 Native Events (`PROTOCOL_UNKNOWN`) strictly honest.

## Privacy & Non-Goals
- Personal calendar titles/descriptions/locations are never written to logcat.
- Local calendar reading does **not** synchronize events to Xiaomi Watch S4 (requires M3 research and M4 physical verification).
