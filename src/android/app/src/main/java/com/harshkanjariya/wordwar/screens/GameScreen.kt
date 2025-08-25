package com.harshkanjariya.wordwar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.harshkanjariya.wordwar.network.service.GameActionPayload
import com.harshkanjariya.wordwar.network.service.CellCoordinatePayload
import com.harshkanjariya.wordwar.network.service.ClaimedWordPayload
import com.harshkanjariya.wordwar.network.service.GameService
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder
import com.harshkanjariya.wordwar.network.service_holder.isWordValid
import kotlinx.coroutines.coroutineScope
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

@Composable
fun GameScreen(navController: NavController, matchId: String?) {
    val scope = rememberCoroutineScope()
    val gridSize = 10
    val cells = remember { mutableStateListOf<String>().apply { addAll(List(gridSize * gridSize) { "" }) } }
    var currentMode by remember { mutableStateOf(GameMode.FILLING) }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var selectedCellIndexForInput by remember { mutableIntStateOf(-1) }
    val filledCell = remember { mutableStateOf<Cell?>(null) }
    var claimedWords by remember { mutableStateOf(listOf<String>()) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isNavigatingToResults = remember { mutableStateOf(false) }

    val database = FirebaseDatabase.getInstance()
    val gameRef = database.getReference("live_games").child(matchId ?: "")
    val functions = FirebaseFunctions.getInstance("us-central1")
    val gameService = GameServiceHolder.api

    val token by LocalStorage.getToken(navController.context).collectAsState(initial = null)
    val userId = remember(token) {
        if (token.isNullOrEmpty()) {
            null
        } else {
            getUserIdFromJwt(token)
        }
    }

    var currentPlayer by remember { mutableStateOf("") }
    var turnTimestamp by remember { mutableLongStateOf(0L) }
    var remainingTime by remember { mutableIntStateOf(30) }
    var hasTriggeredTurnAdvance by remember { mutableStateOf(false) }

    // Live Firebase Listener
    DisposableEffect(matchId, userId) {
        if (matchId.isNullOrBlank() || userId.isNullOrBlank()) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        gameRef.child("players").child(userId).child("status").setValue("Online")
        gameRef.child("players").child(userId).child("status").onDisconnect().setValue("Offline")

        val gameListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isNavigatingToResults.value) {
                    if (!snapshot.exists()) {
                        // Game has been deleted
                        isNavigatingToResults.value = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Game has ended.")
                            delay(1000) // Give time for snackbar to show
                            // Use your game results screen route here
                            navController.navigate("game_results/$matchId") {
                                // Pop all back stack to prevent user from returning
                                popUpTo(navController.graph.id) {
                                    inclusive = true
                                }
                            }
                        }
                        return
                    }

                    val gameData = snapshot.value as? Map<String, Any>
                    val players = gameData?.get("players") as? Map<String, Any>

                    val newCurrentPlayer = gameData?.get("currentPlayer") as? String ?: ""
                    val newTurnTimestamp = gameData?.get("turnTimestamp") as? Long ?: 0L

                    currentPlayer = newCurrentPlayer
                    if (newTurnTimestamp != turnTimestamp) {
                        turnTimestamp = newTurnTimestamp
                        hasTriggeredTurnAdvance = false
                    }

                    if (players != null) {
                        val actions = players.flatMap { (playerId, playerInfo) ->
                            val infoMap = playerInfo as? Map<String, Any>
                            val actionsMap = infoMap?.get("actions") as? Map<String, Map<String, Any>>
                            actionsMap?.mapNotNull { (actionId, actionInfo) ->
                                val row = actionInfo["row"] as? Long
                                val col = actionInfo["col"] as? Long
                                val char = actionInfo["char"] as? String
                                if (row != null && col != null && char != null) {
                                    (row.toInt() * gridSize) + col.toInt() to char
                                } else null
                            } ?: emptyList()
                        }
                        actions.forEach { (index, char) -> cells[index] = char }
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

        if (remainingTime > 0) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
        }

        if (remainingTime <= 0 && !hasTriggeredTurnAdvance && userId == currentPlayer) {
            hasTriggeredTurnAdvance = true
            scope.launch {
                try {
                    val data = hashMapOf("gameId" to matchId)
                    functions
                        .getHttpsCallable("advanceTurn")
                        .call(data)
                        .await()
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
            if (userId == currentPlayer) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            val character = filledCell.value?.char
                            val row = filledCell.value?.index?.div(gridSize)
                            val col = filledCell.value?.index?.rem(gridSize)

                            val claimedWordPayloads = if (currentMode == GameMode.SELECTION) {
                                if (selectedCells.isNotEmpty()) {
                                    val word = buildString {
                                        selectedCells.sorted().forEach { index -> append(cells[index]) }
                                    }
                                    if (word.isNotBlank() && isWordValid(word) && !claimedWords.contains(word)) {
                                        val coordinates = selectedCells.map { index ->
                                            CellCoordinatePayload(row = index / gridSize, col = index % gridSize)
                                        }
                                        listOf(ClaimedWordPayload(word = word, cellCoordinates = coordinates))
                                    } else {
                                        snackbarHostState.showSnackbar("Invalid or already claimed word.")
                                        emptyList()
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("No cells selected.")
                                    emptyList()
                                }
                            } else {
                                emptyList()
                            }

                            if (character != null || claimedWordPayloads.isNotEmpty()) {
                                val payload = GameActionPayload(
                                    character = character,
                                    row = row,
                                    col = col,
                                    claimedWords = claimedWordPayloads
                                )

                                try {
                                    gameService.submitAction(payload)
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Failed to submit action: ${e.message}")
                                }
                            } else {
                                snackbarHostState.showSnackbar("Please make a move or select a word to claim.")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GameHeader(
                currentMode = currentMode,
                onToggleMode = {
                    currentMode = if (currentMode == GameMode.FILLING) GameMode.SELECTION else GameMode.FILLING
                    selectedCells = emptySet()
                    isKeyboardVisible = false
                },
                onBackClick = { navController.popBackStack() }
            )
            Spacer(Modifier.height(12.dp))

            Text(text = if (userId == currentPlayer) "Your Turn!" else "Opponent's Turn", fontSize = 20.sp)
            Spacer(Modifier.height(8.dp))
            Text(text = "Time Left: $remainingTime s", fontSize = 24.sp)
            Spacer(Modifier.height(16.dp))

            WordGrid(
                gridSize = gridSize,
                cells = cells,
                currentMode = currentMode,
                filledCell = filledCell.value,
                highlightFilledCell = currentMode == GameMode.FILLING,
                onCellClick = { index ->
                    if (currentMode == GameMode.FILLING) {
                        if (cells[index].isBlank()) {
                            isKeyboardVisible = true
                            selectedCellIndexForInput = index
                        } else if (index == filledCell.value?.index) {
                            filledCell.value = null
                        }
                    } else {
                        scope.launch { snackbarHostState.showSnackbar("It's not your turn.") }
                    }
                },
                onCellsSelected = { newCells -> selectedCells = newCells },
                selectedCells = selectedCells
            )
            Spacer(Modifier.height(16.dp))

            GameControls(
                onClaimWord = { /* Logic handled by FAB now */ },
                onEndGame = {
                    scope.launch {
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
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    onClick = {
                        isKeyboardVisible = false
                        selectedCellIndexForInput = -1
                    }
                )
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            CustomKeyboard(
                onBackspaceClicked = {
                    filledCell.value = null
                },
                onCharClicked = { char ->
                    scope.launch {
                        if (selectedCellIndexForInput != -1 && userId == currentPlayer) {
                            filledCell.value = Cell(
                                index = selectedCellIndexForInput,
                                char = char
                            )
                        }
                        isKeyboardVisible = false
                        selectedCellIndexForInput = -1
                    }
                },
                modifier = Modifier.clickable(onClick = { /* consume click to prevent parent from closing */ })
            )
        }
    }
}