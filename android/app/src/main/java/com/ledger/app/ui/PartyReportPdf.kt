package com.ledger.app.ui

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

    val netBalance = orderedEntries.sumOf { entry ->
        val interest = calculateTransactionInterest(
            amount = entry.amount,
            annualRate = entry.interestRate,
            transactionDate = entry.transactionDate,
            createdAt = entry.createdAt,
            
        )

        val total = entry.amount + interest

        if (entry.type == EntryType.CREDIT) total else -total
    }

    writer.startPage()


    writer.subtitle(party.name)

    if (party.mobile.isNotBlank()) {
        writer.text("Mobile: ${party.mobile}", 10f, false)
    }

    writer.text("Generated: ${dateTimeFormat.format(Date(today))}", 9f, false)

    writer.space(14f)

    writer.section("Account Summary")

    val gaveLabel = "${profileName.trim().ifEmpty { "You" }} Gave"
    val gotLabel = "${party.name.trim()} Gave"

    writer.summaryRow("Total $gaveLabel", money(gave))
    writer.summaryRow("Interest on $gaveLabel", money(gaveInterest))
    writer.summaryRow("Total $gotLabel", money(got))
    writer.summaryRow("Interest on $gotLabel", money(gotInterest))
    writer.summaryRow("Current Balance", moneySigned(netBalance), bold = true)

    writer.space(14f)

    writer.section(
        "Transactions (${orderedEntries.size})"
    )

    if (orderedEntries.isEmpty()) {
        writer.text("No transactions recorded.", 10f, false)
    } else {
        orderedEntries.forEachIndexed { index, entry ->
            val transactionDate =
                if (entry.transactionDate > 0L) {
                    entry.transactionDate
                } else {
                    entry.createdAt
                }

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

            val total = entry.amount + interest
            val type = if (entry.type == EntryType.CREDIT) {
                gaveLabel.uppercase()
            } else {
                gotLabel.uppercase()
            }

            writer.transactionHeader(
                number = index + 1,
                date = dateFormat.format(Date(transactionDate)),
                type = type
            )

            writer.transactionRow(
                "Principal",
                money(entry.amount)
            )

            if (entry.interestRate > 0.0) {
                writer.transactionRow(
                    "Interest",
                    "${formatRate(entry.interestRate)}% p.a. • $days days • ${money(interest)}"
                )
            } else {
                writer.transactionRow(
                    "Interest",
                    "0%"
                )
            }

            writer.transactionRow(
                "Transaction Total",
                money(total)
            )

            if (entry.note.isNotBlank()) {
                writer.note("Note: ${entry.note}")
            }

            writer.separator()
            writer.space(8f)
        }
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

private fun money(value: Double): String {
    return "₹${kotlin.math.round(value).toLong()}"
}

private fun moneySigned(value: Double): String {
    val rounded = kotlin.math.round(value).toLong()
    return when {
        rounded > 0 -> "+₹$rounded"
        rounded < 0 -> "-₹${abs(rounded)}"
        else -> "₹0"
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

    fun finish() {
        footer()
        page?.let {
            pdf.finishPage(it)
        }
        page = null
        canvas = null
    }
}
