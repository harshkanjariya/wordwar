package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.components.GameBackground
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.network.service_holder.GameServiceHolder

@Composable
fun MatchmakingOptionsScreen(navController: NavController) {
    val gameService = GameServiceHolder.api
    val token by LocalStorage.getToken().collectAsState(initial = null)

    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(token) {
        if (token != null) {
            try {
                val response = gameService.getActiveGame()
                if (response.status == 200) {
                    if (response.data != null) {
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
        GameBackground(
            backgroundColor = MaterialTheme.colorScheme.background,
            letterCount = 30
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    } else {
        val playerCounts = listOf(2, 3, 4, 5)

        GameBackground(
            backgroundColor = MaterialTheme.colorScheme.background,
            letterCount = 30
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Choose Players",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select the number of players for your game",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(40.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.widthIn(max = 450.dp)
                    ) {
                        items(playerCounts) { count ->
                            PlayerCountCard(
                                playerCount = count,
                                onClick = { navController.navigate("queue/$count") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCountCard(
    playerCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overlapping user profile silhouettes
                OverlappingUserIcons(playerCount = playerCount)

                Text(
                    text = "$playerCount",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )

                Text(
                    text = "Players",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
private fun OverlappingUserIcons(playerCount: Int) {
    Box(
        modifier = Modifier.size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        when (playerCount) {
            2 -> {
                // Two overlapping icons
                UserIconWithBorder(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = (-16).dp, y = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 44.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = 16.dp, y = 0.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 44.dp
                )
            }

            3 -> {
                // Three overlapping icons in triangle formation
                UserIconWithBorder(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = 0.dp, y = (-18).dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 44.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = (-22).dp, y = 18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 44.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(44.dp)
                        .offset(x = 22.dp, y = 18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 44.dp
                )
            }

            4 -> {
                // Four overlapping icons in square formation
                UserIconWithBorder(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = (-16).dp, y = (-16).dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 42.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = 16.dp, y = (-16).dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 42.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = (-16).dp, y = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 42.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(42.dp)
                        .offset(x = 16.dp, y = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 42.dp
                )
            }

            5 -> {
                // Five overlapping icons in 2 rows: 2 above, 3 below
                UserIconWithBorder(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = (-16).dp, y = (-10).dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 16.dp, y = (-10).dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = (-24).dp, y = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 0.dp, y = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
                UserIconWithBorder(
                    modifier = Modifier
                        .size(40.dp)
                        .offset(x = 24.dp, y = 6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    size = 40.dp
                )
            }
        }
    }
}


@Composable
private fun UserIconWithBorder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    size: androidx.compose.ui.unit.Dp = 24.dp
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // White border (larger icon) - centered behind
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(size + 8.dp),
            tint = Color.White
        )
        // Main colored icon (original size) - centered on top
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(size - 4.dp),
            tint = color
        )
    }
}