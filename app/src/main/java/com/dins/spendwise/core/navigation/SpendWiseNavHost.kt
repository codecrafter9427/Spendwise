package com.dins.spendwise.core.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dins.spendwise.feature.analytics.AnalyticsRoute
import com.dins.spendwise.feature.budget.BudgetDialogHost
import com.dins.spendwise.feature.dashboard.DashboardRoute
import com.dins.spendwise.feature.dashboard.DashboardViewModel
import com.dins.spendwise.feature.settings.SettingsRoute
import com.dins.spendwise.feature.transaction.TransactionDetailsRoute
import com.dins.spendwise.feature.transaction.TransactionFormRoute
import com.dins.spendwise.feature.transaction.TransactionListRoute

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun SpendWiseApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in Routes.bottomBarDestinations
    val showFab = currentRoute == Routes.HOME || currentRoute == Routes.TRANSACTIONS

    var showBudgetDialog by remember { mutableStateOf(false) }
    val dashboardViewModel: DashboardViewModel = hiltViewModel()
    val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()

    val bottomItems = listOf(
        BottomNavItem(Routes.HOME, "Home", Icons.Outlined.Home),
        BottomNavItem(Routes.TRANSACTIONS, "Transactions", Icons.AutoMirrored.Outlined.ReceiptLong),
        BottomNavItem(Routes.ANALYTICS, "Analytics", Icons.Outlined.Analytics),
        BottomNavItem(Routes.SETTINGS, "Settings", Icons.Outlined.Settings)
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(item.icon, contentDescription = item.label)
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { navController.navigate(Routes.ADD_TRANSACTION) }
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add transaction")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.HOME) {
                DashboardRoute(
                    onViewAllTransactions = {
                        navController.navigate(Routes.TRANSACTIONS) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onTransactionClick = { id ->
                        navController.navigate(Routes.transactionDetails(id))
                    },
                    onSetBudget = { showBudgetDialog = true },
                    viewModel = dashboardViewModel
                )
            }
            composable(Routes.TRANSACTIONS) {
                TransactionListRoute(
                    onTransactionClick = { id ->
                        navController.navigate(Routes.transactionDetails(id))
                    }
                )
            }
            composable(Routes.ANALYTICS) {
                AnalyticsRoute()
            }
            composable(Routes.SETTINGS) {
                SettingsRoute()
            }
            composable(Routes.ADD_TRANSACTION) {
                TransactionFormRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.EDIT_TRANSACTION,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionFormRoute(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.TRANSACTION_DETAILS,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                TransactionDetailsRoute(
                    onDismiss = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.popBackStack()
                        navController.navigate(Routes.editTransaction(id))
                    }
                )
            }
        }
    }

    BudgetDialogHost(
        visible = showBudgetDialog,
        yearMonth = dashboardState.yearMonth,
        existingAmountMinor = dashboardState.budgetProgress.budgetMinor,
        onDismiss = { showBudgetDialog = false }
    )
}
