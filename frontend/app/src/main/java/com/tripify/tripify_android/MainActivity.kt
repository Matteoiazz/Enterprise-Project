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
import com.tripify.tripify_android.auth.viewmodel.RegisterViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel

// 1. NUOVO IMPORT per il Profilo
import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel

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

                val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }
                val companionsViewModel = remember { CompanionsViewModel(profileApi) }

                TripifyApp(
                    loginViewModel = loginViewModel,
                    registerViewModel = registerViewModel,
                    catalogViewModel = catalogViewModel,
                    profileViewModel = profileViewModel,
                    companionsViewModel = companionsViewModel
                )
            }
        }
    }
}