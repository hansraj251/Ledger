package com.ledger.app.ui

import com.ledger.app.data.CurrencySettings

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import com.ledger.app.data.calculateInterestPosition
import com.ledger.app.data.calculateInterestAccountPosition
import com.ledger.app.data.PartyEntity
import com.ledger.app.data.calculateTransactionInterest
import com.ledger.app.data.transactionInterestDays
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

private const val PAGE_WIDTH = 595
private const val PAGE_HEIGHT = 842
private const val LEFT = 36f
private const val RIGHT = 559f
private const val TOP = 42f
private const val BOTTOM = 800f

internal fun createPartyReportPdf(
    context: Context,
    party: PartyEntity,
    entries: List<LedgerEntryEntity>,
    interestRecords: List<InterestAccountEntryEntity> = emptyList(),
    profileName: String = ""
): File {
    val pdf = PdfDocument()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val today = System.currentTimeMillis()

    val writer = ReportPdfWriter(pdf, dateFormat, dateTimeFormat)

    val orderedEntries = entries.sortedWith(
        compareByDescending<LedgerEntryEntity> {
            if (it.transactionDate > 0L) it.transactionDate else it.createdAt
        }.thenByDescending { it.id }
    )

    val gave = orderedEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { it.amount }

    val got = orderedEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { it.amount }

    val gaveInterest = orderedEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val gotInterest = orderedEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val principalBalance = orderedEntries.sumOf { entry ->
        if (entry.type == EntryType.CREDIT) entry.amount else -entry.amount
    }

    val accruedInterest = calculateInterestPosition(
        entries = orderedEntries,
        asOf = today
    )

    val remainingInterest = calculateInterestAccountPosition(
        transactions = orderedEntries,
        records = interestRecords,
        asOf = today
    )

    val recordedInterestGot = interestRecords
        .filter { it.type == "GOT" }
        .sumOf { it.amount }

    val recordedInterestGave = interestRecords
        .filter { it.type == "GAVE" }
        .sumOf { it.amount }

    val netBalance = principalBalance +
        remainingInterest.receivable -
        remainingInterest.payable

    writer.startPage()


    writer.subtitle(party.name)

    // Simple report metadata without a table.
    if (party.mobile.isNotBlank()) {
        writer.text("Mobile: ${party.mobile}", 10f, false)
    }
    writer.text(
        "Generated: ${dateTimeFormat.format(Date(today))}",
        9f,
        false
    )

    writer.space(14f)
    writer.section("Account Summary")

    val gaveLabel = "${profileName.trim().ifEmpty { "You" }} Gave"
    val gotLabel = "${party.name.trim()} Gave"

    val summaryRows = mutableListOf<List<String>>()
    summaryRows.add(listOf("Total $gaveLabel", money(gave)))
    summaryRows.add(listOf("Total $gotLabel", money(got)))

    val hasInterest = accruedInterest.receivable > 0.0 ||
        accruedInterest.payable > 0.0 ||
        recordedInterestGot > 0.0 ||
        recordedInterestGave > 0.0

    if (accruedInterest.receivable > 0.0) {
        summaryRows.add(
            listOf("Total Interest of ${party.name.trim()}", money(accruedInterest.receivable))
        )
    }
    if (recordedInterestGot > 0.0) {
        summaryRows.add(
            listOf("Interest $gotLabel", money(recordedInterestGot))
        )
    }
    if (remainingInterest.receivable > 0.0) {
        summaryRows.add(
            listOf("Remaining Interest of ${party.name.trim()}", money(remainingInterest.receivable))
        )
    }
    if (accruedInterest.payable > 0.0) {
        summaryRows.add(
            listOf(
                "Total Interest of ${profileName.trim().ifEmpty { "You" }}",
                money(accruedInterest.payable)
            )
        )
    }
    if (recordedInterestGave > 0.0) {
        summaryRows.add(
            listOf("Interest $gaveLabel", money(recordedInterestGave))
        )
    }
    if (remainingInterest.payable > 0.0) {
        summaryRows.add(
            listOf(
                "Remaining Interest of ${profileName.trim().ifEmpty { "You" }}",
                money(remainingInterest.payable)
            )
        )
    }

    val balanceLabel = when {
        netBalance > 0.0 ->
            "Current Balance${if (hasInterest) " (With Int.)" else ""} - ${party.name.trim()} Will Pay"
        netBalance < 0.0 ->
            "Current Balance${if (hasInterest) " (With Int.)" else ""} - ${profileName.trim().ifEmpty { "You" }} Will Pay"
        else ->
            if (hasInterest) "Current Balance (With Int.)" else "Current Balance"
    }

    summaryRows.add(
        listOf(balanceLabel, money(kotlin.math.abs(netBalance)))
    )

    val visibleSummaryRows = summaryRows.filter { row ->
        val amount = row.getOrNull(1)
            ?.replace(",", "")
            ?.replace("₹", "")
            ?.trim()
        amount != null && amount.toDoubleOrNull() != 0.0
    }

    writer.table(
        headers = listOf("Account Summary", "Amount"),
        rows = visibleSummaryRows,
        notes = List(visibleSummaryRows.size) { null },
        widths = listOf(360f, 163f)
    )

    writer.space(14f)
    writer.section("Transactions (${orderedEntries.size})")

    if (orderedEntries.isEmpty()) {
        writer.text("No transactions recorded.", 10f, false)
    } else {
        val transactionRows = mutableListOf<List<String>>()
        val transactionNotes = mutableListOf<String?>()
        var hasTransactionInterest = false

        orderedEntries.forEach { entry ->
            val transactionDate =
                if (entry.transactionDate > 0L) entry.transactionDate
                else entry.createdAt

            val days = transactionInterestDays(
                transactionDate = entry.transactionDate,
                createdAt = entry.createdAt
            )
            val interest = calculateTransactionInterest(
                amount = entry.amount,
                annualRate = entry.interestRate,
                transactionDate = entry.transactionDate,
                createdAt = entry.createdAt
            )
            if (interest > 0.0) hasTransactionInterest = true

            val type = if (entry.type == EntryType.CREDIT) {
                gaveLabel.uppercase()
            } else {
                gotLabel.uppercase()
            }

            val interestDetails = if (entry.interestRate > 0.0) {
                "${money(interest)}\n${formatRate(entry.interestRate)}% p.a.\n$days days"
            } else {
                "${money(interest)}\n0% rate"
            }

            transactionRows.add(
                listOf(
                    dateFormat.format(Date(transactionDate)),
                    if (entry.note.isNotBlank()) "$type\n${entry.note}" else type,
                    money(entry.amount),
                    interestDetails
                )
            )
            transactionNotes.add(null)
        }

        val transactionHeaders = if (hasTransactionInterest) {
            listOf("Date", "Type / Note", "Amount", "Interest")
        } else {
            transactionRows.replaceAll { row -> row.take(3) }
            listOf("Date", "Type / Note", "Amount")
        }

        writer.table(
            headers = transactionHeaders,
            rows = transactionRows,
            notes = transactionNotes
        )
    }

    if (interestRecords.isNotEmpty()) {
        writer.space(14f)
        writer.section("Interest Account Records (${interestRecords.size})")

        val orderedInterestRecords = interestRecords.sortedWith(
            compareByDescending<InterestAccountEntryEntity> {
                if (it.transactionDate > 0L) it.transactionDate else it.createdAt
            }.thenByDescending { it.id }
        )

        val interestRows = mutableListOf<List<String>>()
        val interestNotes = mutableListOf<String?>()

        orderedInterestRecords.forEach { record ->
            val recordDate = if (record.transactionDate > 0L) {
                record.transactionDate
            } else {
                record.createdAt
            }
            val recordType = if (record.type == "GAVE") {
                "INTEREST $gaveLabel".uppercase()
            } else {
                "INTEREST $gotLabel".uppercase()
            }

            interestRows.add(
                listOf(
                    dateFormat.format(Date(recordDate)),
                    if (record.note.isNotBlank()) "$recordType\nNote: ${record.note}" else recordType,
                    money(record.amount)
                )
            )
            interestNotes.add(null)
        }

        writer.table(
            headers = listOf("Date", "Type / Note", "Amount"),
            rows = interestRows,
            notes = interestNotes
        )
    }

    writer.finish()

    val reportsDir = File(
        context.cacheDir,
        "reports"
    ).apply {
        mkdirs()
    }

    val safeName = party.name
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .ifBlank { "party" }

    val output = File(
        reportsDir,
        "${safeName}_ledger_report.pdf"
    )

    output.outputStream().use { pdf.writeTo(it) }
    pdf.close()

    return output
}

