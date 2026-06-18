package com.tripify.tripify_android.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch

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

    fun logout() {
        viewModelScope.launch {
            tokenManager.clearToken()
            isLoggedOut = true
        }
    }
}