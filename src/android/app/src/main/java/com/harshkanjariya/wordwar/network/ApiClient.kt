package com.harshkanjariya.wordwar.network

import com.pluto.plugins.network.interceptors.okhttp.PlutoOkhttpInterceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    // OkHttp client is now private and lazily initialized
    private val okHttpClient: OkHttpClient by lazy {
        // Create an HTTP logging interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // Build the OkHttpClient with interceptors
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging) // Add logging for all builds
            .apply {
                addInterceptor(PlutoOkhttpInterceptor)
            }
            .build()
    }

    // Generic function to create any API service with a custom base URL
    fun <T> createService(baseUrl: String, serviceClass: Class<T>): T {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(serviceClass)
    }
}