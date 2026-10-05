package com.gtc.app_finance.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.gtc.app_finance.data.repository.FinanceRepository
import com.gtc.app_finance.ui.components.NavTab
import com.gtc.app_finance.ui.screens.analytics.AnalyticsScreen
import com.gtc.app_finance.ui.screens.analytics.AnalyticsViewModel
import com.gtc.app_finance.ui.screens.credits.CreditsScreen
import com.gtc.app_finance.ui.screens.credits.CreditsViewModel
import com.gtc.app_finance.ui.screens.dashboard.DashboardScreen
import com.gtc.app_finance.ui.screens.dashboard.DashboardViewModel
import com.gtc.app_finance.ui.screens.transactions.TransactionsScreen
import com.gtc.app_finance.ui.screens.transactions.TransactionsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController,
    repository: FinanceRepository,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavTab.Dashboard.route,
        modifier = modifier
    ) {
        composable(NavTab.Dashboard.route) {
            val viewModel: DashboardViewModel = viewModel(
                factory = SimpleViewModelFactory { DashboardViewModel(repository) }
            )
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToTransactions = { navController.navigate(NavTab.Transactions.route) },
                onNavigateToCredits = { navController.navigate(NavTab.Credits.route) }
            )
        }

        composable(NavTab.Transactions.route) {
            val viewModel: TransactionsViewModel = viewModel(
                factory = SimpleViewModelFactory { TransactionsViewModel(repository) }
            )
            TransactionsScreen(viewModel = viewModel)
        }

        composable(NavTab.Credits.route) {
            val viewModel: CreditsViewModel = viewModel(
                factory = SimpleViewModelFactory { CreditsViewModel(repository) }
            )
            CreditsScreen(viewModel = viewModel)
        }

        composable(NavTab.Analytics.route) {
            val viewModel: AnalyticsViewModel = viewModel(
                factory = SimpleViewModelFactory { AnalyticsViewModel(repository) }
            )
            AnalyticsScreen(viewModel = viewModel)
        }
    }
}
