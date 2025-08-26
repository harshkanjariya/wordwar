package com.harshkanjariya.wordwar.data

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("_id")
    val id: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("name")
    val name: String? = null,
)

data class UserStatistics(
    @SerializedName("totalGames")
    val totalGames: Int = 0,
    @SerializedName("gamesWon")
    val gamesWon: Int = 0,
    @SerializedName("totalPoints")
    val totalPoints: Int = 0,
    @SerializedName("winRate")
    val winRate: Int = 0
)