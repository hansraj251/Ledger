package com.ledger.app.cloudgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.ComponentActivity
import android.content.Context
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun CloudAccountCard() {
    val context = LocalContext.current
    val activity = remember(context) {
        var current: Context? = context
        while (current is android.content.ContextWrapper &&
            current !is ComponentActivity
        ) {
            current = current.baseContext
        }
        current as? ComponentActivity
    }
    val authManager = remember(activity) {
        activity?.let { CloudAuthManager(it) }
    }
    val scope = rememberCoroutineScope()

    var user by remember { mutableStateOf<CloudAuthenticatedUser?>(null) }
    var loading by remember { mutableStateOf(true) }
    var working by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authManager) {
        loading = true
        try {
            if (authManager != null) {
                user = authManager.restoreSession()
            }
        } catch (error: Exception) {
            errorMessage = error.message ?: "Could not restore account session."
        } finally {
            loading = false
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Ledger Online",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Sign in with Google to access groups shared with your Gmail account.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when {
                loading -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator()
                        Text("Checking saved account...")
                    }
                }

                user != null -> {
                    Text(
                        text = "Connected Google Account",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = user!!.email,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Online Groups account connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                working = true
                                errorMessage = null
                                try {
                                    val manager = authManager
                                        ?: error("Activity context is unavailable.")
                                    manager.signOut()
                                    user = null
                                } catch (error: Exception) {
                                    errorMessage = error.message ?: "Sign-out failed."
                                } finally {
                                    working = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !working
                    ) {
                        Text("Sign Out")
                    }
                }

                else -> {
                    Button(
                        onClick = {
                            scope.launch {
                                working = true
                                errorMessage = null
                                try {
                                    val manager = authManager
                                        ?: error("Activity context is unavailable.")
                                    user = manager.signInWithGoogle()
                                } catch (error: Exception) {
                                    errorMessage = error.message
                                        ?: "Google sign-in failed."
                                } finally {
                                    working = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !working
                    ) {
                        if (working) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(4.dp))
                        } else {
                            Text("Continue with Google")
                        }
                    }
                }
            }

            if (working && user == null) {
                Text(
                    text = "Connecting account...",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
