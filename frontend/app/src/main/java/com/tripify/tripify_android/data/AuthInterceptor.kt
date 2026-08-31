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

            // Nessun refresh possibile: per un utente anonimo (nessun token) non c'e'
            // niente da pulire, e il 401/403 va restituito cosi' com'e', SENZA
            // chiuderlo prima -- altrimenti Retrofit trova una response gia' chiusa
            // e la scambia per un errore di connessione invece del vero stato 401.
            if (!token.isNullOrEmpty()) {
                runBlocking { tokenManager.clearTokens() }

                // Un token presente ma non piu' valido (es. sessione precedente a un
                // riavvio del backend/Keycloak) fa rifiutare la richiesta ANCHE su un
                // endpoint pubblico come la ricerca del catalogo: Spring Security
                // scarta un Bearer non valido prima ancora di guardare se serve
                // davvero autenticazione. Riprovo subito senza token: se l'endpoint
                // era pubblico funziona al volo, altrimenti resta un 401 legittimo.
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
