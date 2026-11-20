package com.nothingwear.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.usecase.ObserveThemePreferencesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    observeThemePreferences: ObserveThemePreferencesUseCase,
) : ViewModel() {

    val themePreferences: StateFlow<AppThemePreferences> = observeThemePreferences()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = AppThemePreferences(),
        )
}
