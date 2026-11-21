package com.nothingwear.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.domain.usecase.ObserveDashboardOverviewUseCase
import com.nothingwear.app.domain.usecase.RefreshHealthDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    observeDashboardOverview: ObserveDashboardOverviewUseCase,
    private val refreshHealthData: RefreshHealthDataUseCase,
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = observeDashboardOverview()
        .map<_, DashboardUiState> { overview -> DashboardUiState.Ready(overview) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = DashboardUiState.Loading,
        )

    init {
        viewModelScope.launch {
            refreshHealthData()
        }
    }
}
