package com.harshkanjariya.wordwar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomKeyboard(
    onCharClicked: (String) -> Unit,
    onBackspaceClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    val rowSize = 7

    Column(
        modifier = modifier
            .background(Color.LightGray)
            .padding(8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        for (i in keys.indices.step(rowSize)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (j in 0 until rowSize) {
                    val keyIndex = i + j
                    if (keyIndex < keys.length) {
                        val key = keys[keyIndex].toString()
                        Button(
                            onClick = { if (key != " ") onCharClicked(key) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(key)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                if (i == 21) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    Button(
                        onClick = onBackspaceClicked,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Backspace"
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .width(60.dp)
                    .height(40.dp)
            ) {}
        }
    }
}