package com.harshkanjariya.wordwar.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun CustomKeyboard(
    onCharClicked: (String) -> Unit,
    onBackspaceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val rowSize = 7
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .padding(12.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Keyboard header with subtle indicator
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .background(
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    RoundedCornerShape(2.dp)
                )
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        for (i in keys.indices.step(rowSize)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until rowSize) {
                    val keyIndex = i + j
                    if (keyIndex < keys.length) {
                        val key = keys[keyIndex].toString()
                        KeyboardKey(
                            key = key,
                            onClick = { 
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onCharClicked(key) 
                            }
                        )
                    }
                }
                
                // Add backspace button to the last row
                if (i == 21) {
                    BackspaceKey(
                        onClick = { 
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBackspaceClicked() 
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
private fun KeyboardKey(
    key: String,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "keyScale"
    )
    
            Button(
            onClick = onClick,
            modifier = Modifier
                .size(36.dp)
                .scale(scale),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            shape = RoundedCornerShape(12.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 1.dp
            ),
            contentPadding = PaddingValues(0.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Text(
                text = key,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
}

@Composable
private fun BackspaceKey(
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "backspaceScale"
    )
    
    Button(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .scale(scale),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Backspace",
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Preview(
    name = "Custom Keyboard Preview",
    showBackground = true,
    backgroundColor = 0xFFF5F5F5
)
@Composable
private fun CustomKeyboardPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            CustomKeyboard(
                onCharClicked = { char -> 
                    // Preview callback
                },
                onBackspaceClicked = { 
                    // Preview callback
                }
            )
        }
    }
}