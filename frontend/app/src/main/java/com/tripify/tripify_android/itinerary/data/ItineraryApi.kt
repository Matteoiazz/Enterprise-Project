package com.tripify.tripify_android.itinerary.data

import retrofit2.Response
import retrofit2.http.*

interface ItineraryApi {

    @GET("api/v1/itinerary/public")
    suspend fun getPublicFeed(
        @Query("city") city: String? = null,
        @Query("sort") sort: String? = null
    ): Response<List<FavoriteListDto>>

    @GET("api/v1/itinerary/public/{publicToken}")
    suspend fun getByPublicToken(@Path("publicToken") publicToken: String): Response<FavoriteListDto>

    @GET("api/v1/itinerary/{id}")
    suspend fun getById(@Path("id") id: Long): Response<FavoriteListDto>

    @GET("api/v1/itinerary/mine")
    suspend fun getMyLists(): Response<List<FavoriteListDto>>

    @POST("api/v1/itinerary")
    suspend fun createList(@Body request: CreateListRequest): Response<FavoriteListDto>

    @POST("api/v1/itinerary/{id}/items")
    suspend fun addItem(@Path("id") id: Long, @Query("itemId") itemId: Long): Response<Unit>

    @PUT("api/v1/itinerary/{id}/share")
    suspend fun share(@Path("id") id: Long, @Query("userId") userId: String): Response<Unit>

    @PATCH("api/v1/itinerary/{id}/visibility")
    suspend fun updateVisibility(@Path("id") id: Long, @Body request: UpdateVisibilityRequest): Response<FavoriteListDto>

    @POST("api/v1/itinerary/{id}/like")
    suspend fun toggleLike(@Path("id") id: Long): Response<LikeResponse>

    @POST("api/v1/itinerary/{id}/booked")
    suspend fun registerBookingAttempt(@Path("id") id: Long): Response<Unit>
}
