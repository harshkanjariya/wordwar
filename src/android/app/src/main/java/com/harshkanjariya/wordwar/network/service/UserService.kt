package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import com.harshkanjariya.wordwar.data.UserProfile
import retrofit2.http.GET

interface UserService {
    @GET("/api/users")
    suspend fun getUserProfile(): ApiResponse<UserProfile>
}
