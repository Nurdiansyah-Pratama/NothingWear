package com.nothingwear.app.domain.repository

import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface ThemePreferencesRepository {
    val themePreferences: Flow<AppThemePreferences>

    suspend fun setThemeMode(themeMode: ThemeMode)

    suspend fun setAccentColor(accentColor: AccentColorOption)
}
