package com.harshkanjariya.wordwar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.harshkanjariya.wordwar.screens.Cell
import com.harshkanjariya.wordwar.screens.GamePhase
import com.harshkanjariya.wordwar.util.getCellsInPath
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
@Composable
fun WordGrid(
    gridSize: Int,
    cells: SnapshotStateList<String>,
    currentMode: GamePhase,
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
            .aspectRatio(1f)
            .pointerInput(currentMode) {
                val totalCellSizePx = (size.width.toFloat() / gridSize)
                if (currentMode == GamePhase.SELECT) {
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
                                    val hasEmptyCells =
                                        currentPath.any { cells.getOrNull(it)?.isBlank() == true }
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
                            val hasEmptyCells =
                                selectedCells.any { cells.getOrNull(it)?.isBlank() == true }
                            if (hasEmptyCells) {
                                onCellsSelected(emptySet())
                            }
                        }
                    )
                }
            }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            repeat(gridSize) { r ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    repeat(gridSize) { c ->
                        val index = r * gridSize + c
                        val isSelected =
                            selectedCells.contains(index) || (filledCell?.index == index && highlightFilledCell)
                        val cellContent =
                            if (filledCell?.index == index && filledCell.char.isNotBlank()) filledCell.char else cells.getOrNull(
                                index
                            ) ?: ""

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    when {
                                        isSelected -> Color(0xFF2E7D32) // Dark green for selection
                                        cellContent.isBlank() -> MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )

                                        else -> MaterialTheme.colorScheme.primaryContainer
                                    },
                                    RoundedCornerShape(4.dp)
                                )
                                .then(
                                    if (currentMode == GamePhase.EDIT) {
                                        Modifier.clickable { onCellClick(index) }
                                    } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cellContent.isNotBlank()) {
                                Text(
                                    text = cellContent,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
