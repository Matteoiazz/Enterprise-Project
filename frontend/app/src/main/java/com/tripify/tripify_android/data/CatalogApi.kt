package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.CatalogItemDto
import com.tripify.tripify_android.data.model.PagedResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CatalogApi {
    @GET("/api/v1/catalog/items/search")
    suspend fun searchCatalog(
        @Query("category") category: String,
        @Query("query") query: String,
        @Query("maxPrice") maxPrice: Int? = null,
        @Query("minRating") minRating: Int,
        @Query("destination") destination: String? = null,
        @Query("departure") departure: String? = null,
        @Query("guideIncluded") guideIncluded: Boolean? = null,
        @Query("amenities") amenities: List<String>? = null,
        @Query("directOnly") directOnly: Boolean? = null,
        @Query("departureDate") departureDate: String? = null,
        @Query("minSeats") minSeats: Int? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<CatalogItemDto>

    @GET("/api/v1/catalog/items/{id}")
    suspend fun getItemById(@Path("id") id: Int): CatalogItemDto

    @GET("/api/v1/catalog/cities")
    suspend fun getCitySuggestions(@Query("query") query: String): List<String>
}
