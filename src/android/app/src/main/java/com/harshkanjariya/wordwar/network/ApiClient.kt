package com.harshkanjariya.wordwar.network

import com.harshkanjariya.wordwar.WordWarApp
import com.pluto.plugins.network.interceptors.okhttp.PlutoOkhttpInterceptor
import com.harshkanjariya.wordwar.data.LocalStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

object ApiClient {
    // Custom interceptor to add the authorization header
    private val authInterceptor = Interceptor { chain ->
        val requestBuilder = chain.request().newBuilder()
        val token = runBlocking {
            LocalStorage.getToken().first()
        }
        token?.let {
            requestBuilder.header("Authorization", "Bearer $it")
        }
        val request = requestBuilder.build()
        chain.proceed(request)
    }

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
            .addInterceptor(authInterceptor) // Add the custom authorization interceptor
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