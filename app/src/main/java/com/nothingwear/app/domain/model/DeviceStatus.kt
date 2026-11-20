package com.nothingwear.app.domain.model

/**
 * Protocol adapter and connection lifecycle states defined in Master Project Plan Section 6.
 */
enum class WatchConnectionState {
    UNSUPPORTED,
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PROTOCOL_UNKNOWN,
    READY,
    SYNCING,
    SUCCESS,
    FAILED,
}

/**
 * Honest summary of wearable connection state.
 * In M0, no BLE scanning or connection is performed and battery/health values remain null.
 */
data class DeviceStatusSummary(
    val targetDeviceName: String,
    val connectionState: WatchConnectionState,
    val statusDetail: String,
    val batteryPercent: Int? = null,
    val bleDiscoveryAvailable: Boolean = false,
)
