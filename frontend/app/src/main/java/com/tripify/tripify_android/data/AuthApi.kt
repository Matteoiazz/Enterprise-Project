package com.tripify.tripify_android.data

import retrofit2.Response
import retrofit2.http.GET

data class AuthResponse(val token: String)

interface AuthApi {

    @GET("/api/v1/profile/me")
    suspend fun getCurrentUser(): Response<UserResponse>
}