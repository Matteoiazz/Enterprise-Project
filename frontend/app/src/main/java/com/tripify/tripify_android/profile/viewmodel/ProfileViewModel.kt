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
import com.tripify.tripify_android.data.model.UpdateProfileRequest

class ProfileViewModel(private val tokenManager: TokenManager) : ViewModel() {

    var name by mutableStateOf("")
    var surname by mutableStateOf("")
    var email by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoggedOut by mutableStateOf(false)

    private val api = RetrofitClient.createApi(tokenManager)

    private val profileApi = RetrofitClient.createProfileApi(tokenManager)

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

    fun updateProfile(
        newName: String,
        newSurname: String,
        newPhone: String,
        newAddress: String,
        newEmail: String,
        newPassword: String?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                val request = UpdateProfileRequest(
                    name = newName.ifBlank { null },
                    surname = newSurname.ifBlank { null },
                    phone = newPhone.ifBlank { null },
                    address = newAddress.ifBlank { null },
                    email = newEmail.ifBlank { null },
                    newPassword = if (newPassword.isNullOrBlank()) null else newPassword
                )

                val response = profileApi.updateProfile(request)

                if (response.isSuccessful) {
                    if (newName.isNotBlank()) name = newName
                    if (newSurname.isNotBlank()) surname = newSurname
                    if (newEmail.isNotBlank()) email = newEmail
                    onSuccess()
                } else {
                    errorMessage = "Errore durante il salvataggio: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                errorMessage = "Errore di rete: ${e.localizedMessage}"
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