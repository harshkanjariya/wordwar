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