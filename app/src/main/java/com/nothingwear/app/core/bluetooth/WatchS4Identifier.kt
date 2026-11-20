package com.nothingwear.app.core.bluetooth

enum class WatchMatchConfidence(val label: String) {
    LIKELY_WATCH_S4("Likely Watch S4"),
    XIAOMI_ECOSYSTEM("Xiaomi Device"),
    OTHER_PERIPHERAL("BLE Peripheral"),
}

data class WatchMatchResult(
    val confidence: WatchMatchConfidence,
    val reason: String,
)

/**
 * Evidence-based heuristic classifier for nearby or bonded BLE devices.
 * Never claims protocol compatibility without verified GATT/protocol inspection.
 */
object WatchS4Identifier {

    const val XIAOMI_MANUFACTURER_ID: Int = 0x038F // 911 — Xiaomi Inc.
    const val HUAMI_MANUFACTURER_ID: Int = 0x0157  // 343 — Anhui Huami Information Technology

    private val xiaomiServicePrefixes = setOf(
        "0000fe95-", // Xiaomi Inc. Mi Service
        "0000fdab-", // Xiaomi Wearable Service
    )

    fun classify(
        name: String?,
        manufacturerIds: Set<Int> = emptySet(),
        advertisedServiceUuids: List<String> = emptyList(),
    ): WatchMatchResult {
        val normalizedName = name?.trim().orEmpty()
        val lowerName = normalizedName.lowercase()

        if (lowerName.contains("watch s4")) {
            return WatchMatchResult(
                confidence = WatchMatchConfidence.LIKELY_WATCH_S4,
                reason = "Device name matches Xiaomi Watch S4",
            )
        }

        val hasXiaomiName = lowerName.contains("xiaomi watch") ||
            lowerName.contains("mi watch") ||
            lowerName.contains("redmi watch") ||
            lowerName.contains("xiaomi smart band")
        val hasXiaomiMfg = XIAOMI_MANUFACTURER_ID in manufacturerIds
        val hasHuamiMfg = HUAMI_MANUFACTURER_ID in manufacturerIds
        val hasXiaomiService = advertisedServiceUuids.any { uuid ->
            val lower = uuid.lowercase()
            xiaomiServicePrefixes.any { prefix -> lower.startsWith(prefix) }
        }

        return when {
            hasXiaomiName -> WatchMatchResult(
                confidence = WatchMatchConfidence.XIAOMI_ECOSYSTEM,
                reason = "Xiaomi wearable naming pattern",
            )

            hasXiaomiMfg -> WatchMatchResult(
                confidence = WatchMatchConfidence.XIAOMI_ECOSYSTEM,
                reason = "Bluetooth SIG Manufacturer ID 0x038F (Xiaomi Inc.)",
            )

            hasXiaomiService -> WatchMatchResult(
                confidence = WatchMatchConfidence.XIAOMI_ECOSYSTEM,
                reason = "Advertises Xiaomi service UUID (0xFE95/0xFDAB)",
            )

            hasHuamiMfg -> WatchMatchResult(
                confidence = WatchMatchConfidence.XIAOMI_ECOSYSTEM,
                reason = "Bluetooth SIG Manufacturer ID 0x0157 (Huami/Xiaomi partner)",
            )

            else -> WatchMatchResult(
                confidence = WatchMatchConfidence.OTHER_PERIPHERAL,
                reason = "Standard BLE advertisement",
            )
        }
    }
}
