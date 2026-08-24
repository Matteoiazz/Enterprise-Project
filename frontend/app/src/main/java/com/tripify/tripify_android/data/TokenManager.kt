package com.tripify.tripify_android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        val ID_TOKEN_KEY = stringPreferencesKey("id_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token") // 👉 AGGIUNTO

        val METRIC_SYSTEM_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("use_metric_system")
        val CURRENCY_KEY = androidx.datastore.preferences.core.stringPreferencesKey("selected_currency")
        val NOTIFICATIONS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        val CHAT_ALERTS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("chat_alerts_enabled")
    }

    val useMetricSystemFlow: Flow<Boolean> = context.dataStore.data.map { it[METRIC_SYSTEM_KEY] ?: true }
    val currencyFlow: Flow<String> = context.dataStore.data.map { it[CURRENCY_KEY] ?: "EUR" }
    val notificationsFlow: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_KEY] ?: true }
    val chatAlertsFlow: Flow<Boolean> = context.dataStore.data.map { it[CHAT_ALERTS_KEY] ?: true }

    suspend fun setUseMetricSystem(value: Boolean) { context.dataStore.edit { it[METRIC_SYSTEM_KEY] = value } }
    suspend fun setCurrency(currency: String) { context.dataStore.edit { it[CURRENCY_KEY] = currency } }
    suspend fun setNotificationsEnabled(enabled: Boolean) { context.dataStore.edit { it[NOTIFICATIONS_KEY] = enabled } }
    suspend fun setChatAlertsEnabled(enabled: Boolean) { context.dataStore.edit { it[CHAT_ALERTS_KEY] = enabled } }

    suspend fun saveToken(token: String) {
        context.dataStore.edit { it[JWT_TOKEN_KEY] = token }
    }

    suspend fun saveIdToken(idToken: String) {
        context.dataStore.edit { it[ID_TOKEN_KEY] = idToken }
    }

    suspend fun saveRefreshToken(refreshToken: String) {
        context.dataStore.edit { it[REFRESH_TOKEN_KEY] = refreshToken }
    }

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[JWT_TOKEN_KEY] }

    suspend fun getIdToken(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[ID_TOKEN_KEY]
    }

    suspend fun getRefreshToken(): String? {
        val preferences = context.dataStore.data.first()
        return preferences[REFRESH_TOKEN_KEY]
    }

    suspend fun clearTokens() {
        context.dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN_KEY)
            preferences.remove(ID_TOKEN_KEY)
            preferences.remove(REFRESH_TOKEN_KEY)
        }
    }
}