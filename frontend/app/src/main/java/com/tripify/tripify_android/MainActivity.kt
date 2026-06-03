package com.tripify.tripify_android // o il tuo package di base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tripify.tripify_android.catalog.ui.HybridHomeScreen
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.auth.ui.LoginScreen
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current

                // Inizializziamo il manager del token e il ViewModel
                val tokenManager = remember { TokenManager(context) }
                val loginViewModel = remember { LoginViewModel(tokenManager) }

                // 1. Creiamo il controller della navigazione
                val navController = rememberNavController()

                // 2. Definiamo la mappa delle schermate (NavHost)
                NavHost(navController = navController, startDestination = "login") {

                    // Rotta A: Schermata di Login
                    composable("login") {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onNavigateToCatalog = {
                                // Quando il login ha successo, naviga verso il catalogo...
                                navController.navigate("catalog") {
                                    // ...e rimuovi il login dalla cronologia (BackStack)
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Rotta B: Il Catalogo principale
                    composable("catalog") {
                        // Richiamiamo la schermata creata dal tuo collega
                        HybridHomeScreen()
                    }
                }
            }
        }
    }
}