package com.harshkanjariya.wordwar.network.service_holder

import com.harshkanjariya.wordwar.BuildConfig
import com.harshkanjariya.wordwar.network.ApiClient
import com.harshkanjariya.wordwar.network.service.GameService

object GameServiceHolder {
    private const val BASE_URL = BuildConfig.BACKEND_URL

    val api: GameService = ApiClient.createService(
        baseUrl = BASE_URL,
        serviceClass = GameService::class.java
    )
}
