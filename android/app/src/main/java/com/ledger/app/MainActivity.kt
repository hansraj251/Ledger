package com.ledger.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.identity.Identity
import com.ledger.app.backup.GoogleDriveAuthorization
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import com.ledger.app.ui.HomeScreen
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PartyLedgerScreen

class MainActivity : ComponentActivity() {

    private lateinit var googleDriveAuthorization: GoogleDriveAuthorization

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        googleDriveAuthorization = GoogleDriveAuthorization(
            this
        )

        val database = LedgerDatabaseProvider.get(
            applicationContext
        )

        val repository = LedgerRepository(
            database
        )

        setContent {
            MaterialTheme {
                var selectedParty by remember {
                    mutableStateOf<PartyEntity?>(null)
                }

                var showProfileScreen by remember {
                    mutableStateOf(false)
                }

                val viewModel = remember {
                    LedgerViewModel(
                        repository
                    )
                }

                if (showProfileScreen) {
                    ProfileScreen(
                        onBack = {
                            showProfileScreen = false
                        },
                        onConnectGoogleDrive = {
                            connectGoogleDrive()
                        }
                    )
                } else if (selectedParty == null) {
                    HomeScreen(
                        viewModel = viewModel,
                        onPartyClick = { party ->
                            selectedParty = party
                        },
                        onProfileClick = {
                            showProfileScreen = true
                        }
                    )
                } else {
                    PartyLedgerScreen(
                        party = selectedParty!!,
                        viewModel = viewModel,
                        onBack = {
                            selectedParty = null
                        }
                    )
                }
            }
        }
    }

    private fun connectGoogleDrive() {
        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        result.pendingIntent?.let { pendingIntent ->
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                GOOGLE_DRIVE_AUTH_REQUEST_CODE,
                                null,
                                0,
                                0,
                                0
                            )
                        }
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Google Drive connected.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Google Drive authorization failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    companion object {
        private const val GOOGLE_DRIVE_AUTH_REQUEST_CODE = 9001
    }
}

@androidx.compose.runtime.Composable
private fun ProfileScreen(
    onBack: () -> Unit,
    onConnectGoogleDrive: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier.padding(
                paddingValues
            )
        ) {
            Text(
                text = "Profile",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            Text(
                text = "Backup",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "Google Drive",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AssistChip(
                onClick = onConnectGoogleDrive,
                label = {
                    Text("Connect Google Drive")
                }
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            AssistChip(
                onClick = onBack,
                label = {
                    Text("Back")
                }
            )
        }
    }
}
