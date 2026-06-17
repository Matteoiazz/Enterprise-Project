package com.tripify.tripify_android.data

import androidx.compose.ui.semantics.Role
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String)

data class RegisterRequest(
    val name: String,
    val surname: String,
    val email: String,
    val password: String,
    val role: String = "ROLE_TRAVELER"
)

interface AuthApi {
    @POST("/api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
}