internal fun downloadPartyReportPdf(
    context: Context,
    source: File,
    partyName: String
): String {
    val safeName = partyName
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .ifBlank { "party" }

    val fileName =
        "${safeName}_ledger_report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/Ledger"
            )
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            values
        ) ?: error("Unable to create download file")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            } ?: error("Unable to open download stream")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            return "Saved to Downloads/Ledger/$fileName"
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    val legacyDir = File(
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
        "Ledger"
    ).apply {
        mkdirs()
    }

    val output = File(legacyDir, fileName)
    source.inputStream().use { input ->
        output.outputStream().use { out ->
            input.copyTo(out)
        }
    }

    return "Saved to ${output.absolutePath}"
}

private fun money(value: Double): String = CurrencySettings.format(value)

private fun moneySigned(value: Double): String {
    val rounded = kotlin.math.round(value).toLong()
    return when {
        rounded > 0 -> "+" + CurrencySettings.format(kotlin.math.abs(value))
        rounded < 0 -> "-" + CurrencySettings.format(kotlin.math.abs(value))
        else -> CurrencySettings.format(0.0)
    }
}

internal fun formatRate(rate: Double): String {
    return if (rate == rate.toLong().toDouble()) {
        rate.toLong().toString()
    } else {
        String.format(Locale.getDefault(), "%.2f", rate)
            .trimEnd('0')
            .trimEnd('.')
    }
}

