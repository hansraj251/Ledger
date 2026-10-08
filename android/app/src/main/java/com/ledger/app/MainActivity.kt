package com.ledger.app

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
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
import com.ledger.app.backup.GoogleDriveSyncCoordinator
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import com.ledger.app.ui.HomeScreen
import com.ledger.app.ui.AllPartiesReportScreen
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PartyLedgerScreen

class MainActivity : ComponentActivity() {

    private lateinit var googleDriveAuthorization: GoogleDriveAuthorization
    private lateinit var googleDriveBackup: GoogleDriveBackup
    private lateinit var googleDriveRestore: GoogleDriveRestore
    private lateinit var googleDriveSyncCoordinator: GoogleDriveSyncCoordinator
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

        googleDriveSyncCoordinator =
            GoogleDriveSyncCoordinator(
                context = applicationContext,
                authorization = googleDriveAuthorization,
                backup = googleDriveBackup,
                database = ledgerSqliteDatabase,
                onAuthorizationRequired = { intentSender ->
                    try {
                        startIntentSenderForResult(
                            intentSender,
                            GOOGLE_DRIVE_SYNC_REQUEST_CODE,
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
                },
                onSyncStarted = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Syncing backup...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSyncSuccess = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Backup synced to Google Drive.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSyncFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Backup failed: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onRemoteBackupNewer = { fileId, modifiedTime, accessToken ->
                    runOnUiThread {
                        performAutomaticRemoteRestore(
                            fileId = fileId,
                            modifiedTime = modifiedTime,
                            accessToken = accessToken
                        )
                    }
                }
            )

        val repository = LedgerRepository(
            database
        )

        setContent {
            LedgerTheme {

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

                var showAllPartiesReport by remember {
                    mutableStateOf(false)
                }

                val viewModel = remember {
                    LedgerViewModel(
                        repository,
                        onDataChanged = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        }
                    )
                }

                LaunchedEffect(initialDriveSetupRequired) {
                    if (!initialDriveSetupRequired) {
                        googleDriveSyncCoordinator.resume()
                    }
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
                } else if (showAllPartiesReport) {
                    AllPartiesReportScreen(
                        repository = repository,
                        onBack = {
                            showAllPartiesReport = false
                        }
                    )
                } else if (showProfileScreen) {
                    ProfileScreen(
                        onBack = {
                            showProfileScreen = false
                        },
                        onProfileSaved = {
                            googleDriveSyncCoordinator.requestAutoSync()
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
                        onReportsClick = {
                            showAllPartiesReport = true
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

    /*
     * Manual and automatic backup requests are serialized by
     * GoogleDriveSyncCoordinator. No direct Drive upload is started here.
     */

    private fun performAutomaticRemoteRestore(
        fileId: String,
        modifiedTime: String,
        accessToken: String
    ) {
        Toast.makeText(
            this,
            "Newer Google Drive backup found. Restoring...",
            Toast.LENGTH_LONG
        ).show()

        googleDriveRestore.restore(
            accessToken = accessToken,
            fileIdOverride = fileId
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
                            File(databaseFile.path + "-wal")

                        val shmFile =
                            File(databaseFile.path + "-shm")

                        val journalFile =
                            File(databaseFile.path + "-journal")

                        databaseFile.delete()
                        walFile.delete()
                        shmFile.delete()
                        journalFile.delete()

                        validatedFile.copyTo(
                            databaseFile,
                            overwrite = true
                        )

                        validatedFile.delete()

                        googleDriveSyncCoordinator
                            .markRemoteBackupSynced(
                                modifiedTime
                            )

                        Toast.makeText(
                            this,
                            "Newer backup restored successfully.",
                            Toast.LENGTH_LONG
                        ).show()

                        window.decorView.postDelayed(
                            {
                                val launchIntent =
                                    packageManager
                                        .getLaunchIntentForPackage(
                                            packageName
                                        )

                                finishAffinity()

                                if (launchIntent != null) {
                                    launchIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    )

                                    startActivity(launchIntent)
                                }
                            },
                            500
                        )
                    } catch (error: Exception) {
                        googleDriveSyncCoordinator
                            .resumeAfterRemoteRestoreFailure()

                        Toast.makeText(
                            this,
                            "Automatic restore failed: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .onFailure { error ->
                    googleDriveSyncCoordinator
                        .resumeAfterRemoteRestoreFailure()

                    Toast.makeText(
                        this,
                        "Automatic restore failed: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun performGoogleDriveRestore() {
        googleDriveSyncCoordinator.pause()

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
                googleDriveSyncCoordinator.resume()

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
            googleDriveSyncCoordinator.resume()

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
                        ""
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
    onProfileSaved: () -> Unit
) {
    BackHandler {
        onBack()
    }

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val profileViewModel: com.ledger.app.ui.ProfileViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory =
                com.ledger.app.ui.ProfileViewModelFactory(
                    context.applicationContext
                        as android.app.Application
                )
        )

    val profileState by
        profileViewModel.uiState.collectAsState()

    val scope =
        androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    var isEditing by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(false)
        }

    var name by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var mobile by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var editName by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var editMobile by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    androidx.compose.runtime.LaunchedEffect(
        profileState.name,
        profileState.mobile
    ) {
        name = profileState.name
        mobile = profileState.mobile

        if (!isEditing) {
            editName = profileState.name
            editMobile = profileState.mobile
        }
    }

    val initials =
        name.trim()
            .split(Regex("\\s+"))
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .joinToString("") {
                it.first().uppercase()
            }
            .ifBlank {
                "U"
            }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.Surface(
                tonalElevation = 3.dp
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier =
                        androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = onBack
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector =
                                androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Profile",
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        androidx.compose.material3.Text(
                            text = "Your personal details",
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        androidx.compose.foundation.lazy.LazyColumn(
            modifier =
                androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(14.dp),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    top = 16.dp,
                    bottom = 24.dp
                )
        ) {

            item {
                androidx.compose.material3.Card(
                    modifier =
                        androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape =
                        androidx.compose.material3.MaterialTheme
                            .shapes
                            .extraLarge,
                    elevation =
                        androidx.compose.material3.CardDefaults
                            .cardElevation(
                                defaultElevation = 2.dp
                            )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalAlignment =
                            androidx.compose.ui.Alignment.CenterHorizontally
                    ) {

                        androidx.compose.foundation.layout.Box(
                            modifier =
                                androidx.compose.ui.Modifier
                                    .size(88.dp)
                                    .clip(
                                        androidx.compose.foundation
                                            .shape
                                            .CircleShape
                                    )
                                    .background(
                                        androidx.compose.material3.MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                    ),
                            contentAlignment =
                                androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = initials,
                                style =
                                    androidx.compose.material3.MaterialTheme
                                        .typography
                                        .headlineMedium,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                color =
                                    androidx.compose.material3.MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(14.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                name.ifBlank {
                                    "Your Name"
                                },
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(4.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                mobile.ifBlank {
                                    "Add your mobile number"
                                },
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }
            }

            item {
                androidx.compose.material3.Card(
                    modifier =
                        androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape =
                        androidx.compose.material3.MaterialTheme
                            .shapes
                            .large
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        verticalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(14.dp)
                    ) {

                        androidx.compose.foundation.layout.Row(
                            modifier =
                                androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier =
                                    androidx.compose.ui.Modifier.weight(1f)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Personal Details",
                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        androidx.compose.ui.text.font.FontWeight
                                            .Bold
                                )

                                androidx.compose.material3.Text(
                                    text =
                                        "Manage your name and mobile number",
                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            if (!isEditing) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        editName = name
                                        editMobile = mobile
                                        isEditing = true
                                    }
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Edit",
                                        fontWeight =
                                            androidx.compose.ui.text.font.FontWeight
                                                .Bold
                                    )
                                }
                            }
                        }

                        if (isEditing) {

                            androidx.compose.material3.OutlinedTextField(
                                value = editName,
                                onValueChange = {
                                    editName = it
                                },
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                label = {
                                    androidx.compose.material3.Text(
                                        "Name"
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            androidx.compose.material.icons.Icons
                                                .Outlined
                                                .Person,
                                        contentDescription = null
                                    )
                                }
                            )

                            androidx.compose.material3.OutlinedTextField(
                                value = editMobile,
                                onValueChange = {
                                    editMobile = it
                                },
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                label = {
                                    androidx.compose.material3.Text(
                                        "Mobile Number"
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            androidx.compose.material.icons.Icons
                                                .Outlined
                                                .Phone,
                                        contentDescription = null
                                    )
                                }
                            )

                            androidx.compose.foundation.layout.Row(
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    androidx.compose.foundation.layout.Arrangement
                                        .spacedBy(10.dp)
                            ) {

                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        editName = name
                                        editMobile = mobile
                                        isEditing = false
                                    },
                                    modifier =
                                        androidx.compose.ui.Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        "Cancel"
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = {
                                        val cleanName =
                                            editName.trim()

                                        val cleanMobile =
                                            editMobile.trim()

                                        if (cleanName.isNotBlank()) {
                                            scope.launch {
                                                val saved =
                                                    profileViewModel.saveProfile(
                                                        name = cleanName,
                                                        mobile = cleanMobile
                                                    )

                                                if (saved) {
                                                    name = cleanName
                                                    mobile = cleanMobile
                                                    isEditing = false
                                            onProfileSaved()
                                                }
                                            }
                                        }
                                    },
                                    modifier =
                                        androidx.compose.ui.Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Save Changes",
                                        fontWeight =
                                            androidx.compose.ui.text.font.FontWeight
                                                .Bold
                                    )
                                }
                            }

                        } else {

                            ProfileDetailRow(
                                icon =
                                    androidx.compose.material.icons.Icons
                                        .Outlined
                                        .Person,
                                label = "Name",
                                value =
                                    name.ifBlank {
                                        "Not added"
                                    }
                            )

                            ProfileDetailRow(
                                icon =
                                    androidx.compose.material.icons.Icons
                                        .Outlined
                                        .Phone,
                                label = "Mobile Number",
                                value =
                                    mobile.ifBlank {
                                        "Not added"
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalAlignment =
            androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape =
                androidx.compose.foundation.shape.CircleShape,
            color =
                androidx.compose.material3.MaterialTheme
                    .colorScheme
                    .secondaryContainer,
            modifier =
                androidx.compose.ui.Modifier.size(42.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment =
                    androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(
            modifier =
                androidx.compose.ui.Modifier.width(14.dp)
        )

        androidx.compose.foundation.layout.Column(
            modifier =
                androidx.compose.ui.Modifier.weight(1f)
        ) {
            androidx.compose.material3.Text(
                text = label,
                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .labelMedium
            )

            androidx.compose.material3.Text(
                text = value,
                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .bodyLarge,
                fontWeight =
                    androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}

