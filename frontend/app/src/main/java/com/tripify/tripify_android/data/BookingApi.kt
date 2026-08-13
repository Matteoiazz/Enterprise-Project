package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.BookingResponseDTO
import com.tripify.tripify_android.data.model.CartDTO
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookingApi {

    @GET("api/v1/cart/{userId}")
    suspend fun getCartForUser(
        @Path("userId") userId: String
    ): Response<CartDTO>

    @POST("api/v1/cart/{userId}/add")
    suspend fun addToCart(
        @Path("userId") userId: String,
        @Query("catalogItemId") catalogItemId: Long,
        @Query("quantity") quantity: Int,
    ): Response<Unit>

    // NUOVO: Chiede lo storico dei viaggi leggendo l'utente sicuro dall'Header
    @GET("api/v1/bookings/user")
    suspend fun getUserBookings(
        @Header("X-User-Id") userId: String
    ): Response<List<BookingResponseDTO>>

    // NUOVO: Permette al Leader di invitare un amico a un viaggio
    @POST("api/v1/bookings/{bookingId}/invite")
    suspend fun inviteFriend(
        @Path("bookingId") bookingId: Long,
        @Header("X-User-Id") leaderId: String,
        @Query("friendId") friendId: String
    ): Response<BookingResponseDTO>
}