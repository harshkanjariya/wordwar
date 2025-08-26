package com.harshkanjariya.wordwar.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.components.*
import com.harshkanjariya.wordwar.network.service.GameData
import com.harshkanjariya.wordwar.network.service.PlayerDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Dummy data for How to Play screen
private val dummyGameData = GameData(
    createdAt = "2024-01-01T00:00:00Z",
    updatedAt = "2024-01-01T00:00:00Z",
    players = listOf(
        PlayerDto(
            _id = "player1",
            name = "Demo Player",
            joinedAt = "2024-01-01T00:00:00Z",
            claimedWords = listOf("HELLO", "WORLD", "GAME")
        ),
        PlayerDto(
            _id = "player2",
            name = "Demo Opponent",
            joinedAt = "2024-01-01T00:00:00Z",
            claimedWords = listOf("PLAY", "FUN")
        )
    ),
    cellData = List(10) { row ->
        List(10) { col ->
            when {
                row == 4 && col in 0..4 -> "HELLO"[col].toString()
                row == 5 && col in 0..3 -> "WORLD"[col].toString()
                row == 6 && col in 0..3 -> "PLAY"[col].toString()
                row == 7 && col in 0..2 -> "FUN"[col].toString()
                else -> ""
            }
        }
    }
)

@Composable
fun HowToPlayScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val gridSize = 10

    // Local state for demo interactions
    val cells = remember {
        mutableStateListOf<String>().apply {
            addAll(dummyGameData.cellData.flatten())
        }
    }
    var isKeyboardVisible by remember { mutableStateOf(false) }
    var selectedCellIndexForInput by remember { mutableIntStateOf(-1) }
    val filledCell = remember { mutableStateOf<Cell?>(null) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    var isSubmitted by remember { mutableStateOf(false) }

    // Demo game state
    var currentPlayer by remember { mutableStateOf("player1") }
    var phase by remember { mutableStateOf(GamePhase.EDIT) }
    var remainingTime by remember { mutableIntStateOf(30) }
    var showPlayerInfo by remember { mutableStateOf(false) }

    // Tour guide state
    var currentTourStep by remember { mutableStateOf(0) }
    var showTourGuide by remember { mutableStateOf(true) }

    val tourSteps = listOf(
        TourStep(
            title = "Welcome to Word War!",
            description = "This is a word-building game where you compete with other players. Let me show you how to play!",
            highlight = TourHighlight.NONE
        ),
        TourStep(
            title = "Game Grid",
            description = "This is your game board. You'll place letters and form words here. Notice some words are already placed as examples.",
            highlight = TourHighlight.GRID
        ),
        TourStep(
            title = "Game Phases",
            description = "The game has two phases: EDIT (place letters) and SELECT (form words). Watch the timer and phase indicator at the top.",
            highlight = TourHighlight.STATUS_BAR
        ),
        TourStep(
            title = "Placing Letters",
            description = "In EDIT phase, tap an empty cell and select a letter from the keyboard. Try it now!",
            highlight = TourHighlight.GRID
        ),
        TourStep(
            title = "Forming Words",
            description = "In SELECT phase, tap cells in sequence to form words. The cells must be connected horizontally, vertically, or diagonally.",
            highlight = TourHighlight.GRID
        ),
        TourStep(
            title = "Submit Actions",
            description = "Use the floating action button or the Submit button below to confirm your moves.",
            highlight = TourHighlight.SUBMIT_BUTTON
        ),
        TourStep(
            title = "Player Info",
            description = "Tap the player icon to see game statistics and claimed words for all players.",
            highlight = TourHighlight.PLAYER_INFO
        ),
        TourStep(
            title = "Timer & Turns",
            description = "Each player has 30 seconds per turn. Use your time wisely!",
            highlight = TourHighlight.TIMER
        ),
        TourStep(
            title = "Ready to Play!",
            description = "You now know the basics! Try interacting with the demo to practice. When you're ready, start a real game!",
            highlight = TourHighlight.NONE
        )
    )

    // Demo timer effect
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (remainingTime > 0) {
                remainingTime--
            } else {
                remainingTime = 30
                // Switch phases for demo
                phase = if (phase == GamePhase.EDIT) GamePhase.SELECT else GamePhase.EDIT
                isSubmitted = false
                selectedCells = emptySet()
                filledCell.value = null
            }
        }
    }

    BackHandler(enabled = isKeyboardVisible) {
        isKeyboardVisible = false
        selectedCellIndexForInput = -1
    }

    BackHandler(enabled = showTourGuide) {
        showTourGuide = false
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
                isCurrentPlayer = true, // Always show as current player for demo
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
                        if (phase == GamePhase.EDIT && !isSubmitted) {
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

            // Bottom Game Controls
            BottomGameControls(
                phase = phase,
                isCurrentPlayer = true,
                isSubmitted = isSubmitted,
                selectedCells = selectedCells,
                filledCell = filledCell.value,
                onEndGame = { navController.popBackStack() },
                onSubmit = {
                    // Demo submit logic
                    if (phase == GamePhase.EDIT && filledCell.value != null) {
                        // Simulate successful submission
                        isSubmitted = true
                        scope.launch {
                            snackbarHostState.showSnackbar("Demo: Letter placed successfully!")
                        }
                    } else if (phase == GamePhase.SELECT && selectedCells.isNotEmpty()) {
                        // Simulate word submission
                        val word = buildString {
                            selectedCells.sorted().forEach { index -> append(cells[index]) }
                        }
                        if (word.isNotBlank()) {
                            isSubmitted = true
                            scope.launch {
                                snackbarHostState.showSnackbar("Demo: Word '$word' submitted!")
                            }
                        }
                    }
                }
            )
        }

        // Floating Action Button for Submit
        if (!isSubmitted) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 40.dp)
            ) {
                FloatingActionButton(
                    onClick = {
                        // Demo submit logic
                        if (phase == GamePhase.EDIT && filledCell.value != null) {
                            isSubmitted = true
                            scope.launch {
                                snackbarHostState.showSnackbar("Demo: Letter placed successfully!")
                            }
                        } else if (phase == GamePhase.SELECT && selectedCells.isNotEmpty()) {
                            val word = buildString {
                                selectedCells.sorted().forEach { index -> append(cells[index]) }
                            }
                            if (word.isNotBlank()) {
                                isSubmitted = true
                                scope.launch {
                                    snackbarHostState.showSnackbar("Demo: Word '$word' submitted!")
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
                        if (selectedCellIndexForInput != -1) {
                            filledCell.value = Cell(index = selectedCellIndexForInput, char = char)
                            isKeyboardVisible = false
                            selectedCellIndexForInput = -1
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // Player Info Overlay
        if (showPlayerInfo) {
            PlayerInfoOverlay(
                activeGame = dummyGameData,
                onDismiss = { showPlayerInfo = false }
            )
        }

        // Tour Guide Overlay
        if (showTourGuide) {
            TourGuideOverlay(
                currentStep = tourSteps[currentTourStep],
                totalSteps = tourSteps.size,
                currentStepIndex = currentTourStep,
                onNext = {
                    if (currentTourStep < tourSteps.size - 1) {
                        currentTourStep++
                    } else {
                        showTourGuide = false
                    }
                },
                onPrevious = {
                    if (currentTourStep > 0) {
                        currentTourStep--
                    }
                },
                onSkip = { showTourGuide = false }
            )
        }

        // Snackbar Host
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

// Reuse all the same UI components from GameScreen
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
                    Text("Back to Menu")
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

// Tour guide data classes
data class TourStep(
    val title: String,
    val description: String,
    val highlight: TourHighlight
)

enum class TourHighlight {
    NONE, GRID, STATUS_BAR, SUBMIT_BUTTON, PLAYER_INFO, TIMER
}

@Composable
private fun TourGuideOverlay(
    currentStep: TourStep,
    totalSteps: Int,
    currentStepIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentStep.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = currentStep.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )
                )

                Spacer(Modifier.height(24.dp))

                // Progress indicator
                LinearProgressIndicator(
                    progress = (currentStepIndex + 1) / totalSteps.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Step ${currentStepIndex + 1} of $totalSteps",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(Modifier.height(24.dp))

                // Navigation buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onSkip,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Skip Tour")
                    }

                    if (currentStepIndex > 0) {
                        OutlinedButton(
                            onClick = onPrevious,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Previous")
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Button(
                        onClick = onNext,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (currentStepIndex == totalSteps - 1) "Finish" else "Next")
                    }
                }
            }
        }
    }
}
