package com.tripify.tripify_android.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.tokenFlow.first() }

        val requestBuilder = chain.request().newBuilder()
        if (!token.isNullOrEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        val response = chain.proceed(requestBuilder.build())

        if (response.code == 401 || response.code == 403) {
            val newAccessToken = runBlocking { tokenManager.refreshAccessToken() }

            if (newAccessToken != null) {
                response.close()
                val newRequest = chain.request().newBuilder()
                    .header("Authorization", "Bearer $newAccessToken")
                    .build()
                return chain.proceed(newRequest)
            }

            // Nessun refresh possibile per un utente anonimo (nessun token): la response
            // va restituita cosi' com'e', senza chiuderla, per non farla scambiare per
            // un errore di connessione da chi la legge dopo.
            if (!token.isNullOrEmpty()) {
                runBlocking { tokenManager.clearTokens() }

                // Un Bearer non valido fa rifiutare la richiesta anche su un endpoint
                // pubblico (Spring Security lo scarta prima di valutare l'autorizzazione):
                // si riprova subito senza token.
                response.close()
                val anonymousRequest = chain.request().newBuilder()
                    .removeHeader("Authorization")
                    .build()
                return chain.proceed(anonymousRequest)
            }
        }

        return response
    }
}
