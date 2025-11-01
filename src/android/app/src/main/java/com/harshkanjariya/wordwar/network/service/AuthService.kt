package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

data class SocialLoginParam(
    val type: String,
    val accessToken: String
)

data class RegisterParam(
    val name: String,
    val email: String,
    val password: String
)

data class LoginParam(
    val email: String,
    val password: String
)

interface AuthService {
    @POST("auth/social-login")
    suspend fun socialLogin(
        @Body() param: SocialLoginParam,
    ): ApiResponse<SocialLoginResponse>

    @POST("auth/register")
    suspend fun register(
        @Body() param: RegisterParam,
    ): ApiResponse<AuthResponse>

    @POST("auth/login")
    suspend fun login(
        @Body() param: LoginParam,
    ): ApiResponse<AuthResponse>
}

data class SocialLoginResponse(
    val token: String
)

data class AuthResponse(
    val token: String
)
