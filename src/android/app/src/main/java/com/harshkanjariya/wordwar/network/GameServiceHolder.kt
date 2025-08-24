package com.harshkanjariya.wordwar.network

import com.google.firebase.auth.FirebaseAuth
import com.harshkanjariya.wordwar.BuildConfig

object GameServiceHolder {
    private const val BASE_URL = BuildConfig.BACKEND_URL

    val api: GameService = ApiClient.createService(
        baseUrl = BASE_URL,
        serviceClass = GameService::class.java
    )
}