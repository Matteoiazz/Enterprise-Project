package com.tripify.tripify_android.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.RegisterRequest
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.launch

class RegisterViewModel(private val tokenManager: TokenManager) : ViewModel() {

    // Stati dei campi di testo
    var firstName by mutableStateOf("")
    var lastName by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var confirmPassword by mutableStateOf("")

    // Stati di caricamento ed errore
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var isRegistrationSuccessful by mutableStateOf(false)

    // Inizializza l'API
    private val api = RetrofitClient.createApi(tokenManager)

    fun performRegistration() {
        // 1. Controlli base lato client
        if (firstName.isBlank() || lastName.isBlank() || email.isBlank() || password.isBlank()) {
            errorMessage = "Compila tutti i campi"
            return
        }

        if (password != confirmPassword) {
            errorMessage = "Le password non coincidono"
            return
        }

        // 2. Chiamata di rete
        viewModelScope.launch {
            isLoading = true
            errorMessage = null

            try {
                // IL FIX È QUI: associamo le variabili locali ai nomi esatti richiesti dal backend
                val response = api.register(
                    RegisterRequest(
                        name = firstName,
                        surname = lastName,
                        email = email,
                        password = password
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    // Successo! Salviamo il token e diamo il via libera
                    tokenManager.saveToken(response.body()!!.token)
                    isRegistrationSuccessful = true
                } else {
                    errorMessage = "Errore durante la registrazione. Forse l'email esiste già?"
                }
            } catch (e: Exception) {
                errorMessage = "Impossibile connettersi al server"
            } finally {
                isLoading = false
            }
        }
    }
}