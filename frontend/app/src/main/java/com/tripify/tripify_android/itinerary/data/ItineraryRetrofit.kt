package com.tripify.tripify_android.itinerary.data

import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.data.AuthInterceptor
import com.tripify.tripify_android.data.TokenManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Factory autonoma per ItineraryApi: stesso pattern di RetrofitClient (OkHttp +
 * AuthInterceptor + Gson), tenuta separata per non toccare file condivisi fuori
 * dal perimetro di questa feature.
 */
object ItineraryRetrofit {
    fun create(tokenManager: TokenManager): ItineraryApi {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .build()

        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ItineraryApi::class.java)
    }
}
