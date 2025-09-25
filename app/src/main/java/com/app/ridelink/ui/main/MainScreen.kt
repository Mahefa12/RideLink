package com.app.ridelink.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.ridelink.ui.home.HomeScreen
import com.app.ridelink.ui.home.RideRequest
import com.app.ridelink.ui.profile.ProfileScreen
import com.app.ridelink.ui.settings.UserSettingsScreen
import com.app.ridelink.ui.search.SearchScreen
import com.app.ridelink.ui.messages.MessagesScreen

sealed class BottomNavScreen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : BottomNavScreen("home", "Home", Icons.Filled.Home)
    object Search : BottomNavScreen("search", "Search", Icons.Filled.Search)
    object Messages : BottomNavScreen("messages", "Messages", Icons.Filled.Email)
    object Profile : BottomNavScreen("profile", "Profile", Icons.Filled.Person)
    object Settings : BottomNavScreen("settings", "Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToRideDetail: (RideRequest) -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val bottomNavItems = listOf(
        BottomNavScreen.Home,
        BottomNavScreen.Search,
        BottomNavScreen.Messages,
        BottomNavScreen.Profile,
        BottomNavScreen.Settings
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavScreen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavScreen.Home.route) {
                HomeScreen(
                    onNavigateToSettings = {
                        navController.navigate(BottomNavScreen.Settings.route)
                    },
                    onNavigateToProfile = {
                        navController.navigate(BottomNavScreen.Profile.route)
                    },
                    onLogout = onLogout,
                    onNavigateToMap = onNavigateToMap,
                    onNavigateToMessages = {
                        navController.navigate(BottomNavScreen.Messages.route)
                    },
                    onNavigateToRideDetail = onNavigateToRideDetail
                )
            }
            
            composable(BottomNavScreen.Search.route) {
                SearchScreen(
                    onNavigateToRideDetail = onNavigateToRideDetail
                )
            }
            
            composable(BottomNavScreen.Messages.route) {
                MessagesScreen(
                    onNavigateToChat = { conversationId ->
                        onNavigateToChat(conversationId)
                    }
                )
            }
            
            composable(BottomNavScreen.Profile.route) {
                ProfileScreen(
                    onNavigateBack = {
                        navController.navigate(BottomNavScreen.Home.route)
                    }
                )
            }
            
            composable(BottomNavScreen.Settings.route) {
                UserSettingsScreen(
                    onNavigateBack = {
                        navController.navigate(BottomNavScreen.Home.route)
                    }
                )
            }
        }
    }
}