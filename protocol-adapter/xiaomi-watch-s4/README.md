# Xiaomi Watch S4 Protocol Adapter (`protocol-adapter/xiaomi-watch-s4`)

Isolated home for the Xiaomi Watch S4 protocol adapter (scheduled for M4).
Per the Master Project Plan, no event transmission or protocol writes may be implemented here until M3 reverse engineering provides sufficient verified evidence.

Planned adapter lifecycle states (already modeled in `com.nothingwear.app.domain.model.WatchConnectionState`):
- `UNSUPPORTED`
- `DISCONNECTED`
- `CONNECTING`
- `CONNECTED`
- `PROTOCOL_UNKNOWN`
- `READY`
- `SYNCING`
- `SUCCESS`
- `FAILED`
