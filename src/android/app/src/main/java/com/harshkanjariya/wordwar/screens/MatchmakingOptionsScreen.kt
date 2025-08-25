package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder

@Composable
fun MatchmakingOptionsScreen(navController: NavController) {
    val gameService = GameServiceHolder.api
    val token by LocalStorage.getToken(navController.context).collectAsState(initial = null)

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(token) {
        if (token != null) {
            try {
                val response = gameService.getActiveGame()
                if (response.status == 200) {
                    if (response.data.currentGameId != null) {
                        navController.navigate("game/${response.data.currentGameId}") {
                            popUpTo("matchmaking_options") { inclusive = true }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Failed to fetch active game: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val playerCounts = listOf(2, 3, 4, 5)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Number of Players",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(32.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.widthIn(max = 400.dp)
                ) {
                    items(playerCounts) { count ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f) // Makes the card square
                                .padding(8.dp),
                            onClick = { navController.navigate("queue/$count") }
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = count.toString(),
                                    style = MaterialTheme.typography.displayMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}