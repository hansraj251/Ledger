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
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.calculateTransactionInterest
import com.ledger.app.data.transactionInterestDays
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createAllPartiesReportPdf(
    context: Context,
    parties: List<PartyEntity>,
    entriesByParty: Map<Long, List<LedgerEntryEntity>>,
    profileName: String = ""
): File {
    val pdf = PdfDocument()

    val dateFormat = SimpleDateFormat(
        "dd MMM yyyy",
        Locale.getDefault()
    )

    val dateTimeFormat = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.getDefault()
    )

    val writer = ReportPdfWriter(
        pdf,
        dateFormat,
        dateTimeFormat
    )

    val gaveLabel =
        "${profileName.trim().ifEmpty { "You" }} Gave"

    val allEntries = parties.flatMap { party ->
        entriesByParty[party.id].orEmpty()
    }

    val totalGave = allEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { it.amount }

    val totalGot = allEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { it.amount }

    val totalGaveInterest = allEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val totalGotInterest = allEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val totalBalance =
        totalGave + totalGaveInterest -
            (totalGot + totalGotInterest)

    fun money(value: Double): String {
        return "₹${String.format(Locale.getDefault(), "%,.0f", value)}"
    }

    fun moneySigned(value: Double): String {
        val sign = if (value >= 0.0) "+" else "-"
        return "$sign₹${String.format(Locale.getDefault(), "%,.0f", kotlin.math.abs(value))}"
    }

    writer.startPage()

    writer.title("All Parties Report")
    writer.subtitle(
        profileName.trim().ifEmpty { "Ledger" }
    )

    writer.text(
        "Generated ${dateTimeFormat.format(Date())}",
        10f,
        false
    )

    writer.space(8f)

    writer.section("Overall Summary")

    writer.summaryRow(
        "Total $gaveLabel",
        money(totalGave)
    )

    if (totalGaveInterest > 0.0) {
        writer.summaryRow(
            "Interest on $gaveLabel",
            money(totalGaveInterest)
        )
    }

    writer.summaryRow(
        "Total Party Gave",
        money(totalGot)
    )

    if (totalGotInterest > 0.0) {
        writer.summaryRow(
            "Interest on Party Gave",
            money(totalGotInterest)
        )
    }

    writer.summaryRow(
        "Current Balance",
        moneySigned(totalBalance),
        bold = true
    )

    writer.summaryRow(
        "Total Parties",
        parties.size.toString()
    )

    writer.summaryRow(
        "Total Transactions",
        allEntries.size.toString()
    )

    writer.space(12f)

    writer.section("Party-wise Details")

    parties.forEachIndexed { index, party ->

        val orderedEntries = entriesByParty[party.id]
            .orEmpty()
            .sortedWith(
                compareByDescending<LedgerEntryEntity> {
                    if (it.transactionDate > 0L) {
                        it.transactionDate
                    } else {
                        it.createdAt
                    }
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

        val balance =
            gave + gaveInterest -
                (got + gotInterest)

        writer.space(8f)

        writer.subtitle(
            "${index + 1}. ${party.name}"
        )

        if (party.mobile.isNotBlank()) {
            writer.text(
                party.mobile,
                9.5f,
                false
            )
        }

        writer.section("Account Summary")

        writer.summaryRow(
            "Total $gaveLabel",
            money(gave)
        )

        if (gaveInterest > 0.0) {
            writer.summaryRow(
                "Interest on $gaveLabel",
                money(gaveInterest)
            )
        }

        writer.summaryRow(
            "Total ${party.name.trim()} Gave",
            money(got)
        )

        if (gotInterest > 0.0) {
            writer.summaryRow(
                "Interest on ${party.name.trim()} Gave",
                money(gotInterest)
            )
        }

        writer.summaryRow(
            "Current Balance",
            moneySigned(balance),
            bold = true
        )

        writer.summaryRow(
            "Transactions",
            orderedEntries.size.toString()
        )

        if (orderedEntries.isNotEmpty()) {
            writer.space(8f)

            writer.section(
                "Transactions (${orderedEntries.size})"
            )

            orderedEntries.forEachIndexed { transactionIndex, entry ->

                val dateMillis =
                    if (entry.transactionDate > 0L) {
                        entry.transactionDate
                    } else {
                        entry.createdAt
                    }

                val type =
                    if (entry.type == EntryType.CREDIT) {
                        gaveLabel.uppercase()
                    } else {
                        "${party.name.trim()} Gave".uppercase()
                    }

                val interest =
                    calculateTransactionInterest(
                        amount = entry.amount,
                        annualRate = entry.interestRate,
                        transactionDate = entry.transactionDate,
                        createdAt = entry.createdAt
                    )

                val days =
                    transactionInterestDays(
                        transactionDate = entry.transactionDate,
                        createdAt = entry.createdAt
                    )

                writer.transactionHeader(
                    number = transactionIndex + 1,
                    date = dateFormat.format(Date(dateMillis)),
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
                }

                writer.transactionRow(
                    "Transaction Total",
                    money(entry.amount + interest)
                )

                if (entry.note.isNotBlank()) {
                    writer.transactionRow(
                        "Note",
                        entry.note
                    )
                }

                writer.separator()
                writer.space(8f)
            }
        }

        if (index < parties.lastIndex) {
            writer.space(10f)
        }
    }

    writer.finish()

    val reportsDir = File(
        context.cacheDir,
        "reports"
    ).apply {
        mkdirs()
    }

    val output = File(
        reportsDir,
        "all_parties_ledger_report.pdf"
    )

    output.outputStream().use { outputStream ->
        pdf.writeTo(outputStream)
    }

    pdf.close()

    return output
}

internal fun downloadAllPartiesReportPdf(
    context: Context,
    source: File
): String {

    val fileName =
        "all_parties_ledger_report_${SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())}.pdf"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        val values = ContentValues().apply {
            put(
                MediaStore.Downloads.DISPLAY_NAME,
                fileName
            )

            put(
                MediaStore.Downloads.MIME_TYPE,
                "application/pdf"
            )

            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/Ledger"
            )

            put(
                MediaStore.Downloads.IS_PENDING,
                1
            )
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

            values.put(
                MediaStore.Downloads.IS_PENDING,
                0
            )

            resolver.update(
                uri,
                values,
                null,
                null
            )

            return "Saved to Downloads/Ledger/$fileName"

        } catch (error: Exception) {

            resolver.delete(
                uri,
                null,
                null
            )

            throw error
        }

    } else {

        val downloads = Environment
            .getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )

        val ledgerDir = File(
            downloads,
            "Ledger"
        ).apply {
            mkdirs()
        }

        val target = File(
            ledgerDir,
            fileName
        )

        source.copyTo(
            target,
            overwrite = true
        )

        return "Saved to Downloads/Ledger/$fileName"
    }
}
