package com.tripify.tripify_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// Import fondamentali
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current

                // 1. Inizializziamo il manager del token e il ViewModel di Dario (Login)
                val tokenManager = remember { TokenManager(context) }
                val loginViewModel = remember { LoginViewModel(tokenManager) }

                // 2. NUOVO: Creiamo l'API del catalogo e il nostro ViewModel!
                val catalogApi = remember { RetrofitClient.createCatalogApi(tokenManager) }
                val catalogViewModel = remember { CatalogViewModel(catalogApi) }

                // 3. Passiamo ENTRAMBI i ViewModel al nostro "Cervello" centrale
                TripifyApp(
                    loginViewModel = loginViewModel,
                    catalogViewModel = catalogViewModel
                )
            }
        }
    }
}