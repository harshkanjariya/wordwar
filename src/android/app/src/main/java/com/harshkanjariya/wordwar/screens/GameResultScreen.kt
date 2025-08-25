package com.harshkanjariya.wordwar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.network.service.GameInfo
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder

@Composable
fun GameResultScreen(navController: NavController, matchId: String?) {
    var gameInfo by remember { mutableStateOf<GameInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(matchId) {
        if (matchId == null) {
            errorMessage = "Game ID not provided."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val response = GameServiceHolder.api.getGameInfo(matchId)
            if (response.status == 200 && response.data != null) {
                gameInfo = response.data
            } else {
                errorMessage = response.message ?: "Failed to fetch game data."
            }
        } catch (e: Exception) {
            errorMessage = "An error occurred: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp
            )
        } else if (gameInfo != null) {
            Text(text = "Game Results", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Game ID: $matchId", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Players: ${gameInfo!!.players.joinToString()}", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            // You can display more information here
            Text(text = "Final Board:", fontSize = 16.sp)
            // Example of showing the board
            gameInfo!!.cellData.forEach { row ->
                Text(text = row.joinToString(" "), fontSize = 14.sp)
            }
        } else {
            Text("No game data found.")
        }

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                navController.navigate("menu")
            }
        ) {
            Text("Back to Menu")
        }
    }
}
