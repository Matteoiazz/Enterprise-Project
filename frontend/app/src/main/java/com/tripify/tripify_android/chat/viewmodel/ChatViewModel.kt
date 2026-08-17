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
import com.google.gson.Gson // Assicurati di avere Gson nelle dipendenze (di solito c'è già)
import kotlinx.coroutines.Dispatchers
import ua.naiksoftware.stomp.dto.LifecycleEvent

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private lateinit var stompClient: StompClient
    private var subscriptions: Disposable? = null
    private val gson = Gson()

    // Esempio: ID temporanei per testare la chat tra l'utente 1 e l'utente 2
    val currentUserId: Long = 1L
    private val targetUserId: Long = 2L

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

        // Ascolto sulla coda privata configurata dal backend (/user/queue/messages)
        subscriptions = stompClient.topic("/user/$currentUserId/queue/messages")
            .subscribeOn(Schedulers.io())
            .subscribe({ stompMessage ->
                val jsonPayload = stompMessage.payload
                try {
                    // Converte il JSON ricevuto dal backend nell'oggetto ChatMessage
                    val incomingMessage = gson.fromJson(jsonPayload, ChatMessage::class.java)
                    _messages.value = _messages.value + incomingMessage
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

                val url = "http://172.20.10.2:8084/chat/history/1/2"
                val response = java.net.URL(url).readText()

                // Converte il JSON ricevuto in una lista di ChatMessage
                val type = object : com.google.gson.reflect.TypeToken<List<ChatMessage>>() {}.type
                val history: List<ChatMessage> = gson.fromJson(response, type)

                // Aggiorna la lista dei messaggi con lo storico + quelli inviati in sessione
                _messages.value = history
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank()) return

        // Crea l'oggetto da inviare al backend
        val chatMessage = ChatMessage(
            senderId = currentUserId,
            receiverId = targetUserId,
            content = messageText
        )

        _messages.value = _messages.value + chatMessage

        // Converte l'oggetto in formato JSON stringa
        val jsonPayload = gson.toJson(chatMessage)

        // Invia all'endpoint @MessageMapping("/chat.sendMessage") del backend
        stompClient.send("/app/chat.sendMessage", jsonPayload).subscribe(
            {
                // Messaggio inviato con successo tramite STOMP
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