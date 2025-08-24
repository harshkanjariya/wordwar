package com.harshkanjariya.wordwar.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import com.google.firebase.database.FirebaseDatabase
import com.harshkanjariya.wordwar.data.LocalStorage // Import LocalStorage
import com.harshkanjariya.wordwar.data.getUserIdFromJwt

@Composable
fun QueueScreen(navController: NavController, matchSize: Int) {
    // Get the user ID from LocalStorage asynchronously
    val token by LocalStorage.getToken(navController.context).collectAsState(initial = null)

    val userId = remember(token) {
        if (token.isNullOrEmpty()) {
            null
        } else {
            getUserIdFromJwt(token)
        }
    }

    val database = FirebaseDatabase.getInstance()
    val queueRef = database.getReference("matchmaking_queues").child(matchSize.toString())

    var statusMessage by remember { mutableStateOf("In queue...") }
    var foundMatchId by remember { mutableStateOf<String?>(null) }

    // Show a loading screen while the userId is not available
    if (userId.isNullOrEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return // Return to prevent the rest of the code from running
    }

    // Use a LaunchedEffect to join the queue, keyed by userId
    LaunchedEffect(userId) {
        // Add user to the queue
        queueRef.child(userId).setValue(mapOf("timestamp" to System.currentTimeMillis())).await()
        queueRef.child(userId).onDisconnect().removeValue()
    }

    // Use a DisposableEffect for the listener, keyed by userId
    DisposableEffect(userId) {
        val userStatusRef = database.getReference("live_games")
            .orderByChild("players").equalTo(userId)
            .limitToFirst(1)

        val listener = userStatusRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val match = snapshot.value as? Map<String, Any>
                if (match != null) {
                    val players = match["players"] as? List<String>
                    if (players?.contains(userId) == true) {
                        foundMatchId = snapshot.key
                        statusMessage = "Match found!"
                    }
                }
            }
            // Other methods omitted for brevity
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })

        // The onDispose block is guaranteed to run when the composable leaves the screen
        onDispose {
            userStatusRef.removeEventListener(listener)
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
                    queueRef.child(userId).removeValue()
                    navController.popBackStack()
                },
                modifier = Modifier.width(220.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}