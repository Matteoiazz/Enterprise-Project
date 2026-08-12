package com.tripify.tripify_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel // <-- IMPORT AGGIUNTO PER IL VIEWMODEL IN COMPOSE

// Import fondamentali
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel

// Import per il Profilo e Documenti
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val context = LocalContext.current

                val tokenManager = remember { TokenManager(context) }

                val loginViewModel = remember { LoginViewModel(tokenManager) }
                val registerViewModel = remember { RegisterViewModel(tokenManager) }

                val profileViewModel = remember { ProfileViewModel(tokenManager) }

                val catalogApi = remember { RetrofitClient.createCatalogApi(tokenManager) }
                val catalogViewModel = remember { CatalogViewModel(catalogApi) }

                // DICHIARIAMO L'API DEL PROFILO UNA SOLA VOLTA
                val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }
                val companionsViewModel = remember { CompanionsViewModel(profileApi) }

                // INIZIALIZZAZIONE CORRETTA DEL VIEWMODEL PER COMPOSE
                val travelDocumentsViewModel: TravelDocumentsViewModel = viewModel(
                    factory = TravelDocumentsViewModelFactory(profileApi)
                )

                TripifyApp(
                    loginViewModel = loginViewModel,
                    registerViewModel = registerViewModel,
                    catalogViewModel = catalogViewModel,
                    profileViewModel = profileViewModel,
                    companionsViewModel = companionsViewModel,
                    travelDocumentsViewModel = travelDocumentsViewModel
                )
            }
        }
    }
}