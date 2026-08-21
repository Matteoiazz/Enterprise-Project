package com.tripify.tripify_android.auth.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object LocalConnectionBuilder : ConnectionBuilder {
    override fun openConnection(uri: Uri): HttpURLConnection {
        val connection = URL(uri.toString()).openConnection() as HttpURLConnection
        connection.connectTimeout = TimeUnit.SECONDS.toMillis(15).toInt()
        connection.readTimeout = TimeUnit.SECONDS.toMillis(10).toInt()
        connection.instanceFollowRedirects = false
        return connection
    }
}

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoginSuccessful by mutableStateOf(false)

    private val clientId = "tripify-android-client"
    private val redirectUri = Uri.parse("com.tripify.app://oauth")

    private val appAuthConfig = AppAuthConfiguration.Builder()
        .setConnectionBuilder(LocalConnectionBuilder)
        .build()

    fun getAuthorizationIntent(context: Context): Intent {
        val serviceConfig = AuthorizationServiceConfiguration(
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/auth"),
            Uri.parse("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/token")
        )

        val authRequest = AuthorizationRequest.Builder(
            serviceConfig,
            clientId,
            ResponseTypeValues.CODE,
            redirectUri
        ).setScopes("openid", "profile", "email").setPrompt("select_account").build()

        val authService = AuthorizationService(context, appAuthConfig)
        return authService.getAuthorizationRequestIntent(authRequest)
    }

    fun handleAuthorizationResponse(context: Context, intent: Intent?) {
        if (intent == null) {
            errorMessage = "Login annullato"
            return
        }

        val response = AuthorizationResponse.fromIntent(intent)
        val exception = AuthorizationException.fromIntent(intent)

        if (exception != null) {
            errorMessage = "Errore durante il login: ${exception.message}"
            return
        }

        if (response != null) {
            isLoading = true
            val authService = AuthorizationService(context, appAuthConfig)
            authService.performTokenRequest(
                response.createTokenExchangeRequest()
            ) { tokenResponse, tokenException ->
                if (tokenException != null) {
                    errorMessage = "Errore nel recupero del token"
                    isLoading = false
                } else if (tokenResponse != null) {
                    viewModelScope.launch {
                        tokenManager.saveToken(tokenResponse.accessToken ?: "")
                        tokenManager.saveIdToken(tokenResponse.idToken ?: "")
                        isLoginSuccessful = true
                        isLoading = false
                    }
                }
            }
        }
    }
}