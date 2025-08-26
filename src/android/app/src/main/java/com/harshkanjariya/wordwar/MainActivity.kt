package com.harshkanjariya.wordwar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.FirebaseApp
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.screens.AuthScreen
import com.harshkanjariya.wordwar.screens.GameResultScreen
import com.harshkanjariya.wordwar.screens.GameScreen
import com.harshkanjariya.wordwar.screens.HowToPlayScreen
import com.harshkanjariya.wordwar.screens.MatchmakingOptionsScreen
import com.harshkanjariya.wordwar.screens.MenuScreen
import com.harshkanjariya.wordwar.screens.ProfileScreen
import com.harshkanjariya.wordwar.screens.QueueScreen
import com.harshkanjariya.wordwar.ui.theme.WordWarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            WordWarTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()
                    val token by LocalStorage.getToken().collectAsState(initial = null)

                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(token) {
                        startDestination = if (token.isNullOrEmpty()) "auth" else "menu"
                    }

                    if (startDestination != null) {
                        NavHost(navController = navController, startDestination = startDestination!!) {
                            composable("auth") { AuthScreen(navController) }
                            composable("menu") { MenuScreen(navController) }
                            composable("matchmaking_options") { MatchmakingOptionsScreen(navController) }
                            composable("profile") { ProfileScreen(navController) }
                            composable("how_to_play") { HowToPlayScreen(navController) }
                            composable("queue/{matchSize}", arguments = listOf(navArgument("matchSize") { type = NavType.IntType })) { backStackEntry ->
                                val matchSize = backStackEntry.arguments?.getInt("matchSize") ?: 3
                                QueueScreen(navController, matchSize)
                            }
                            composable("game/{matchId}", arguments = listOf(navArgument("matchId") { type = NavType.StringType })) { backStackEntry ->
                                val matchId = backStackEntry.arguments?.getString("matchId")
                                GameScreen(navController, matchId)
                            }
                            composable("game_results/{matchId}", arguments = listOf(navArgument("matchId") { type = NavType.StringType })) { backStackEntry ->
                                val matchId = backStackEntry.arguments?.getString("matchId")
                                GameResultScreen(navController, matchId)
                            }
                        }
                    }
                }
            }
        }
    }
}