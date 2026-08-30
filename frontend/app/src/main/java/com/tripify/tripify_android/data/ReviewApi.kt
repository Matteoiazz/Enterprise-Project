package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.ReviewDto
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {
    @POST("api/v1/reviews")
    suspend fun addReview(
        @Query("rating") rating: Int,
        @Query("comment") comment: String,
        @Query("catalogItemId") catalogItemId: Long
    ): Response<ReviewDto>

    @PUT("api/v1/reviews/{id}")
    suspend fun updateReview(
        @Path("id") id: Long,
        @Query("rating") rating: Int,
        @Query("comment") comment: String
    ): Response<ReviewDto>

    @DELETE("api/v1/reviews/{id}")
    suspend fun deleteReview(
        @Path("id") id: Long
    ): Response<Unit>

    @GET("api/v1/reviews/item/{catalogItemId}")
    suspend fun getReviewsForItem(
        @Path("catalogItemId") catalogItemId: Long
    ): Response<List<ReviewDto>>
}
