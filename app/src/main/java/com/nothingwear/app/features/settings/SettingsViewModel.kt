package com.nothingwear.app.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.ThemeMode
import com.nothingwear.app.domain.usecase.ObserveThemePreferencesUseCase
import com.nothingwear.app.domain.usecase.UpdateAccentColorUseCase
import com.nothingwear.app.domain.usecase.UpdateThemeModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeThemePreferences: ObserveThemePreferencesUseCase,
    private val updateThemeMode: UpdateThemeModeUseCase,
    private val updateAccentColor: UpdateAccentColorUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeThemePreferences()
        .map(SettingsUiState::from)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = SettingsUiState(),
        )

    fun onSelectThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            updateThemeMode(themeMode)
        }
    }

    fun onSelectAccentColor(accentColor: AccentColorOption) {
        viewModelScope.launch {
            updateAccentColor(accentColor)
        }
    }
}
