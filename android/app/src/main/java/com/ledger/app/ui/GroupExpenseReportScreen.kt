package com.ledger.app.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.ledger.app.data.CurrencySettings
import com.ledger.app.data.GroupExpenseEntity
import com.ledger.app.data.GroupExpenseSplitEntity
import com.ledger.app.data.GroupSettlementEntity
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.SharingGroupEntity
import com.ledger.app.data.SharingMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun GroupExpenseReportScreen(
    groupId: Long,
    onBack: () -> Unit,
    memberId: Long? = null
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var reportFile by remember { mutableStateOf<File?>(null) }
    var isGenerating by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var groupName by remember { mutableStateOf("Group Expense Report") }

    LaunchedEffect(groupId, memberId) {
        isGenerating = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val db = LedgerDatabaseProvider.get(context)
                val group = db.sharingGroupDao().getAll().firstOrNull { it.id == groupId }
                    ?: error("Group not found")
                val allMembers = db.sharingMemberDao().getByGroup(groupId)
                val allExpenses = db.groupExpenseDao().getByGroup(groupId)
                val allSplits = allExpenses.associate {
                    it.id to db.groupExpenseSplitDao().getByExpense(it.id)
                }
                val allPayments = allExpenses.associate {
                    it.id to db.groupExpensePaymentDao().getByExpense(it.id)
                }
                val allSettlements = db.groupSettlementDao().getByGroup(groupId)
                val selectedMember = memberId?.let { id -> allMembers.firstOrNull { it.id == id } }
                if (memberId != null && selectedMember == null) error("Member not found")

                val filteredExpenses = if (memberId == null) allExpenses else allExpenses.filter { expense ->
                    allSplits[expense.id].orEmpty().any { it.memberId == memberId } ||
                        allPayments[expense.id].orEmpty().any { it.memberId == memberId }
                }
                val filteredSplits = filteredExpenses.associate { expense ->
                    expense.id to allSplits[expense.id].orEmpty().filter {
                        memberId == null || it.memberId == memberId
                    }
                }
                val filteredMembers = if (memberId == null) allMembers else allMembers.filter { it.id == memberId }
                val filteredSettlements = if (memberId == null) allSettlements
                    else allSettlements.filter { it.memberId == memberId }
                val file = createGroupExpenseReportPdf(
                    context, group, filteredMembers, filteredExpenses, filteredSplits, filteredSettlements
                )
                (selectedMember?.let { "${group.name} - ${it.name}" } ?: group.name) to file
            }
            groupName = result.first
            reportFile = result.second
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unable to generate group report"
        } finally {
            isGenerating = false
        }
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column(Modifier.weight(1f)) {
                    Text(if (memberId == null) "Group Expense Report" else "Member Expense Report", style = MaterialTheme.typography.titleLarge)
                    Text(groupName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    enabled = reportFile != null,
                    onClick = {
                        try {
                            val message = downloadGroupReportPdf(context, reportFile!!, groupName)
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Download failed", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = "Download report")
                }
                IconButton(
                    enabled = reportFile != null,
                    onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(
                                context, "${context.packageName}.fileprovider", reportFile!!
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "$groupName Expense Report")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share group report"))
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Share failed", Toast.LENGTH_LONG).show()
                        }
                    }
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share report")
                }
            }

            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isGenerating -> CircularProgressIndicator()
                    errorMessage != null -> Text(
                        errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                    reportFile != null -> PdfPreview(
                        file = reportFile!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

private fun createGroupExpenseReportPdf(
    context: Context,
    group: SharingGroupEntity,
    members: List<SharingMemberEntity>,
    expenses: List<GroupExpenseEntity>,
    splits: Map<Long, List<GroupExpenseSplitEntity>>,
    settlements: List<GroupSettlementEntity>
): File {
    val pdf = PdfDocument()
    val writer = GroupExpensePdfWriter(pdf)
    val money: (Double) -> String = { CurrencySettings.format(it) }
    val date = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateTime = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val now = System.currentTimeMillis()
    val ordered = expenses.sortedWith(compareByDescending<GroupExpenseEntity> { it.expenseDate }.thenByDescending { it.id })
    val total = ordered.sumOf { it.amount }
    val splitTotal = splits.values.flatten().sumOf { it.amount }
    val settlementTotal = settlements.sumOf { it.amount }

    try {
        writer.heading("GROUP EXPENSE REPORT", 19f)
        writer.line(group.name, 14f, true)
        writer.line("Generated: ${dateTime.format(Date(now))}")
        writer.line("Members: ${members.size}    Expenses: ${ordered.size}")
        writer.gap(10f)
        writer.section("EXPENSE SUMMARY")
        writer.pair("Total recorded expenses", money(total))
        writer.pair("Number of expenses", ordered.size.toString())
        writer.pair("Total allocated member shares", money(splitTotal))
        writer.pair("Total recorded settlements", money(settlementTotal))
        writer.gap(12f)

        writer.section("MEMBER-WISE SUMMARY")
        members.forEach { member ->
            val share = splits.values.flatten()
                .filter { it.memberId == member.id }
                .sumOf { it.amount }
            val settled = settlements.filter { it.memberId == member.id }.sumOf { it.amount }
            val remaining = (share - settled).coerceAtLeast(0.0)
            writer.line(member.name, 11f, true)
            writer.pair("  Allocated expense share", money(share))
            writer.pair("  Recorded settlements", money(settled))
            writer.pair("  Remaining balance", money(remaining))
            writer.gap(5f)
        }

        writer.section("DETAILED EXPENSES")
        if (ordered.isEmpty()) writer.line("No expenses recorded for this group.")
        ordered.forEachIndexed { index, expense ->
            writer.line("${index + 1}. ${expense.title}", 12f, true)
            writer.pair("Date", date.format(Date(expense.expenseDate)))
            writer.pair("Category", expense.category.ifBlank { "Uncategorised" })
            writer.pair("Amount", money(expense.amount))
            writer.pair("Split type", expense.splitMode)
            if (expense.note.isNotBlank()) writer.line("Note: ${expense.note}")
            val expenseSplits = splits[expense.id].orEmpty()
            writer.line("Member allocation:", 10f, true)
            if (expenseSplits.isEmpty()) {
                writer.line("  No split details recorded")
            } else {
                expenseSplits.forEach { split ->
                    val units = if (expense.splitMode.equals("Share", true))
                        " (${split.shareUnits} shares)" else ""
                    writer.pair("  ${split.memberNameSnapshot}$units", money(split.amount))
                }
            }
            writer.gap(8f)
        }

        writer.section("SETTLEMENT HISTORY")
        if (settlements.isEmpty()) {
            writer.line("No settlements recorded.")
        } else {
            settlements.sortedByDescending { it.receivedAt }.forEachIndexed { index, settlement ->
                writer.line("${index + 1}. ${settlement.memberNameSnapshot}", 11f, true)
                writer.pair("Date", dateTime.format(Date(settlement.receivedAt)))
                writer.pair("Amount", money(settlement.amount))
                writer.gap(4f)
            }
        }
        writer.line("End of report", 9f)
        writer.finish()
        val directory = File(context.cacheDir, "group_reports").apply { mkdirs() }
        val safe = group.name.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_")
            .ifBlank { "group" }
        val output = File(directory, "${safe}_expense_report.pdf")
        output.outputStream().use { pdf.writeTo(it) }
        return output
    } finally {
        pdf.close()
    }
}

private class GroupExpensePdfWriter(private val pdf: PdfDocument) {
    private var page: PdfDocument.Page? = null
    private var y = 0f
    private var pageNumber = 0
    private val left = 38f
    private val right = 557f

    private fun startPage() {
        page?.let { pdf.finishPage(it) }
        pageNumber++
        page = pdf.startPage(
            PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()
        )
        y = 48f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(210, 210, 210)
            strokeWidth = 0.7f
        }
        page!!.canvas.drawLine(left, 28f, right, 28f, paint)
        val footer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 8f
        }
        page!!.canvas.drawText("Ledger • Group Expense Report • Page $pageNumber", left, 824f, footer)
    }

    private fun ensureSpace(height: Float) {
        if (page == null) startPage()
        if (y + height > 790f) startPage()
    }

    fun heading(text: String, size: Float) {
        ensureSpace(size + 18f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(28, 48, 78)
            textSize = size
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        page!!.canvas.drawText(text.take(75), left, y, paint)
        y += size + 12f
    }

    fun section(text: String) {
        ensureSpace(34f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(35, 73, 112)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        page!!.canvas.drawText(text, left, y, paint)
        y += 10f
        val rule = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.LTGRAY
            strokeWidth = 0.8f
        }
        page!!.canvas.drawLine(left, y, right, y, rule)
        y += 16f
    }

    fun line(text: String, size: Float = 10f, bold: Boolean = false) {
        val safe = text.replace(Regex("[\\r\\n\\t]+"), " ")
        val maxChars = if (size >= 14f) 60 else 88
        val pieces = safe.chunked(maxChars).ifEmpty { listOf("") }
        pieces.forEach { part ->
            ensureSpace(size + 9f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.rgb(38, 43, 50)
                textSize = size
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            page!!.canvas.drawText(part, left, y, paint)
            y += size + 7f
        }
    }

    fun pair(label: String, value: String) {
        ensureSpace(19f)
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.DKGRAY
            textSize = 9.5f
        }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        page!!.canvas.drawText(label.take(64), left + 4f, y, labelPaint)
        val valueText = value.take(28)
        val width = valuePaint.measureText(valueText)
        page!!.canvas.drawText(valueText, right - width, y, valuePaint)
        y += 16f
    }

    fun gap(height: Float) {
        ensureSpace(height)
        y += height
    }

    fun finish() {
        page?.let { pdf.finishPage(it) }
        page = null
    }
}

private fun downloadGroupReportPdf(context: Context, source: File, groupName: String): String {
    val safe = groupName.trim().replace(Regex("[^A-Za-z0-9._-]+"), "_").ifBlank { "group" }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "${safe}_expense_report_$stamp.pdf"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Ledger")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create download file")
        try {
            resolver.openOutputStream(uri)?.use { out ->
                source.inputStream().use { input -> input.copyTo(out) }
            } ?: error("Unable to open download stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Saved to Downloads/Ledger/$fileName"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    val directory = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "Ledger"
    ).apply { mkdirs() }
    val output = File(directory, fileName)
    source.inputStream().use { input ->
        output.outputStream().use { out -> input.copyTo(out) }
    }
    return "Saved to ${output.absolutePath}"
}
