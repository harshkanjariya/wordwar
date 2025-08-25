package com.harshkanjariya.wordwar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.harshkanjariya.wordwar.components.ClaimedWordsList
import com.harshkanjariya.wordwar.components.CustomKeyboard
import com.harshkanjariya.wordwar.components.GameControls
import com.harshkanjariya.wordwar.components.GameHeader
import com.harshkanjariya.wordwar.components.WordGrid
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.getUserIdFromJwt
import com.harshkanjariya.wordwar.network.service.CellCoordinatePayload
import com.harshkanjariya.wordwar.network.service.ClaimedWordPayload
import com.harshkanjariya.wordwar.network.service.GameActionPayload
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder
import com.harshkanjariya.wordwar.network.service_holder.isWordValid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class GameMode {
    FILLING,
    SELECTION
}

data class Cell(
    val index: Int,
    val char: String
)

enum class GamePhase {
    EDIT, SELECT
}

@Composable
fun GameScreen(navController: NavController, matchId: String?) {
    val scope = rememberCoroutineScope()
    val gridSize = 10
    val cells = remember { mutableStateListOf<String>().apply { addAll(List(gridSize * gridSize) { "" }) } }
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
    var phase by remember { mutableStateOf(GamePhase.EDIT) } // 🔥 new
    var turnTimestamp by remember { mutableLongStateOf(0L) }
    var remainingTime by remember { mutableIntStateOf(30) }
    var hasTriggeredTurnAdvance by remember { mutableStateOf(false) }

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
                    val newPhase = (gameData?.get("phase") as? String ?: "edit").uppercase()

                    phase = if (newPhase == "SELECT") GamePhase.SELECT else GamePhase.EDIT

                    if (newTurnTimestamp != turnTimestamp) {
                        turnTimestamp = newTurnTimestamp
                        hasTriggeredTurnAdvance = false
                        isSubmitted = false
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

    // --- Timer ---
    LaunchedEffect(turnTimestamp) {
        val initialTimeLeft = 30 - ((System.currentTimeMillis() - turnTimestamp) / 1000).toInt()
        remainingTime = initialTimeLeft.coerceAtLeast(0)

        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }

        if (remainingTime <= 0 && !hasTriggeredTurnAdvance && userId == currentPlayer) {
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (userId == currentPlayer && !isSubmitted) {
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
                                        gameService.submitAction(payload)
                                        isSubmitted = true
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
                                    if (word.isNotBlank() && isWordValid(word) && !claimedWords.contains(word)) {
                                        val coordinates = selectedCells.map { index ->
                                            CellCoordinatePayload(row = index / gridSize, col = index % gridSize)
                                        }
                                        val claimedWordPayload = ClaimedWordPayload(word, coordinates)

                                        val payload = GameActionPayload(
                                            character = null,
                                            row = null,
                                            col = null,
                                            claimedWords = listOf(claimedWordPayload)
                                        )
                                        try {
                                            gameService.submitAction(payload)
                                            isSubmitted = true
                                        } catch (e: Exception) {
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
                ) {
                    Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Submit")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                currentMode = if (phase == GamePhase.EDIT) GameMode.FILLING else GameMode.SELECTION,
                onToggleMode = {}, // 🔒 No manual toggle anymore
                onBackClick = { navController.popBackStack() }
            )
            Spacer(Modifier.height(12.dp))

            Text(text = if (userId == currentPlayer) "Your Turn!" else "Opponent's Turn", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = "Phase: ${phase.name}", fontSize = 18.sp)
            Text(text = "Time Left: $remainingTime s", fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))

            WordGrid(
                gridSize = gridSize,
                cells = cells,
                currentMode = if (phase == GamePhase.EDIT) GameMode.FILLING else GameMode.SELECTION,
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
            Spacer(Modifier.height(16.dp))

            GameControls(
                onClaimWord = { /* handled by FAB */ },
                onEndGame = {
                    scope.launch {
                        gameRef.child("players").child(userId).child("status").onDisconnect().cancel()
                        val result = GameServiceHolder.api.quitGame()
                        if (result.status == 200 && result.data != null) {
                            navController.popBackStack("menu", false)
                        }
                    }
                }
            )
            Spacer(Modifier.height(16.dp))
            ClaimedWordsList(claimedWords = claimedWords)
        }
    }

    if (isKeyboardVisible) {
        Box(
            modifier = Modifier.fillMaxSize().clickable {
                isKeyboardVisible = false
                selectedCellIndexForInput = -1
            }.background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            CustomKeyboard(
                onBackspaceClicked = { filledCell.value = null },
                onCharClicked = { char ->
                    scope.launch {
                        if (selectedCellIndexForInput != -1 && userId == currentPlayer) {
                            filledCell.value = Cell(index = selectedCellIndexForInput, char = char)
                        }
                        isKeyboardVisible = false
                        selectedCellIndexForInput = -1
                    }
                },
                modifier = Modifier.clickable { /* consume click */ }
            )
        }
    }
}