package com.prempoddar.loadstone.ui.nav

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
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
