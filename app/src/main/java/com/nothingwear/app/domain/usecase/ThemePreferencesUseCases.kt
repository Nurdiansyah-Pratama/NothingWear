package com.nothingwear.app.domain.usecase

import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.model.ThemeMode
import com.nothingwear.app.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveThemePreferencesUseCase @Inject constructor(
    private val repository: ThemePreferencesRepository,
) {
    operator fun invoke(): Flow<AppThemePreferences> = repository.themePreferences
}

class UpdateThemeModeUseCase @Inject constructor(
    private val repository: ThemePreferencesRepository,
) {
    suspend operator fun invoke(themeMode: ThemeMode) {
        repository.setThemeMode(themeMode)
    }
}

class UpdateAccentColorUseCase @Inject constructor(
    private val repository: ThemePreferencesRepository,
) {
    suspend operator fun invoke(accentColor: AccentColorOption) {
        repository.setAccentColor(accentColor)
    }
}
