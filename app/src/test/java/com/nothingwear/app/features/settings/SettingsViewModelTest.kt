package com.nothingwear.app.features.settings

import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.model.ThemeMode
import com.nothingwear.app.domain.repository.ThemePreferencesRepository
import com.nothingwear.app.domain.usecase.ObserveThemePreferencesUseCase
import com.nothingwear.app.domain.usecase.UpdateAccentColorUseCase
import com.nothingwear.app.domain.usecase.UpdateThemeModeUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultsToDarkThemeAndSignalRedAccent() = runTest {
        val fakeRepo = FakeThemePreferencesRepository()
        val viewModel = SettingsViewModel(
            observeThemePreferences = ObserveThemePreferencesUseCase(fakeRepo),
            updateThemeMode = UpdateThemeModeUseCase(fakeRepo),
            updateAccentColor = UpdateAccentColorUseCase(fakeRepo),
        )

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(AccentColorOption.SIGNAL_RED, viewModel.uiState.value.accentColor)
    }

    @Test
    fun updatesThemeModeAndAccentColorReactively() = runTest {
        val fakeRepo = FakeThemePreferencesRepository()
        val viewModel = SettingsViewModel(
            observeThemePreferences = ObserveThemePreferencesUseCase(fakeRepo),
            updateThemeMode = UpdateThemeModeUseCase(fakeRepo),
            updateAccentColor = UpdateAccentColorUseCase(fakeRepo),
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        viewModel.onSelectThemeMode(ThemeMode.LIGHT)
        viewModel.onSelectAccentColor(AccentColorOption.CYBER_CYAN)
        advanceUntilIdle()

        assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
        assertEquals(AccentColorOption.CYBER_CYAN, viewModel.uiState.value.accentColor)

        collectJob.cancel()
    }

    private class FakeThemePreferencesRepository : ThemePreferencesRepository {
        private val state = MutableStateFlow(AppThemePreferences())
        override val themePreferences: Flow<AppThemePreferences> = state

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = state.value.copy(themeMode = themeMode)
        }

        override suspend fun setAccentColor(accentColor: AccentColorOption) {
            state.value = state.value.copy(accentColor = accentColor)
        }
    }
}
