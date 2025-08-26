package com.harshkanjariya.wordwar.screens

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.components.GameBackground
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.UserProfile
import com.harshkanjariya.wordwar.data.UserStatistics
import com.harshkanjariya.wordwar.network.service_holder.UserServiceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val user = remember { mutableStateOf<UserProfile?>(null) }
    val statistics = remember { mutableStateOf<UserStatistics?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Fetch user profile
                val profileResponse = UserServiceHolder.api.getUserProfile()
                user.value = if (profileResponse.status == 200) {
                    profileResponse.data
                } else {
                    null
                }
                
                // Fetch user statistics
                val statsResponse = UserServiceHolder.api.getUserStatistics()
                statistics.value = if (statsResponse.status == 200) {
                    statsResponse.data
                } else {
                    null
                }
            } catch (e: Exception) {
                user.value = null
                statistics.value = null
                e.printStackTrace()
            }
        }
    }

    GameBackground(
        backgroundColor = MaterialTheme.colorScheme.background,
        letterCount = 40
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, bottom = 80.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp) // Reduced padding
            ) {
                // Header Section
                ProfileHeader()

                Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing

                // Profile Content
                if (user.value != null) {
                    ProfileContent(user.value!!)
                } else {
                    LoadingProfile()
                }

                Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing

                // Stats Section
                ProfileStats(statistics.value)

                Spacer(modifier = Modifier.height(16.dp)) // Reduced spacing

                // Action Buttons
                ProfileActions(navController, coroutineScope)
            }
        }
    }
}

@Composable
private fun ProfileHeader() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8) // Smooth, light green like Material Design
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No elevation
        shape = RoundedCornerShape(16.dp), // Smaller radius
        border = BorderStroke(1.dp, Color(0xFF4CAF50)) // Smooth green border
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp), // Reduced padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Player Profile",
                style = MaterialTheme.typography.titleLarge.copy( // Smaller text
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF2E7D32), // Darker green for text
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ProfileContent(user: UserProfile) {
    val infiniteTransition = rememberInfiniteTransition(label = "profile")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No elevation
        shape = RoundedCornerShape(16.dp), // Smaller radius
        border = BorderStroke(1.dp, Color(0xFF4CAF50)) // Smooth green border
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(80.dp) // Smaller avatar
                    .background(
                        Color(0xFF4CAF50), // Smooth green
                        RoundedCornerShape(40.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile Avatar",
                    modifier = Modifier.size(48.dp), // Smaller icon
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

            // User Name
            Text(
                text = user.name ?: "Unknown Player",
                style = MaterialTheme.typography.titleLarge.copy( // Smaller text
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFF2E7D32), // Darker green
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing

            // User Email
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = "Email",
                    modifier = Modifier.size(14.dp), // Smaller icon
                    tint = Color(0xFF4CAF50) // Smooth green
                )
                Spacer(modifier = Modifier.width(6.dp)) // Reduced spacing
                Text(
                    text = user.email ?: "No email provided",
                    style = MaterialTheme.typography.bodyMedium, // Smaller text
                    color = Color(0xFF4CAF50), // Smooth green
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LoadingProfile() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No elevation
        shape = RoundedCornerShape(16.dp) // Smaller radius
    ) {
        Column(
            modifier = Modifier.padding(24.dp), // Reduced padding
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF4CAF50), // Smooth green
                modifier = Modifier.size(40.dp) // Smaller size
            )

            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

            Text(
                text = "Loading Profile...",
                style = MaterialTheme.typography.titleSmall, // Smaller text
                color = Color(0xFF2E7D32) // Darker green
            )
        }
    }
}

@Composable
private fun ProfileStats(statistics: UserStatistics?) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp), // No elevation
        shape = RoundedCornerShape(12.dp) // Smaller radius
    ) {
        Column(
            modifier = Modifier.padding(16.dp) // Reduced padding
        ) {
            Text(
                text = "Game Statistics",
                style = MaterialTheme.typography.titleMedium.copy( // Smaller text
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp)) // Reduced spacing

            if (statistics == null) {
                // Loading state for statistics
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Default.Favorite,
                        label = "Games Won",
                        value = statistics.gamesWon.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )

                    StatItem(
                        icon = Icons.Default.Star,
                        label = "Total Points",
                        value = formatNumber(statistics.totalPoints),
                        color = MaterialTheme.colorScheme.secondary
                    )

                    StatItem(
                        icon = Icons.Default.PlayArrow,
                        label = "Games Played",
                        value = statistics.totalGames.toString(),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                
                // Win Rate display
                if (statistics.totalGames > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Win Rate: ${statistics.winRate}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp) // Smaller size
                .background(color, RoundedCornerShape(10.dp)), // Smaller radius
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp), // Smaller icon
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(6.dp)) // Reduced spacing

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy( // Smaller text
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ProfileActions(navController: NavController, coroutineScope: CoroutineScope) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        LocalStorage.removeToken()
                        navController.navigate("auth") {
                            popUpTo(0) { inclusive = true } // clear backstack
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Logout",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }

        // Delete Account Button
        val showDeleteConfirmation = remember { mutableStateOf(false) }
        
        Button(
            onClick = { showDeleteConfirmation.value = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD32F2F) // Darker red for delete
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Account",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Delete Account",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
        
        // Delete Confirmation Dialog
        if (showDeleteConfirmation.value) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation.value = false },
                title = {
                    Text(
                        text = "Delete Account",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete your account? This action cannot be undone and will permanently remove all your data.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmation.value = false
                            coroutineScope.launch {
                                try {
                                    val response = UserServiceHolder.api.deleteAccount()
                                    if (response.status == 200) {
                                        // Clear local data and navigate to auth
                                        LocalStorage.removeToken()
                                        navController.navigate("auth") {
                                            popUpTo(0) { inclusive = true } // clear backstack
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Delete Permanently")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteConfirmation.value = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        OutlinedButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Back to Menu",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

private fun formatNumber(number: Int): String {
    return when {
        number >= 1000000 -> "${(number / 1000000.0).toInt()}M"
        number >= 1000 -> "${(number / 1000.0).toInt()}K"
        else -> number.toString()
    }
}
