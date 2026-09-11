package com.dietary.tracker.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dietary.tracker.ui.ViewModelFactory
import com.dietary.tracker.ui.screens.bmi.BmiScreen
import com.dietary.tracker.ui.screens.bmi.BmiViewModel
import com.dietary.tracker.ui.screens.dashboard.DashboardScreen
import com.dietary.tracker.ui.screens.dashboard.DashboardViewModel
import com.dietary.tracker.ui.screens.foodentry.AddFoodScreen
import com.dietary.tracker.ui.screens.foodentry.AddFoodViewModel
import com.dietary.tracker.ui.screens.foodentry.BarcodeScanScreen
import com.dietary.tracker.ui.screens.foodentry.PhotoScanScreen
import com.dietary.tracker.ui.screens.history.HistoryScreen
import com.dietary.tracker.ui.screens.history.HistoryViewModel
import com.dietary.tracker.ui.screens.profile.ProfileScreen
import com.dietary.tracker.ui.screens.profile.ProfileViewModel
import com.dietary.tracker.ui.screens.profile.WearableScreen
import com.dietary.tracker.ui.screens.recipes.RecipeScreen
import com.dietary.tracker.ui.screens.voice.ChandraScreen

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Filled.Home)
    object History : Screen("history", "History", Icons.Filled.History)
    object Bmi : Screen("bmi", "Plan", Icons.Filled.MonitorWeight)
    object Water : Screen("water", "Water", Icons.Filled.WaterDrop)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
    object AddFood : Screen("add_food", "Add Food", Icons.Filled.Add)
    object BarcodeScan : Screen("barcode_scan", "Barcode", Icons.Filled.QrCodeScanner)
    object PhotoScan : Screen("photo_scan", "Photo", Icons.Filled.CameraAlt)
    object Wearables : Screen("wearables", "Wearables", Icons.Filled.Watch)
    object Recipes : Screen("recipes", "My Meals", Icons.Filled.RestaurantMenu)
    object Chandra : Screen("chandra", "Chandra", Icons.Filled.Mic)
}

private val bottomNavItems = listOf(Screen.Dashboard, Screen.Bmi, Screen.History, Screen.Water, Screen.Profile)

@Composable
fun AppNavGraph(factory: ViewModelFactory, startRoute: String = Screen.Dashboard.route) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Dashboard.route) {
                val vm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(
                    viewModel = vm,
                    onAddFoodClick = { navController.navigate(Screen.AddFood.route) }
                )
            }
            composable(Screen.History.route) {
                val vm: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(viewModel = vm)
            }
            composable(Screen.Bmi.route) {
                val vm: BmiViewModel = viewModel(factory = factory)
                BmiScreen(viewModel = vm)
            }
            composable(Screen.Water.route) {
                val vm: com.dietary.tracker.ui.screens.water.WaterReminderViewModel = viewModel(factory = factory)
                com.dietary.tracker.ui.screens.water.WaterReminderScreen(viewModel = vm)
            }
            composable(Screen.Profile.route) {
                val vm: ProfileViewModel = viewModel(factory = factory)
                ProfileScreen(viewModel = vm, onWearablesClick = { navController.navigate(Screen.Wearables.route) }, onRecipesClick = { navController.navigate(Screen.Recipes.route) }, onChandraClick = { navController.navigate(Screen.Chandra.route) })
            }

            composable(Screen.Wearables.route) {
                WearableScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Recipes.route) {
                RecipeScreen(repository = factory.repositoryForUi(), onBack = { navController.popBackStack() })
            }
            composable(Screen.Chandra.route) { ChandraScreen(onBack = { navController.popBackStack() }) }
            composable(Screen.AddFood.route) { backStackEntry ->
                val vm: AddFoodViewModel = viewModel(factory = factory)
                AddFoodScreen(
                    viewModel = vm,
                    onScanBarcode = { navController.navigate(Screen.BarcodeScan.route) },
                    onScanPhoto = { navController.navigate(Screen.PhotoScan.route) },
                    onDone = { navController.popBackStack(Screen.Dashboard.route, inclusive = false) }
                )
            }
            composable(Screen.BarcodeScan.route) {
                val parentEntry = androidx.compose.runtime.remember(navController) {
                    navController.getBackStackEntry(Screen.AddFood.route)
                }
                val vm: AddFoodViewModel = viewModel(parentEntry, factory = factory)
                BarcodeScanScreen(
                    viewModel = vm,
                    onBarcodeResolved = { result ->
                        vm.setResultDirectly(result)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
            composable(Screen.PhotoScan.route) {
                val parentEntry = androidx.compose.runtime.remember(navController) {
                    navController.getBackStackEntry(Screen.AddFood.route)
                }
                val vm: AddFoodViewModel = viewModel(parentEntry, factory = factory)
                PhotoScanScreen(
                    onParsed = { result ->
                        vm.setResultDirectly(result)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}
