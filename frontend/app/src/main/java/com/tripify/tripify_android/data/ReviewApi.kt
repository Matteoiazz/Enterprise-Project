package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.ReviewDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApi {
    @POST("api/v1/reviews")
    suspend fun addReview(
        @Query("rating") rating: Int,
        @Query("comment") comment: String,
        @Query("catalogItemId") catalogItemId: Long
    ): Response<ReviewDto>

    @GET("api/v1/reviews/item/{catalogItemId}")
    suspend fun getReviewsForItem(
        @Path("catalogItemId") catalogItemId: Long
    ): Response<List<ReviewDto>>
}