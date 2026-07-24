package com.prempoddar.loadstone.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class Settings(
    val unitSystem: UnitSystem,
    val themeMode: ThemeMode,
    val onboardingComplete: Boolean,
    val arcHintDismissed: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val UNIT = stringPreferencesKey("unit_system")
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING = booleanPreferencesKey("onboarding_complete")
        val ARC_HINT = booleanPreferencesKey("arc_hint_dismissed")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            unitSystem = prefs[Keys.UNIT]?.let { UnitSystem.valueOf(it) } ?: defaultUnitSystem(),
            themeMode = prefs[Keys.THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            arcHintDismissed = prefs[Keys.ARC_HINT] ?: false,
        )
    }

    suspend fun setArcHintDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.ARC_HINT] = dismissed }
    }

    suspend fun setUnitSystem(system: UnitSystem) {
        context.dataStore.edit { it[Keys.UNIT] = system.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING] = complete }
    }

    /** Imperial for the handful of imperial-first locales, metric elsewhere. */
    private fun defaultUnitSystem(): UnitSystem {
        val country = Locale.getDefault().country.uppercase(Locale.US)
        return if (country in setOf("US", "LR", "MM")) UnitSystem.IMPERIAL else UnitSystem.METRIC
    }
}
