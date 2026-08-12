package com.tripify.tripify_android.profile.api

import com.tripify.tripify_android.data.model.TravelDocumentDto
import com.tripify.tripify_android.profile.model.CompanionDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ProfileApiService {
    @GET("/api/v1/profile/companions")
    suspend fun getCompanions(): List<CompanionDto>

    @POST("/api/v1/profile/companions")
    suspend fun addCompanion(@Body companion: CompanionDto): CompanionDto

    @DELETE("/api/v1/profile/companions/{id}")
    suspend fun deleteCompanion(@Path("id") id: String)

    @GET("/api/v1/profile/documents")
    suspend fun getTravelDocuments(): List<TravelDocumentDto>

    @POST("/api/v1/profile/documents")
    suspend fun addTravelDocument(@Body document: TravelDocumentDto): TravelDocumentDto

    @DELETE("/api/v1/profile/documents/{id}")
    suspend fun deleteTravelDocument(@Path("id") id: String)
}