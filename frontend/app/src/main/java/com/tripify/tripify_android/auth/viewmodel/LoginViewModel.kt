package com.tripify.tripify_android.auth.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.LoginRequest
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")

    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isLoginSuccessful by mutableStateOf(false)

    private val api = RetrofitClient.createApi(tokenManager)

    fun performLogin() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Inserisci email e password"
            return
        }

        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                val response = api.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    tokenManager.saveToken(response.body()!!.token)
                    isLoginSuccessful = true
                } else {
                    errorMessage = "Credenziali errate"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile connettersi al server"
            } finally {
                isLoading = false
            }
        }
    }
}