package com.nothingwear.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.nothingwear.app.domain.model.AccentColorOption
import com.nothingwear.app.domain.model.AppThemePreferences
import com.nothingwear.app.domain.model.ThemeMode
import com.nothingwear.app.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreThemePreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ThemePreferencesRepository {

    override val themePreferences: Flow<AppThemePreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val themeMode = prefs[KEY_THEME_MODE]
                ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
                ?: ThemeMode.DARK
            val accentColor = AccentColorOption.fromId(prefs[KEY_ACCENT_COLOR])
            AppThemePreferences(
                themeMode = themeMode,
                accentColor = accentColor,
            )
        }

    override suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = themeMode.name
        }
    }

    override suspend fun setAccentColor(accentColor: AccentColorOption) {
        dataStore.edit { prefs ->
            prefs[KEY_ACCENT_COLOR] = accentColor.id
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR = stringPreferencesKey("accent_color")
    }
}
