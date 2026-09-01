package com.tripify.tripify_android.chat.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.TokenManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompHeader
import java.net.HttpURLConnection
import java.net.URL

class ChatViewModel(
    val roomId: String,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private lateinit var stompClient: StompClient
    private val compositeDisposable = CompositeDisposable()
    private val gson = Gson()

    private val baseUrl = BuildConfig.BASE_URL

    var currentUserId: String = ""
        private set

    init {
        viewModelScope.launch {
            // Forza il prelievo del token più recente ed evita token obsoleti
            val token = tokenManager.tokenFlow.first() ?: ""
            currentUserId = extractUserIdFromToken(token)

            if (token.isNotBlank()) {
                loadHistory(baseUrl, token)
                connectWebSocket(baseUrl, token)
            } else {
                android.util.Log.e("STOMP", "Impossibile connettersi: Token vuoto o non disponibile")
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

    private fun connectWebSocket(serverUrl: String, token: String) {
        val wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws-chat"
        val httpHeaders = mutableMapOf("Authorization" to "Bearer $token")

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl, httpHeaders)

        // 1. Gestione del ciclo di vita della connessione
        val lifecycleDisposable = stompClient.lifecycle()
            .subscribeOn(Schedulers.io())
            .subscribe(
                { lifecycleEvent ->
                    when (lifecycleEvent.type) {
                        LifecycleEvent.Type.OPENED -> {
                            android.util.Log.d("STOMP", "WebSocket socket aperto")
                        }
                        LifecycleEvent.Type.CLOSED -> {
                            android.util.Log.d("STOMP", "WebSocket connessione chiusa")
                        }
                        LifecycleEvent.Type.ERROR -> {
                            android.util.Log.e("STOMP", "Errore connessione STOMP", lifecycleEvent.exception)
                        }
                        else -> {}
                    }
                },
                { error ->
                    android.util.Log.e("STOMP", "Errore lifecycle RxJava", error)
                }
            )
        compositeDisposable.add(lifecycleDisposable)

        // 2. Sottoscrizione al topic: la libreria lo spedisce solo DOPO il frame STOMP CONNECTED
        val topicDisposable = stompClient.topic("/topic/room/$roomId")
            .subscribeOn(Schedulers.io())
            .subscribe(
                { stompMessage ->
                    val jsonPayload = stompMessage.payload
                    try {
                        val incomingMessage = gson.fromJson(jsonPayload, ChatMessage::class.java)
                        if (incomingMessage.senderId != currentUserId) {
                            _messages.value = _messages.value + incomingMessage
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                { error ->
                    android.util.Log.e("STOMP", "Errore ricezione messaggio", error)
                }
            )
        compositeDisposable.add(topicDisposable)

        // 3. Avvio dell'handshake
        val stompHeaders = listOf(StompHeader("Authorization", "Bearer $token"))
        stompClient.connect(stompHeaders)
    }

    private fun loadHistory(serverUrl: String, token: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL("$serverUrl/chat/history/$roomId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<ChatMessage>>() {}.type
                    val history: List<ChatMessage> = gson.fromJson(response, type)
                    _messages.value = history
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        val chatMessage = ChatMessage(
            roomId = roomId,
            senderId = currentUserId,
            content = messageText
        )

        // Aggiornamento ottimistico dell'UI
        _messages.value = _messages.value + chatMessage

        val jsonPayload = gson.toJson(chatMessage)

        val sendDisposable = stompClient.send("/app/chat.sendMessage", jsonPayload)
            .subscribeOn(Schedulers.io())
            .subscribe(
                {
                    android.util.Log.d("STOMP", "Messaggio inviato con successo")
                },
                { error ->
                    android.util.Log.e("STOMP", "Errore durante l'invio", error)
                }
            )
        compositeDisposable.add(sendDisposable)
    }

    override fun onCleared() {
        super.onCleared()
        try {
            compositeDisposable.clear()
            if (::stompClient.isInitialized && stompClient.isConnected) {
                stompClient.disconnect()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

class ChatViewModelFactory(
    private val roomId: String,
    private val tokenManager: TokenManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(roomId, tokenManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}