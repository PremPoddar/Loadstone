package com.prempoddar.loadstone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.prempoddar.loadstone.core.format.CoordinateFormat
import com.prempoddar.loadstone.data.settings.Settings
import com.prempoddar.loadstone.data.settings.ThemeMode
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.ui.AppViewModel
import com.prempoddar.loadstone.ui.LocalUnitSystem
import com.prempoddar.loadstone.ui.drop.SavePinSheet
import com.prempoddar.loadstone.ui.nav.LoadstoneNavHost
import com.prempoddar.loadstone.ui.nav.Routes
import com.prempoddar.loadstone.ui.theme.LoadstoneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val importedCoords = parseIncomingCoordinates(intent)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsState()
            val pendingImport by appViewModel.pendingImport.collectAsState()
            val current: Settings? = settings

            LaunchedEffect(importedCoords) {
                importedCoords?.let { appViewModel.onImportCoordinates(it.first, it.second) }
            }

            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (current?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                else -> systemDark
            }

            LoadstoneTheme(darkTheme = darkTheme) {
                CompositionLocalProvider(
                    LocalUnitSystem provides (current?.unitSystem ?: UnitSystem.METRIC),
                ) {
                    if (current != null) {
                        val navController = rememberNavController()
                        val start = if (current.onboardingComplete) Routes.HOME else Routes.ONBOARDING
                        LoadstoneNavHost(navController = navController, startDestination = start)

                        if (pendingImport != null) {
                            SavePinSheet(
                                onDismiss = { appViewModel.clearImport() },
                                onSave = { name, note, category, photoPaths ->
                                    appViewModel.saveImported(name, note, category, photoPaths)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    /** Extracts a lat/lon pair from a geo: VIEW or text/plain SEND intent, if present. */
    private fun parseIncomingCoordinates(intent: Intent?): Pair<Double, Double>? {
        intent ?: return null
        val raw = when (intent.action) {
            Intent.ACTION_VIEW -> intent.dataString
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            else -> null
        } ?: return null
        return CoordinateFormat.parse(raw)
    }
}
