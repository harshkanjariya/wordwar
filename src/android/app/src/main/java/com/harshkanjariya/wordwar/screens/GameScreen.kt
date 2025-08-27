package com.harshkanjariya.wordwar.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

data class WordWithCoordinates(
    val word: String,
    val coordinates: List<CellCoordinatePayload>
)

enum class GamePhase {
    EDIT, SELECT
}

fun phaseStringToEnum(phase: String?): GamePhase {
    if (phase != null && phase.uppercase() == "SELECT") return GamePhase.SELECT
    return GamePhase.EDIT
}

// Gamified Message System Types
enum class MessageType {
    INFO, SUCCESS, WARNING, ERROR
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
    // Add new state for selected words list
    var selectedWords by remember { mutableStateOf(listOf<WordWithCoordinates>()) }
    val isNavigatingToResults = remember { mutableStateOf(false) }
    var isSubmitted by remember { mutableStateOf(false) }

    // Gamified Message System States
    var showMessage by remember { mutableStateOf(false) }
    var messageText by remember { mutableStateOf("") }
    var messageType by remember { mutableStateOf(MessageType.INFO) }
    var messageIcon by remember { mutableStateOf<ImageVector?>(null) }

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
    var voteEndGamePlayers by remember { mutableStateOf<List<String>>(emptyList()) }

    fun showGamifiedMessage(text: String, type: MessageType, icon: ImageVector? = null) {
        messageText = text
        messageType = type
        messageIcon = icon
        showMessage = true

        scope.launch {
            val duration = when (type) {
                MessageType.SUCCESS -> 4000L // Success messages stay longer
                MessageType.ERROR -> 5000L   // Error messages stay longer
                MessageType.WARNING -> 3500L // Warning messages
                MessageType.INFO -> 3000L    // Info messages
            }
            delay(duration)
            showMessage = false
        }

        // Add haptic feedback for different message types
        when (type) {
            MessageType.SUCCESS -> {
                // Could add haptic feedback here
            }

            MessageType.ERROR -> {
                // Could add haptic feedback here
            }

            else -> {}
        }
    }

    fun calculateWordPoints(word: String): Int {
        return word.length * 10
    }

    // Add function to add selected word to list
    suspend fun addSelectedWord() {
        if (selectedCells.isNotEmpty()) {
            val word = buildString {
                selectedCells.forEach { index -> append(cells[index]) }
            }
            if (word.isBlank()) {
                showGamifiedMessage(
                    "No word selected!",
                    MessageType.WARNING,
                    Icons.Default.Info
                )
                return
            }
            if (!isWordValid(word)) {
                showGamifiedMessage(
                    "Invalid word: '$word'",
                    MessageType.ERROR,
                    Icons.Default.Warning
                )
                return
            }
            if (claimedWords.contains(word)) {
                showGamifiedMessage(
                    "Word '$word' already claimed!",
                    MessageType.ERROR,
                    Icons.Default.Warning
                )
                return
            }
            if (selectedWords.any { it.word == word }) {
                showGamifiedMessage(
                    "Word '$word' already in your list!",
                    MessageType.ERROR,
                    Icons.Default.Warning
                )
                return
            }
            
            selectedWords = selectedWords + WordWithCoordinates(word, selectedCells.map { index ->
                CellCoordinatePayload(
                    row = index / gridSize,
                    col = index % gridSize
                )
            })
            selectedCells = emptySet()
            showGamifiedMessage(
                "Added '$word' to your list! 📝",
                MessageType.SUCCESS,
                Icons.Default.CheckCircle
            )
        } else {
            showGamifiedMessage(
                "Select cells to form a word first! 🔤",
                MessageType.WARNING,
                Icons.Default.Info
            )
        }
    }

    // Add function to remove word from selected list
    fun removeSelectedWord(word: String) {
        selectedWords = selectedWords.filter { it.word != word }
        showGamifiedMessage(
            "Removed '$word' from your list! 🗑️",
            MessageType.INFO,
            Icons.Default.Info
        )
    }

