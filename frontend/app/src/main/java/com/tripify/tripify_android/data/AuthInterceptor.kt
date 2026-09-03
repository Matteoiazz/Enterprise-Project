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

        if (response.code == 401) {
            when (val result = runBlocking { tokenManager.refreshAccessToken(token) }) {
                is TokenManager.RefreshResult.Success -> {
                    response.close()
                    return chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Bearer ${result.accessToken}")
                            .build()
                    )
                }
                TokenManager.RefreshResult.InvalidGrant -> {
                    if (!token.isNullOrEmpty()) {
                        runBlocking { tokenManager.clearTokens() }
                        response.close()
                        return chain.proceed(
                            chain.request().newBuilder().removeHeader("Authorization").build()
                        )
                    }
                }
                TokenManager.RefreshResult.TransientError -> {
                }
            }
        }

        return response
    }
}
