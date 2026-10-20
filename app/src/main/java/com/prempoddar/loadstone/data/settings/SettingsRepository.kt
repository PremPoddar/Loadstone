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
    /** Wallpaper colours, a chosen accent and black backgrounds; light or dark is [themeMode]. */
    val dynamicColor: Boolean = false,
    val accent: Int? = null,
    val pureBlack: Boolean = false,
    /** Font, shapes, haptics and the rest of Look & feel, as saved by Appearance.extras. */
    val look: String? = null,
    /** Ask for fingerprint, face or the screen lock to open the app. */
    val appLock: Boolean = false,
    /** Recording every fix to a file, for accuracy testing. */
    val accuracyLog: Boolean = false,
) {
    val appearance get() = com.prempoddar.loadstone.ui.design.Appearance(
        mode = when (themeMode) {
            ThemeMode.LIGHT -> com.prempoddar.loadstone.ui.design.Appearance.Mode.LIGHT
            ThemeMode.DARK -> com.prempoddar.loadstone.ui.design.Appearance.Mode.DARK
            ThemeMode.SYSTEM -> com.prempoddar.loadstone.ui.design.Appearance.Mode.SYSTEM
        },
        dynamic = dynamicColor, accent = accent, pureBlack = pureBlack,
    ).withExtras(look)
}

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
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val ACCENT = androidx.datastore.preferences.core.intPreferencesKey("accent")
        val BLACK = booleanPreferencesKey("pure_black")
        val LOOK = stringPreferencesKey("look")
        val LOCK = booleanPreferencesKey("app_lock")
        val LOG = booleanPreferencesKey("accuracy_log")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            unitSystem = prefs[Keys.UNIT]?.let { UnitSystem.valueOf(it) } ?: defaultUnitSystem(),
            themeMode = prefs[Keys.THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM,
            onboardingComplete = prefs[Keys.ONBOARDING] ?: false,
            arcHintDismissed = prefs[Keys.ARC_HINT] ?: false,
            dynamicColor = prefs[Keys.DYNAMIC] ?: false,
            accent = prefs[Keys.ACCENT],
            pureBlack = prefs[Keys.BLACK] ?: false,
            look = prefs[Keys.LOOK],
            appLock = prefs[Keys.LOCK] ?: false,
            accuracyLog = prefs[Keys.LOG] ?: false,
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

    suspend fun setAppearance(a: com.prempoddar.loadstone.ui.design.Appearance) {
        context.dataStore.edit { p ->
            p[Keys.THEME] = when (a.mode) {
                com.prempoddar.loadstone.ui.design.Appearance.Mode.LIGHT -> ThemeMode.LIGHT
                com.prempoddar.loadstone.ui.design.Appearance.Mode.DARK -> ThemeMode.DARK
                com.prempoddar.loadstone.ui.design.Appearance.Mode.SYSTEM -> ThemeMode.SYSTEM
            }.name
            p[Keys.DYNAMIC] = a.dynamic
            p[Keys.BLACK] = a.pureBlack
            p[Keys.LOOK] = a.extras()
            a.accent?.let { p[Keys.ACCENT] = it } ?: p.remove(Keys.ACCENT)
        }
    }

    suspend fun setAccuracyLog(on: Boolean) {
        context.dataStore.edit { it[Keys.LOG] = on }
    }

    suspend fun setAppLock(on: Boolean) {
        context.dataStore.edit { it[Keys.LOCK] = on }
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
