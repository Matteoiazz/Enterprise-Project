package com.tripify.tripify_android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.auth.viewmodel.LoginViewModel
import com.tripify.tripify_android.catalog.viewmodel.CatalogViewModel
import com.tripify.tripify_android.chat.viewmodel.ChatViewModel
import com.tripify.tripify_android.notification.data.NotificationRepository
import com.tripify.tripify_android.notification.viewmodel.NotificationViewModel
import com.tripify.tripify_android.notification.viewmodel.NotificationViewModelFactory
import com.tripify.tripify_android.profile.viewmodel.CompanionsViewModel
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModel
import com.tripify.tripify_android.profile.viewmodel.PaymentMethodsViewModelFactory

import com.tripify.tripify_android.profile.viewmodel.ProfileViewModel
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModel
import com.tripify.tripify_android.profile.viewmodel.TravelDocumentsViewModelFactory

class MainActivity : ComponentActivity() {
    private var pendingDeepLinkIntent by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingDeepLinkIntent = intent
        setContent {
            MaterialTheme {
                val context = LocalContext.current

                val tokenManager = remember { TokenManager(context) }

                val loginViewModel = remember { LoginViewModel(tokenManager) }

                val profileViewModel = remember { ProfileViewModel(tokenManager) }

                val catalogApi = remember { RetrofitClient.createCatalogApi(tokenManager) }
                val catalogViewModel = remember { CatalogViewModel(catalogApi, tokenManager) }

                val profileApi = remember { RetrofitClient.createProfileApi(tokenManager) }
                val companionsViewModel = remember { CompanionsViewModel(profileApi) }

                val notificationApi = remember { RetrofitClient.createNotificationApi(tokenManager) }
                val notificationRepository = remember { NotificationRepository(notificationApi) }
                val notificationViewModel: NotificationViewModel = viewModel(
                    factory = NotificationViewModelFactory(
                        repository = notificationRepository,
                        tokenManager = tokenManager
                    )
                )

                val travelDocumentsViewModel: TravelDocumentsViewModel = viewModel(
                    factory = TravelDocumentsViewModelFactory(profileApi)
                )
                val paymentMethodsViewModel: PaymentMethodsViewModel = viewModel(
                    factory = PaymentMethodsViewModelFactory(profileApi)
                )

                val settingsViewModel = remember {
                    com.tripify.tripify_android.profile.viewmodel.SettingsViewModel(tokenManager, profileApi)
                }
                TripifyApp(
                    loginViewModel = loginViewModel,
                    catalogViewModel = catalogViewModel,
                    profileViewModel = profileViewModel,
                    companionsViewModel = companionsViewModel,
                    travelDocumentsViewModel = travelDocumentsViewModel,
                    paymentMethodsViewModel = paymentMethodsViewModel,
                    settingsViewModel = settingsViewModel,
                    notificationViewModel = notificationViewModel,
                    pendingDeepLinkIntent = pendingDeepLinkIntent,
                    onDeepLinkHandled = { pendingDeepLinkIntent = null }
                )
            }
        }
    }

    // launchMode="singleTask": ad app già avviata un link non ricrea l'Activity,
    // arriva qui invece che in onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLinkIntent = intent
    }
}