package com.harshkanjariya.wordwar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harshkanjariya.wordwar.screens.Cell
import com.harshkanjariya.wordwar.screens.GameMode
import com.harshkanjariya.wordwar.util.darker
import com.harshkanjariya.wordwar.util.getCellsInPath
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@Composable
fun GameHeader(
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
fun GameControls(onClaimWord: () -> Unit, onEndGame: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onClaimWord) { Text("Claim Word") }
        OutlinedButton(onClick = onEndGame) { Text("End Game") }
    }
}

@Composable
fun ClaimedWordsList(claimedWords: List<String>) {
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
fun WordGrid(
    gridSize: Int,
    cells: SnapshotStateList<String>,
    currentMode: GameMode,
    onCellClick: (Int) -> Unit,
    onCellsSelected: (Set<Int>) -> Unit,
    filledCell: Cell?,
    highlightFilledCell: Boolean,
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
                                        startOffset = dragStartOffset,
                                        endOffset = currentDragOffset,
                                        gridSize = gridSize,
                                        totalCellSizePx = totalCellSizePx
                                    )
                                    val hasEmptyCells = currentPath.any { cells.getOrNull(it)?.isBlank() == true }
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
                            val hasEmptyCells = selectedCells.any { cells.getOrNull(it)?.isBlank() == true }
                            if (hasEmptyCells) { onCellsSelected(emptySet()) }
                        }
                    )
                }
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            repeat(gridSize) { r ->
                Row {
                    repeat(gridSize) { c ->
                        val index = r * gridSize + c
                        val isSelected = selectedCells.contains(index) || (filledCell?.index == index && highlightFilledCell)
                        val backgroundColor = if (isSelected) Color(0xFF3F51B5).darker() else Color(0xFFEAEAEA)
                        val textColor = if (isSelected) Color.White else Color.Black
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(2.dp)
                                .background(backgroundColor)
                                .then(if (currentMode == GameMode.FILLING) {
                                    Modifier.clickable { onCellClick(index) }
                                } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (filledCell?.index == index && filledCell.char.isNotBlank()) filledCell.char else cells.getOrNull(index) ?: "",
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

@Composable
fun FillCellDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    characterInput: String,
    onCharacterInputChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fill Cell") },
        text = {
            TextField(
                value = characterInput,
                onValueChange = onCharacterInputChange,
                label = { Text("Enter a character") },
                singleLine = true
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}