package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

data class SocialLoginParam(
    val type: String,
    val accessToken: String
)

interface AuthService {
    @POST("auth/social-login")
    suspend fun socialLogin(
        @Body() param: SocialLoginParam,
    ): ApiResponse<SocialLoginResponse>
}

data class SocialLoginResponse(
    val token: String
)
