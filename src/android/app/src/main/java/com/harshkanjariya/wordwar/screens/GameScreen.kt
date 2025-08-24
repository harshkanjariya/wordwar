package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.functions.FirebaseFunctions
import com.harshkanjariya.wordwar.components.ClaimedWordsList
import com.harshkanjariya.wordwar.components.FillCellDialog
import com.harshkanjariya.wordwar.components.GameControls
import com.harshkanjariya.wordwar.components.GameHeader
import com.harshkanjariya.wordwar.components.WordGrid
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.getUserIdFromJwt
import com.harshkanjariya.wordwar.network.isWordValid
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class GameMode {
    FILLING,
    SELECTION
}

@Composable
fun GameScreen(navController: NavController, matchId: String?) {
    val scope = rememberCoroutineScope()
    val gridSize = 10
    val cells = remember { mutableStateListOf<String>().apply { addAll(List(gridSize * gridSize) { "" }) } }
    var currentMode by remember { mutableStateOf(GameMode.FILLING) }
    var showFillDialog by remember { mutableStateOf(false) }
    var cellToFillIndex by remember { mutableIntStateOf(-1) }
    var characterInput by remember { mutableStateOf("") }
    var claimedWords by remember { mutableStateOf(listOf<String>()) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }

    val database = FirebaseDatabase.getInstance()
    val gameRef = database.getReference("live_games").child(matchId ?: "")
    val functions = FirebaseFunctions.getInstance("us-central1")

    val token by LocalStorage.getToken(navController.context).collectAsState(initial = null)
    val userId = remember(token) {
        if (token.isNullOrEmpty()) {
            null
        } else {
            getUserIdFromJwt(token)
        }
    }

    // New state variables for game logic and timer
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

        // Set player status on connect and disconnect
        gameRef.child("players").child(userId).child("status").setValue("Online")
        gameRef.child("players").child(userId).child("status").onDisconnect().setValue("Offline")

        val gameListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameData = snapshot.value as? Map<String, Any>
                val players = gameData?.get("players") as? Map<String, Any>

                // Get the current player and turn timestamp from the server
                val newCurrentPlayer = gameData?.get("currentPlayer") as? String ?: ""
                val newTurnTimestamp = gameData?.get("turnTimestamp") as? Long ?: 0L

                // Update state variables
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

            override fun onCancelled(error: DatabaseError) {
                scope.launch { snackbarHostState.showSnackbar("Failed to load game data: ${error.message}") }
            }
        }
        gameRef.addValueEventListener(gameListener)
        onDispose { gameRef.removeEventListener(gameListener) }
    }


    // Timer logic to run whenever the turnTimestamp updates
    LaunchedEffect(turnTimestamp) {
        val initialTimeLeft = 30 - ((System.currentTimeMillis() - turnTimestamp) / 1000).toInt()
        remainingTime = initialTimeLeft.coerceAtLeast(0)

        if (remainingTime > 0) {
            while (remainingTime > 0) {
                delay(1000)
                remainingTime--
            }
        }

        // Trigger the Firebase Function when the timer hits zero
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            Modifier
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
                },
                onBackClick = { navController.popBackStack() }
            )
            Spacer(Modifier.height(12.dp))

            // Display turn info and timer
            Text(
                text = if (userId == currentPlayer) "Your Turn!" else "Opponent's Turn",
                fontSize = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Time Left: $remainingTime s",
                fontSize = 24.sp
            )
            Spacer(Modifier.height(16.dp))

            WordGrid(
                gridSize = gridSize,
                cells = cells,
                currentMode = currentMode,
                onCellClick = { index ->
                    // Only allow filling a cell if it's the current player's turn and the cell is empty
                    if (userId == currentPlayer && currentMode == GameMode.FILLING) {
                        if (cells[index].isBlank()) {
                            cellToFillIndex = index
                            showFillDialog = true
                        }
                    } else {
                        // Optionally show a message if it's not their turn
                        scope.launch {
                            snackbarHostState.showSnackbar("It's not your turn.")
                        }
                    }
                },
                onCellsSelected = { newCells -> selectedCells = newCells },
                selectedCells = selectedCells
            )
            Spacer(Modifier.height(16.dp))

            GameControls(
                onClaimWord = {
                    scope.launch {
                        // Only allow claiming if it's the current player's turn
                        if (userId == currentPlayer && currentMode == GameMode.SELECTION && selectedCells.isNotEmpty()) {
                            val claimedWord = buildString { selectedCells.sorted().forEach { index -> append(cells[index]) } }
                            if (claimedWord.isNotBlank()) {
                                if (claimedWords.contains(claimedWord)) {
                                    snackbarHostState.showSnackbar("This word is already claimed!")
                                } else if (isWordValid(claimedWord)) {
                                    claimedWords = claimedWords + claimedWord
                                    selectedCells = emptySet()
                                    // TODO: Add logic to update Firebase with the claimed word
                                } else {
                                    snackbarHostState.showSnackbar("Invalid word.")
                                    selectedCells = emptySet()
                                }
                            }
                        }
                    }
                },
                onEndGame = { /* TODO: Implement end game logic */ }
            )
            Spacer(Modifier.height(16.dp))
            ClaimedWordsList(claimedWords = claimedWords)
        }

        if (showFillDialog) {
            FillCellDialog(
                onDismiss = { showFillDialog = false; characterInput = "" },
                onConfirm = {
                    // Only confirm and send to Firebase if it's the current player's turn
                    if (userId == currentPlayer && characterInput.isNotBlank()) {
                        scope.launch {
                            // Update Firebase with the move
                            if (!matchId.isNullOrBlank() && !userId.isNullOrBlank()) {
                                val moveRef = gameRef.child("players").child(userId).child("actions").push()
                                val moveData = mapOf(
                                    "row" to cellToFillIndex / gridSize,
                                    "col" to cellToFillIndex % gridSize,
                                    "char" to characterInput,
                                    "timestamp" to System.currentTimeMillis()
                                )
                                runCatching { moveRef.setValue(moveData).await() }
                                    .onFailure { snackbarHostState.showSnackbar("Failed to send move: ${it.message}") }
                            }
                        }
                        showFillDialog = false
                        characterInput = ""
                    }
                },
                characterInput = characterInput,
                onCharacterInputChange = { newValue ->
                    if (newValue.length <= 1 && newValue.all { it.isLetter() }) {
                        characterInput = newValue.uppercase()
                    }
                }
            )
        }
    }
}
