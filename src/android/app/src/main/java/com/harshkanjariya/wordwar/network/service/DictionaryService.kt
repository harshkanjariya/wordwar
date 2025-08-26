package com.harshkanjariya.wordwar.network.service

import retrofit2.http.GET
import retrofit2.http.Path

// Main response structure
data class DictionaryResponse(
    val word: String,
    val phonetic: String?,
    val phonetics: List<Phonetic>,
    val meanings: List<Meaning>,
    val license: License?,
    val sourceUrls: List<String>?
)

// Phonetic information
data class Phonetic(
    val text: String?,
    val audio: String?,
    val sourceUrl: String?,
    val license: License?
)

// Meaning and definitions
data class Meaning(
    val partOfSpeech: String,
    val definitions: List<Definition>,
    val synonyms: List<String>,
    val antonyms: List<String>
)

// Individual definition
data class Definition(
    val definition: String,
    val example: String?,
    val synonyms: List<String>?,
    val antonyms: List<String>?
)

// License information
data class License(
    val name: String,
    val url: String
)

// Define the Retrofit service interface
interface DictionaryApiService {
    @GET("api/v2/entries/en/{word}")
    suspend fun getWordInfo(@Path("word") word: String): List<DictionaryResponse>
}

