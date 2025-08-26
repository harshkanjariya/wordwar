package com.harshkanjariya.wordwar.network

import com.harshkanjariya.wordwar.WordWarApp
import com.pluto.plugins.network.interceptors.okhttp.PlutoOkhttpInterceptor
import com.harshkanjariya.wordwar.data.LocalStorage
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.google.gson.Gson
import com.harshkanjariya.wordwar.data.ApiResponse

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

    // Custom interceptor to handle non-2xx responses and convert them to successful responses
    private val responseInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        
        // If response is not successful (2xx), convert it to a successful response
        if (!response.isSuccessful) {
            val responseBody = response.body?.string() ?: ""
            val contentType = response.body?.contentType()
            
            // Try to parse the error response as ApiResponse
            try {
                val gson = Gson()
                val apiResponse = gson.fromJson(responseBody, ApiResponse::class.java)
                
                // Create a new successful response with the error body
                val newResponseBody = ResponseBody.create(contentType, responseBody)
                return@Interceptor response.newBuilder()
                    .code(200) // Change to 200 OK
                    .body(newResponseBody)
                    .build()
            } catch (e: Exception) {
                // If parsing fails, create a generic error response
                val errorResponse = """{"status":${response.code},"message":"${response.message}","data":null}"""
                val newResponseBody = ResponseBody.create(contentType, errorResponse)
                return@Interceptor response.newBuilder()
                    .code(200) // Change to 200 OK
                    .body(newResponseBody)
                    .build()
            }
        }
        
        response
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
            .addInterceptor(responseInterceptor) // Add the custom response interceptor
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