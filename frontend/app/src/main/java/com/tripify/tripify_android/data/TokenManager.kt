package com.tripify.tripify_android.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tripify.tripify_android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        // Istanza unica: il DataStore "auth_prefs" e' gia' un singleton di
        // fatto, ma RetrofitClient cachea l'OkHttpClient legato al PRIMO
        // TokenManager che riceve. Se ogni schermata ne creava uno nuovo con
        // "TokenManager(context)", l'interceptor restava quello della prima
        // istanza. getInstance() garantisce che sia sempre lo stesso oggetto.
        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }

        val JWT_TOKEN_KEY = stringPreferencesKey("jwt_token")
        val ID_TOKEN_KEY = stringPreferencesKey("id_token")
        val REFRESH_TOKEN_KEY = stringPreferencesKey("refresh_token") // 👉 AGGIUNTO

        val METRIC_SYSTEM_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("use_metric_system")
        val CURRENCY_KEY = androidx.datastore.preferences.core.stringPreferencesKey("selected_currency")
        val NOTIFICATIONS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("notifications_enabled")
        val CHAT_ALERTS_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("chat_alerts_enabled")
        private val refreshMutex = Mutex()

        private const val CLIENT_ID = "tripify-android-client"
        private val TOKEN_ENDPOINT: String
            get() = "${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/token"

        private fun tokenHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    sealed class RefreshResult {
        data class Success(val accessToken: String) : RefreshResult()
        object InvalidGrant : RefreshResult()
        object TransientError : RefreshResult()
    }

    sealed class CodeExchangeResult {
        data class Success(val accessToken: String) : CodeExchangeResult()
        data class Failure(val detail: String) : CodeExchangeResult()
    }

    suspend fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String?,
        redirectUri: String
    ): CodeExchangeResult = withContext(Dispatchers.IO) {
        try {
            val form = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("client_id", CLIENT_ID)
                .add("code", code)
                .add("redirect_uri", redirectUri)
            if (!codeVerifier.isNullOrEmpty()) form.add("code_verifier", codeVerifier)

            val request = Request.Builder().url(TOKEN_ENDPOINT).post(form.build()).build()
            val response = tokenHttpClient().newCall(request).execute()
            val body = response.body?.string()

            if (response.isSuccessful && body != null) {
                val json = JSONObject(body)
                val accessToken = json.getString("access_token")
                val refreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else ""
                val idToken = if (json.has("id_token")) json.getString("id_token") else ""
                if (accessToken.isBlank() || refreshToken.isBlank()) {
                    return@withContext CodeExchangeResult.Failure("Il server non ha restituito una sessione completa")
                }
                saveSession(accessToken, idToken, refreshToken)
                CodeExchangeResult.Success(accessToken)
            } else {
                val reason = body?.let { runCatching { JSONObject(it).optString("error_description").ifBlank { JSONObject(it).optString("error") } }.getOrNull() }
                CodeExchangeResult.Failure(reason?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}")
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            CodeExchangeResult.Failure(e.message ?: "errore di rete verso Keycloak")
        }
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

    suspend fun saveSession(accessToken: String, idToken: String, refreshToken: String) {
        context.dataStore.edit {
            it[JWT_TOKEN_KEY] = accessToken
            it[ID_TOKEN_KEY] = idToken
            it[REFRESH_TOKEN_KEY] = refreshToken
        }
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

    suspend fun refreshAccessToken(previousAccessToken: String? = null): RefreshResult = refreshMutex.withLock {
        val currentAccessToken = tokenFlow.first()
        if (!currentAccessToken.isNullOrEmpty() && previousAccessToken != null && currentAccessToken != previousAccessToken) {
            return@withLock RefreshResult.Success(currentAccessToken)
        }

        val refreshToken = getRefreshToken()?.takeIf { it.isNotEmpty() }
            ?: return@withLock RefreshResult.InvalidGrant

        withContext(Dispatchers.IO) {
            try {
                val requestBody = FormBody.Builder()
                    .add("grant_type", "refresh_token")
                    .add("client_id", CLIENT_ID)
                    .add("refresh_token", refreshToken)
                    .build()

                val request = Request.Builder()
                    .url(TOKEN_ENDPOINT)
                    .post(requestBody)
                    .build()

                val response = tokenHttpClient().newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && body != null) {
                    val json = JSONObject(body)
                    val newAccessToken = json.getString("access_token")
                    val newRefreshToken = if (json.has("refresh_token")) json.getString("refresh_token") else refreshToken
                    saveToken(newAccessToken)
                    saveRefreshToken(newRefreshToken)
                    RefreshResult.Success(newAccessToken)
                } else if (response.code == 400 || response.code == 401 || body?.contains("invalid_grant") == true) {
                    RefreshResult.InvalidGrant
                } else {
                    RefreshResult.TransientError
                }
            } catch (e: Exception) {
                e.printStackTrace()
                RefreshResult.TransientError
            }
        }
    }
}
