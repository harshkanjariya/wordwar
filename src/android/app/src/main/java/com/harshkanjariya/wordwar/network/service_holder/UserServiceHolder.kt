package com.harshkanjariya.wordwar.network.service_holder

import com.harshkanjariya.wordwar.BuildConfig
import com.harshkanjariya.wordwar.network.ApiClient
import com.harshkanjariya.wordwar.network.service.UserService

object UserServiceHolder {
    val api: UserService by lazy {
        ApiClient.createService(
            baseUrl = BuildConfig.BACKEND_URL,
            serviceClass = UserService::class.java
        )
    }
}