# Native Watch S4 Event Synchronization & Fallback (`M4`)

**Status:** Implemented in `0.4.0-m4` (Option B — Isolated Protocol Adapter + Verified Watch Reminder Bridge Fallback).

## 1. Isolated Protocol Adapter (`:protocol-adapter:xiaomi-watch-s4`)
- Implemented as a pure-Kotlin JVM Gradle module completely decoupled from Android UI/framework classes.
- Models all Master Project Plan Section 6 states (`WatchS4AdapterState`) and evidence-backed sanitized event payloads (`WatchS4EventPayload`).
- `XiaomiWatchS4ProtocolAdapter` honestly reports **`UNSUPPORTED`** based on verified M2/M3 findings:
  - Verified Watch S4 surfaces: GATT `0xFDAB` and Classic SPP `0x1101`.
  - Blocking boundary: `Xiaomi WearAuthV1 / WearAuthV2` authenticated encryption session owned by Mi Fitness.
  - Zero unauthorized BLE writes, zero token extraction, zero APK patching, and zero fabricated wire frames.

## 2. Working Fallback — Watch Reminder Bridge (`WorkManager` + `NotifySyncService`)
- Uses `androidx.work:work-runtime-ktx:2.12.0` (`CalendarReminderWorker`) and `CalendarNotificationPoster` (`nothingwear_calendar_reminders` high-importance channel) to schedule local reminders for upcoming Room-cached calendar events (48-hour lookahead, configurable lead times: `At start`, `5 min`, `15 min`, `30 min`, all-day events anchored at `08:00` local time).
- Requests `android.permission.POST_NOTIFICATIONS` **on-demand only** when the user enables or tests the Reminder Bridge.
- Automatically reschedules reminders whenever local calendars are refreshed or calendar filters change.
- Includes a 1-tap **Test Watch Alert** button and Infinix XOS 15 + Mi Fitness notification forwarding checklist.

## Physical Verification Checklist (Infinix X6880 + Xiaomi Watch S4)
1. Enable **NothingWear** inside **Mi Fitness → Device → Notifications → App notifications**.
2. Set NothingWear battery mode to **Unrestricted** in Infinix XOS 15 App Management.
3. Open **Calendar → Watch Bridge** (or tap the **Synchronization Status** card on Home), grant `POST_NOTIFICATIONS`, enable **Calendar Watch Alerts**, and tap **Test Watch Alert**.
4. Confirm the alert vibrates and displays cleanly on Xiaomi Watch S4 while Mi Fitness pairing remains stable.
