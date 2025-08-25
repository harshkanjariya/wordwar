package com.harshkanjariya.wordwar.network.service

import com.harshkanjariya.wordwar.data.ApiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Path

data class PlayerDto(
    val _id: String,
    val name: String,
    val joinedAt: String?,
    val claimedWords: List<String>
)

data class GameData(
    val createdAt: String,
    val updatedAt: String,
    val cellData: List<List<String>>,
    val players: List<PlayerDto>
)

data class ActiveGameResponse(
    val currentGameId: String? = null,
    val gameData: GameData? = null,
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

data class GameInfo(
    val players: List<String>,
    val joinedAt: Map<String, String>,
    val leftAt: Map<String, String>,
    val startedAt: String,
    val endedAt: String,
    val cellData: List<List<String>>,
    val claimedWords: Map<String, List<String>>
)

interface GameService {
    @POST("game/submit_action")
    suspend fun submitAction(
        @Body payload: GameActionPayload
    )

    @GET("game/active")
    suspend fun getActiveGame(): ApiResponse<ActiveGameResponse>

    @GET("game/quit")
    suspend fun quitGame(): ApiResponse<Boolean>

    @GET("game/info/{gameId}")
    suspend fun getGameInfo(
        @Path("gameId") gameId: String
    ): ApiResponse<GameInfo>
}
