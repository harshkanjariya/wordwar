package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header

data class ActiveGameResponse(
    val currentGameId: String? = null,
    val liveGameData: Map<String, Any>? = null
)

data class CellCoordinatePayload(
    val row: Int? = null,
    val col: Int? = null
)

data class ClaimedWordPayload(
    val word: String? = null,
    val cellCoordinates: List<CellCoordinatePayload>? = null
)

data class GameActionPayload(
    val character: String? = null,
    val row: Int? = null,
    val col: Int? = null,
    val claimedWords: List<ClaimedWordPayload>? = null
)

interface GameService {
    @POST("/api/game/submit_action")
    suspend fun submitAction(
        @Body payload: GameActionPayload
    )

    @GET("/api/game/active")
    suspend fun getActiveGame(): ApiResponse<ActiveGameResponse>

    @GET("/api/game/quit")
    suspend fun quitGame(): ApiResponse<Boolean>
}
