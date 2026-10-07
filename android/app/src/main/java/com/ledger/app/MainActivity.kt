package com.ledger.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.auth.api.identity.Identity
import java.io.File
import com.ledger.app.backup.GoogleDriveAuthorization
import com.ledger.app.backup.GoogleDriveBackup
import com.ledger.app.backup.GoogleDriveRestore
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import com.ledger.app.ui.HomeScreen
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PartyLedgerScreen

class MainActivity : ComponentActivity() {

    private lateinit var googleDriveAuthorization: GoogleDriveAuthorization
    private lateinit var googleDriveBackup: GoogleDriveBackup
    private lateinit var googleDriveRestore: GoogleDriveRestore
    private lateinit var ledgerSqliteDatabase: SupportSQLiteDatabase


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        googleDriveAuthorization = GoogleDriveAuthorization(
            this
        )

        googleDriveBackup = GoogleDriveBackup(
            applicationContext
        )

        googleDriveRestore = GoogleDriveRestore(
            applicationContext
        )

        val database = LedgerDatabaseProvider.get(
            applicationContext
        )

        ledgerSqliteDatabase =
            database.openHelper.writableDatabase

        val repository = LedgerRepository(
            database
        )

        setContent {
            LedgerTheme {
                var showRestoreDialog by remember {
                    mutableStateOf(false)
                }

                var initialDriveSetupRequired by remember {
                    mutableStateOf(
                        !isInitialDriveSetupComplete()
                    )
                }

                var initialDriveSetupRunning by remember {
                    mutableStateOf(false)
                }

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

                if (initialDriveSetupRequired) {
                    InitialGoogleDriveSetupScreen(
                        isRunning = initialDriveSetupRunning,
                        onConnect = {
                            initialDriveSetupRunning = true
                            performInitialGoogleDriveSetup(
                                onComplete = {
                                    initialDriveSetupRunning = false
                                    initialDriveSetupRequired = false
                                }
                            )
                        }
                    )
                } else if (showProfileScreen) {
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
                        },
                        onSyncClick = {
                            syncGoogleDrive()
                        },
                        onRestoreClick = {
                            showRestoreDialog = true
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

                if (showRestoreDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = {
                            showRestoreDialog = false
                        },
                        title = {
                            androidx.compose.material3.Text(
                                "Restore backup?"
                            )
                        },
                        text = {
                            androidx.compose.material3.Text(
                                "This will replace the current Ledger data with the Google Drive backup. Current local changes will be lost."
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showRestoreDialog = false
                                    performGoogleDriveRestore()
                                }
                            ) {
                                androidx.compose.material3.Text(
                                    "Restore"
                                )
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(
                                onClick = {
                                    showRestoreDialog = false
                                }
                            ) {
                                androidx.compose.material3.Text(
                                    "Cancel"
                                )
                            }
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

    private fun syncGoogleDrive() {
        Toast.makeText(
            this,
            "Syncing backup...",
            Toast.LENGTH_SHORT
        ).show()

        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        val pendingIntent =
                            result.pendingIntent

                        if (pendingIntent == null) {
                            Toast.makeText(
                                this,
                                "Google Drive authorization is required.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            GOOGLE_DRIVE_SYNC_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )

                        Toast.makeText(
                            this,
                            "Complete Google Drive authorization, then tap Sync again.",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@addOnSuccessListener
                }

                val accessToken =
                    result.accessToken

                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "Google Drive access token is unavailable.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                googleDriveBackup.sync(
                    ledgerSqliteDatabase,
                    accessToken
                ) { syncResult ->
                    syncResult
                        .onSuccess {
                            Toast.makeText(
                                this,
                                "Backup synced to Google Drive.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this,
                                "Backup failed: ${error.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${error.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun performGoogleDriveRestore() {
        Toast.makeText(
            this,
            "Downloading backup...",
            Toast.LENGTH_SHORT
        ).show()

        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    Toast.makeText(
                        this,
                        "Google Drive authorization is required. Connect Google Drive first.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                val accessToken =
                    result.accessToken

                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "Google Drive access token is unavailable.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                googleDriveRestore.restore(
                    accessToken
                ) { restoreResult ->
                    restoreResult
                        .onSuccess {
                            try {
                                val validatedFile =
                                    googleDriveRestore.getValidatedBackupFile()

                                LedgerDatabaseProvider.close()

                                val databaseFile =
                                    getDatabasePath(
                                        LedgerDatabaseProvider.DATABASE_NAME
                                    )

                                val walFile =
                                    File(
                                        databaseFile.path + "-wal"
                                    )

                                val shmFile =
                                    File(
                                        databaseFile.path + "-shm"
                                    )

                                val journalFile =
                                    File(
                                        databaseFile.path + "-journal"
                                    )

                                databaseFile.delete()
                                walFile.delete()
                                shmFile.delete()
                                journalFile.delete()

                                validatedFile.copyTo(
                                    databaseFile,
                                    overwrite = true
                                )

                                validatedFile.delete()

                                Toast.makeText(
                                    this,
                                    "Backup restored successfully.",
                                    Toast.LENGTH_LONG
                                ).show()

                                window.decorView.postDelayed(
                                    {
                                        val launchIntent =
                                            packageManager.getLaunchIntentForPackage(
                                                packageName
                                            )

                                        finishAffinity()

                                        if (launchIntent != null) {
                                            launchIntent.addFlags(
                                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                                    Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            )

                                            startActivity(
                                                launchIntent
                                            )
                                        }
                                    },
                                    500
                                )
                            } catch (error: Exception) {
                                Toast.makeText(
                                    this,
                                    "Restore failed: ${error.message ?: "Unable to replace database"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this,
                                "Restore failed: ${error.message ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${error.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }


    private fun isInitialDriveSetupComplete(): Boolean {
        return getSharedPreferences(
            "ledger_setup",
            MODE_PRIVATE
        ).getBoolean(
            "initial_drive_setup_complete",
            false
        )
    }

    private fun markInitialDriveSetupComplete() {
        getSharedPreferences(
            "ledger_setup",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "initial_drive_setup_complete",
                true
            )
            .apply()
    }

    private fun performInitialGoogleDriveSetup(
        onComplete: () -> Unit
    ) {
        Toast.makeText(
            this,
            "Connect Google Drive to continue.",
            Toast.LENGTH_SHORT
        ).show()

        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        val pendingIntent =
                            result.pendingIntent

                        if (pendingIntent == null) {
                            Toast.makeText(
                                this,
                                "Google Drive authorization is required.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@addOnSuccessListener
                }

                continueInitialGoogleDriveSetup(
                    onComplete
                )
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${
                        error.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun continueInitialGoogleDriveSetup(
        onComplete: () -> Unit
    ) {
        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        val pendingIntent =
                            result.pendingIntent

                        if (pendingIntent == null) {
                            Toast.makeText(
                                this,
                                "Google Drive authorization is required.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@addOnSuccessListener
                }

                val accessToken =
                    result.accessToken

                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "Google Drive access token is unavailable.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@addOnSuccessListener
                }

                Toast.makeText(
                    this,
                    "Checking Google Drive backup...",
                    Toast.LENGTH_SHORT
                ).show()

                googleDriveRestore.findBackupFile(
                    accessToken
                ) { backupResult ->
                    backupResult
                        .onSuccess { fileId ->
                            if (fileId.isNullOrBlank()) {
                                markInitialDriveSetupComplete()

                                Toast.makeText(
                                    this,
                                    "No backup found. Starting fresh.",
                                    Toast.LENGTH_SHORT
                                ).show()

                                onComplete()
                                return@onSuccess
                            }

                            Toast.makeText(
                                this,
                                "Backup found. Restoring...",
                                Toast.LENGTH_LONG
                            ).show()

                            googleDriveRestore.restore(
                                accessToken,
                                fileId
                            ) { restoreResult ->
                                restoreResult
                                    .onSuccess {
                                        replaceDatabaseAndRestart(
                                            onComplete
                                        )
                                    }
                                    .onFailure { error ->
                                        Toast.makeText(
                                            this,
                                            "Backup restore failed: ${
                                                error.message ?: "Unknown error"
                                            }",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                        .onFailure { error ->
                            Toast.makeText(
                                this,
                                "Unable to check Google Drive: ${
                                    error.message ?: "Unknown error"
                                }",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${
                        error.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun replaceDatabaseAndRestart(
        onComplete: () -> Unit
    ) {
        try {
            val validatedFile =
                googleDriveRestore.getValidatedBackupFile()

            LedgerDatabaseProvider.close()

            val databaseFile =
                getDatabasePath(
                    LedgerDatabaseProvider.DATABASE_NAME
                )

            File(
                databaseFile.path + "-wal"
            ).delete()

            File(
                databaseFile.path + "-shm"
            ).delete()

            File(
                databaseFile.path + "-journal"
            ).delete()

            databaseFile.delete()

            validatedFile.copyTo(
                databaseFile,
                overwrite = true
            )

            validatedFile.delete()

            markInitialDriveSetupComplete()

            Toast.makeText(
                this,
                "Backup restored successfully.",
                Toast.LENGTH_LONG
            ).show()

            window.decorView.postDelayed(
                {
                    val launchIntent =
                        packageManager.getLaunchIntentForPackage(
                            packageName
                        )

                    finishAffinity()

                    if (launchIntent != null) {
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )

                        startActivity(
                            launchIntent
                        )
                    }
                },
                500
            )
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "Restore failed: ${
                    error.message ?: "Unable to replace database"
                }",
                Toast.LENGTH_LONG
            ).show()

            onComplete()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode ==
            GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE
        ) {
            if (resultCode == RESULT_OK) {
                continueInitialGoogleDriveSetup(
                    onComplete = {}
                )
            } else {
                Toast.makeText(
                    this,
                    "Google Drive connection is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val GOOGLE_DRIVE_AUTH_REQUEST_CODE = 9001
        private const val GOOGLE_DRIVE_SYNC_REQUEST_CODE = 9002
        private const val GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE = 9003
    }
}

@androidx.compose.runtime.Composable
private fun InitialGoogleDriveSetupScreen(
    isRunning: Boolean,
    onConnect: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(24.dp)
        ) {
            Spacer(
                modifier =
                    Modifier.height(80.dp)
            )

            Text(
                text = "Connect Google Drive",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "Ledger uses Google Drive to protect your data. " +
                        "Connect Google Drive to continue."
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "If a Ledger backup is found, it will be restored automatically. " +
                        "If no backup exists, a new Ledger will be created."
            )

            Spacer(
                modifier =
                    Modifier.height(32.dp)
            )

            if (isRunning) {
                CircularProgressIndicator()

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text =
                        "Connecting and checking your backup..."
                )
            } else {
                Button(
                    onClick = onConnect
                ) {
                    Text(
                        text = "Connect to Google Drive"
                    )
                }
            }
        }
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
