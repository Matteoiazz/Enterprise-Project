package com.tripify.tripify_android.notification.data

import com.tripify.tripify_android.communication.data.model.NotificationModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApi {

    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<NotificationModel>>

    @PATCH("api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: Long): Response<NotificationModel>

    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<Long>
}