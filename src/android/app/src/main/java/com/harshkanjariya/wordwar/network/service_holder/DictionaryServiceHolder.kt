package com.harshkanjariya.wordwar.network.service_holder

import com.harshkanjariya.wordwar.network.ApiClient
import com.harshkanjariya.wordwar.network.service.DictionaryApiService
import retrofit2.HttpException
import java.io.IOException

object DictionaryServiceHolder {
    // We create the service instance using our flexible ApiClient
    val api: DictionaryApiService = ApiClient.createService(
        baseUrl = "https://api.dictionaryapi.dev/",
        serviceClass = DictionaryApiService::class.java
    )
}

suspend fun isWordValid(word: String): Boolean {
    return try {
        val response = DictionaryServiceHolder.api.getWordInfo(word)
        response.isNotEmpty()
    } catch (e: HttpException) {
        if (e.code() == 404) {
            return false
        }
        false
    } catch (e: IOException) {
        false
    } catch (e: Exception) {
        false
    }
}