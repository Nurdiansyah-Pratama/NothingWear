package com.nothingwear.app.features.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.domain.model.BleHeartRateMonitorState
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.MetricAvailabilityStatus
import com.nothingwear.app.domain.model.UnifiedHeartRateSource
import com.nothingwear.app.domain.model.UnifiedHeartRateSummary
import com.nothingwear.app.domain.usecase.GetRequiredHealthPermissionsUseCase
import com.nothingwear.app.domain.usecase.InspectDeviceGattUseCase
import com.nothingwear.app.domain.usecase.ObserveBleDiagnosticsUseCase
import com.nothingwear.app.domain.usecase.ObserveHealthSnapshotUseCase
import com.nothingwear.app.domain.usecase.RecordVerifiedBleHeartRateUseCase
import com.nothingwear.app.domain.usecase.RefreshBleEnvironmentUseCase
import com.nothingwear.app.domain.usecase.RefreshHealthDataUseCase
import com.nothingwear.app.domain.usecase.StartStandardHeartRateMonitorUseCase
import com.nothingwear.app.domain.usecase.StopStandardHeartRateMonitorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthDashboardViewModel @Inject constructor(
    observeHealthSnapshot: ObserveHealthSnapshotUseCase,
    private val observeBleDiagnostics: ObserveBleDiagnosticsUseCase,
    private val refreshHealthData: RefreshHealthDataUseCase,
    private val refreshBleEnvironment: RefreshBleEnvironmentUseCase,
    private val getRequiredHealthPermissions: GetRequiredHealthPermissionsUseCase,
    private val recordVerifiedBleHeartRate: RecordVerifiedBleHeartRateUseCase,
    private val inspectDeviceGatt: InspectDeviceGattUseCase,
    private val startStandardHeartRateMonitor: StartStandardHeartRateMonitorUseCase,
    private val stopStandardHeartRateMonitor: StopStandardHeartRateMonitorUseCase,
) : ViewModel() {

    private val localControls = MutableStateFlow(LocalControls())

    val requiredHealthPermissions: Set<String>
        get() = getRequiredHealthPermissions()

    val uiState: StateFlow<HealthDashboardUiState> = combine(
        observeHealthSnapshot(),
        observeBleDiagnostics(),
        localControls,
    ) { health, ble, controls ->
        val detectedWatch = ble.devices.firstOrNull {
            it.matchConfidence == WatchMatchConfidence.LIKELY_WATCH_S4 ||
                it.matchConfidence == WatchMatchConfidence.XIAOMI_ECOSYSTEM
        }
        val bleHr = ble.gattSession.heartRateState
        HealthDashboardUiState(
            isRefreshing = controls.isRefreshing,
            healthSnapshot = health,
            unifiedHeartRate = buildUnifiedHeartRate(bleHr, health),
            adapterStatus = ble.adapterStatus,
            detectedWatch = detectedWatch,
            gattSession = ble.gattSession,
            bleHeartRate = bleHr,
            feedbackMessage = controls.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HealthDashboardUiState(),
    )

    init {
        onRefreshAll()
        viewModelScope.launch {
            observeBleDiagnostics()
                .mapNotNull { it.gattSession.heartRateState.latestSample }
                .distinctUntilChanged()
                .collect { sample ->
                    recordVerifiedBleHeartRate(sample.bpm, sample.timestampMillis)
                }
        }
    }

    fun onRefreshAll() {
        viewModelScope.launch {
            localControls.update { it.copy(isRefreshing = true, feedbackMessage = null) }
            refreshBleEnvironment()
            refreshHealthData()
            localControls.update { it.copy(isRefreshing = false) }
        }
    }

    fun onHealthPermissionsResult(granted: Set<String>) {
        viewModelScope.launch {
            localControls.update {
                it.copy(
                    isRefreshing = true,
                    feedbackMessage = if (granted.containsAll(requiredHealthPermissions)) {
                        "Health Connect read permissions granted. Loading verified records..."
                    } else if (granted.isNotEmpty()) {
                        "Granted ${granted.size}/${requiredHealthPermissions.size} Health Connect read permissions."
                    } else {
                        "Health Connect permission not granted. Health cards remain strictly unavailable."
                    },
                )
            }
            refreshHealthData()
            localControls.update { it.copy(isRefreshing = false) }
        }
    }

    fun onConnectWatchForHr() {
        val targetAddress = uiState.value.detectedWatch?.addressKey
        if (targetAddress == null) {
            localControls.update {
                it.copy(
                    feedbackMessage = "No paired Watch S4 found yet. Open the Devices tab to scan or check Bluetooth permissions.",
                )
            }
            return
        }
        inspectDeviceGatt(targetAddress)
        localControls.update {
            it.copy(
                feedbackMessage = "Opening read-only GATT session to ${uiState.value.detectedWatch?.displayName}...",
            )
        }
    }

    fun onToggleLiveBleHeartRate() {
        if (uiState.value.bleHeartRate.isListening) {
            stopStandardHeartRateMonitor()
        } else {
            startStandardHeartRateMonitor()
        }
    }

    private fun buildUnifiedHeartRate(
        bleHr: BleHeartRateMonitorState,
        health: HealthDashboardSnapshot,
    ): UnifiedHeartRateSummary {
        val liveSample = bleHr.latestSample
        if (bleHr.isListening && liveSample != null) {
            return UnifiedHeartRateSummary(
                primaryBpmText = liveSample.bpm.toString(),
                source = UnifiedHeartRateSource.LIVE_BLE,
                sourceCaption = "Live Watch S4 Broadcast (BLE 0x2A37) · ${liveSample.sensorContact.label}",
                detailNote = "Real-time stream active. When broadcast turns off, NothingWear automatically switches to Health Connect background readings.",
                timestampMillis = liveSample.timestampMillis,
            )
        }

        if (health.heartRateSummary.status == MetricAvailabilityStatus.VERIFIED) {
            return UnifiedHeartRateSummary(
                primaryBpmText = health.heartRateSummary.primaryValue,
                source = UnifiedHeartRateSource.HEALTH_CONNECT,
                sourceCaption = "Automatic background sync · ${health.heartRateSummary.sourceLabel}",
                detailNote = "${health.heartRateSummary.detailNote}. Works even when Watch S4 Heart Rate Broadcast is OFF.",
                timestampMillis = health.heartRateSummary.updatedAtMillis,
            )
        }

        val cached = health.lastVerifiedHeartRate
        if (cached != null) {
            return UnifiedHeartRateSummary(
                primaryBpmText = cached.bpm.toString(),
                source = UnifiedHeartRateSource.LAST_VERIFIED_CACHE,
                sourceCaption = "Last verified reading · ${cached.sourceLabel}",
                detailNote = "Watch S4 Broadcast is off and no newer Health Connect sample arrived yet. Enable Mi Fitness → Health Connect for automatic periodic updates.",
                timestampMillis = cached.timestampMillis,
            )
        }

        return UnifiedHeartRateSummary(
            primaryBpmText = "--",
            source = UnifiedHeartRateSource.UNAVAILABLE,
            sourceCaption = "No verified heart rate reading yet",
            detailNote = "For automatic BPM outside live broadcast, enable Mi Fitness → Profile → Third-party data → Health Connect and grant read access below.",
            timestampMillis = null,
        )
    }

    private data class LocalControls(
        val isRefreshing: Boolean = false,
        val feedbackMessage: String? = null,
    )
}
