package com.tripify.tripify_android.profile.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.EndSessionRequest

class ProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    var name by mutableStateOf("")
    var surname by mutableStateOf("")
    var email by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoggedOut by mutableStateOf(false)

    private val api = RetrofitClient.createApi(tokenManager)

    fun loadUserProfile() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = api.getCurrentUser()

                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    name = user.name ?: ""
                    surname = user.surname ?: ""
                    email = user.email
                } else {
                    errorMessage = "Impossibile recuperare i dati. Errore: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Errore: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    suspend fun getIdToken(): String? {
        return tokenManager.getIdToken()
    }

    fun getEndSessionIntent(context: Context, idToken: String): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/auth"),
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/token")
        )

        val endSessionEndpoint = Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/logout")

        val endSessionConfig = AuthorizationServiceConfiguration(
            serviceConfig.authorizationEndpoint,
            serviceConfig.tokenEndpoint,
            null,
            endSessionEndpoint
        )

        val endSessionRequest = EndSessionRequest.Builder(endSessionConfig)
            .setIdTokenHint(idToken)
            .setPostLogoutRedirectUri(Uri.parse("com.tripify.app://oauth"))
            .build()

        val authService = AuthorizationService(context)
        return authService.getEndSessionRequestIntent(endSessionRequest)
    }

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearTokens()
            isLoggedOut = true
        }
    }
}