package com.tripify.tripify_android.data

import com.tripify.tripify_android.data.model.CreateReviewRequest
import com.tripify.tripify_android.data.model.ReviewDto
import com.tripify.tripify_android.data.model.UpdateReviewRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ReviewApi {
    @POST("api/v1/reviews")
    suspend fun addReview(@Body request: CreateReviewRequest): Response<ReviewDto>

    @PUT("api/v1/reviews/{id}")
    suspend fun updateReview(
        @Path("id") id: Long,
        @Body request: UpdateReviewRequest
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