    fun checkForNewWordClaims(oldGameData: GameData, newGameData: GameData) {
        newGameData.players.forEach { newPlayer ->
            val oldPlayer = oldGameData.players.find { it._id == newPlayer._id }
            if (oldPlayer != null) {
                val newWords = newPlayer.claimedWords.toSet()
                val oldWords = oldPlayer.claimedWords.toSet()
                val newlyClaimedWords = newWords - oldWords

                if (newlyClaimedWords.isNotEmpty()) {
                    // Combine all new words into one message
                    val totalPoints = newlyClaimedWords.sumOf { calculateWordPoints(it) }
                    val wordsList = newlyClaimedWords.joinToString(", ")
                    val wordCount = newlyClaimedWords.size

                    val message = if (wordCount == 1) {
                        "🎯 ${newPlayer.name} claimed '${newlyClaimedWords.first()}' (+$totalPoints points)!"
                    } else {
                        "🎯 ${newPlayer.name} claimed $wordCount words: $wordsList (+$totalPoints points total)!"
                    }

                    showGamifiedMessage(
                        message,
                        MessageType.SUCCESS,
                        Icons.Default.ThumbUp
                    )
                }
            }
        }
    }

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
                            showGamifiedMessage(
                                "Game has ended! 🏁",
                                MessageType.INFO,
                                Icons.Default.Info
                            )
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

                    // Update vote end game players
                    val newVoteEndGamePlayers =
                        gameData?.get("voteEndGame") as? List<String> ?: emptyList()
                    voteEndGamePlayers = newVoteEndGamePlayers

