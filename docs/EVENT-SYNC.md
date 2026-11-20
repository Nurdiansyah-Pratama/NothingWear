# Native Watch S4 Event Synchronization (`M4`)

**Status:** Pending Approval after M3 Research (`protocol-lab/protocol-docs/M3-RESEARCH-REPORT.md`).

## Key M3 Findings Governing M4
- **Root Cause on Infinix XOS 15:** Confirmed OEM whitelist in `com.xiaomi.wearable 3.60.0i` (`FeatureItemModule` + `DeviceSettingsSyncHelperImpl.isCalendarSyncAndroidVersion` allowing only `Xiaomi`, `Vivo`, `Oppo`, `Honor`), despite standard Android Calendar Provider (`content://com.android.calendar/*`) and `READ_CALENDAR` already working.
- **Protocol & Auth Boundary:** `BlueToothSender.handleCalendar()` serializes events to Protobuf (`Lt3u` → `Lgq3` → `Lhq3`) over `DeviceContact`, guarded by `WearAuthV1` / `WearAuthV2` authenticated encryption (`AESEncryption`, `TokenEncryption`, `AuthenticatedCipherText`).
- **M4 Implementation Rule:** Never bypass authentication, extract private Mi Fitness tokens, or patch Mi Fitness. If optional Reference A (Xiaomi 14T) HCI capture confirms encrypted wire payloads, M4 delivers:
  1. Isolated `protocol-adapter/xiaomi-watch-s4` reporting honest `UNSUPPORTED` / `AUTH_REQUIRED` state with zero speculative BLE writes.
  2. Verified **Calendar Reminder Notification Bridge** fallback delivering upcoming Room-cached calendar reminders cleanly through Android notifications → official Mi Fitness `NotifySyncService` → Xiaomi Watch S4.

## Adapter Lifecycle States
Modeled in `com.nothingwear.app.domain.model.WatchConnectionState`:
- `UNSUPPORTED`
- `DISCONNECTED`
- `CONNECTING`
- `CONNECTED`
- `PROTOCOL_UNKNOWN`
- `READY`
- `SYNCING`
- `SUCCESS`
- `FAILED`

## Physical Verification Acceptance Criteria (M4)
Event sync may only be marked successful when physically verified on a real Xiaomi Watch S4:
1. Watch receives the event payload without breaking Mi Fitness pairing.
2. Event appears in the native Watch S4 **Events** application.
3. Title, date, and start/end time match accurately.
4. Multiple events synchronize reliably.
5. Event updates and deletions behave predictably.

If protocol encryption/authentication prevents third-party transmission, document the exact limitation and provide a clean local/notification fallback rather than fabricating sync status.
