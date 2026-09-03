package com.tripify.tripify_android.data

import retrofit2.Response

// Mantenuta per compatibilità con i ViewModel di booking che la usano gia'.
// Ora delega al parser unico (data/ApiError.kt) che gestisce tutti i formati
// di errore del backend, non solo {"message": ...}.
fun <T> Response<T>.parseErrorMessage(): String =
    parseApiError("Errore ${code()}: riprova più tardi.")
