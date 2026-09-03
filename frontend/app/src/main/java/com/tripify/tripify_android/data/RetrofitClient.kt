package com.tripify.tripify_android.data


import com.tripify.tripify_android.BuildConfig
import com.tripify.tripify_android.notification.data.NotificationApi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private val BASE_URL = BuildConfig.BASE_URL
    @Volatile
    private var okHttpClient: OkHttpClient? = null

    private fun httpClient(tokenManager: TokenManager): OkHttpClient {
        return okHttpClient ?: synchronized(this) {
            okHttpClient ?: OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenManager))
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS)
                .build()
                .also { okHttpClient = it }
        }
    }

    private fun <T> build(tokenManager: TokenManager, service: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(httpClient(tokenManager))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(service)
    }

    fun createApi(tokenManager: TokenManager): AuthApi =
        build(tokenManager, AuthApi::class.java)

    fun createCatalogApi(tokenManager: TokenManager): CatalogApi =
        build(tokenManager, CatalogApi::class.java)

    fun createBookingApi(tokenManager: TokenManager): BookingApi =
        build(tokenManager, BookingApi::class.java)

    fun createProfileApi(tokenManager: TokenManager): com.tripify.tripify_android.profile.api.ProfileApiService =
        build(tokenManager, com.tripify.tripify_android.profile.api.ProfileApiService::class.java)

    fun createNotificationApi(tokenManager: TokenManager): NotificationApi =
        build(tokenManager, NotificationApi::class.java)

    fun createReviewApi(tokenManager: TokenManager): com.tripify.tripify_android.data.ReviewApi =
        build(tokenManager, com.tripify.tripify_android.data.ReviewApi::class.java)
}
