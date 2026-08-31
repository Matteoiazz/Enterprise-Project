package com.tripify.tripify_android.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.model.PaymentMethodDto
import com.tripify.tripify_android.profile.api.ProfileApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.YearMonth

class PaymentMethodsViewModel(private val apiService: ProfileApiService) : ViewModel() {

    private val _paymentMethods = MutableStateFlow<List<PaymentMethodDto>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethodDto>> = _paymentMethods.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadPaymentMethods() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _paymentMethods.value = apiService.getPaymentMethods()
            } catch (e: Exception) {
                _errorMessage.value = "Errore nel caricamento carte."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun addPaymentMethod(
        provider: String, cardNumber: String, expiration: String, onSuccess: () -> Unit
    ) {
        if (provider.isBlank() || cardNumber.isBlank() || expiration.isBlank()) {
            _errorMessage.value = "Tutti i campi sono obbligatori"
            return
        }

        try {

            val parts = expiration.split("/")
            if (parts.size != 2 || parts[0].length != 2 || parts[1].length != 2) {
                _errorMessage.value = "Formato scadenza non valido. Usa MM/AA."
                return
            }

            val month = parts[0].toInt()
            if (month !in 1..12) {
                _errorMessage.value = "Formato scadenza non valido. Usa MM/AA."
                return
            }
            val year = parts[1].toInt() + 2000

            val cardYM = YearMonth.of(year, month)
            if (cardYM.isBefore(YearMonth.now())) {
                _errorMessage.value = "Impossibile salvare: la carta è scaduta."
                return
            }
        } catch (e: Exception) {
            _errorMessage.value = "Formato scadenza non valido."
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCard = PaymentMethodDto(
                    id = null,
                    cardProvider = provider,
                    cardNumber = cardNumber,
                    expirationMonthYear = expiration
                )

                apiService.addPaymentMethod(newCard)
                loadPaymentMethods()
                onSuccess()
            } catch (e: HttpException) {
                val serverMessage = try { e.response()?.errorBody()?.string() } catch (ex: Exception) { null }
                _errorMessage.value = if (!serverMessage.isNullOrBlank()) serverMessage else "Errore durante il salvataggio."
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante il salvataggio."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePaymentMethod(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                apiService.deletePaymentMethod(id)
                loadPaymentMethods()
            } catch (e: Exception) {
                _errorMessage.value = "Errore durante l'eliminazione."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

class PaymentMethodsViewModelFactory(private val apiService: ProfileApiService) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentMethodsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PaymentMethodsViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}