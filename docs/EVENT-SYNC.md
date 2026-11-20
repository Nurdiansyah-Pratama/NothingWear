# Native Watch S4 Event Synchronization (`M4`)

**Status:** Blocked on M3 Research (`PROTOCOL_UNKNOWN`).

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
