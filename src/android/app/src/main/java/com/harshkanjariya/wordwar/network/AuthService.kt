package com.harshkanjariya.wordwar.network

import com.harshkanjariya.wordwar.data.ApiResponse
import retrofit2.http.POST
import retrofit2.http.Query

interface AuthService {
    @POST("/auth/social-login")
    suspend fun socialLogin(
        @Query("type") provider: String,
        @Query("accessToken") token: String
    ): ApiResponse<SocialLoginResponse>
}

data class SocialLoginResponse(
    val token: String
)
