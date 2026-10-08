package com.ledger.app.ui

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import com.ledger.app.data.LedgerDatabaseProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PartyReportScreen(
    party: com.ledger.app.data.PartyEntity,
    entries: List<com.ledger.app.data.LedgerEntryEntity>,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var reportFile by remember {
        mutableStateOf<File?>(null)
    }

    var isGenerating by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(party.id, entries) {
        isGenerating = true
        errorMessage = null

        try {
            reportFile = withContext(Dispatchers.IO) {
                val database = LedgerDatabaseProvider.get(context)
                val profileName = database.profileDao()
                    .getProfile()
                    ?.name
                    ?.trim()
                    .orEmpty()

                createPartyReportPdf(
                    context = context,
                    party = party,
                    entries = entries,
                    profileName = profileName
                )
            }
        } catch (error: Exception) {
            errorMessage = error.message ?: "Unable to generate report"
        } finally {
            isGenerating = false
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
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
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Reports",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = party.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (reportFile != null) {
                    IconButton(
                        onClick = {
                            try {
                                val message = downloadPartyReportPdf(
                                    context = context,
                                    source = reportFile!!,
                                    partyName = party.name
                                )

                                Toast.makeText(
                                    context,
                                    message,
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    error.message ?: "Download failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = "Download report"
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    reportFile!!
                                )

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "${party.name} Ledger Report"
                                    )
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }

                                context.startActivity(
                                    Intent.createChooser(
                                        shareIntent,
                                        "Share report"
                                    )
                                )
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    error.message ?: "Share failed",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share report"
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isGenerating -> {
                        CircularProgressIndicator()
                    }

                    errorMessage != null -> {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }

                    reportFile != null -> {
                        PdfPreview(
                            file = reportFile!!,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun PdfPreview(
    file: File,
    modifier: Modifier = Modifier
) {
    var pages by remember(file) {
        mutableStateOf<List<Bitmap>>(emptyList())
    }

    LaunchedEffect(file) {
        pages = withContext(Dispatchers.IO) {
            renderPdfPages(file)
        }
    }

    if (pages.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                12.dp
            )
        ) {
            items(
                items = pages
            ) { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "PDF page",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun renderPdfPages(
    file: File
): List<Bitmap> {
    val descriptor = ParcelFileDescriptor.open(
        file,
        ParcelFileDescriptor.MODE_READ_ONLY
    )

    descriptor.use {
        PdfRenderer(it).use { renderer ->
            val result = ArrayList<Bitmap>(renderer.pageCount)

            for (index in 0 until renderer.pageCount) {
                renderer.openPage(index).use { page ->
                    val targetWidth = 1000
                    val scale =
                        targetWidth.toFloat() / page.width.toFloat()

                    val targetHeight = maxOf(
                        1,
                        (page.height * scale).toInt()
                    )

                    val bitmap = Bitmap.createBitmap(
                        targetWidth,
                        targetHeight,
                        Bitmap.Config.ARGB_8888
                    )

                    Canvas(bitmap).drawColor(
                        android.graphics.Color.WHITE
                    )

                    page.render(
                        bitmap,
                        null,
                        null,
                        PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                    )

                    result += bitmap
                }
            }

            return result
        }
    }
}
