package com.prempoddar.loadstone

import android.content.Intent
import android.os.Bundle
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
class MainActivity : androidx.fragment.app.FragmentActivity() {
    /** Unlocked since the app last came to the front (only matters with app lock on). */
    private val unlocked = androidx.compose.runtime.mutableStateOf(false)
    private var leftAt = 0L

    override fun onStop() {
        super.onStop()
        leftAt = android.os.SystemClock.elapsedRealtime()
    }

    override fun onStart() {
        super.onStart()
        // A quick trip out (the camera, a share sheet) doesn't lock; being away for a while does.
        if (leftAt > 0 && android.os.SystemClock.elapsedRealtime() - leftAt > RELOCK_AFTER_MS) unlocked.value = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.prempoddar.loadstone.ui.design.Haptics.attach(window.decorView)
        enableEdgeToEdge()
        val imported = parseIncomingLink(intent)
        val shortcutRoute = routeFor(intent)

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val settings by appViewModel.settings.collectAsState()
            val pendingImport by appViewModel.pendingImport.collectAsState()
            val current: Settings? = settings
            com.prempoddar.loadstone.data.location.FixLog.enabled = current?.accuracyLog == true

            LaunchedEffect(imported) {
                imported?.let { appViewModel.onImport(it) }
            }

            LoadstoneTheme(current?.appearance ?: com.prempoddar.loadstone.ui.design.Appearance()) {
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
                        // A home-screen shortcut: open its screen once, over the home screen.
                        LaunchedEffect(shortcutRoute) {
                            if (shortcutRoute != null && current.onboardingComplete) navController.navigate(shortcutRoute)
                        }

                        pendingImport?.let { pending ->
                            SavePinSheet(
                                onDismiss = { appViewModel.clearImport() },
                                onSave = { name, note, category, photoPaths, folder ->
                                    appViewModel.saveImported(name, note, category, photoPaths, folder)
                                },
                                initialName = pending.name.orEmpty(),
                                initialNote = pending.note.orEmpty(),
                            )
                        }
                        if (current.appLock && !unlocked.value) {
                            com.prempoddar.loadstone.ui.lock.LockScreen(this@MainActivity) { unlocked.value = true }
                        }
                    }
                }
            }
        }
    }

    /** Where a shortcut asks to go: dropping a pin, or finding one. */
    private fun routeFor(intent: Intent?): String? = when (intent?.action) {
        ACTION_DROP -> Routes.DROP
        ACTION_FIND -> intent.getLongExtra(EXTRA_PIN, -1L).takeIf { it > 0 }?.let { Routes.navigate(it) }
        else -> null
    }

    companion object {
        const val ACTION_DROP = "com.prempoddar.loadstone.action.DROP"
        const val ACTION_FIND = "com.prempoddar.loadstone.action.FIND"
        const val EXTRA_PIN = "pin"
        private const val RELOCK_AFTER_MS = 30_000L
    }

    /** Extracts an imported pin from a Loadstone App Link, if the app was opened by one. */
    private fun parseIncomingLink(intent: Intent?): AppViewModel.PendingImport? {
        val data = intent?.takeIf { it.action == Intent.ACTION_VIEW }?.data ?: return null
        val parsed = AppLinks.parse(data) ?: return null
        return AppViewModel.PendingImport(parsed.lat, parsed.lon, parsed.name, parsed.note)
    }
}
