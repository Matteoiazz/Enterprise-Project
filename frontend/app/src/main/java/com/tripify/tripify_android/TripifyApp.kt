package com.tripify.tripify_android

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.core.navigation.Route

// Importiamo il lavoro
import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.ui.RegisterScreen
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel
import com.tripify.tripify_android.booking.ui.BookingsScreen
import com.tripify.tripify_android.catalog.ui.DetailScreen

// Import Profilo
import com.tripify.tripify_android.profile.ui.ProfileScreen
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

// Import Catalogo
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.profile.ui.CompanionsScreen
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel

import com.tripify.tripify_android.profile.ui.TravelDocumentsScreen
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    catalogViewModel: CatalogViewModel,
    profileViewModel: ProfileViewModel,
    companionsViewModel: CompanionsViewModel,
    travelDocumentsViewModel: TravelDocumentsViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Home.path
    ) {

        // ROTTA 1: La Vetrina (Catalogo)
        composable(Route.Home.path) {
            HomeScreen(
                viewModel = catalogViewModel,
                onNavigateToAuth = {
                    navController.navigate(Route.Auth.path)
                },
                onNavigateToDetail = { itemId ->
                    navController.navigate("detail/$itemId")
                },
                onNavigateToProfile = {
                    navController.navigate(Route.Profile.path)
                },
                onNavigateToBookings = {
                    navController.navigate(Route.Bookings.path)
                }
            )
        }

        // ROTTA 2: La schermata di Login
        composable(Route.Auth.path) {
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToCatalog = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Route.Register.path)
                }
            )
        }

        // ROTTA 3: La schermata di Registrazione
        composable(Route.Register.path) {
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToCatalog = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ROTTA 4: La schermata di Dettaglio
        composable(route = "detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId") ?: ""

            DetailScreen(
                itemId = itemId,
                viewModel = catalogViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onBookNow = { clickedItemId ->
                    println("Inizio prenotazione per l'ID: $clickedItemId")
                }
            )
        }

        // ROTTA 5: La schermata Profilo UNIFICATA
        composable(Route.Profile.path) {
            ProfileScreen(
                viewModel = profileViewModel,
                onLogoutSuccess = {
                    navController.navigate(Route.Auth.path) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Route.Auth.path)
                },
                onNavigateToCompanions = {
                    navController.navigate(Route.Companions.path)
                },
                onNavigateToTravelDocuments = {
                    navController.navigate(Route.TravelDocuments.path)
                }
            )
        }

        // ROTTA 6: Compagni di Viaggio
        composable(Route.Companions.path) {
            CompanionsScreen(
                viewModel = companionsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ROTTA 7: Le mie Prenotazioni
        composable(Route.Bookings.path) {
            BookingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Route.TravelDocuments.path) {
            TravelDocumentsScreen(
                viewModel = travelDocumentsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}