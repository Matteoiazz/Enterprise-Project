package com.tripify.tripify_android.chat.repository

import android.util.Log
import com.google.gson.Gson
import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.chat.viewmodel.ChatRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object ChatRepository {

    private val gson = Gson()
    private val baseUrl: String
        get() = BuildConfig.BASE_URL

    suspend fun getOrCreateChatRoom(hostId: String, authToken: String?): ChatRoom? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("ChatRepository", "Chiamata a getOrCreateChatRoom con hostId: $hostId")

                val url = URL("$baseUrl/chat/room?hostId=$hostId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                // RIMOSSO connection.doOutput = true perché non stiamo inviando un body JSON

                if (!authToken.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                val responseCode = connection.responseCode
                Log.d("ChatRepository", "Response Code: $responseCode")

                // Accettiamo QUALSIASI codice di successo (200 OK, 201 CREATED, ecc.)
                if (responseCode in 200..299) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("ChatRepository", "Response JSON: $responseJson")
                    gson.fromJson(responseJson, ChatRoom::class.java)
                } else {
                    // Leggiamo l'errore del server per capire cosa non va!
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("ChatRepository", "Errore server HTTP $responseCode: $errorStream")
                    null
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Eccezione di rete", e)
                null
            }
        }
    }

    suspend fun getUserChatRooms(authToken: String?): List<ChatRoom> {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("$baseUrl/chat/rooms")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                if (!authToken.isNullOrBlank()) {
                    connection.setRequestProperty("Authorization", "Bearer $authToken")
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..299) {
                    val responseJson = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : com.google.gson.reflect.TypeToken<List<ChatRoom>>() {}.type
                    gson.fromJson(responseJson, type)
                } else {
                    val errorStream = connection.errorStream?.bufferedReader()?.use { it.readText() }
                    Log.e("ChatRepository", "Errore getRooms HTTP $responseCode: $errorStream")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.e("ChatRepository", "Eccezione getRooms", e)
                emptyList()
            }
        }
    }
}