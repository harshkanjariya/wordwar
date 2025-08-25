package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.network.service_holder.UserServiceHolder
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(navController: NavController) {
    var userName by remember { mutableStateOf("Loading...") }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val response = UserServiceHolder.api.getUserProfile()
                userName = if (response.status == 200 && response.data != null) {
                    response.data.name ?: ""
                } else if (response.status == 404) {
                    LocalStorage.removeToken()
                    navController.navigate("auth")
                    ""
                } else {
                    "Error"
                }
            } catch (e: Exception) {
                userName = "Error"
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Word War",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    Row(
                        modifier = Modifier
                            .clickable { navController.navigate("profile") }
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile",
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(16.dp))
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