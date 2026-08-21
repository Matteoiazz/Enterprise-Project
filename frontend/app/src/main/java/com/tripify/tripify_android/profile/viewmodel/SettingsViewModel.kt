package com.tripify.tripify_android.profile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.data.TokenManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun toggleMetricSystem(value: Boolean) {
        viewModelScope.launch { tokenManager.setUseMetricSystem(value) }
    }

    fun toggleCurrency() {
        viewModelScope.launch {
            val newCurrency = if (selectedCurrency.value == "EUR") "USD" else "EUR"
            tokenManager.setCurrency(newCurrency)
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
            try {
                val response = profileApi.deleteMyAccount()

                if (response.isSuccessful) {
                    tokenManager.clearTokens()
                    onSuccess()
                } else {
                    println("Errore durante l'eliminazione dell'account: ${response.code()}")
                }

            } catch (e: Exception) {
                println("Eccezione durante la chiamata di eliminazione:")
                e.printStackTrace()
            }
        }
    }
}