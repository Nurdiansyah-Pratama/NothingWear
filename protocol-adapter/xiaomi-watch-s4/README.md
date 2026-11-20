# Xiaomi Watch S4 Protocol Adapter (`:protocol-adapter:xiaomi-watch-s4`)

Pure-Kotlin JVM Gradle module isolating Xiaomi Watch S4 protocol contracts, sanitized event payload models, and honest capability reporting from the main Android `:app` module.

## M4 Status
- `WatchS4AdapterState`: Lifecycle state enum (`UNSUPPORTED`, `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `PROTOCOL_UNKNOWN`, `READY`, `SYNCING`, `SUCCESS`, `FAILED`).
- `WatchS4EventPayload`: Evidence-backed sanitized event model aligned with M3 `BlueToothSender.handleCalendar` structural findings (zero fabricated wire command IDs or keys).
- `XiaomiWatchS4ProtocolAdapter`: Reports honest `UNSUPPORTED` state documenting the M3 `WearAuthV1`/`WearAuthV2` authenticated encryption constraint and recommends the NothingWear Watch Reminder Bridge fallback.

Run module unit tests:
```powershell
.\gradlew.bat :protocol-adapter:xiaomi-watch-s4:test
```
