package com.harshkanjariya.wordwar.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.harshkanjariya.wordwar.components.GoogleSignInButton
import com.harshkanjariya.wordwar.data.LocalStorage
import com.harshkanjariya.wordwar.network.service_holder.AuthServiceHolder
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.harshkanjariya.wordwar.network.service.SocialLoginParam
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import kotlin.math.acos

@Composable
fun AuthScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val googleIdOption = remember {
        GetGoogleIdOption.Builder()
            .setServerClientId("535433341383-vhff3t60a72trbuunimcgpl30bevbuf6.apps.googleusercontent.com")
            .setFilterByAuthorizedAccounts(false)
            .build()
    }

    val request = remember {
        GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GoogleSignInButton(
            onClick = {
                scope.launch {
                    try {
                        val result: GetCredentialResponse =
                            credentialManager.getCredential(context, request)

                        val credential = result.credential
                        if (credential is androidx.credentials.CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val idToken = googleIdTokenCredential.idToken

                            // 1. Authenticate with Firebase using the Google ID token
                            val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                            val authResult = FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()

                            // 2. Get the Firebase ID token
                            val firebaseIdToken = authResult.user?.getIdToken(true)?.await()?.token

                            if (firebaseIdToken != null) {
                                // 3. Send the Firebase ID token to your backend
                                val authResponse = AuthServiceHolder.api.socialLogin(
                                    SocialLoginParam(
                                        type = "google",
                                        accessToken = firebaseIdToken
                                    )
                                )

                                val tokenFromApi = authResponse.data?.token

                                if (tokenFromApi != null && tokenFromApi.isNotEmpty()) {
                                    LocalStorage.saveToken(tokenFromApi)

                                    navController.navigate("menu") {
                                        popUpTo("auth") { inclusive = true }
                                    }
                                } else {
                                    Toast.makeText(context, "Authentication failed.", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to get Firebase ID token.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}