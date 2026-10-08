package com.ledger.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun AllPartiesReportScreen(
    repository: LedgerRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var reportFile by remember {
        mutableStateOf<File?>(null)
    }

    var isGenerating by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val snackbarHostState = remember {
        SnackbarHostState()
    }

    LaunchedEffect(Unit) {

        isGenerating = true
        errorMessage = null

        try {

            reportFile = withContext(Dispatchers.IO) {

                val database =
                    LedgerDatabaseProvider.get(context)

                val profileName =
                    database.profileDao()
                        .getProfile()
                        ?.name
                        ?.trim()
                        .orEmpty()

                val parties =
                    repository.getParties()

                val entriesByParty =
                    parties.associate { party ->
                        party.id to repository.getEntries(
                            party.id
                        )
                    }

                createAllPartiesReportPdf(
                    context = context,
                    parties = parties,
                    entriesByParty = entriesByParty,
                    profileName = profileName
                )
            }

        } catch (error: Exception) {

            errorMessage =
                error.message
                    ?: "Unable to generate report"

        } finally {

            isGenerating = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 8.dp,
                        end = 12.dp,
                        top = 8.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {

                IconButton(
                    onClick = onBack
                ) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Text(
                    text = "All Parties Report",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {

                        val file = reportFile ?: return@IconButton

                        try {

                            val message =
                                downloadAllPartiesReportPdf(
                                    context = context,
                                    source = file
                                )

                            snackbarHostState.currentSnackbarData
                                ?.dismiss()

                            // Snackbar is shown through a launched coroutine
                            // below via a small helper state.
                            android.widget.Toast
                                .makeText(
                                    context,
                                    message,
                                    android.widget.Toast.LENGTH_LONG
                                )
                                .show()

                        } catch (error: Exception) {

                            android.widget.Toast
                                .makeText(
                                    context,
                                    error.message
                                        ?: "Unable to download report",
                                    android.widget.Toast.LENGTH_LONG
                                )
                                .show()
                        }
                    },
                    enabled = reportFile != null
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.Download,
                        contentDescription = "Download report"
                    )
                }

                IconButton(
                    onClick = {

                        val file = reportFile
                            ?: return@IconButton

                        try {

                            val uri =
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )

                            val shareIntent =
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(
                                        Intent.EXTRA_STREAM,
                                        uri
                                    )
                                    addFlags(
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    )
                                }

                            context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    "Share report"
                                )
                            )

                        } catch (error: Exception) {

                            android.widget.Toast
                                .makeText(
                                    context,
                                    error.message
                                        ?: "Unable to share report",
                                    android.widget.Toast.LENGTH_LONG
                                )
                                .show()
                        }
                    },
                    enabled = reportFile != null
                ) {
                    Icon(
                        imageVector =
                            Icons.Outlined.Share,
                        contentDescription = "Share report"
                    )
                }
            }

            when {

                isGenerating -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                errorMessage != null -> {

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color =
                                MaterialTheme.colorScheme.error,
                            modifier =
                                Modifier.padding(24.dp)
                        )
                    }
                }

                reportFile != null -> {

                    PdfPreview(
                        file = reportFile!!,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}
