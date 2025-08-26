package com.harshkanjariya.wordwar.screens


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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.harshkanjariya.wordwar.components.GameBackground
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.data.WordService
import com.harshkanjariya.wordwar.data.WordInfo
import com.harshkanjariya.wordwar.data.getUserIdFromJwt
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.DisposableEffect


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

    // Word of the day using WordService
    var currentWord by remember { mutableStateOf<WordInfo?>(null) }

    // Load initial word and prepare next word
    LaunchedEffect(Unit) {
        try {
            currentWord = WordService.getCurrentWord()
            WordService.loadNextWord() // Prepare next word
        } catch (e: Exception) {
            println("Error loading initial word: ${e.message}")
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
        GameBackground(
            backgroundColor = MaterialTheme.colorScheme.background,
            letterCount = 20
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        return
    }

    GameBackground(
        backgroundColor = MaterialTheme.colorScheme.background,
        letterCount = 20
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val scrollState = rememberScrollState()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(24.dp)
                    .padding(top = 48.dp, bottom = 80.dp)
                    .verticalScroll(scrollState)
            ) {
                // Queue status
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Waiting for ${matchSize - 1} player${if (matchSize - 1 == 1) "" else "s"} to join...",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(20.dp))

                // Word of the day card
                currentWord?.let { word ->
                    WordOfTheDayCard(
                        currentWord = word,
                        onRefresh = {
                            // Load next word in a coroutine
                            kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                try {
                                    currentWord = WordService.getCurrentWord()
                                    WordService.loadNextWord() // Prepare next word
                                } catch (e: Exception) {
                                    println("Error refreshing word: ${e.message}")
                                }
                            }
                        }
                    )
                } ?: run {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Cancel button
                OutlinedButton(
                    onClick = {
                        if (userId.isNotEmpty()) {
                            queueRef.child(userId).removeValue()
                        }
                        navController.popBackStack()
                    },
                    modifier = Modifier.width(220.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Cancel Queue")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun WordOfTheDayCard(
    currentWord: WordInfo,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with icon and refresh button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Word of the Day",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Refresh button
                IconButton(
                    onClick = onRefresh,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Get new word",
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Main word
            Text(
                text = currentWord.word,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Part of speech and pronunciation
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.clickable { },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = currentWord.partOfSpeech,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                if (currentWord.pronunciation.isNotEmpty()) {
                    Text(
                        text = currentWord.pronunciation,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }



            Spacer(Modifier.height(20.dp))

            // Definition
            Text(
                text = currentWord.meaning,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                ),
                textAlign = TextAlign.Center
            )

            // Additional definitions if available
            if (currentWord.definitions.size > 1) {
                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Additional Definitions:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        )
                        Spacer(Modifier.height(8.dp))

                        currentWord.definitions.drop(1).take(2).forEach { definition ->
                            Text(
                                text = "• $definition",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Start
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            // Examples if available
            if (currentWord.examples.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = if (currentWord.examples.size == 1) "Example:" else "Examples:",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                        Spacer(Modifier.height(8.dp))

                        currentWord.examples.take(2).forEach { example ->
                            Text(
                                text = "• $example",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = FontStyle.Italic,
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Start
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                    }
                }
            }

            // Synonyms and Antonyms if available
            if (currentWord.synonyms.isNotEmpty() || currentWord.antonyms.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Synonyms
                    if (currentWord.synonyms.isNotEmpty()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = "Synonyms:",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    currentWord.synonyms.take(3).forEach { synonym ->
                                        Text(
                                            text = "• $synonym",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        )
                                        Spacer(Modifier.height(2.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Antonyms
                    if (currentWord.antonyms.isNotEmpty()) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Antonyms:",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                )
                                Spacer(Modifier.height(8.dp))

                                currentWord.antonyms.take(3).forEach { antonym ->
                                    Text(
                                        text = "• $antonym",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Spacer(Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }

            Spacer(Modifier.height(16.dp))

            // Fun fact icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Learn while you wait!",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.tertiary,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}