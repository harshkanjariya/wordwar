package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import com.harshkanjariya.wordwar.data.UserProfile
import com.harshkanjariya.wordwar.data.UserStatistics
import retrofit2.http.GET
import retrofit2.http.DELETE

interface UserService {
    @GET("users")
    suspend fun getUserProfile(): ApiResponse<UserProfile>
    
    @GET("users/statistics")
    suspend fun getUserStatistics(): ApiResponse<UserStatistics>
    
    @DELETE("users/account")
    suspend fun deleteAccount(): ApiResponse<Unit>
}
