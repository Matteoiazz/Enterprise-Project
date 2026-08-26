package com.tripify.tripify_android.notification.data

import com.tripify.tripify_android.communication.data.model.NotificationModel
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface NotificationApi {

    // 1. Ottiene la lista delle notifiche dell'utente loggato
    @GET("api/notifications")
    suspend fun getNotifications(): Response<List<NotificationModel>>

    // 2. Segna una specifica notifica come letta
    @PATCH("api/notifications/{id}/read")
    suspend fun markAsRead(@Path("id") notificationId: Long): Response<NotificationModel>

    // 3. Ottiene il conteggio delle notifiche non lette
    @GET("api/notifications/unread-count")
    suspend fun getUnreadCount(): Response<Long>
}