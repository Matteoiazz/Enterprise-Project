package com.tripify.tripify_android.notification.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tripify.tripify_android.communication.data.model.NotificationModel
import com.tripify.tripify_android.notification.data.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel(
    private val repository: NotificationRepository
) : ViewModel() {

    var notifications by mutableStateOf<List<NotificationModel>>(emptyList())
        private set

    var unreadCount by mutableStateOf(0L)
        private set

    var isLoading by mutableStateOf(false)
        private set

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications() {
        viewModelScope.launch {
            isLoading = true
            notifications = repository.getNotifications()
            isLoading = false
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            unreadCount = repository.getUnreadCount()
        }
    }

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            val success = repository.markAsRead(notificationId)
            if (success) {
                // Aggiorniamo la lista localmente per mostrare subito la notifica come letta
                notifications = notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                loadUnreadCount() // Ricarichiamo il conteggio
            }
        }
    }
}