package com.harshkanjariya.wordwar.network

object DictionaryServiceHolder {
    // We create the service instance using our flexible ApiClient
    val api: DictionaryApiService = ApiClient.createService(
        baseUrl = "https://api.dictionaryapi.dev/",
        serviceClass = DictionaryApiService::class.java
    )
}
