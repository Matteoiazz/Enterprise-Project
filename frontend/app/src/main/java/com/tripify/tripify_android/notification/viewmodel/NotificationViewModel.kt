package com.tripify.tripify_android.notification.viewmodel

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.communication.data.model.NotificationModel
import com.tripify.tripify_android.data.TokenManager
import com.tripify.tripify_android.notification.data.NotificationRepository
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.StompHeader

class NotificationViewModel(
    private val repository: NotificationRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    var notifications by mutableStateOf<List<NotificationModel>>(emptyList())
        private set

    var unreadCount by mutableStateOf(0L)
        private set

    var isLoading by mutableStateOf(false)
        private set

    private lateinit var stompClient: StompClient
    private var notificationSubscription: Disposable? = null
    private val gson = Gson()
    private val baseUrl = BuildConfig.BASE_URL

    init {
        loadNotifications()
        loadUnreadCount()
        initRealTimeNotifications()
    }

    private fun initRealTimeNotifications() {
        viewModelScope.launch {
            val token = tokenManager.tokenFlow.first() ?: ""
            if (token.isNotBlank()) {
                val userId = extractUserIdFromToken(token)
                if (userId.isNotBlank()) {
                    connectNotificationWebSocket(baseUrl, token, userId)
                }
            }
        }
    }

    private fun extractUserIdFromToken(token: String): String {
        try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                val jsonObject = JSONObject(payload)
                return jsonObject.optString("sub", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun connectNotificationWebSocket(serverUrl: String, token: String, userId: String) {
        val wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws-chat/websocket"

        val httpHeaders = mutableMapOf("Authorization" to "Bearer $token")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl, httpHeaders)

        val stompHeaders = listOf(StompHeader("Authorization", "Bearer $token"))
        stompClient.connect(stompHeaders)

        notificationSubscription = stompClient.topic("/topic/notifications/$userId")
            .subscribeOn(Schedulers.io())
            .subscribe({ stompMessage ->
                try {
                    val event = gson.fromJson(stompMessage.payload, NotificationModel::class.java)

                    notifications = listOf(event) + notifications

                    unreadCount += 1
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, { error ->
                error.printStackTrace()
            })
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
                notifications = notifications.map {
                    if (it.id == notificationId) it.copy(isRead = true) else it
                }
                loadUnreadCount()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationSubscription?.dispose()
        if (::stompClient.isInitialized) {
            stompClient.disconnect()
        }
    }
}