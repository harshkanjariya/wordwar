package com.harshkanjariya.wordwar.network.service

import retrofit2.http.GET
import retrofit2.http.Path

data class DictionaryResponse(
    val word: String,
    val phonetics: List<Any>,
    val meanings: List<Any>
)

// Define the Retrofit service interface
interface DictionaryApiService {
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordInfo(@Path("word") word: String): List<DictionaryResponse>
}

