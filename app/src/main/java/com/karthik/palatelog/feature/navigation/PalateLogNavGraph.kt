package com.karthik.palatelog.feature.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.karthik.palatelog.feature.dashboard.DashboardScreen
import com.karthik.palatelog.feature.dashboard.DashboardViewModel
import com.karthik.palatelog.feature.entry_editor.EntryEditorScreen
import com.karthik.palatelog.feature.entry_editor.EntryEditorViewModel
import com.karthik.palatelog.feature.map.MapScreen
import com.karthik.palatelog.feature.map.MapViewModel
import com.karthik.palatelog.feature.quick_capture.QuickCaptureScreen
import com.karthik.palatelog.feature.quick_capture.QuickCaptureViewModel
import com.karthik.palatelog.feature.restaurant_detail.RestaurantDetailScreen
import com.karthik.palatelog.feature.restaurant_detail.RestaurantDetailViewModel
import com.karthik.palatelog.feature.settings.SettingsScreen
import com.karthik.palatelog.feature.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Dashboard : Screen("dashboard", "Explore", Icons.Default.Explore)
    object Map : Screen("map", "Map", Icons.Default.Map)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object QuickCapture : Screen("quick_capture?lat={lat}&lng={lng}&address={address}&city={city}", "Quick Log") {
        fun createRoute(
            lat: Double? = null,
            lng: Double? = null,
            address: String? = null,
            city: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (lat != null) params.add("lat=$lat")
            if (lng != null) params.add("lng=$lng")
            if (!address.isNullOrBlank()) params.add("address=${android.net.Uri.encode(address)}")
            if (!city.isNullOrBlank()) params.add("city=${android.net.Uri.encode(city)}")
            return if (params.isNotEmpty()) "quick_capture?${params.joinToString("&")}" else "quick_capture"
        }
    }
    object EntryEditor : Screen("entry_editor?restaurantId={restaurantId}&lat={lat}&lng={lng}&address={address}&city={city}", "Edit Entry") {
        fun createRoute(
            restaurantId: String? = null,
            lat: Double? = null,
            lng: Double? = null,
            address: String? = null,
            city: String? = null
        ): String {
            val params = mutableListOf<String>()
            if (!restaurantId.isNullOrBlank()) params.add("restaurantId=$restaurantId")
            if (lat != null) params.add("lat=$lat")
            if (lng != null) params.add("lng=$lng")
            if (!address.isNullOrBlank()) params.add("address=${android.net.Uri.encode(address)}")
            if (!city.isNullOrBlank()) params.add("city=${android.net.Uri.encode(city)}")
            return if (params.isNotEmpty()) "entry_editor?${params.joinToString("&")}" else "entry_editor"
        }
    }
    object RestaurantDetail : Screen("restaurant_detail/{restaurantId}", "Restaurant Details") {
        fun createRoute(restaurantId: String): String = "restaurant_detail/$restaurantId"
    }
}

@Composable
fun PalateLogNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarScreens = listOf(
        Screen.Dashboard,
        Screen.Map,
        Screen.Settings
    )

    val showBottomBar = currentRoute in bottomBarScreens.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomBarScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab 1: Dashboard
            composable(Screen.Dashboard.route) {
                val viewModel: DashboardViewModel = hiltViewModel()
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToRestaurantDetail = { restaurantId ->
                        navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                    },
                    onNavigateToQuickCapture = {
                        navController.navigate(Screen.QuickCapture.route)
                    },
                    onNavigateToEntryEditor = { restaurantId ->
                        navController.navigate(Screen.EntryEditor.createRoute(restaurantId))
                    }
                )
            }

            // Tab 2: Map
            composable(Screen.Map.route) {
                val viewModel: MapViewModel = hiltViewModel()
                MapScreen(
                    viewModel = viewModel,
                    onNavigateToRestaurantDetail = { restaurantId ->
                        navController.navigate(Screen.RestaurantDetail.createRoute(restaurantId))
                    },
                    onNavigateToQuickCapture = { lat, lng, address, city ->
                        navController.navigate(Screen.QuickCapture.createRoute(lat = lat, lng = lng, address = address, city = city))
                    },
                    onNavigateToEntryEditor = { restaurantId, lat, lng, address, city ->
                        navController.navigate(Screen.EntryEditor.createRoute(restaurantId = restaurantId, lat = lat, lng = lng, address = address, city = city))
                    }
                )
            }

            // Tab 3: Settings
            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            // Sub-Screen: 3-Second Quick Logger
            composable(
                route = Screen.QuickCapture.route,
                arguments = listOf(
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("address") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("city") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val viewModel: QuickCaptureViewModel = hiltViewModel()
                QuickCaptureScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-Screen: Full Entry Editor
            composable(
                route = Screen.EntryEditor.route,
                arguments = listOf(
                    navArgument("restaurantId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("lat") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("lng") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("address") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("city") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val viewModel: EntryEditorViewModel = hiltViewModel()
                EntryEditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Sub-Screen: Restaurant Detail
            composable(
                route = Screen.RestaurantDetail.route,
                arguments = listOf(
                    navArgument("restaurantId") {
                        type = NavType.StringType
                    }
                )
            ) {
                val viewModel: RestaurantDetailViewModel = hiltViewModel()
                RestaurantDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToLogVisit = { restaurantId ->
                        navController.navigate(Screen.EntryEditor.createRoute(restaurantId))
                    }
                )
            }
        }
    }
}
