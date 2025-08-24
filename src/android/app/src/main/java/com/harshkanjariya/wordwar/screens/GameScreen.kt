package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.network.DictionaryServiceHolder
import com.harshkanjariya.wordwar.network.GameServiceHolder
import com.harshkanjariya.wordwar.network.MovePayload
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

// An enum to represent the two game modes
enum class GameMode {
    FILLING,
    SELECTION
}

@Composable
fun GameScreen(navController: NavController, matchId: String?) {
    val scope = rememberCoroutineScope()
    val gridSize = 10
    val cells =
        remember { mutableStateListOf<String>().apply { addAll(List(gridSize * gridSize) { "" }) } }
    var currentMode by remember { mutableStateOf(GameMode.FILLING) }
    var showFillDialog by remember { mutableStateOf(false) }
    var cellToFillIndex by remember { mutableIntStateOf(-1) }
    var characterInput by remember { mutableStateOf("") }
    var claimedWords by remember { mutableStateOf(listOf<String>()) }
    var selectedCells by remember { mutableStateOf(emptySet<Int>()) }
    val snackbarHostState = remember { SnackbarHostState() }


    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        content = { innerPadding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {

                GameHeader(
                    currentMode = currentMode,
                    onToggleMode = {
                        currentMode =
                            if (currentMode == GameMode.FILLING) GameMode.SELECTION else GameMode.FILLING
                        selectedCells = emptySet()
                    },
                    onBackClick = { navController.popBackStack() }
                )
                Spacer(Modifier.height(12.dp))

                WordGrid(
                    gridSize = gridSize,
                    cells = cells,
                    currentMode = currentMode,
                    onCellClick = { index ->
                        if (currentMode == GameMode.FILLING) {
                            if (cells[index].isBlank()) {
                                cellToFillIndex = index
                                showFillDialog = true
                            }
                        }
                    },
                    onCellsSelected = { newCells ->
                        selectedCells = newCells
                    },
                    selectedCells = selectedCells
                )
                Spacer(Modifier.height(16.dp))

                GameControls(
                    onClaimWord = {
                        scope.launch {
                            if (currentMode == GameMode.SELECTION && selectedCells.isNotEmpty()) {
                                val claimedWord = buildString {
                                    selectedCells.sorted().forEach { index ->
                                        append(cells[index])
                                    }
                                }

                                if (claimedWord.isNotBlank()) {
                                    // Check for duplicates here
                                    if (claimedWords.contains(claimedWord)) {
                                        snackbarHostState.showSnackbar(
                                            message = "This word is already claimed!",
                                            duration = SnackbarDuration.Short
                                        )
                                    } else if (isWordValid(claimedWord)) {
                                        claimedWords = claimedWords + claimedWord
                                        selectedCells = emptySet()
                                    } else {
                                        snackbarHostState.showSnackbar(
                                            message = "Invalid word.",
                                            duration = SnackbarDuration.Short
                                        )
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
                AlertDialog(
                    onDismissRequest = {
                        showFillDialog = false
                        characterInput = ""
                    },
                    title = { Text("Fill Cell") },
                    text = {
                        TextField(
                            value = characterInput,
                            onValueChange = { newValue ->
                                if (newValue.length <= 1 && newValue.all { it.isLetter() }) {
                                    characterInput = newValue.uppercase()
                                }
                            },
                            label = { Text("Enter a character") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (characterInput.isNotBlank()) {
                                    cells[cellToFillIndex] = characterInput
                                    scope.launch {
                                        runCatching {
                                            GameServiceHolder.api.playMove(
                                                gameId = "sampleGameId",
                                                payload = MovePayload(
                                                    userId = "yourUserId",
                                                    row = cellToFillIndex / gridSize,
                                                    col = cellToFillIndex % gridSize,
                                                    char = characterInput
                                                )
                                            )
                                        }
                                    }
                                    characterInput = ""
                                    showFillDialog = false
                                }
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showFillDialog = false
                            characterInput = ""
                        }) { Text("Cancel") }
                    }
                )
            }
        }
    )
}

suspend fun isWordValid(word: String): Boolean {
    return try {
        val response = DictionaryServiceHolder.api.getWordInfo(word)
        response.isNotEmpty()
    } catch (e: HttpException) {
        if (e.code() == 404) {
            return false
        }
        false
    } catch (e: IOException) {
        false
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun GameHeader(
    currentMode: GameMode,
    onToggleMode: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onToggleMode) {
                Text(if (currentMode == GameMode.FILLING) "Select Mode" else "Fill Mode")
            }
            OutlinedButton(onClick = onBackClick) { Text("Back") }
        }
    }
}

@Composable
private fun GameControls(onClaimWord: () -> Unit, onEndGame: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onClaimWord) { Text("Claim Word") }
        OutlinedButton(onClick = onEndGame) { Text("End Game") }
    }
}

@Composable
private fun ClaimedWordsList(claimedWords: List<String>) {
    Column {
        Text("Claimed Words", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (claimedWords.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(claimedWords) { word ->
                    Text(text = "-> $word", style = MaterialTheme.typography.bodyLarge)
                }
            }
        } else {
            Text("No words claimed yet.")
        }
    }
}

@OptIn(FlowPreview::class)
@Composable
private fun WordGrid(
    gridSize: Int,
    cells: SnapshotStateList<String>,
    currentMode: GameMode,
    onCellClick: (Int) -> Unit,
    onCellsSelected: (Set<Int>) -> Unit,
    selectedCells: Set<Int>
) {
    val scope = rememberCoroutineScope()
    var dragStartOffset by remember { mutableStateOf(Offset.Unspecified) }
    val dragState = remember { mutableStateOf(Offset.Unspecified) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(currentMode) {
                val totalCellSizePx = (size.width.toFloat() / gridSize)

                if (currentMode == GameMode.SELECTION) {
                    scope.launch {
                        snapshotFlow { dragState.value }
                            .filter { it != Offset.Unspecified }
                            .debounce(10)
                            .distinctUntilChanged()
                            .collect { currentDragOffset ->
                                if (dragStartOffset != Offset.Unspecified) {
                                    val currentPath = getCellsInPath(
                                        dragStartOffset,
                                        currentDragOffset,
                                        gridSize,
                                        totalCellSizePx
                                    )

                                    val hasEmptyCells = currentPath.any { cells[it].isBlank() }
                                    if (hasEmptyCells) {
                                        onCellsSelected(emptySet())
                                    } else {
                                        onCellsSelected(currentPath)
                                    }
                                }
                            }
                    }
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragStartOffset = offset
                            dragState.value = offset
                        },
                        onDrag = { change, _ ->
                            dragState.value = change.position
                        },
                        onDragEnd = {
                            dragStartOffset = Offset.Unspecified
                            dragState.value = Offset.Unspecified

                            val finalPath = selectedCells
                            val hasEmptyCells = finalPath.any { cells[it].isBlank() }

                            if (hasEmptyCells) {
                                onCellsSelected(emptySet())
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            repeat(gridSize) { r ->
                Row {
                    repeat(gridSize) { c ->
                        val index = r * gridSize + c
                        val isSelected = selectedCells.contains(index)
                        val backgroundColor =
                            if (isSelected) Color(0xFF3F51B5).darker() else Color(0xFFEAEAEA)
                        val textColor = if (isSelected) Color.White else Color.Black

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp)
                                .background(backgroundColor)
                                .then(
                                    if (currentMode == GameMode.FILLING) {
                                        Modifier.clickable { onCellClick(index) }
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cells.getOrNull(index) ?: "",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall.copy(color = textColor)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun Color.darker(factor: Float = 0.2f): Color =
    Color(
        red = (red - factor).coerceIn(0f, 1f),
        green = (green - factor).coerceIn(0f, 1f),
        blue = (blue - factor).coerceIn(0f, 1f),
        alpha = alpha
    )

fun getCellsInPath(
    startOffset: Offset,
    endOffset: Offset,
    gridSize: Int,
    totalCellSizePx: Float
): Set<Int> {
    val startRow = floor(startOffset.y / totalCellSizePx).roundToInt().coerceIn(0, gridSize - 1)
    val startCol = floor(startOffset.x / totalCellSizePx).roundToInt().coerceIn(0, gridSize - 1)
    val endRow = floor(endOffset.y / totalCellSizePx).roundToInt().coerceIn(0, gridSize - 1)
    val endCol = floor(endOffset.x / totalCellSizePx).roundToInt().coerceIn(0, gridSize - 1)

    val path = mutableSetOf<Int>()

    // Check if the drag has actually started
    if (startRow == endRow && startCol == endCol) {
        path.add(startRow * gridSize + startCol)
        return path
    }

    val dRow = endRow - startRow
    val dCol = endCol - startCol
    val absDRow = abs(dRow)
    val absDCol = abs(dCol)

    // Horizontal path
    if (absDCol > absDRow) {
        val colDirection = if (dCol > 0) 1 else -1
        for (col in 0..absDCol) {
            val currentCol = startCol + col * colDirection
            path.add(startRow * gridSize + currentCol)
        }
    }
    // Vertical path
    else if (absDRow > absDCol) {
        val rowDirection = if (dRow > 0) 1 else -1
        for (row in 0..absDRow) {
            val currentRow = startRow + row * rowDirection
            path.add(currentRow * gridSize + startCol)
        }
    }
    // Diagonal path
    else {
        val rowDirection = if (dRow > 0) 1 else -1
        val colDirection = if (dCol > 0) 1 else -1
        for (step in 0..absDCol) {
            val currentRow = startRow + step * rowDirection
            val currentCol = startCol + step * colDirection
            path.add(currentRow * gridSize + currentCol)
        }
    }

    return path
}