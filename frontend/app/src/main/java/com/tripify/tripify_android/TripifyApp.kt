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
import com.tripify.tripify_android.catalog.ui.DetailScreen

// Import Profilo
import com.tripify.tripify_android.profile.ui.ProfileScreen
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

// Import Catalogo
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.chat.ui.ChatScreen
import com.tripify.tripify_android.chat.viewmodel.ChatViewModel

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel,
    catalogViewModel: CatalogViewModel,
    profileViewModel: ProfileViewModel,
    chatViewModel: ChatViewModel
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
                    navController.navigate("detail/$itemId") // Qui lo lasciamo a stringa per via del parametro
                },
                onNavigateToProfile = { // <-- Assicurati di aggiungere questo parametro in HomeScreen!
                    navController.navigate(Route.Profile.path)
                },
                onNavigateToChat = {
                    navController.navigate("chat") // <-- AGGIUNGI QUESTO
                }
            )
        }

        // ROTTA 2: La schermata di Login
        composable(Route.Auth.path) {
            LoginScreen(
                viewModel = loginViewModel,
                onNavigateToCatalog = {
                    // Dopo il login, vai alla Home e cancella la rotta Auth dallo stack
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Auth.path) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Route.Register.path) // Usiamo la sealed class
                }
            )
        }

        // ROTTA 3: La schermata di Registrazione
        composable(Route.Register.path) { // Usiamo la sealed class
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToCatalog = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(0) { inclusive = true } // Pulisce tutto se la registrazione è ok
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
                    navController.popBackStack() // Fa tornare l'utente indietro alla Home
                },
                onBookNow = { clickedItemId ->
                    // TODO per Mattia: L'utente ha premuto PRENOTA!
                    // Usa clickedItemId per aggiungerlo al Booking/Itinerary.
                    // es: navController.navigate("booking/$clickedItemId")
                    println("Inizio prenotazione per l'ID: $clickedItemId")
                }
            )
        }

        // ROTTA 5: La schermata Profilo <-- NUOVA ROTTA
        composable(Route.Profile.path) {
            ProfileScreen(
                viewModel = profileViewModel,
                onLogoutSuccess = {
                    // Svuota l'intera cronologia per non far tornare indietro col tasto back e naviga al Login
                    navController.navigate(Route.Auth.path) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        // ROTTA 6: La schermata Chat
        composable("chat") {
            ChatScreen(viewModel = chatViewModel, onBackClick = { navController.popBackStack() })
        }
    }
}