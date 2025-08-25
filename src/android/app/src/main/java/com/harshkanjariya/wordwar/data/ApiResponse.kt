package com.harshkanjariya.wordwar.data

data class ApiResponse<T>(
    val status: Int,
    val data: T?,
    val message: String?
)
