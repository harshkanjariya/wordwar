package com.harshkanjariya.wordwar.network

import com.harshkanjariya.wordwar.BuildConfig

object AuthServiceHolder {
    val api: AuthService by lazy {
        ApiClient.createService(
            baseUrl = BuildConfig.BACKEND_URL,
            serviceClass = AuthService::class.java
        )
    }
}