package com.harshkanjariya.wordwar.network.service

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

data class MovePayload(
    val userId: String,
    val row: Int,
    val col: Int,
    val char: String
)

interface GameService {
    @POST("/game/{gameId}/move")
    suspend fun playMove(
        @Path("gameId") gameId: String,
        @Body payload: MovePayload
    )
}
