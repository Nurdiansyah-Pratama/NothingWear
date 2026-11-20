package com.nothingwear.protocol.watchs4

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Isolated protocol adapter contract for Xiaomi Watch S4 native Events synchronization.
 *
 * Strictly forbids speculative BLE writes, authentication bypasses, or fabricated
 * success states.
 */
interface WatchS4ProtocolAdapter {
    val state: Flow<WatchS4AdapterState>

    fun getCapabilityReport(): NativeSyncCapabilityReport

    fun prepareValidatedPayloads(rawEvents: List<WatchS4EventPayload>): List<WatchS4EventPayload>

    suspend fun attemptNativeSync(events: List<WatchS4EventPayload>): WatchS4SyncOutcome
}

/**
 * Default M4 implementation reflecting verified M2 physical GATT/SDP observations and
 * M3 static research on `com.xiaomi.wearable 3.60.0i`.
 */
class XiaomiWatchS4ProtocolAdapter : WatchS4ProtocolAdapter {

    private val _state = MutableStateFlow(WatchS4AdapterState.UNSUPPORTED)
    override val state: Flow<WatchS4AdapterState> = _state.asStateFlow()

    override fun getCapabilityReport(): NativeSyncCapabilityReport = NativeSyncCapabilityReport(
        state = WatchS4AdapterState.UNSUPPORTED,
        verifiedGattServiceUuid = VERIFIED_GATT_SERVICE_FDAB,
        verifiedSppUuid = VERIFIED_CLASSIC_SPP_1101,
        authenticationMechanism = "Xiaomi WearAuthV1 / WearAuthV2 (authenticated & encrypted session owned by Mi Fitness)",
        summary = "Direct third-party writes into the native Xiaomi Watch S4 Events app are blocked by WearAuthV2 session authentication. NothingWear strictly refuses to bypass authentication, extract private Mi Fitness tokens, or send unverified BLE commands.",
        fallbackRecommendation = "Use the NothingWear Watch Reminder Bridge to schedule local calendar alerts that Mi Fitness (NotifySyncService) forwards natively to Xiaomi Watch S4.",
    )

    override fun prepareValidatedPayloads(rawEvents: List<WatchS4EventPayload>): List<WatchS4EventPayload> =
        rawEvents
            .map { event ->
                WatchS4EventPayload.sanitized(
                    title = event.title,
                    location = event.location,
                    description = event.description,
                    startEpochSeconds = event.startEpochSeconds,
                    endEpochSeconds = event.endEpochSeconds,
                    isAllDay = event.isAllDay,
                    reminderMinutes = event.reminderMinutes,
                    calendarName = event.calendarName,
                )
            }
            .sortedBy { it.startEpochSeconds }

    override suspend fun attemptNativeSync(events: List<WatchS4EventPayload>): WatchS4SyncOutcome {
        val validated = prepareValidatedPayloads(events)
        val report = getCapabilityReport()
        _state.value = WatchS4AdapterState.UNSUPPORTED
        return WatchS4SyncOutcome.BlockedUnsupported(
            state = report.state,
            reason = report.summary,
            validatedPayloadCount = validated.size,
        )
    }

    companion object {
        const val VERIFIED_GATT_SERVICE_FDAB = "0000fdab-0000-1000-8000-00805f9b34fb"
        const val VERIFIED_CLASSIC_SPP_1101 = "00001101-0000-1000-8000-00805f9b34fb"
    }
}
