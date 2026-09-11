package com.jaidensiu.quickmaths

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.jaidensiu.quickmaths.ui.CountdownScreen
import com.jaidensiu.quickmaths.ui.GameScreen
import com.jaidensiu.quickmaths.ui.ResultsScreen
import com.jaidensiu.quickmaths.ui.SettingsScreen
import com.jaidensiu.quickmaths.ui.StartScreen
import com.jaidensiu.quickmaths.ui.TOTAL_QUESTIONS

private const val NAV_TRANSITION_MS = 300

@Composable
fun QuickMathsApp(onMusicAllowedChanged: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isMusicAllowed = destination == null ||
            !(destination.hasRoute<AppRoute.Countdown>() || destination.hasRoute<AppRoute.Game>())

    LaunchedEffect(key1 = isMusicAllowed) {
        onMusicAllowedChanged(isMusicAllowed)
    }

    NavHost(
        navController = navController,
        startDestination = AppRoute.Start,
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.background),
        enterTransition = { fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) },
    ) {
        composable<AppRoute.Start> {
            StartScreen(
                onStartGame = dropUnlessResumed { navController.navigate(route = AppRoute.Countdown) },
                onOpenSettings = dropUnlessResumed { navController.navigate(route = AppRoute.Settings) },
            )
        }
        composable<AppRoute.Settings> {
            SettingsScreen(onBack = dropUnlessResumed { navController.popBackStack() })
        }
        composable<AppRoute.Countdown> {
            CountdownScreen(
                onCountdownFinished = {
                    navController.navigate(route = AppRoute.Game) {
                        popUpTo<AppRoute.Countdown> { inclusive = true }
                    }
                },
            )
        }
        composable<AppRoute.Game> {
            GameScreen(
                onGameFinished = { elapsedTimeMs ->
                    navController.navigate(route = AppRoute.Results(elapsedTimeMs = elapsedTimeMs)) {
                        popUpTo<AppRoute.Game> { inclusive = true }
                    }
                },
                onExitGame = dropUnlessResumed { navController.popBackStack() },
            )
        }
        composable<AppRoute.Results> { backStackEntry ->
            val route = backStackEntry.toRoute<AppRoute.Results>()
            ResultsScreen(
                elapsedTimeMs = route.elapsedTimeMs,
                totalQuestions = TOTAL_QUESTIONS,
                onPlayAgain = dropUnlessResumed {
                    navController.navigate(route = AppRoute.Countdown) {
                        popUpTo<AppRoute.Results> { inclusive = true }
                    }
                },
                onBackToHome = dropUnlessResumed { navController.popBackStack() },
            )
        }
    }
}
