package com.nothingwear.app.domain.model

enum class HealthConnectAvailability(val label: String) {
    AVAILABLE("Health Connect Ready"),
    UPDATE_OR_INSTALL_REQUIRED("Install / Update Required"),
    UNSUPPORTED("Unsupported on OS"),
}

enum class MetricAvailabilityStatus(val label: String) {
    VERIFIED("VERIFIED"),
    NO_DATA("NO DATA"),
    PERMISSION_REQUIRED("PERMISSION NEEDED"),
    UNAVAILABLE("UNAVAILABLE"),
}

enum class UnifiedHeartRateSource(val badgeLabel: String) {
    LIVE_BLE("LIVE BLE 0x2A37"),
    HEALTH_CONNECT("HEALTH CONNECT AUTO"),
    LAST_VERIFIED_CACHE("LAST VERIFIED"),
    UNAVAILABLE("NO READING"),
}

data class LastVerifiedHeartRate(
    val bpm: Int,
    val timestampMillis: Long,
    val sourceLabel: String,
)

data class UnifiedHeartRateSummary(
    val primaryBpmText: String = "--",
    val source: UnifiedHeartRateSource = UnifiedHeartRateSource.UNAVAILABLE,
    val sourceCaption: String = "No verified heart rate reading yet.",
    val detailNote: String = "Enable Mi Fitness → Health Connect for automatic background BPM, or turn on Watch S4 Heart Rate Broadcast for live 0x2A37 streaming.",
    val timestampMillis: Long? = null,
)

data class HealthMetricSummary(
    val title: String,
    val primaryValue: String,
    val unit: String,
    val status: MetricAvailabilityStatus,
    val sourceLabel: String,
    val detailNote: String,
    val updatedAtMillis: Long? = null,
)

data class HostPhoneEnvironment(
    val deviceModel: String,
    val androidRelease: String,
    val sdkInt: Int,
    val batteryPercent: Int?,
    val isCharging: Boolean,
)

data class HealthDashboardSnapshot(
    val healthConnectAvailability: HealthConnectAvailability = HealthConnectAvailability.UNSUPPORTED,
    val hasHealthPermissions: Boolean = false,
    val grantedPermissions: Set<String> = emptySet(),
    val requiredPermissions: Set<String> = emptySet(),
    val stepsToday: HealthMetricSummary = HealthMetricSummary(
        title = "Steps Today",
        primaryValue = "--",
        unit = "steps",
        status = MetricAvailabilityStatus.UNAVAILABLE,
        sourceLabel = "Health Connect",
        detailNote = "Grant Health Connect read access to inspect synced steps.",
    ),
    val heartRateSummary: HealthMetricSummary = HealthMetricSummary(
        title = "Heart Rate",
        primaryValue = "--",
        unit = "BPM",
        status = MetricAvailabilityStatus.UNAVAILABLE,
        sourceLabel = "BLE 0x2A37 / Health Connect",
        detailNote = "No verified heart rate reading yet.",
    ),
    val sleepLast24h: HealthMetricSummary = HealthMetricSummary(
        title = "Sleep (24h)",
        primaryValue = "--",
        unit = "",
        status = MetricAvailabilityStatus.UNAVAILABLE,
        sourceLabel = "Health Connect",
        detailNote = "Grant Health Connect read access to inspect synced sleep sessions.",
    ),
    val dataOrigins: List<String> = emptyList(),
    val lastVerifiedHeartRate: LastVerifiedHeartRate? = null,
    val lastRefreshedMillis: Long? = null,
    val hostPhone: HostPhoneEnvironment = HostPhoneEnvironment(
        deviceModel = "Android Device",
        androidRelease = "",
        sdkInt = 0,
        batteryPercent = null,
        isCharging = false,
    ),
    val errorMessage: String? = null,
)
