package com.nothingwear.protocol.watchs4

/**
 * Isolated adapter lifecycle states defined in Master Project Plan Section 6.
 */
enum class WatchS4AdapterState {
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
 * Evidence-backed technical report describing why native Watch S4 Events protocol
 * injection is currently unsupported without violating NothingWear security rules.
 */
data class NativeSyncCapabilityReport(
    val state: WatchS4AdapterState,
    val verifiedGattServiceUuid: String,
    val verifiedSppUuid: String,
    val authenticationMechanism: String,
    val summary: String,
    val fallbackRecommendation: String,
)
