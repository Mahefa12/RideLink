package com.app.ridelink.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

@Singleton
class ThemeManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore: DataStore<Preferences> = context.dataStore
    private val isDarkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val themeModeKey = booleanPreferencesKey("theme_mode_is_system")
    
    val isDarkTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[isDarkThemeKey] ?: false
    }
    
    val isSystemTheme: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[themeModeKey] ?: true
    }
    
    suspend fun setDarkTheme(isDark: Boolean) {
        dataStore.edit { preferences ->
            preferences[isDarkThemeKey] = isDark
            preferences[themeModeKey] = false // Disable system theme when manually set
        }
    }
    
    suspend fun setSystemTheme(useSystem: Boolean) {
        dataStore.edit { preferences ->
            preferences[themeModeKey] = useSystem
        }
    }
    
    suspend fun toggleTheme() {
        dataStore.edit { preferences ->
            val currentDark = preferences[isDarkThemeKey] ?: false
            preferences[isDarkThemeKey] = !currentDark
            preferences[themeModeKey] = false // Disable system theme when toggling
        }
    }
}