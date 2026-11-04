package com.ritmo.app.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class AccentTheme { OCEAN, VIOLET, FOREST, SUNSET, CUSTOM }
data class AppSettings(val themeMode: ThemeMode = ThemeMode.SYSTEM, val accent: AccentTheme = AccentTheme.OCEAN, val customColor: Long = 0xFF006C84, val language: String = "ru")

private val Context.settingsDataStore by preferencesDataStore("app_settings")

@Singleton
class SettingsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val accentKey = stringPreferencesKey("accent")
    private val languageKey = stringPreferencesKey("language")
    private val customColorKey = longPreferencesKey("custom_color")
    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { values ->
        AppSettings(
            runCatching { ThemeMode.valueOf(values[themeKey] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM),
            runCatching { AccentTheme.valueOf(values[accentKey] ?: AccentTheme.OCEAN.name) }.getOrDefault(AccentTheme.OCEAN),
            values[customColorKey] ?: 0xFF006C84,
            values[languageKey] ?: "ru",
        )
    }
    suspend fun current() = settings.first()
    suspend fun setTheme(value: ThemeMode) = context.settingsDataStore.edit { it[themeKey] = value.name }
    suspend fun setAccent(value: AccentTheme) = context.settingsDataStore.edit { it[accentKey] = value.name }
    suspend fun setCustomColor(value: Long) = context.settingsDataStore.edit { it[customColorKey] = value; it[accentKey] = AccentTheme.CUSTOM.name }
    suspend fun setLanguage(value: String) = context.settingsDataStore.edit { it[languageKey] = value }
}

fun applyAppLanguage(context: Context, language: String) {
    val locale = Locale.forLanguageTag(language)
    Locale.setDefault(locale)
    if (Build.VERSION.SDK_INT >= 33) context.getSystemService(android.app.LocaleManager::class.java).applicationLocales = LocaleList(locale)
    else {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }
}
