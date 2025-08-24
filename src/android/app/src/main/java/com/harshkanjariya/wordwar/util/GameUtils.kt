package com.harshkanjariya.wordwar.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

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
    if (startRow == endRow && startCol == endCol) {
        path.add(startRow * gridSize + startCol)
        return path
    }

    val dRow = endRow - startRow
    val dCol = endCol - startCol
    val absDRow = abs(dRow)
    val absDCol = abs(dCol)

    if (absDCol > absDRow) {
        val colDirection = if (dCol > 0) 1 else -1
        for (col in 0..absDCol) {
            val currentCol = startCol + col * colDirection
            path.add(startRow * gridSize + currentCol)
        }
    }
    else if (absDRow > absDCol) {
        val rowDirection = if (dRow > 0) 1 else -1
        for (row in 0..absDRow) {
            val currentRow = startRow + row * rowDirection
            path.add(currentRow * gridSize + startCol)
        }
    }
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