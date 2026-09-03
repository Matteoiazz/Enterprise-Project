package com.tripify.tripify_android.data.model

data class CreateReviewRequest(
    val rating: Int,
    val comment: String,
    val catalogItemId: Long,
    val showName: Boolean = false
)

data class UpdateReviewRequest(
    val rating: Int,
    val comment: String,
    val showName: Boolean = false
)

data class ReplyReviewRequest(
    val reply: String
)
