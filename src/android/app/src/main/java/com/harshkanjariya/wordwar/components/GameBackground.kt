package com.harshkanjariya.wordwar.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun GameBackground(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    letterCount: Int = 15,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Animated floating letters background
        FloatingLettersBackground(
            modifier = Modifier.fillMaxSize(),
            letterCount = letterCount,
            backgroundColor = backgroundColor
        )
        
        // Content on top
        content()
    }
}
