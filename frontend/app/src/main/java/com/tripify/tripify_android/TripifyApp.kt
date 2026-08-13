package com.tripify.tripify_android

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.core.navigation.Route

import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.ui.RegisterScreen
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel
import com.tripify.tripify_android.catalog.ui.DetailScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.unit.dp
import com.tripify.tripify_android.profile.ui.ProfileScreen
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

private val Ink = Color(0xFF1A1A1A)
private val InkMuted = Color(0xFF7A7A73)
private val TripifyGreen = Color(0xFF2E7D32) // Sostituisci con il tuo verde esatto se diverso

data class BottomNavItem(val route: String, val title: String, val icon: ImageVector)

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    catalogViewModel: CatalogViewModel,
    profileViewModel: ProfileViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(Route.Home.path, "Home", Icons.Filled.Home),
        BottomNavItem("saved", "Salvati", Icons.Filled.FavoriteBorder),
        BottomNavItem("chat", "Messaggi", Icons.Filled.ChatBubbleOutline),
        BottomNavItem("bookings", "Prenotazioni", Icons.Filled.ConfirmationNumber),
        BottomNavItem(Route.Profile.path, "Profilo", Icons.Filled.PersonOutline)
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Ink,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = {
                                Text(
                                    item.title,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(item.route) {
                                        popUpTo(Route.Home.path) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TripifyGreen,
                                selectedTextColor = TripifyGreen,
                                unselectedIconColor = InkMuted,
                                unselectedTextColor = InkMuted,
                                indicatorColor = TripifyGreen.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Home.path,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Route.Home.path) {
                HomeScreen(
                    viewModel = catalogViewModel,
                    onNavigateToAuth = { navController.navigate(Route.Auth.path) },
                    onNavigateToDetail = { itemId -> navController.navigate("detail/$itemId") }
                )
            }

            composable("saved") { Box { Text("Salvati (In costruzione)") } }
            composable("chat") { Box { Text("Messaggi (In costruzione)") } }
            composable("bookings") { Box { Text("Prenotazioni (In costruzione)") } }

            composable(Route.Profile.path) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onLogoutSuccess = {
                        navController.navigate(Route.Auth.path) {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Route.Auth.path) {
                LoginScreen(
                    viewModel = loginViewModel,
                    onNavigateToCatalog = {
                        navController.navigate(Route.Home.path) { popUpTo(Route.Auth.path) { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate(Route.Register.path) }
                )
            }

            composable(Route.Register.path) {
                RegisterScreen(
                    viewModel = registerViewModel,
                    onNavigateToCatalog = {
                        navController.navigate(Route.Home.path) { popUpTo(0) { inclusive = true } }
                    },
                    onNavigateBackToLogin = { navController.popBackStack() }
                )
            }

            composable(route = "detail/{itemId}") { backStackEntry ->
                val itemId = backStackEntry.arguments?.getString("itemId") ?: ""
                DetailScreen(
                    itemId = itemId,
                    viewModel = catalogViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onBookNow = { clickedItemId -> println("Inizio prenotazione per l'ID: $clickedItemId") },
                    onChatWithOrganizer = { clickedItemId -> println("Apertura chat WebSocket per ID: $clickedItemId") } // Gancio per il collega
                )
            }
        }
    }
}