                    if (phase != newPhase) {
                        phase = newPhase
                        // Reset selected words when phase changes
                        selectedWords = emptyList()
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
                                val newGameData = response.data?.gameData

                                // Check for new word claims
                                if (newGameData != null && activeGame != null) {
                                    checkForNewWordClaims(activeGame!!, newGameData)
                                }

                                activeGame = newGameData
                            } catch (e: Exception) {
                                println("Error fetching active game: ${e.message}")
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                scope.launch {
                    showGamifiedMessage(
                        "Failed to load game data: ${error.message}",
                        MessageType.ERROR,
                        Icons.Default.Warning
                    )
                }
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
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Main layout with status bar and scrollable content
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Game Status Bar (Sticky)
            TopGameStatusBar(
                isCurrentPlayer = userId == currentPlayer,
                phase = phase,
                remainingTime = remainingTime,
                currentPlayerId = currentPlayer,
                activeGame = activeGame,
                onExitClick = {
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
                onPlayerInfoClick = { showPlayerInfo = true }
            )

            // Scrollable Game Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Game Grid Section
                Box(
                    modifier = Modifier
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
                    voteEndGamePlayers = voteEndGamePlayers,
                    currentUserId = userId,
                    selectedWords = selectedWords,
                    onAddWord = { 
                        scope.launch {
                            addSelectedWord()
                        }
                    },
                    onRemoveWord = { removeSelectedWord(it) },
                    onSpectate = {
                        scope.launch {
                            gameRef.child("players").child(userId).child("status").onDisconnect()
                                .cancel()
                            val result = GameServiceHolder.api.quitGame()
                            if (result.status == 200 && result.data != null) {
                                showGamifiedMessage(
                                    "You are now spectating! 👀",
                                    MessageType.INFO,
                                    Icons.Default.Info
                                )
                            }
                        }
                    },
                    onVoteEndGame = {
                        scope.launch {
                            try {
                                val isVoted = voteEndGamePlayers.contains(userId)
                                if (isVoted) {
                                    val updatedVotes = voteEndGamePlayers.filter { it != userId }
                                    gameRef.child("voteEndGame").setValue(updatedVotes)
                                    showGamifiedMessage(
                                        "Vote removed! 🗳️",
                                        MessageType.INFO,
                                        Icons.Default.Info
                                    )
                                } else {
                                    // Add vote
                                    val updatedVotes = voteEndGamePlayers + userId
                                    gameRef.child("voteEndGame").setValue(updatedVotes)
                                    showGamifiedMessage(
                                        "Vote added! 🗳️",
                                        MessageType.INFO,
                                        Icons.Default.Info
                                    )
                                }
                            } catch (e: Exception) {
                                showGamifiedMessage(
                                    "Failed to update vote: ${e.message}",
                                    MessageType.ERROR,
                                    Icons.Default.Warning
                                )
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
                                            showGamifiedMessage(
                                                response.message ?: "Action failed",
                                                MessageType.ERROR,
                                                Icons.Default.Warning
                                            )
                                        } else {
                                            showGamifiedMessage(
                                                "Letter placed successfully! 🎯",
                                                MessageType.SUCCESS,
                                                Icons.Default.CheckCircle
                                            )
                                        }
                                    } catch (e: Exception) {
                                        showGamifiedMessage(
                                            "Failed: ${e.message}",
                                            MessageType.ERROR,
                                            Icons.Default.Warning
                                        )
                                    }
                                } else {
                                    showGamifiedMessage(
                                        "Pick a cell and letter first! 📝",
                                        MessageType.WARNING,
                                        Icons.Default.Edit
                                    )
                                }

                            } else if (phase == GamePhase.SELECT) {
                                if (selectedWords.isNotEmpty()) {
                                    val claimedWordPayloads = selectedWords.map { wordWithCoords ->
                                        ClaimedWordPayload(wordWithCoords.word, wordWithCoords.coordinates)
                                    }
                                    
                                    val payload = GameActionPayload(
                                        character = null,
                                        row = null,
                                        col = null,
                                        claimedWords = claimedWordPayloads
                                    )
                                    
                                    try {
                                        val response = gameService.submitAction(payload)
                                        if (response.status == 200) {
                                            isSubmitted = true
                                            val totalPoints = selectedWords.sumOf { calculateWordPoints(it.word) }
                                            val wordsList = selectedWords.joinToString(", ") { it.word }
                                            showGamifiedMessage(
                                                "🎯 You claimed ${selectedWords.size} words: $wordsList (+$totalPoints points total)!",
                                                MessageType.SUCCESS,
                                                Icons.Default.ThumbUp
                                            )
                                            selectedWords = emptyList() // Clear the list after successful submission
                                        } else {
                                            showGamifiedMessage(
                                                response.message ?: "Action failed",
                                                MessageType.ERROR,
                                                Icons.Default.Warning
                                            )
                                        }
                                    } catch (e: Exception) {
                                        Log.e("submitAction", e.toString())
                                        showGamifiedMessage(
                                            "Failed: ${e.message}",
                                            MessageType.ERROR,
                                            Icons.Default.Warning
                                        )
                                    }
                                } else {
                                    showGamifiedMessage(
                                        "Add some words to your list first! 📝",
                                        MessageType.WARNING,
                                        Icons.Default.Info
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        if (isKeyboardVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }


    // Player Info Overlay (replaces bottom sheet)
    if (showPlayerInfo && activeGame != null) {
        PlayerInfoOverlay(
            activeGame = activeGame!!,
            onDismiss = { showPlayerInfo = false }
        )
    }

    // Gamified Message System
    if (showMessage) {
        GamifiedMessageCard(
            message = messageText,
            type = messageType,
            icon = messageIcon,
            duration = when (messageType) {
                MessageType.SUCCESS -> 4000L
                MessageType.ERROR -> 5000L
                MessageType.WARNING -> 3500L
                MessageType.INFO -> 3000L
            },
            onDismiss = { showMessage = false }
        )
    }
}

@Composable
private fun TopGameStatusBar(
    isCurrentPlayer: Boolean,
    phase: GamePhase,
    remainingTime: Int,
    currentPlayerId: String,
    activeGame: GameData?,
    onExitClick: () -> Unit,
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
            // Exit Button
            IconButton(
                onClick = onExitClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Exit Game",
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }

            // Game Status
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Turn Status
                Text(
                    text = if (isCurrentPlayer) {
                        "Your Turn!"
                    } else {
                        // Find the current player's name from activeGame
                        val currentPlayer = activeGame?.players?.find { it._id == currentPlayerId }
                        val currentPlayerName = when {
                            currentPlayer?.name.isNullOrBlank() -> "Opponent"
                            currentPlayer.name == "Unknown Player" -> "Opponent"
                            else -> currentPlayer.name
                        }
                        "$currentPlayerName's Turn"
                    },
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
            progress = { remainingTime / 30f },
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
    voteEndGamePlayers: List<String>,
    currentUserId: String,
    selectedWords: List<WordWithCoordinates>,
    onAddWord: () -> Unit,
    onRemoveWord: (String) -> Unit,
    onSpectate: () -> Unit,
    onVoteEndGame: () -> Unit,
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
                        "Select cells to form a word, then add it to your list"

                    else -> "Waiting for opponent..."
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(Modifier.height(16.dp))

            // Selected Words List (only show in SELECT phase)
            if (phase == GamePhase.SELECT && selectedWords.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Selected Words (${selectedWords.size}):",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        selectedWords.forEach { wordWithCoords ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${wordWithCoords.word}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                
                                if (isCurrentPlayer && !isSubmitted) {
                                    IconButton(
                                        onClick = { onRemoveWord(wordWithCoords.word) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove word",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (phase == GamePhase.SELECT && isCurrentPlayer && !isSubmitted) {
                    Button(
                        onClick = onAddWord,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedCells.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Word",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Add Word",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Game Control Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Spectate Button
                    OutlinedButton(
                        onClick = onSpectate,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.secondary
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Spectate",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Spectate",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Vote for End Game Button
                    OutlinedButton(
                        onClick = onVoteEndGame,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (voteEndGamePlayers.contains(currentUserId)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            }
                        ),
                        border = BorderStroke(
                            width = 2.dp,
                            color = if (voteEndGamePlayers.contains(currentUserId)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (voteEndGamePlayers.contains(currentUserId)) {
                                Icons.Default.ThumbUp
                            } else {
                                Icons.Default.ThumbUp
                            },
                            contentDescription = "Vote",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                voteEndGamePlayers.isEmpty() -> "Vote Close"
                                voteEndGamePlayers.contains(currentUserId) -> "Remove Vote (${voteEndGamePlayers.size})"
                                else -> "Vote Close (${voteEndGamePlayers.size})"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Submit Button (only for current player)
                if (isCurrentPlayer && !isSubmitted) {
                    Button(
                        onClick = onSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = when {
                            phase == GamePhase.EDIT -> filledCell != null
                            phase == GamePhase.SELECT -> selectedWords.isNotEmpty()
                            else -> false
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Submit",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Submit",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
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

@Composable
private fun GamifiedMessageCard(
    message: String,
    type: MessageType,
    icon: ImageVector?,
    duration: Long,
    onDismiss: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    var progressValue by remember { mutableStateOf(1f) }

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else -100f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        )
    )

    LaunchedEffect(Unit) {
        isVisible = true
        delay(100)
        showProgress = true

        // Animate progress bar from 1.0 to 0.0 over the duration
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < duration) {
            val elapsed = System.currentTimeMillis() - startTime
            progressValue = 1f - (elapsed.toFloat() / duration.toFloat())
            delay(16) // ~60 FPS
        }
        progressValue = 0f
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .scale(scale)
                .offset(y = offsetY.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onDismiss() },
            colors = CardDefaults.cardColors(
                containerColor = when (type) {
                    MessageType.SUCCESS -> Color(0xFF4CAF50).copy(alpha = 0.95f)
                    MessageType.WARNING -> Color(0xFFFF9800).copy(alpha = 0.95f)
                    MessageType.ERROR -> Color(0xFFF44336).copy(alpha = 0.95f)
                    MessageType.INFO -> Color(0xFF2196F3).copy(alpha = 0.95f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated Icon with bounce effect
                    if (icon != null) {
                        var iconScale by remember { mutableStateOf(0f) }
                        LaunchedEffect(Unit) {
                            delay(200)
                            iconScale = 1f
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = message,
                            modifier = Modifier
                                .size(32.dp)
                                .scale(iconScale),
                            tint = Color.White
                        )
                    }

                    // Message Text
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    // Close Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Progress Bar for auto-dismiss
                if (showProgress) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progressValue },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White.copy(alpha = 0.8f),
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}