internal class ReportPdfWriter(
    private val pdf: PdfDocument,
    private val dateFormat: SimpleDateFormat,
    private val dateTimeFormat: SimpleDateFormat
) {
    private var pageNumber = 0
    private var page: PdfDocument.Page? = null
    private var canvas: Canvas? = null
    private var y = TOP

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(35, 35, 35)
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(20, 20, 20)
        typeface = Typeface.create("sans-serif", Typeface.BOLD)
    }

    private val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(105, 105, 105)
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(220, 220, 220)
        strokeWidth = 1f
    }

    fun startPage() {
        pageNumber++
        page = pdf.startPage(
            PdfDocument.PageInfo.Builder(
                PAGE_WIDTH,
                PAGE_HEIGHT,
                pageNumber
            ).create()
        )
        canvas = page!!.canvas
        canvas!!.drawColor(android.graphics.Color.WHITE)
        y = TOP
    }

    private fun newPage() {
        footer()
        page?.let { pdf.finishPage(it) }
        startPage()
    }

    private fun ensure(height: Float) {
        if (y + height > BOTTOM) {
            newPage()
        }
    }

    fun title(value: String) {
        ensure(34f)
        boldPaint.textSize = 22f
        canvas!!.drawText(value, LEFT, y, boldPaint)
        y += 30f
    }

    fun subtitle(value: String) {
        ensure(28f)
        boldPaint.textSize = 16f
        canvas!!.drawText(value, LEFT, y, boldPaint)
        y += 22f
    }

    fun text(
        value: String,
        size: Float,
        bold: Boolean
    ) {
        ensure(size + 10f)
        val paint = if (bold) boldPaint else normalPaint
        paint.textSize = size
        canvas!!.drawText(value, LEFT, y, paint)
        y += size + 7f
    }

    fun space(height: Float) {
        ensure(height)
        y += height
    }

    fun section(value: String) {
        ensure(32f)
        boldPaint.textSize = 13f
        canvas!!.drawText(value, LEFT, y, boldPaint)
        y += 7f
        line()
        y += 12f
    }

    fun summaryRow(
        label: String,
        value: String,
        bold: Boolean = false
    ) {
        ensure(24f)

        val paint = if (bold) boldPaint else normalPaint
        paint.textSize = 10.5f

        canvas!!.drawText(label, LEFT, y, paint)

        val width = paint.measureText(value)
        canvas!!.drawText(
            value,
            RIGHT - width,
            y,
            paint
        )

        y += 20f
    }

    fun transactionHeader(
        number: Int,
        date: String,
        type: String
    ) {
        ensure(42f)

        boldPaint.textSize = 11.5f
        canvas!!.drawText(
            "$number. $date",
            LEFT,
            y,
            boldPaint
        )

        val typeWidth = boldPaint.measureText(type)
        canvas!!.drawText(
            type,
            RIGHT - typeWidth,
            y,
            boldPaint
        )

        y += 20f
    }

    fun transactionRow(
        label: String,
        value: String
    ) {
        ensure(20f)

        normalPaint.textSize = 9.5f
        canvas!!.drawText(
            label,
            LEFT + 12f,
            y,
            normalPaint
        )

        val width = normalPaint.measureText(value)
        canvas!!.drawText(
            value,
            RIGHT - width,
            y,
            normalPaint
        )

        y += 17f
    }

    fun table(
        headers: List<String>,
        rows: List<List<String>>,
        notes: List<String?>,
        widths: List<Float> = listOf(68f, 155f, 130f, 170f)
    ) {
        // Widths are supplied by each table; defaults suit transaction tables.

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.6f
            color = android.graphics.Color.rgb(215, 220, 226)
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(90, 96, 105)
            textSize = 8.5f
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        }

        cellPaint.textSize = 8f

        fun wrapCell(value: String, maxWidth: Float): List<String> {
            val output = mutableListOf<String>()

            value.split("\n").forEach { paragraph ->
                if (paragraph.isEmpty()) {
                    output.add("")
                } else {
                    var current = ""

                    paragraph.split(Regex("\\s+")).forEach { word ->
                        if (cellPaint.measureText(word) > maxWidth) {
                            if (current.isNotEmpty()) {
                                output.add(current)
                                current = ""
                            }

                            var fragment = ""
                            word.forEach { character ->
                                val candidate = fragment + character
                                if (fragment.isNotEmpty() &&
                                    cellPaint.measureText(candidate) > maxWidth
                                ) {
                                    output.add(fragment)
                                    fragment = character.toString()
                                } else {
                                    fragment = candidate
                                }
                            }
                            current = fragment
                        } else {
                            val candidate =
                                if (current.isEmpty()) word else "$current $word"

                            if (cellPaint.measureText(candidate) <= maxWidth) {
                                current = candidate
                            } else {
                                if (current.isNotEmpty()) output.add(current)
                                current = word
                            }
                        }
                    }

                    if (current.isNotEmpty()) output.add(current)
                }
            }

            return output.ifEmpty { listOf("") }
        }

        fun drawRow(cells: List<String>, header: Boolean): Float {
            val wrappedCells = widths.indices.map { index ->
                wrapCell(cells.getOrElse(index) { "" }, widths[index] - 8f)
            }

            val maxLines = wrappedCells.maxOfOrNull { it.size } ?: 1
            val rowHeight = maxOf(31f, 14f + maxLines * 12f)

            if (!header && y + rowHeight > BOTTOM) {
                newPage()
                drawRow(headers, true)
            }

            val currentCanvas = canvas ?: return rowHeight
            var x = LEFT

            widths.forEachIndexed { index, width ->
                fillPaint.color = if (header) {
                    android.graphics.Color.rgb(35, 73, 112)
                } else {
                    android.graphics.Color.WHITE
                }

                cellPaint.color = if (header) {
                    android.graphics.Color.WHITE
                } else {
                    android.graphics.Color.rgb(38, 43, 50)
                }

                cellPaint.textSize = 8f
                cellPaint.typeface = Typeface.create(
                    "sans-serif",
                    if (header) Typeface.BOLD else Typeface.NORMAL
                )

                val rect = android.graphics.RectF(
                    x, y - 14f, x + width, y + rowHeight - 8f
                )
                currentCanvas.drawRect(rect, fillPaint)
                currentCanvas.drawRect(rect, borderPaint)

                wrappedCells[index].forEachIndexed { lineIndex, line ->
                    currentCanvas.drawText(
                        line,
                        x + 4f,
                        y + 1f + lineIndex * 12f,
                        cellPaint
                    )
                }

                x += width
            }

            y += rowHeight
            return rowHeight
        }

        fun drawNote(note: String) {
            val maxWidth = RIGHT - LEFT - 16f
            val lines = mutableListOf<String>()
            var current = ""

            note.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isBlank()) word else "$current $word"
                if (notePaint.measureText(candidate) <= maxWidth) {
                    current = candidate
                } else {
                    if (current.isNotBlank()) lines.add(current)
                    current = word
                }
            }
            if (current.isNotBlank()) lines.add(current)

            val totalHeight = lines.size * 12f + 8f
            if (y + totalHeight > BOTTOM) {
                newPage()
                drawRow(headers, true)
            }

            val currentCanvas = canvas ?: return
            val rect = android.graphics.RectF(
                LEFT, y - 8f, RIGHT, y + totalHeight - 3f
            )
            fillPaint.color = android.graphics.Color.rgb(247, 248, 250)
            currentCanvas.drawRect(rect, fillPaint)
            currentCanvas.drawRect(rect, borderPaint)

            lines.forEachIndexed { index, line ->
                currentCanvas.drawText(
                    line, LEFT + 8f, y + index * 12f, notePaint
                )
            }
            y += totalHeight
        }

        drawRow(headers, true)

        rows.forEachIndexed { index, row ->
            drawRow(row, false)
            notes.getOrNull(index)
                ?.takeIf { it.isNotBlank() }
                ?.let { drawNote(it) }
        }

        space(8f)
    }

    fun note(value: String) {
        val maxWidth = RIGHT - LEFT - 24f
        val words = value.split(" ")
        var lineText = ""

        normalPaint.textSize = 9.5f

        words.forEach { word ->
            val candidate =
                if (lineText.isBlank()) word
                else "$lineText $word"

            if (normalPaint.measureText(candidate) <= maxWidth) {
                lineText = candidate
            } else {
                ensure(18f)
                canvas!!.drawText(
                    lineText,
                    LEFT + 12f,
                    y,
                    mutedPaint.apply { textSize = 9.5f }
                )
                y += 15f
                lineText = word
            }
        }

        if (lineText.isNotBlank()) {
            ensure(18f)
            canvas!!.drawText(
                lineText,
                LEFT + 12f,
                y,
                mutedPaint.apply { textSize = 9.5f }
            )
            y += 17f
        }
    }

    fun separator() {
        ensure(14f)
        y += 3f
        line()
        y += 10f
    }

    private fun line() {
        canvas!!.drawLine(
            LEFT,
            y,
            RIGHT,
            y,
            linePaint
        )
    }

    private fun footer() {
        val paint = mutedPaint.apply {
            textSize = 8f
        }

        canvas!!.drawText(
            "Ledger • Page $pageNumber",
            LEFT,
            PAGE_HEIGHT - 20f,
            paint
        )
    }

    fun finishCurrentPage() {
        page?.let {
            footer()
            pdf.finishPage(it)
        }
        page = null
        canvas = null
    }

    fun finish() {
        finishCurrentPage()
    }
}
