package com.tripify.tripify_android.notification.data

import android.util.Log
import com.tripify.tripify_android.communication.data.model.NotificationModel

class NotificationRepository(
    private val notificationApi: NotificationApi
) {

    suspend fun getNotifications(): List<NotificationModel> {
        return try {
            val response = notificationApi.getNotifications()
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Errore nel recupero notifiche", e)
            emptyList()
        }
    }

    suspend fun markAsRead(notificationId: Long): Boolean {
        return try {
            val response = notificationApi.markAsRead(notificationId)
            response.isSuccessful
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Errore nel marcare la notifica come letta", e)
            false
        }
    }

    suspend fun getUnreadCount(): Long {
        return try {
            val response = notificationApi.getUnreadCount()
            if (response.isSuccessful) {
                response.body() ?: 0L
            } else {
                0L
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Errore nel conteggio non lette", e)
            0L
        }
    }
}