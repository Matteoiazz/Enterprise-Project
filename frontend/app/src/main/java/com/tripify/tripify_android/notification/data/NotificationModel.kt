package com.tripify.tripify_android.communication.data.model // Adatta il package in base alla struttura del vostro progetto

import com.google.gson.annotations.SerializedName

data class NotificationModel(
    @SerializedName("id")
    val id: Long,

    @SerializedName("userId")
    val userId: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("isRead")
    val isRead: Boolean,

    @SerializedName("createdAt")
    val createdAt: String // O LocalDateTime/Long a seconda di come gestite le date nel parsing JSON
)