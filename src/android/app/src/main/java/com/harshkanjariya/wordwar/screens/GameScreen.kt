package com.harshkanjariya.wordwar.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.harshkanjariya.wordwar.components.CustomKeyboard
import com.harshkanjariya.wordwar.components.WordGrid
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.getUserIdFromJwt
import com.harshkanjariya.wordwar.network.service.CellCoordinatePayload
import com.harshkanjariya.wordwar.network.service.ClaimedWordPayload
import com.harshkanjariya.wordwar.network.service.GameActionPayload
import com.harshkanjariya.wordwar.network.service.GameData
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder
import com.harshkanjariya.wordwar.network.service_holder.isWordValid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class Cell(
    val index: Int,
    val char: String
)

enum class GamePhase {
    EDIT, SELECT
}

fun phaseStringToEnum(phase: String?): GamePhase {
    if (phase != null && phase.uppercase() == "SELECT") return GamePhase.SELECT
    return GamePhase.EDIT
}

@Composable
fun GameScreen(navController: NavController, matchId: String?) {
    val scope = rememberCoroutineScope()
    val gridSize = 10
    val cells =
        remember { mutableStateListOf<String>().apply { addAll(List(gridSize * gridSize) { "" }) } }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var selectedCellIndexForInput by remember { mutableIntStateOf(-1) }
    val filledCell = remember { mutableStateOf<Cell?>(null) }
    var claimedWords by remember { mutableStateOf(listOf<String>()) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isNavigatingToResults = remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val gameRef = database.getReference("live_games").child(matchId ?: "")
    val functions = FirebaseFunctions.getInstance("us-central1")
    val gameService = GameServiceHolder.api

    val token by LocalStorage.getToken().collectAsState(initial = null)
    val userId: String = remember(token) {
        if (token.isNullOrEmpty()) "" else getUserIdFromJwt(token) ?: ""
    }

    var currentPlayer by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(GamePhase.EDIT) }
    var turnTimestamp by remember { mutableLongStateOf(0L) }
    var remainingTime by remember { mutableIntStateOf(30) }
    var hasTriggeredTurnAdvance by remember { mutableStateOf(false) }

    var activeGame by remember { mutableStateOf<GameData?>(null) }
    var showPlayerInfo by remember { mutableStateOf(false) }

    DisposableEffect(matchId, userId) {
        if (matchId.isNullOrBlank() || userId.isBlank()) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        val onDisconnectRef = gameRef.child("players").child(userId).child("status").onDisconnect()
        gameRef.child("players").child(userId).child("status").setValue("Online")
        onDisconnectRef.setValue("Offline")

        val gameListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isNavigatingToResults.value) {
                    if (!snapshot.exists()) {
                        onDisconnectRef.cancel()
                        isNavigatingToResults.value = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Game has ended.")
                            delay(1000)
                            navController.navigate("game_results/$matchId") {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                        return
                    }

                    val gameData = snapshot.value as? Map<String, Any>
                    val newCellData2D = gameData?.get("cellData") as? List<List<String>>
                    if (newCellData2D != null) {
                        val newCellData1D = newCellData2D.flatten()
                        cells.clear()
                        cells.addAll(newCellData1D)
                    }

                    currentPlayer = gameData?.get("currentPlayer") as? String ?: ""
                    val newTurnTimestamp = gameData?.get("turnTimestamp") as? Long ?: 0L
                    val newPhase = phaseStringToEnum(gameData?.get("phase") as? String)

                    if (phase != newPhase) {
                        phase = newPhase
                    }

                    selectedCellIndexForInput = -1
                    filledCell.value = null
                    selectedCells = emptySet()

                    if (newTurnTimestamp != turnTimestamp) {
                        turnTimestamp = newTurnTimestamp
                        hasTriggeredTurnAdvance = false
                        isSubmitted = false

                        scope.launch {
                            try {
                                val response = gameService.getActiveGame()
                                activeGame = response.data?.gameData
                            } catch (e: Exception) {
                                println("Error fetching active game: ${e.message}")
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                scope.launch { snackbarHostState.showSnackbar("Failed to load game data: ${error.message}") }
            }
        }
        gameRef.addValueEventListener(gameListener)
        onDispose { gameRef.removeEventListener(gameListener) }
    }

    LaunchedEffect(turnTimestamp) {
        val initialTimeLeft = 30 - ((System.currentTimeMillis() - turnTimestamp) / 1000).toInt()
        remainingTime = initialTimeLeft.coerceAtLeast(0)

        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }

        if (!hasTriggeredTurnAdvance && userId == currentPlayer) {
            hasTriggeredTurnAdvance = true
            scope.launch {
                try {
                    val data = hashMapOf("gameId" to matchId)
                    functions.getHttpsCallable("advanceTurn").call(data).await()
                } catch (e: Exception) {
                    println("Failed to call advanceTurn: ${e.message}")
                }
            }
        }
    }

    BackHandler(enabled = isKeyboardVisible) {
        isKeyboardVisible = false
        selectedCellIndexForInput = -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Main Game Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 80.dp)
        ) {
            // Top Game Status Bar
            TopGameStatusBar(
                isCurrentPlayer = userId == currentPlayer,
                phase = phase,
                remainingTime = remainingTime,
                onBackClick = { navController.popBackStack() },
                onPlayerInfoClick = { showPlayerInfo = true }
            )

            // Game Grid Section
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                WordGrid(
                    gridSize = gridSize,
                    cells = cells,
                    currentMode = phase,
                    filledCell = filledCell.value,
                    highlightFilledCell = (phase == GamePhase.EDIT),
                    onCellClick = { index ->
                        if (phase == GamePhase.EDIT && currentPlayer == userId && !isSubmitted) {
                            if (cells[index].isBlank()) {
                                isKeyboardVisible = true
                                selectedCellIndexForInput = index
                            } else if (index == filledCell.value?.index) {
                                filledCell.value = null
                            }
                        }
                    },
                    onCellsSelected = { newCells ->
                        if (phase == GamePhase.SELECT) selectedCells = newCells
                    },
                    selectedCells = selectedCells
                )
            }

            BottomGameControls(
                phase = phase,
                isCurrentPlayer = userId == currentPlayer,
                isSubmitted = isSubmitted,
                selectedCells = selectedCells,
                filledCell = filledCell.value,
                onEndGame = {
                    scope.launch {
                        gameRef.child("players").child(userId).child("status").onDisconnect()
                            .cancel()
                        val result = GameServiceHolder.api.quitGame()
                        if (result.status == 200 && result.data != null) {
                            navController.navigate("menu") {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    }
                },
                onSubmit = {
                    scope.launch {
                        if (phase == GamePhase.EDIT) {
                            val character = filledCell.value?.char
                            val row = filledCell.value?.index?.div(gridSize)
                            val col = filledCell.value?.index?.rem(gridSize)

                            if (character != null && row != null && col != null) {
                                val payload = GameActionPayload(
                                    character = character,
                                    row = row,
                                    col = col,
                                    claimedWords = emptyList()
                                )
                                try {
                                    val response = gameService.submitAction(payload)
                                    if (response.status != 200) {
                                        snackbarHostState.showSnackbar(response.message ?: "Action failed")
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed: ${e.message}")
                                }
                            } else {
                                snackbarHostState.showSnackbar("Pick a cell and letter first.")
                            }

                        } else if (phase == GamePhase.SELECT) {
                            if (selectedCells.isNotEmpty()) {
                                val word = buildString {
                                    selectedCells.sorted().forEach { index -> append(cells[index]) }
                                }
                                if (word.isNotBlank() && isWordValid(word) && !claimedWords.contains(
                                        word
                                    )
                                ) {
                                    val coordinates = selectedCells.map { index ->
                                        CellCoordinatePayload(
                                            row = index / gridSize,
                                            col = index % gridSize
                                        )
                                    }
                                    val claimedWordPayload = ClaimedWordPayload(word, coordinates)

                                    val payload = GameActionPayload(
                                        character = null,
                                        row = null,
                                        col = null,
                                        claimedWords = listOf(claimedWordPayload)
                                    )
                                    try {
                                        val response = gameService.submitAction(payload)
                                        if (response.status == 200) {
                                            isSubmitted = true
                                        } else {
                                            snackbarHostState.showSnackbar(response.message ?: "Action failed")
                                        }
                                    } catch (e: Exception) {
                                        Log.e("submitAction", e.toString())
                                        snackbarHostState.showSnackbar("Failed: ${e.message}")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Invalid or already claimed word.")
                                }
                            } else {
                                snackbarHostState.showSnackbar("Select cells to form a word.")
                            }
                        }
                    }
                }
            )
        }

        // Floating Action Button for Submit
        if (userId == currentPlayer && !isSubmitted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .padding(bottom = 16.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (phase == GamePhase.EDIT) {
                                val character = filledCell.value?.char
                                val row = filledCell.value?.index?.div(gridSize)
                                val col = filledCell.value?.index?.rem(gridSize)

                                if (character != null && row != null && col != null) {
                                    val payload = GameActionPayload(
                                        character = character,
                                        row = row,
                                        col = col,
                                        claimedWords = emptyList()
                                    )
                                    try {
                                        val response = gameService.submitAction(payload)
                                        if (response.status != 200) {
                                            snackbarHostState.showSnackbar(response.message ?: "Action failed")
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("Failed: ${e.message}")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Pick a cell and letter first.")
                                }

                            } else if (phase == GamePhase.SELECT) {
                                if (selectedCells.isNotEmpty()) {
                                    val word = buildString {
                                        selectedCells.sorted()
                                            .forEach { index -> append(cells[index]) }
                                    }
                                    if (word.isNotBlank() && isWordValid(word) && !claimedWords.contains(
                                            word
                                        )
                                    ) {
                                        val coordinates = selectedCells.map { index ->
                                            CellCoordinatePayload(
                                                row = index / gridSize,
                                                col = index % gridSize
                                            )
                                        }
                                        val claimedWordPayload =
                                            ClaimedWordPayload(word, coordinates)

                                        val payload = GameActionPayload(
                                            character = null,
                                            row = null,
                                            col = null,
                                            claimedWords = listOf(claimedWordPayload)
                                        )
                                        try {
                                            val response = gameService.submitAction(payload)
                                            if (response.status == 200) {
                                                isSubmitted = true
                                            } else {
                                                snackbarHostState.showSnackbar(response.message ?: "Action failed")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("submitAction", e.toString())
                                            snackbarHostState.showSnackbar("Failed: ${e.message}")
                                        }
                                    } else {
                                        snackbarHostState.showSnackbar("Invalid or already claimed word.")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("Select cells to form a word.")
                                }
                            }
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = "Submit",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Enhanced Custom Keyboard Overlay
        if (isKeyboardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clickable {
                        isKeyboardVisible = false
                        selectedCellIndexForInput = -1
                    },
                contentAlignment = Alignment.BottomCenter
            ) {
                CustomKeyboard(
                    onBackspaceClicked = { filledCell.value = null },
                    onCharClicked = { char ->
                        scope.launch {
                            if (selectedCellIndexForInput != -1 && userId == currentPlayer) {
                                filledCell.value =
                                    Cell(index = selectedCellIndexForInput, char = char)
                            }
                            isKeyboardVisible = false
                            selectedCellIndexForInput = -1
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Player Info Overlay (replaces bottom sheet)
        if (showPlayerInfo && activeGame != null) {
            PlayerInfoOverlay(
                activeGame = activeGame!!,
                onDismiss = { showPlayerInfo = false }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun TopGameStatusBar(
    isCurrentPlayer: Boolean,
    phase: GamePhase,
    remainingTime: Int,
    onBackClick: () -> Unit,
    onPlayerInfoClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Game Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Turn Status
                Text(
                    text = if (isCurrentPlayer) "Your Turn!" else "Opponent's Turn",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrentPlayer) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )

                // Phase
                Text(
                    text = "Phase: ${phase.name}",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Player Info Button
            IconButton(
                onClick = onPlayerInfoClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Player Info",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Timer Bar
        LinearProgressIndicator(
            progress = remainingTime / 30f,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 0.dp),
            color = if (remainingTime <= 10) {
                MaterialTheme.colorScheme.error
            } else if (remainingTime <= 20) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        // Time Display
        Text(
            text = "$remainingTime s",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = if (remainingTime <= 10) {
                    MaterialTheme.colorScheme.error
                } else if (remainingTime <= 20) {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )
    }
}

@Composable
private fun BottomGameControls(
    phase: GamePhase,
    isCurrentPlayer: Boolean,
    isSubmitted: Boolean,
    selectedCells: Set<Int>,
    filledCell: Cell?,
    onEndGame: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Game Instructions
            Text(
                text = when {
                    phase == GamePhase.EDIT && isCurrentPlayer && !isSubmitted ->
                        "Tap an empty cell and select a letter"

                    phase == GamePhase.SELECT && isCurrentPlayer && !isSubmitted ->
                        "Select cells to form a word"

                    else -> "Waiting for opponent..."
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onEndGame,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("End Game")
                }

                if (isCurrentPlayer && !isSubmitted) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.weight(1f),
                        enabled = when {
                            phase == GamePhase.EDIT -> filledCell != null
                            phase == GamePhase.SELECT -> selectedCells.isNotEmpty()
                            else -> false
                        }
                    ) {
                        Text("Submit")
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PlayerInfoOverlay(
    activeGame: GameData,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable { },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Players",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(20.dp))

                // Player list with actual data
                activeGame.players.forEach { player ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = player.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Claimed Words:",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            )

                            Spacer(Modifier.height(4.dp))

                            if (player.claimedWords.isNotEmpty()) {
                                player.claimedWords.forEach { word ->
                                    Text(
                                        text = "• $word",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 14.sp
                                        ),
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "No words claimed yet",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = FontStyle.Italic
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Close",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }
        }
    }
}
