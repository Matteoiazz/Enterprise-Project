package com.tripify.tripify_android.data

import com.tripify.tripify_android.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.tokenFlow.first() }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401|| response.code == 403) {
            response.close()

            val refreshToken = runBlocking { tokenManager.getRefreshToken() }
            val newAccessToken = refreshToken?.takeIf { it.isNotEmpty() }?.let { refreshAccessToken(it) }

            if (newAccessToken != null) {
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                return chain.proceed(newRequest)
            }

            // Nessun refresh token, o refresh fallito: il token salvato non e' piu'
            // valido. Va pulito subito, altrimenti resta riattaccato a ogni
            // richiesta futura (anche verso endpoint pubblici) e continua a
            // far fallire tutto finche' l'utente non cancella i dati dell'app.
            runBlocking { tokenManager.clearTokens() }
        }

        return response
    }

    private fun refreshAccessToken(refreshToken: String): String? {
        try {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("client_id", "tripify-android-client")
                .add("refresh_token", refreshToken)
                .build()

            val request = Request.Builder()
                .url("${BuildConfig.KEYCLOAK_BASE_URL}/realms/tripify/protocol/openid-connect/token")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return null
                val json = JSONObject(responseBody)

                val newAccessToken = json.getString("access_token")
                val newRefreshToken = json.getString("refresh_token")

                runBlocking {
                    tokenManager.saveToken(newAccessToken)
                    tokenManager.saveRefreshToken(newRefreshToken)
                }
                return newAccessToken
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}