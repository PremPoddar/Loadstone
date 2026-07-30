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
import com.prempoddar.loadstone.data.settings.Settings
import com.prempoddar.loadstone.data.settings.ThemeMode
import com.prempoddar.loadstone.data.settings.UnitSystem
import com.prempoddar.loadstone.share.AppLinks
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
        val imported = parseIncomingLink(intent)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsState()
            val pendingImport by appViewModel.pendingImport.collectAsState()
            val current: Settings? = settings

            LaunchedEffect(imported) {
                imported?.let { appViewModel.onImport(it) }
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
                    // Remembered outside the settings gate so the back stack survives activity
                    // recreation (e.g. returning from the camera); otherwise a fresh controller
                    // would be built after settings load and drop the saved destination.
                    val navController = rememberNavController()
                    if (current != null) {
                        val start = if (current.onboardingComplete) Routes.HOME else Routes.ONBOARDING
                        LoadstoneNavHost(navController = navController, startDestination = start)

                        pendingImport?.let { pending ->
                            SavePinSheet(
                                onDismiss = { appViewModel.clearImport() },
                                onSave = { name, note, category, photoPaths ->
                                    appViewModel.saveImported(name, note, category, photoPaths)
                                },
                                initialName = pending.name.orEmpty(),
                                initialNote = pending.note.orEmpty(),
                            )
                        }
                    }
                }
            }
        }
    }

    /** Extracts an imported pin from a Loadstone App Link, if the app was opened by one. */
    private fun parseIncomingLink(intent: Intent?): AppViewModel.PendingImport? {
        val data = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        val parsed = AppLinks.parse(data) ?: return null
        return AppViewModel.PendingImport(parsed.lat, parsed.lon, parsed.name, parsed.note)
    }
}
