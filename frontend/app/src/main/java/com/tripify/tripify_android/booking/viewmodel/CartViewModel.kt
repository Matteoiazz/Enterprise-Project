package com.tripify.tripify_android.booking.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.booking.model.CartState
import com.tripify.tripify_android.data.RetrofitClient
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CartViewModel(private val tokenManager: TokenManager) : ViewModel() {

    // Creiamo l'istanza dell'API passandole il token per l'autorizzazione
    private val api = RetrofitClient.createBookingApi(tokenManager)

    // Variabile che contiene lo stato attuale (di default parte in Loading)
    private val _uiState = MutableStateFlow<CartState>(CartState.Loading)
    val uiState: StateFlow<CartState> = _uiState

    // Funzione che verrà chiamata dalla UI per caricare il carrello
    fun fetchCart(userId: String) {
        viewModelScope.launch {
            _uiState.value = CartState.Loading
            try {
                val response = api.getCartForUser(userId)

                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = CartState.Success(response.body()!!)
                } else {
                    _uiState.value = CartState.Error("Errore nel caricamento del carrello: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = CartState.Error("Nessuna connessione: ${e.message}")
            }
        }
    }
}