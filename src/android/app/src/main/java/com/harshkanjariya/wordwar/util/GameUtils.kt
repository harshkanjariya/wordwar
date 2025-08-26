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
        // Horizontal movement (same row, different columns)
        if (dCol > 0) {
            // Left to right: increasing column numbers
            for (col in startCol..endCol) {
                path.add(startRow * gridSize + col)
            }
        } else {
            // Right to left: decreasing column numbers
            for (col in startCol downTo endCol) {
                path.add(startRow * gridSize + col)
            }
        }
    }
    else if (absDRow > absDCol) {
        // Vertical movement (same column, different rows)
        if (dRow > 0) {
            // Top to bottom: increasing row numbers
            for (row in startRow..endRow) {
                path.add(row * gridSize + startCol)
            }
        } else {
            // Bottom to top: decreasing row numbers
            for (row in startRow downTo endRow) {
                path.add(row * gridSize + startCol)
            }
        }
    }
    else {
        // Diagonal movement - use proportional calculation for smooth diagonal paths
        val maxSteps = maxOf(absDRow, absDCol)
        for (step in 0..maxSteps) {
            val currentRow = startRow + (step * dRow / maxSteps)
            val currentCol = startCol + (step * dCol / maxSteps)
            path.add(currentRow * gridSize + currentCol)
        }
    }
    return path
}