package com.tripify.tripify_android.profile.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.tripify.tripify_android.profile.api.ProfileApiService

class SettingsViewModel(private val tokenManager: TokenManager, private val profileApi: ProfileApiService) : ViewModel() {

    val useMetricSystem: StateFlow<Boolean> = tokenManager.useMetricSystemFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val selectedCurrency: StateFlow<String> = tokenManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    val notificationsEnabled: StateFlow<Boolean> = tokenManager.notificationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val chatAlertsEnabled: StateFlow<Boolean> = tokenManager.chatAlertsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    var errorMessage by mutableStateOf<String?>(null)

    fun toggleMetricSystem(value: Boolean) {
        viewModelScope.launch { tokenManager.setUseMetricSystem(value) }
    }

    fun toggleCurrency() {
        viewModelScope.launch {
            val current = tokenManager.currencyFlow.first()
            tokenManager.setCurrency(if (current == "EUR") "USD" else "EUR")
        }
    }

    fun toggleNotifications(value: Boolean) {
        viewModelScope.launch { tokenManager.setNotificationsEnabled(value) }
    }

    fun toggleChatAlerts(value: Boolean) {
        viewModelScope.launch { tokenManager.setChatAlertsEnabled(value) }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            try {
                val response = profileApi.deleteMyAccount()

                if (response.isSuccessful || response.code() == 404) {
                    tokenManager.clearTokens()
                    onSuccess()
                } else {
                    errorMessage = "Impossibile eliminare l'account in questo momento (${response.code()}). Riprova."
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "deleteAccount fallita", e)
                errorMessage = "Errore di rete durante l'eliminazione. Riprova."
            }
        }
    }
}
