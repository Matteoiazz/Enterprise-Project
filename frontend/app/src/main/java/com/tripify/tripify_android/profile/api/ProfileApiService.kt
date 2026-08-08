package com.tripify.tripify_android.profile.api

import com.tripify.tripify_android.profile.model.CompanionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ProfileApiService {
    @GET("/api/v1/profile/companions")
    suspend fun getCompanions(): List<CompanionDto>

    @POST("/api/v1/profile/companions")
    suspend fun addCompanion(@Body companion: CompanionDto): CompanionDto
}