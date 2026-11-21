package com.nothingwear.app.domain.model

data class DashboardHealthOverview(
    val stepsValue: String = "--",
    val heartRateValue: String = "--",
    val sleepValue: String = "--",
    val statusBadge: String = "UNAVAILABLE",
    val detailNote: String = "Open Health & Device Dashboard to inspect verified BLE HR (0x2A37) or Health Connect metrics.",
    val hasVerifiedData: Boolean = false,
)

data class DashboardOverview(
    val deviceStatus: DeviceStatusSummary,
    val healthOverview: DashboardHealthOverview = DashboardHealthOverview(),
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
