package com.prempoddar.loadstone.ui.nav

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.prempoddar.loadstone.ui.drop.DropPinScreen
import com.prempoddar.loadstone.ui.home.HomeScreen
import com.prempoddar.loadstone.ui.navigate.NavigateScreen
import com.prempoddar.loadstone.ui.onboarding.OnboardingScreen
import com.prempoddar.loadstone.ui.settings.SettingsScreen

@Composable
fun LoadstoneNavHost(
    navController: NavHostController,
    startDestination: String,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally { it / 4 } + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutHorizontally { it / 4 } + fadeOut() },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onDropPin = { navController.navigate(Routes.DROP) },
                onOpenPin = { id -> navController.navigate(Routes.navigate(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenRadar = { navController.navigate(Routes.RADAR) },
            )
        }
        composable(Routes.DROP) {
            DropPinScreen(
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.NAVIGATE,
            arguments = listOf(navArgument(Routes.NAVIGATE_ARG_PIN_ID) { type = NavType.LongType }),
        ) {
            NavigateScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAppearance = { navController.navigate(Routes.APPEARANCE) },
                onAbout = { navController.navigate(Routes.ABOUT) },
            )
        }
        composable(Routes.RADAR) {
            com.prempoddar.loadstone.ui.radar.RadarScreen(
                onOpenPin = { id -> navController.navigate(Routes.navigate(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.APPEARANCE) {
            val vm: com.prempoddar.loadstone.ui.settings.SettingsViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val settings by vm.settings.collectAsState()
            settings?.let { s ->
                com.prempoddar.loadstone.ui.design.AppearanceScreen(
                    s.appearance, com.prempoddar.loadstone.ui.theme.LOADSTONE_BRAND,
                    onChange = vm::setAppearance, onBack = { navController.popBackStack() },
                )
            }
        }
        composable(Routes.ABOUT) {
            com.prempoddar.loadstone.ui.design.AboutScreen(
                "Loadstone", "Drop a pin, follow it back. Loadstone waits for your GPS to sharpen so every saved spot is exact.",
                icon = { com.prempoddar.loadstone.ui.design.LayeredIcon(com.prempoddar.loadstone.R.drawable.ic_launcher_bg, com.prempoddar.loadstone.R.drawable.ic_launcher_foreground) },
                credits = listOf(
                    com.prempoddar.loadstone.ui.design.Credit("Jetpack Compose and AndroidX", "Apache 2.0"),
                    com.prempoddar.loadstone.ui.design.Credit("Hilt", "Apache 2.0"),
                    com.prempoddar.loadstone.ui.design.Credit("Room", "Apache 2.0"),
                    com.prempoddar.loadstone.ui.design.Credit("MaterialKolor", "Apache 2.0"),
                ),
                onBack = { navController.popBackStack() },
                notes = listOf(
                    "Your location" to "Used only while Loadstone is open, to save and find your pins. It never leaves your phone unless you share a pin.",
                    "Your pins and photos" to "Kept on this phone. Sharing a pin sends only what you choose to share.",
                ),
            )
        }
    }
}
