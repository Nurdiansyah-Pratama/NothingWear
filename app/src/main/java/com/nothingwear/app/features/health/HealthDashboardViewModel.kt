package com.nothingwear.app.features.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.core.bluetooth.WatchMatchConfidence
import com.nothingwear.app.domain.usecase.GetRequiredHealthPermissionsUseCase
import com.nothingwear.app.domain.usecase.InspectDeviceGattUseCase
import com.nothingwear.app.domain.usecase.ObserveBleDiagnosticsUseCase
import com.nothingwear.app.domain.usecase.ObserveHealthSnapshotUseCase
import com.nothingwear.app.domain.usecase.RefreshBleEnvironmentUseCase
import com.nothingwear.app.domain.usecase.RefreshHealthDataUseCase
import com.nothingwear.app.domain.usecase.StartStandardHeartRateMonitorUseCase
import com.nothingwear.app.domain.usecase.StopStandardHeartRateMonitorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HealthDashboardViewModel @Inject constructor(
    observeHealthSnapshot: ObserveHealthSnapshotUseCase,
    observeBleDiagnostics: ObserveBleDiagnosticsUseCase,
    private val refreshHealthData: RefreshHealthDataUseCase,
    private val refreshBleEnvironment: RefreshBleEnvironmentUseCase,
    private val getRequiredHealthPermissions: GetRequiredHealthPermissionsUseCase,
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
        HealthDashboardUiState(
            isRefreshing = controls.isRefreshing,
            healthSnapshot = health,
            adapterStatus = ble.adapterStatus,
            detectedWatch = detectedWatch,
            gattSession = ble.gattSession,
            bleHeartRate = ble.gattSession.heartRateState,
            feedbackMessage = controls.feedbackMessage,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = HealthDashboardUiState(),
    )

    init {
        onRefreshAll()
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

    private data class LocalControls(
        val isRefreshing: Boolean = false,
        val feedbackMessage: String? = null,
    )
}
