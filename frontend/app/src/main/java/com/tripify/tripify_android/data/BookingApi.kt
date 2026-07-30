package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.CartDTO
import retrofit2.Response
import retrofit2.http.GET
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
        @Query("price") price: Double
    ): Response<Unit>
}