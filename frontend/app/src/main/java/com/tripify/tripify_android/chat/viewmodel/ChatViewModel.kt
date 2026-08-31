package com.tripify.tripify_android.chat.viewmodel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.TokenManager
import io.reactivex.disposables.Disposable
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
    private var subscriptions: Disposable? = null
    private val gson = Gson()

    private val baseUrl = BuildConfig.BASE_URL

    // Variabile pubblica accessibile dalla UI per capire se il messaggio è il "mio" o dell'host
    var currentUserId: String = ""
        private set

    init {
        viewModelScope.launch {
            // 1. Recuperiamo il token reale salvato
            val token = tokenManager.tokenFlow.first() ?: ""

            // 2. Estraiamo il vero UUID dell'utente dal token JWT
            currentUserId = extractUserIdFromToken(token)

            // 3. Avviamo le connessioni sicure passando il token
            if (token.isNotBlank()) {
                loadHistory(baseUrl, token)
                connectWebSocket(baseUrl, token)
            }
        }
    }

    private fun extractUserIdFromToken(token: String): String {
        try {
            val parts = token.split(".")
            if (parts.size == 3) {
                val payload = String(Base64.decode(parts[1], Base64.URL_SAFE))
                val jsonObject = JSONObject(payload)
                // In Keycloak l'UUID univoco si trova nel campo 'sub'
                return jsonObject.optString("sub", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    private fun connectWebSocket(serverUrl: String, token: String) {
        // Correggiamo l'URL rimuovendo il /websocket finale che la libreria gestisce in autonomia
        val wsUrl = serverUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws-chat"

        // Iniettiamo il Bearer token per superare Spring Security durante l'handshake HTTP
        val httpHeaders = mutableMapOf("Authorization" to "Bearer $token")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl, httpHeaders)

        stompClient.lifecycle().subscribe(
            { lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> android.util.Log.d("STOMP", "Connessione aperta!")
                    LifecycleEvent.Type.CLOSED -> android.util.Log.d("STOMP", "Connessione chiusa!")
                    LifecycleEvent.Type.ERROR -> android.util.Log.e("STOMP", "Errore connessione: " + lifecycleEvent.exception)
                    else -> android.util.Log.d("STOMP", "Stato: " + lifecycleEvent.message)
                }
            },
            { error ->
                android.util.Log.e("STOMP", "Errore critico RxJava", error)
            }
        )

        // Anche per la connessione STOMP pura inviamo il token
        val stompHeaders = listOf(StompHeader("Authorization", "Bearer $token"))
        stompClient.connect(stompHeaders)

        // Ascolto sul topic della specifica ChatRoom
        subscriptions = stompClient.topic("/topic/room/$roomId")
            .subscribeOn(Schedulers.io())
            .subscribe({ stompMessage ->
                val jsonPayload = stompMessage.payload
                try {
                    val incomingMessage = gson.fromJson(jsonPayload, ChatMessage::class.java)
                    // Se il messaggio arriva da un'altra persona, lo aggiungiamo alla UI
                    if (incomingMessage.senderId != currentUserId) {
                        _messages.value = _messages.value + incomingMessage
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, { error ->
                error.printStackTrace()
            })
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
            senderId = currentUserId, // Adesso l'ID è il VERO Uuid!
            content = messageText
        )

        // Mostra subito il messaggio localmente
        _messages.value = _messages.value + chatMessage

        val jsonPayload = gson.toJson(chatMessage)

        stompClient.send("/app/chat.sendMessage", jsonPayload).subscribe(
            {
                android.util.Log.d("STOMP", "Messaggio inviato con successo")
            },
            { error ->
                error.printStackTrace()
            }
        )
    }

    override fun onCleared() {
        super.onCleared()
        subscriptions?.dispose()
        if (::stompClient.isInitialized) {
            stompClient.disconnect()
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