package com.harshkanjariya.wordwar.data

import com.google.gson.annotations.SerializedName

data class UserProfile(
    @SerializedName("_id")
    val id: String,
    @SerializedName("email")
    val email: String,
    @SerializedName("name")
    val name: String,
)