package com.tripify.tripify_android.data

import com.tripify.tripify_android.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private val BASE_URL = BuildConfig.BASE_URL

    // Funzione originale di Dario (NON TOCCARE)
    fun createApi(tokenManager: TokenManager): AuthApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    // --- LA NOSTRA NUOVA FUNZIONE PER IL CATALOGO ---
    fun createCatalogApi(tokenManager: TokenManager): CatalogApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CatalogApi::class.java)
    }

    // --- LA NUOVA FUNZIONE PER IL BOOKING (DA DEVELOP) ---
    fun createBookingApi(tokenManager: TokenManager): BookingApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BookingApi::class.java)
    }

    // --- LA FUNZIONE PER IL PROFILO (DAL TUO BRANCH) ---
    fun createProfileApi(tokenManager: TokenManager): com.tripify.tripify_android.profile.api.ProfileApiService {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(com.tripify.tripify_android.profile.api.ProfileApiService::class.java)
    }
}
