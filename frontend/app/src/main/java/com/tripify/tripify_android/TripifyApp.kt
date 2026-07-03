package com.tripify.tripify_android

import androidx.compose.material3.Text // <-- Import aggiunto per il test
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
import com.tripify.tripify_android.catalog.ui.DetailScreen

// Importiamo il nuovo ViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    catalogViewModel: CatalogViewModel
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
                onNavigateToDetail = { itemId -> // <-- RICEVE L'ID E APRE IL DETTAGLIO
                    navController.navigate("detail/$itemId")
                }
            )
        }

        // ROTTA 2: La schermata di Login
        composable(Route.Auth.path) {
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToCatalog = {
                    navController.popBackStack() // Torna alla Home
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        // ROTTA 3: La schermata di Registrazione
        composable("register") {
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToCatalog = {
                    navController.popBackStack(Route.Home.path, inclusive = false)
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
                viewModel = catalogViewModel, // (usa il nome della tua variabile qui)
                onNavigateBack = {
                    navController.popBackStack() // Fa tornare l'utente indietro alla Home
                },
                onBookNow = { clickedItemId -> // <-- ECCO IL PARAMETRO MANCANTE!
                    // TODO per Mattia: L'utente ha premuto PRENOTA!
                    // Usa clickedItemId per aggiungerlo al Booking/Itinerary.
                    // es: navController.navigate("booking/$clickedItemId")
                    println("Inizio prenotazione per l'ID: $clickedItemId")
                }
            )
        }
    }
}