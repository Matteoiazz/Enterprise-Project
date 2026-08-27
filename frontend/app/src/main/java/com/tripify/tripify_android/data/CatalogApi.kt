package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.AvailabilityDto
import com.tripify.tripify_android.data.model.CatalogItemDto
import com.tripify.tripify_android.data.model.CreateActivityRequest
import com.tripify.tripify_android.data.model.CreateFlightRequest
import com.tripify.tripify_android.data.model.CreateHotelRequest
import com.tripify.tripify_android.data.model.HoldResultDto
import com.tripify.tripify_android.data.model.OrganizerItemDto
import com.tripify.tripify_android.data.model.PagedResponse
import com.tripify.tripify_android.data.model.RoomHoldRequest
import com.tripify.tripify_android.data.model.SeatHoldRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
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
        @Query("checkIn") checkIn: String? = null,
        @Query("checkOut") checkOut: String? = null,
        @Query("rooms") rooms: Int? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): PagedResponse<CatalogItemDto>

    @GET("/api/v1/catalog/items/{id}")
    suspend fun getItemById(@Path("id") id: Int): CatalogItemDto

    @GET("/api/v1/catalog/cities")
    suspend fun getCitySuggestions(@Query("query") query: String): List<String>

    @GET("/api/v1/catalog/room-types/{id}/availability")
    suspend fun getRoomAvailability(@Path("id") roomTypeId: Int, @Query("checkIn") checkIn: String, @Query("checkOut") checkOut: String): AvailabilityDto

    @GET("/api/v1/catalog/fare-classes/{id}/availability")
    suspend fun getSeatAvailability(@Path("id") fareClassId: Int): AvailabilityDto

    @POST("/api/v1/catalog/room-types/{id}/hold")
    suspend fun holdRoom(@Path("id") roomTypeId: Int, @Body request: RoomHoldRequest): HoldResultDto

    @POST("/api/v1/catalog/fare-classes/{id}/hold")
    suspend fun holdSeats(@Path("id") fareClassId: Int, @Body request: SeatHoldRequest): HoldResultDto

    @POST("/api/v1/catalog/holds/{holdId}/confirm")
    suspend fun confirmHold(@Path("holdId") holdId: String)

    @POST("/api/v1/catalog/holds/{holdId}/release")
    suspend fun releaseHold(@Path("holdId") holdId: String)

    // --- Zona organizzatore: richiede ROLE_ORGANIZER nel token, l'identità è
    // quella del JWT già allegato da AuthInterceptor a ogni richiesta. ---

    @GET("/api/v1/catalog/items/mine")
    suspend fun getMyItems(): Response<List<OrganizerItemDto>>

    @POST("/api/v1/catalog/items/flights")
    suspend fun createFlight(@Body request: CreateFlightRequest): Response<CatalogItemDto>

    @POST("/api/v1/catalog/items/hotels")
    suspend fun createHotel(@Body request: CreateHotelRequest): Response<CatalogItemDto>

    @POST("/api/v1/catalog/items/activities")
    suspend fun createActivity(@Body request: CreateActivityRequest): Response<CatalogItemDto>

    @PUT("/api/v1/catalog/items/flights/{id}")
    suspend fun updateFlight(@Path("id") id: Int, @Body request: CreateFlightRequest): Response<CatalogItemDto>

    @PUT("/api/v1/catalog/items/hotels/{id}")
    suspend fun updateHotel(@Path("id") id: Int, @Body request: CreateHotelRequest): Response<CatalogItemDto>

    @PUT("/api/v1/catalog/items/activities/{id}")
    suspend fun updateActivity(@Path("id") id: Int, @Body request: CreateActivityRequest): Response<CatalogItemDto>

    @DELETE("/api/v1/catalog/items/{id}")
    suspend fun deleteItem(@Path("id") id: Int): Response<Unit>
}
