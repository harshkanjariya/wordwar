package com.harshkanjariya.wordwar.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

object LocalStorage {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")

    suspend fun saveToken(context: Context, token: String) {
        context.dataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token
        }
    }

    fun getToken(context: Context): Flow<String?> = context.dataStore.data
        .map { prefs -> prefs[TOKEN_KEY] }

    suspend fun removeToken(context: Context) {
        context.dataStore.edit {
            it.remove(TOKEN_KEY)
        }
    }
}

fun getUserIdFromJwt(jwtToken: String?): String? {
    if (jwtToken == null) return ""
    try {
        val parts = jwtToken.split(".")
        if (parts.size != 3) return null

        // Decode the payload part of the token
        val payload = parts[1]
        val payloadBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_PADDING)
        val payloadJson = String(payloadBytes, Charsets.UTF_8)

        // Parse the JSON to get the user ID
        val jsonObject = Gson().fromJson(payloadJson, JsonObject::class.java)
        return jsonObject.get("_id")?.asString
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}
