package com.harshkanjariya.wordwar.data

import com.harshkanjariya.wordwar.network.service_holder.DictionaryServiceHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

data class WordInfo(
    val word: String,
    val meaning: String,
    val pronunciation: String,
    val partOfSpeech: String,
    val definitions: List<String>,
    val examples: List<String>,
    val synonyms: List<String>,
    val antonyms: List<String>
)

object WordService {
    private var currentWord: WordInfo? = null
    private var nextWord: WordInfo? = null
    
    suspend fun getCurrentWord(): WordInfo? {
        if (currentWord == null) {
            loadNextWord()
        }
        return currentWord
    }
    
    suspend fun loadNextWord() {
        try {
            // Fetch random word from random-word API
            val randomWord = fetchRandomWord()
            println("WordService: Fetched random word: $randomWord")
            
            // Look up word definition using dictionary API
            val wordInfo = lookupWordDefinition(randomWord)
            println("WordService: Successfully loaded word info for: ${wordInfo.word}")
            
            // Update current and next word
            currentWord = nextWord ?: wordInfo
            nextWord = wordInfo
            
        } catch (e: Exception) {
            println("WordService: Error loading word: ${e.message}")
            // Fallback to default words if API fails
            currentWord = getFallbackWord()
            nextWord = getFallbackWord()
        }
    }
    
    private suspend fun fetchRandomWord(): String = withContext(Dispatchers.IO) {
        try {
            // Using a simple HTTP client to fetch from random-word API
            val url = "https://random-word-api.herokuapp.com/word?number=1"
            val response = java.net.URL(url).readText()
            
            // Parse JSON response: ["word"] - handle the actual format
            val word = response.trim()
                .removeSurrounding("[", "]")
                .removeSurrounding("\"")
                .replace("\"", "") // Remove any remaining quotes
            
            if (word.isNotEmpty()) word else "serendipity" // fallback word
            
        } catch (e: Exception) {
            "serendipity" // fallback word
        }
    }
    
    private suspend fun lookupWordDefinition(word: String): WordInfo = withContext(Dispatchers.IO) {
        try {
            val response = DictionaryServiceHolder.api.getWordInfo(word)
            
            if (response.isNotEmpty()) {
                val wordData = response[0]
                val meanings = wordData.meanings
                
                if (meanings.isNotEmpty()) {
                    // Get the first meaning (usually the most common)
                    val meaning = meanings[0]
                    val definitions = meaning.definitions.map { it.definition }
                    
                    // Collect examples from all definitions
                    val examples = meaning.definitions
                        .mapNotNull { it.example }
                        .filter { it.isNotEmpty() }
                    
                    // Get pronunciation - try phonetic first, then phonetics array
                    val pronunciation = wordData.phonetic ?: 
                        wordData.phonetics.firstOrNull()?.text ?: ""

                    return@withContext WordInfo(
                        word = wordData.word,
                        meaning = definitions.firstOrNull() ?: "Definition not available",
                        pronunciation = pronunciation,
                        partOfSpeech = meaning.partOfSpeech,
                        definitions = definitions,
                        examples = examples,
                        synonyms = meaning.synonyms,
                        antonyms = meaning.antonyms
                    )
                }
            }
            
            // Fallback if API response is empty or invalid
            getFallbackWord()
            
        } catch (e: HttpException) {
            if (e.code() == 404) {
                // Word not found, try to get a fallback word
                getFallbackWord()
            } else {
                getFallbackWord()
            }
        } catch (e: IOException) {
            getFallbackWord()
        } catch (e: Exception) {
            getFallbackWord()
        }
    }
    
    private fun getFallbackWord(): WordInfo {
        val fallbackWords = listOf(
            WordInfo(
                word = "Serendipity",
                meaning = "The occurrence and development of events by chance in a happy or beneficial way",
                pronunciation = "ser-en-dip-i-ty",
                partOfSpeech = "noun",
                definitions = listOf("The occurrence and development of events by chance in a happy or beneficial way"),
                examples = listOf("Finding that book was pure serendipity."),
                synonyms = listOf("chance", "fortune", "luck"),
                antonyms = listOf("misfortune", "bad luck")
            ),
            WordInfo(
                word = "Mellifluous",
                meaning = "Sweet or musical; pleasant to hear",
                pronunciation = "mel-lif-lu-ous",
                partOfSpeech = "adjective",
                definitions = listOf("Sweet or musical; pleasant to hear"),
                examples = listOf("Her mellifluous voice captivated the audience."),
                synonyms = listOf("sweet", "melodious", "harmonious"),
                antonyms = listOf("harsh", "discordant", "unpleasant")
            ),
            WordInfo(
                word = "Petrichor",
                meaning = "A pleasant smell that frequently accompanies the first rain after a long period of warm, dry weather",
                pronunciation = "pet-ri-chor",
                partOfSpeech = "noun",
                definitions = listOf("A pleasant smell that frequently accompanies the first rain after a long period of warm, dry weather"),
                examples = listOf("The petrichor after the summer storm was refreshing."),
                synonyms = listOf("earthy scent", "rain smell"),
                antonyms = listOf("stale air", "dry dust")
            ),
            WordInfo(
                word = "Ineffable",
                meaning = "Too great or extreme to be expressed or described in words",
                pronunciation = "in-ef-fa-ble",
                partOfSpeech = "adjective",
                definitions = listOf("Too great or extreme to be expressed or described in words"),
                examples = listOf("The beauty of the sunset was ineffable."),
                synonyms = listOf("indescribable", "inexpressible", "unspeakable"),
                antonyms = listOf("describable", "expressible", "definable")
            ),
            WordInfo(
                word = "Sonder",
                meaning = "The realization that each random passerby is living a life as vivid and complex as your own",
                pronunciation = "son-der",
                partOfSpeech = "noun",
                definitions = listOf("The realization that each random passerby is living a life as vivid and complex as your own"),
                examples = listOf("Walking through the city gave me a sense of sonder."),
                synonyms = listOf("empathy", "awareness", "realization"),
                antonyms = listOf("ignorance", "indifference", "selfishness")
            )
        )
        
        return fallbackWords.random()
    }
    
    fun clearCache() {
        currentWord = null
        nextWord = null
    }
}
