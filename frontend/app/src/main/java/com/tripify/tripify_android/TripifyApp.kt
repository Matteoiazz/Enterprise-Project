package com.tripify.tripify_android

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripify.tripify_android.catalog.ui.HomeScreen
import com.tripify.tripify_android.core.navigation.Route

@Composable
fun TripifyApp() {
    // Il NavController è il "pilota" che ci sposta da una schermata all'altra
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Route.Home.path
    ) {
        // ROTTA 1: La tua Vetrina
        composable(Route.Home.path) {
            HomeScreen(
                onNavigateToAuth = {
                    navController.navigate(Route.Auth.path)
                }
            )
        }

        // ROTTA 2: Schermata di Login (Provvisoria)
        composable(Route.Auth.path) {
            // Qui i tuoi colleghi inseriranno la vera schermata di Auth
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Schermata di Login/Registrazione In Costruzione 🚧")
            }
        }
    }
}