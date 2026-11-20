package com.nothingwear.app.features.dashboard

import com.nothingwear.app.domain.model.DashboardOverview

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(val overview: DashboardOverview) : DashboardUiState
}
