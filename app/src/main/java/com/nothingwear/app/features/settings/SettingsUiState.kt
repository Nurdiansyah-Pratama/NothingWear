package com.nothingwear.app.features.settings

import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.model.ThemeMode

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accentColor: AccentColorOption = AccentColorOption.SIGNAL_RED,
) {
    companion object {
        fun from(preferences: AppThemePreferences): SettingsUiState = SettingsUiState(
            themeMode = preferences.themeMode,
            accentColor = preferences.accentColor,
        )
    }
}
