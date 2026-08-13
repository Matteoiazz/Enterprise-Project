package com.tripify.tripify_android.data

import com.google.gson.Gson
import com.tripify.tripify_android.data.model.ErrorResponseDTO
import retrofit2.Response

// Questa è un'estensione per Retrofit: aggiunge un superpotere alle risposte!
fun <T> Response<T>.parseErrorMessage(): String {
    return try {
        // Prende il JSON di errore dal backend
        val errorJsonString = this.errorBody()?.string()
        // Lo trasforma nel nostro ErrorResponseDTO
        val errorData = Gson().fromJson(errorJsonString, ErrorResponseDTO::class.java)
        // Restituisce il messaggio pulito
        errorData.message ?: "Errore sconosciuto dal server"
    } catch (e: Exception) {
        // Se va in crash la lettura, restituisce il codice HTTP base
        "Errore ${this.code()}: riprova più tardi."
    }
}