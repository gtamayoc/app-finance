package com.gtc.app_finance.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gtc.app_finance.ui.components.NavTab
import com.gtc.app_finance.ui.screens.analytics.AnalyticsScreen
import com.gtc.app_finance.ui.screens.analytics.AnalyticsViewModel
import com.gtc.app_finance.ui.screens.credits.CreditsScreen
import com.gtc.app_finance.ui.screens.credits.CreditsViewModel
import com.gtc.app_finance.ui.screens.dashboard.DashboardScreen
import com.gtc.app_finance.ui.screens.dashboard.DashboardViewModel
import com.gtc.app_finance.ui.screens.transactions.TransactionsScreen
import com.gtc.app_finance.ui.screens.transactions.TransactionsViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavTab.Dashboard.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing))
        }
    ) {
        composable(NavTab.Dashboard.route) {
            val viewModel: DashboardViewModel = koinViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToTransactions = {
                    navController.navigate(NavTab.Transactions.route) {
                        popUpTo(NavTab.Dashboard.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCredits = {
                    navController.navigate(NavTab.Credits.route) {
                        popUpTo(NavTab.Dashboard.route) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(NavTab.Transactions.route) {
            val viewModel: TransactionsViewModel = koinViewModel()
            TransactionsScreen(viewModel = viewModel)
        }

        composable(NavTab.Credits.route) {
            val viewModel: CreditsViewModel = koinViewModel()
            CreditsScreen(viewModel = viewModel)
        }

        composable(NavTab.Analytics.route) {
            val viewModel: AnalyticsViewModel = koinViewModel()
            AnalyticsScreen(viewModel = viewModel)
        }
    }
}
