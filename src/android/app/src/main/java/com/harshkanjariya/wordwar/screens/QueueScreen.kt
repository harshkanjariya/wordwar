package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.getUserIdFromJwt
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

@Composable
fun QueueScreen(navController: NavController, matchSize: Int) {
    val token by LocalStorage.getToken().collectAsState(initial = null)
    val userId = remember(token) {
        if (token.isNullOrEmpty()) {
            null
        } else {
            getUserIdFromJwt(token)
        }
    }

    val database = FirebaseDatabase.getInstance()
    val queueRef = database.getReference("matchmaking_queue").child(matchSize.toString())
    val liveGamesRef = database.getReference("live_games")

    var statusMessage by remember { mutableStateOf("Checking for existing games...") }
    var foundMatchId by remember { mutableStateOf<String?>(null) }
    var shouldJoinQueue by remember { mutableStateOf(false) }

    // State to track if the initial check for a live game is complete
    var hasCheckedForGame by remember { mutableStateOf(false) }

    // Step 1: Check for an existing game first, before joining the queue.
    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            val userGameRef = liveGamesRef
                .orderByChild("players/$userId")
                .limitToFirst(1)

            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val gameSnapshot = snapshot.children.firstOrNull()
                    if (gameSnapshot != null) {
                        // User is already in a game, navigate to it immediately.
                        foundMatchId = gameSnapshot.key
                        statusMessage = "Already in a game. Redirecting..."
                    } else {
                        // User is not in a game, so they should join the queue.
                        shouldJoinQueue = true
                        statusMessage = "In queue..."
                    }
                    hasCheckedForGame = true
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error, but still proceed to join queue in case of network issue.
                    shouldJoinQueue = true
                    hasCheckedForGame = true
                    statusMessage = "Failed to check for games. Joining queue..."
                }
            }

            userGameRef.addListenerForSingleValueEvent(listener)
        }
    }

    // Step 2: Conditionally join the queue, only if the check passes.
    LaunchedEffect(shouldJoinQueue) {
        if (shouldJoinQueue && !userId.isNullOrEmpty()) {
            queueRef.child(userId).setValue(mapOf("timestamp" to System.currentTimeMillis())).await()
            queueRef.child(userId).onDisconnect().removeValue()
        }
    }

    // Step 3: Listen for a match, only after the check is complete and we are in the queue.
    DisposableEffect(hasCheckedForGame, userId) {
        if (!hasCheckedForGame || userId.isNullOrEmpty()) {
            onDispose { }
            return@DisposableEffect onDispose { }
        }

        val userGameRef = liveGamesRef
            .orderByChild("players/$userId")
            .limitToFirst(1)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val gameSnapshot = snapshot.children.firstOrNull()
                if (gameSnapshot != null) {
                    foundMatchId = gameSnapshot.key
                    statusMessage = "Match found!"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }

        userGameRef.addValueEventListener(listener)
        onDispose {
            userGameRef.removeEventListener(listener)
        }
    }

    // LaunchedEffect to navigate after a match is found
    LaunchedEffect(foundMatchId) {
        if (foundMatchId != null) {
            delay(2000) // Wait 2 seconds
            navController.navigate("game/${foundMatchId}") {
                popUpTo("menu")
            }
        }
    }

    // Show a loading screen while the userId or the check is not complete
    if (userId.isNullOrEmpty() || !hasCheckedForGame) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(statusMessage, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    // Remove user from the queue and navigate back
                    if (!userId.isNullOrEmpty()) {
                        queueRef.child(userId).removeValue()
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.width(220.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}