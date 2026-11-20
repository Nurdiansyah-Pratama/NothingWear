package com.nothingwear.app.domain.model

data class DashboardOverview(
    val deviceStatus: DeviceStatusSummary,
    val calendarOverview: CalendarOverview,
    val currentMilestone: String,
)

enum class WatchFaceCategory(
    val displayName: String,
    val subtitle: String,
) {
    NOTHING("Nothing", "Dot-matrix & minimal geometry"),
    MINIMAL("Minimal", "Clean typography & negative space"),
    ANALOG("Analog", "Precision dial & subtle indices"),
    CYBERPUNK("Cyberpunk", "High-contrast telemetry accents"),
    TACTICAL("Tactical", "Utility data blocks & rugged grid"),
    CUSTOM("Custom", "User-defined layout & assets"),
}
