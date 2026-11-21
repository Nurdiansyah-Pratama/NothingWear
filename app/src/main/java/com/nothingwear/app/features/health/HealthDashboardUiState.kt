package com.nothingwear.app.features.health

import com.nothingwear.app.domain.model.BleDeviceSummary
import com.nothingwear.app.domain.model.BleHeartRateMonitorState
import com.nothingwear.app.domain.model.BluetoothAdapterStatus
import com.nothingwear.app.domain.model.GattInspectionSession
import com.nothingwear.app.domain.model.HealthDashboardSnapshot
import com.nothingwear.app.domain.model.UnifiedHeartRateSummary

data class HealthDashboardUiState(
    val isRefreshing: Boolean = false,
    val healthSnapshot: HealthDashboardSnapshot = HealthDashboardSnapshot(),
    val unifiedHeartRate: UnifiedHeartRateSummary = UnifiedHeartRateSummary(),
    val adapterStatus: BluetoothAdapterStatus = BluetoothAdapterStatus.DISABLED,
    val detectedWatch: BleDeviceSummary? = null,
    val gattSession: GattInspectionSession = GattInspectionSession(),
    val bleHeartRate: BleHeartRateMonitorState = BleHeartRateMonitorState(),
    val feedbackMessage: String? = null,
)
