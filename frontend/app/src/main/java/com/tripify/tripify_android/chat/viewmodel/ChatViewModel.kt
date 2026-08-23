package com.tripify.tripify_android.chat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import ua.naiksoftware.stomp.dto.LifecycleEvent

class ChatViewModel(
    val currentUserId: Long,
    val roomId: Long // ID della stanza passata quando si apre la chat
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private lateinit var stompClient: StompClient
    private var subscriptions: Disposable? = null
    private val gson = Gson()

    init {
        connectWebSocket("http://172.20.10.2:8084")
        loadHistory()
    }

    fun connectWebSocket(baseUrl: String) {
        val wsUrl = baseUrl.replace("http://", "ws://").replace("https://", "wss://") + "/ws-chat"

        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

        stompClient.lifecycle().subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> android.util.Log.d("STOMP", "Connessione aperta!")
                LifecycleEvent.Type.CLOSED -> android.util.Log.d("STOMP", "Connessione chiusa!")
                LifecycleEvent.Type.ERROR -> android.util.Log.e("STOMP", "Errore connessione: " + lifecycleEvent.exception)
                else -> android.util.Log.d("STOMP", "Stato: " + lifecycleEvent.message)
            }
        }

        stompClient.connect()

        // Ascolto sul topic della specifica ChatRoom (/topic/room/{roomId})
        subscriptions = stompClient.topic("/topic/room/$roomId")
            .subscribeOn(Schedulers.io())
            .subscribe({ stompMessage ->
                val jsonPayload = stompMessage.payload
                try {
                    val incomingMessage = gson.fromJson(jsonPayload, ChatMessage::class.java)
                    // Evitiamo di duplicare i messaggi inviati da noi stessi se arrivano di ritorno dal broker
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

    fun loadHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = "http://172.20.10.2:8084/chat/history/$roomId"
                val response = java.net.URL(url).readText()

                val type = object : com.google.gson.reflect.TypeToken<List<ChatMessage>>() {}.type
                val history: List<ChatMessage> = gson.fromJson(response, type)

                _messages.value = history
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

        // Mostra subito il messaggio localmente
        _messages.value = _messages.value + chatMessage

        val jsonPayload = gson.toJson(chatMessage)

        stompClient.send("/app/chat.sendMessage", jsonPayload).subscribe(
            {
                // Inviato con successo
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