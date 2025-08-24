package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MenuScreen(navController: NavController) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Main Menu", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            // This button now navigates to the new options screen
            Button(onClick = { navController.navigate("matchmaking_options") }, modifier = Modifier.width(220.dp)) {
                Text("Start Game")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                navController.navigate("login") { popUpTo("menu") { inclusive = true } }
            }, modifier = Modifier.width(220.dp)) {
                Text("Quit")
            }
        }
    }
}