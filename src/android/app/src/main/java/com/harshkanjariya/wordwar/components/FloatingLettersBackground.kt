package com.harshkanjariya.wordwar.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

data class FloatingLetter(
    val id: Int,
    var letter: String,
    var x: Float, // current position
    var y: Float, // current position
    var xSpeed: Float, // speed in x direction
    var ySpeed: Float, // speed in y direction
    var size: Float,
    var opacity: Float
)

@Composable
fun FloatingLettersBackground(
    modifier: Modifier = Modifier,
    letterCount: Int = 15,
    backgroundColor: Color = Color.Transparent
) {
    // Use mutableStateList to allow updating letter properties
    val letters = remember { mutableStateListOf<FloatingLetter>() }
    var nextId by remember { mutableStateOf(0) }
    
    // Store screen dimensions in state
    var screenWidth by remember { mutableStateOf(0f) }
    var screenHeight by remember { mutableStateOf(0f) }
    
    // Animation state
    var animationFrame by remember { mutableStateOf(0) }
    
    // Recalculate function - updates all letter positions
    fun recalculatePositions() {
        if (screenWidth <= 0 || screenHeight <= 0) return
        
        val updatedLetters = letters.map { letter ->
            // Create a new letter object with updated position
            letter.copy(
                x = letter.x + letter.xSpeed,
                y = letter.y + letter.ySpeed
            ).also { updatedLetter ->
                // Check if letter is outside screen bounds (using actual screen size)
                if (updatedLetter.x < -100 || updatedLetter.x > screenWidth + 100 || 
                    updatedLetter.y < -100 || updatedLetter.y > screenHeight + 100) {
                    // Reset letter to center with new random properties
                    updatedLetter.x = screenWidth * 0.5f + (Random.nextFloat() - 0.5f) * screenWidth * 0.8f
                    updatedLetter.y = screenHeight * 0.5f + (Random.nextFloat() - 0.5f) * screenHeight * 0.8f
                    updatedLetter.letter = ('A'..'Z').random().toString()
                    // New random x and y speeds
                    updatedLetter.xSpeed = (Random.nextFloat() * 4f - 2f) // -2 to +2 pixels per frame
                    updatedLetter.ySpeed = (Random.nextFloat() * 4f - 2f) // -2 to +2 pixels per frame
                    updatedLetter.size = Random.nextFloat() * 25f + 16f
                    updatedLetter.opacity = Random.nextFloat() * 0.6f + 0.2f
                }
            }
        }
        // Replace the entire list to trigger recomposition
        letters.clear()
        letters.addAll(updatedLetters)
    }
    
    // Animation loop using setInterval approach
    LaunchedEffect(Unit) {
        while (true) {
            delay(16) // 60 FPS
            recalculatePositions()
            animationFrame++
        }
    }
    
    Canvas(
        modifier = modifier.fillMaxSize()
    ) {
        // Update screen dimensions
        screenWidth = size.width
        screenHeight = size.height
        
        // Initialize letters only once when we have the actual screen size
        if (letters.isEmpty() && size.width > 0 && size.height > 0) {
            letters.addAll(List(letterCount) {
                createInitialLetter(nextId++, size.width, size.height)
            })
        }
        
        // Draw background
        drawRect(color = backgroundColor)
        
        // Paint function - draws all letters at their current positions
        letters.forEach { letter ->
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = "#4CAF50".toColorInt() // Green color using KTX extension
                    alpha = (letter.opacity * 255).toInt()
                    textSize = letter.size
                    textAlign = android.graphics.Paint.Align.CENTER
                    isFakeBoldText = true
                }
                
                drawText(
                    letter.letter,
                    letter.x,
                    letter.y,
                    paint
                )
            }
        }
    }
}

// Create initial letters that start INSIDE the screen
private fun createInitialLetter(id: Int, screenWidth: Float, screenHeight: Float): FloatingLetter {
    // Start position INSIDE the screen
    val startX = Random.nextFloat() * screenWidth
    val startY = Random.nextFloat() * screenHeight
    
    // Random x and y speeds (can be negative for opposite directions)
    val xSpeed = (Random.nextFloat() * 4f - 2f) // -2 to +2 pixels per frame
    val ySpeed = (Random.nextFloat() * 4f - 2f) // -2 to +2 pixels per frame
    
    return FloatingLetter(
        id = id,
        letter = ('A'..'Z').random().toString(),
        x = startX,
        y = startY,
        xSpeed = xSpeed,
        ySpeed = ySpeed,
        size = Random.nextFloat() * 25f + 16f,
        opacity = Random.nextFloat() * 0.6f + 0.2f
    )
}
