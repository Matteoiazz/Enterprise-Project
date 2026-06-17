package com.tripify.tripify_android

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.core.navigation.Route

// Importiamo il lavoro
import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.ui.RegisterScreen // <-- AGGIUNTO
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel // <-- AGGIUNTO

// Importiamo il nuovo ViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

@Composable
fun TripifyApp(
    loginViewModel: LoginViewModel,
    registerViewModel: RegisterViewModel, // <-- AGGIUNTO
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
                onNavigateToRegister = { // <-- AGGIUNTO
                    // Uso la stringa "register", ma se vuoi puoi aggiungerla in Route.kt
                    navController.navigate("register")
                }
            )
        }

        // ROTTA 3: La schermata di Registrazione <-- NUOVA ROTTA
        composable("register") {
            RegisterScreen(
                viewModel = registerViewModel,
                onNavigateToCatalog = {
                    // Se la registrazione va a buon fine, torna alla Home
                    // popUpTo assicura di non lasciare la schermata di login appesa in memoria
                    navController.popBackStack(Route.Home.path, inclusive = false)
                },
                onNavigateBackToLogin = {
                    // L'utente ha cliccato "Hai già un account? Accedi"
                    navController.popBackStack()
                }
            )
        }
    }
}