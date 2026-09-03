package com.tripify.tripify_android.data

import retrofit2.Response
import retrofit2.http.GET

interface AuthApi {

    @GET("/api/v1/profile/me")
    suspend fun getCurrentUser(): Response<UserResponse>
}
