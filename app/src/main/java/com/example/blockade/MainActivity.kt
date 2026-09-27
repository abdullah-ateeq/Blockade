package com.example.blockade

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.blockade.data.Settings
import com.example.blockade.data.Statistics
import com.example.blockade.data.ThemeMode
import com.example.blockade.ui.game.GameScreen
import com.example.blockade.ui.game.GameViewModel
import com.example.blockade.ui.screens.AboutScreen
import com.example.blockade.ui.screens.HomeScreen
import com.example.blockade.ui.screens.HowToPlayScreen
import com.example.blockade.ui.screens.ResultScreen
import com.example.blockade.ui.screens.SettingsScreen
import com.example.blockade.ui.screens.SplashScreen
import com.example.blockade.ui.theme.BlockadeTheme
import com.example.blockade.ui.theme.LocalBoardPalette
import com.example.blockade.ui.theme.palette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as BlockadeApplication
        setContent { BlockadeApp(app) }
    }
}

private object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val GAME = "game"
    const val RESULT = "result"
    const val HOW_TO_PLAY = "how_to_play"
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

@Composable
private fun BlockadeApp(app: BlockadeApplication) {
    val repository = app.repository
    val settings by repository.settings.collectAsStateWithLifecycle(initialValue = Settings())
    val statistics by repository.statistics.collectAsStateWithLifecycle(initialValue = Statistics())

    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }

    // Activity-scoped so the match survives visits to Settings / How to Play and feeds Result.
    val gameViewModel: GameViewModel = viewModel(
        factory = viewModelFactory { initializer { GameViewModel(repository::recordResult) } }
    )

    BlockadeTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalBoardPalette provides settings.boardTheme.palette()) {
            val nav = rememberNavController()
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
                enterTransition = { fadeIn(tween(220)) },
                exitTransition = { fadeOut(tween(180)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = { fadeOut(tween(180)) }
            ) {
                composable(Routes.SPLASH) {
                    SplashScreen(onFinished = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    })
                }
                composable(Routes.HOME) {
                    HomeScreen(
                        statistics = statistics,
                        onPlay = {
                            gameViewModel.newGame()
                            nav.go(Routes.GAME)
                        },
                        onHowToPlay = { nav.go(Routes.HOW_TO_PLAY) },
                        onSettings = { nav.go(Routes.SETTINGS) },
                        onAbout = { nav.go(Routes.ABOUT) }
                    )
                }
                composable(Routes.GAME) {
                    GameScreen(
                        viewModel = gameViewModel,
                        settings = settings,
                        soundPlayer = app.soundPlayer,
                        onGameOver = {
                            nav.navigate(Routes.RESULT) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        onHome = { nav.back() },
                        onHowToPlay = { nav.go(Routes.HOW_TO_PLAY) },
                        onSettings = { nav.go(Routes.SETTINGS) }
                    )
                }
                composable(Routes.RESULT) {
                    ResultScreen(
                        summary = gameViewModel.summary,
                        onPlayAgain = {
                            gameViewModel.newGame()
                            nav.navigate(Routes.GAME) {
                                popUpTo(Routes.HOME)
                                launchSingleTop = true
                            }
                        },
                        onHome = { nav.back() }
                    )
                }
                composable(Routes.HOW_TO_PLAY) { HowToPlayScreen(onBack = { nav.back() }) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        settings = settings,
                        statistics = statistics,
                        repository = repository,
                        onBack = { nav.back() }
                    )
                }
                composable(Routes.ABOUT) { AboutScreen(onBack = { nav.back() }) }
            }
        }
    }
}

/** Ignores taps while a transition is in flight, preventing duplicate destinations. */
private fun NavHostController.go(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        navigate(route) { launchSingleTop = true }
    }
}

private fun NavHostController.back() {
    if (currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}
