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
import com.ledger.app.data.PartyEntity
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.calculateTransactionInterest
import com.ledger.app.data.calculateInterestPosition
import com.ledger.app.data.calculateInterestAccountPosition
import com.ledger.app.data.transactionInterestDays
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun createAllPartiesReportPdf(
    context: Context,
    parties: List<PartyEntity>,
    entriesByParty: Map<Long, List<LedgerEntryEntity>>,
    interestRecordsByParty: Map<Long, List<InterestAccountEntryEntity>>,
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

    val today = System.currentTimeMillis()
    val allEntries = parties.flatMap { party ->
        entriesByParty[party.id].orEmpty()
    }
    val allInterestRecords = parties.flatMap { party ->
        interestRecordsByParty[party.id].orEmpty()
    }

    val totalGave = allEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { it.amount }
    val totalGot = allEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { it.amount }

    val accruedByParty = parties.associate { party ->
        party.id to calculateInterestPosition(
            entries = entriesByParty[party.id].orEmpty(),
            asOf = today
        )
    }
    val remainingByParty = parties.associate { party ->
        party.id to calculateInterestAccountPosition(
            transactions = entriesByParty[party.id].orEmpty(),
            records = interestRecordsByParty[party.id].orEmpty(),
            asOf = today
        )
    }

    val totalAccruedReceivable =
        accruedByParty.values.sumOf { it.receivable }
    val totalAccruedPayable =
        accruedByParty.values.sumOf { it.payable }
    val totalRecordedGot = allInterestRecords
        .filter { it.type == "GOT" }
        .sumOf { it.amount }
    val totalRecordedGave = allInterestRecords
        .filter { it.type == "GAVE" }
        .sumOf { it.amount }
    val totalRemainingReceivable =
        remainingByParty.values.sumOf { it.receivable }
    val totalRemainingPayable =
        remainingByParty.values.sumOf { it.payable }

    val principalBalance = allEntries.sumOf { entry ->
        if (entry.type == EntryType.CREDIT) entry.amount else -entry.amount
    }
    val totalBalance = principalBalance +
        totalRemainingReceivable - totalRemainingPayable
    val hasInterest = totalAccruedReceivable > 0.0 ||
        totalAccruedPayable > 0.0 ||
        totalRecordedGot > 0.0 ||
        totalRecordedGave > 0.0

    fun money(value: Double): String = CurrencySettings.format(value)

    fun moneySigned(value: Double): String {
        val sign = if (value >= 0.0) "+" else "-"
        return sign + CurrencySettings.format(kotlin.math.abs(value))
    }

    writer.startPage()


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

    val overallRows = mutableListOf<List<String>>()
    overallRows.add(listOf("Total You Gave", money(totalGave)))
    overallRows.add(listOf("Total You Received", money(totalGot)))

    if (totalAccruedReceivable > 0.0) {
        overallRows.add(listOf("Total Interest Receivable", money(totalAccruedReceivable)))
    }
    if (totalRecordedGot > 0.0) {
        overallRows.add(listOf("Interest Received", money(totalRecordedGot)))
    }
    if (totalRemainingReceivable > 0.0) {
        overallRows.add(listOf("Remaining Interest Receivable", money(totalRemainingReceivable)))
    }
    if (totalAccruedPayable > 0.0) {
        overallRows.add(listOf("Total Interest Payable", money(totalAccruedPayable)))
    }
    if (totalRecordedGave > 0.0) {
        overallRows.add(listOf("Interest Paid", money(totalRecordedGave)))
    }
    if (totalRemainingPayable > 0.0) {
        overallRows.add(listOf("Remaining Interest Payable", money(totalRemainingPayable)))
    }

    val overallBalanceLabel = when {
        totalBalance > 0.0 ->
            "Current Balance${if (hasInterest) " (With Int.)" else ""} - Parties Will Pay"
        totalBalance < 0.0 ->
            "Current Balance${if (hasInterest) " (With Int.)" else ""} - ${profileName.trim().ifEmpty { "You" }} Will Pay"
        else ->
            if (hasInterest) "Current Balance (With Int.)" else "Current Balance"
    }

    overallRows.add(listOf("Total Parties", parties.size.toString()))
    overallRows.add(listOf("Total Transactions", allEntries.size.toString()))
    overallRows.add(listOf(overallBalanceLabel, money(kotlin.math.abs(totalBalance))))

    writer.table(
        headers = listOf("Overall Summary", "Amount"),
        rows = overallRows,
        notes = List(overallRows.size) { null },
        widths = listOf(360f, 163f)
    )


    parties.forEachIndexed { index, party ->
        writer.finishCurrentPage()
        writer.startPage()

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

        val records = interestRecordsByParty[party.id].orEmpty()
        val accruedInterest = calculateInterestPosition(
            entries = orderedEntries,
            asOf = today
        )
        val remainingInterest = calculateInterestAccountPosition(
            transactions = orderedEntries,
            records = records,
            asOf = today
        )
        val recordedInterestGot = records
            .filter { it.type == "GOT" }
            .sumOf { it.amount }
        val recordedInterestGave = records
            .filter { it.type == "GAVE" }
            .sumOf { it.amount }
        val principalBalance = orderedEntries.sumOf { entry ->
            if (entry.type == EntryType.CREDIT) entry.amount else -entry.amount
        }
        val balance = principalBalance +
            remainingInterest.receivable - remainingInterest.payable
        val partyHasInterest = accruedInterest.receivable > 0.0 ||
            accruedInterest.payable > 0.0 ||
            recordedInterestGot > 0.0 ||
            recordedInterestGave > 0.0

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
        val partyGaveLabel = "${party.name.trim()} Gave"

        val partySummaryRows = mutableListOf<List<String>>()
        partySummaryRows.add(
            listOf(
                "Total You Gave",
                money(orderedEntries.filter { it.type == EntryType.CREDIT }.sumOf { it.amount })
            )
        )
        partySummaryRows.add(
            listOf(
                "Total You Received",
                money(orderedEntries.filter { it.type == EntryType.DEBIT }.sumOf { it.amount })
            )
        )

        if (accruedInterest.receivable > 0.0) {
            partySummaryRows.add(
                listOf("Total Interest of ${party.name.trim()}", money(accruedInterest.receivable))
            )
        }
        if (recordedInterestGot > 0.0) {
            partySummaryRows.add(listOf("Interest $partyGaveLabel", money(recordedInterestGot)))
        }
        if (remainingInterest.receivable > 0.0) {
            partySummaryRows.add(
                listOf("Remaining Interest of ${party.name.trim()}", money(remainingInterest.receivable))
            )
        }
        if (accruedInterest.payable > 0.0) {
            partySummaryRows.add(
                listOf(
                    "Total Interest of ${profileName.trim().ifEmpty { "You" }}",
                    money(accruedInterest.payable)
                )
            )
        }
        if (recordedInterestGave > 0.0) {
            partySummaryRows.add(listOf("Interest $gaveLabel", money(recordedInterestGave)))
        }
        if (remainingInterest.payable > 0.0) {
            partySummaryRows.add(
                listOf(
                    "Remaining Interest of ${profileName.trim().ifEmpty { "You" }}",
                    money(remainingInterest.payable)
                )
            )
        }

        val balanceLabel = when {
            balance > 0.0 ->
                "Current Balance${if (partyHasInterest) " (With Int.)" else ""} - ${party.name.trim()} Will Pay"
            balance < 0.0 ->
                "Current Balance${if (partyHasInterest) " (With Int.)" else ""} - ${profileName.trim().ifEmpty { "You" }} Will Pay"
            else ->
                if (partyHasInterest) "Current Balance (With Int.)" else "Current Balance"
        }

        partySummaryRows.add(listOf(balanceLabel, money(kotlin.math.abs(balance))))
        partySummaryRows.add(listOf("Transactions", orderedEntries.size.toString()))

        writer.table(
            headers = listOf("Account Summary", "Amount"),
            rows = partySummaryRows,
            notes = List(partySummaryRows.size) { null },
            widths = listOf(360f, 163f)
        )


        if (orderedEntries.isNotEmpty()) {
            writer.space(8f)
            writer.section("Transactions (${orderedEntries.size})")

            val transactionRows = mutableListOf<List<String>>()
            val transactionNotes = mutableListOf<String?>()

            orderedEntries.forEach { entry ->
                val dateMillis = if (entry.transactionDate > 0L) {
                    entry.transactionDate
                } else {
                    entry.createdAt
                }

                val type = if (entry.type == EntryType.CREDIT) {
                    gaveLabel.uppercase()
                } else {
                    "${party.name.trim()} Gave".uppercase()
                }

                val interest = calculateTransactionInterest(
                    amount = entry.amount,
                    annualRate = entry.interestRate,
                    transactionDate = entry.transactionDate,
                    createdAt = entry.createdAt
                )

                val days = transactionInterestDays(
                    transactionDate = entry.transactionDate,
                    createdAt = entry.createdAt
                )

                val interestDetails = if (entry.interestRate > 0.0) {
                    "${money(interest)}\n${formatRate(entry.interestRate)}% p.a.\n$days days"
                } else {
                    "${money(interest)}\n0% rate"
                }

                transactionRows.add(
                    listOf(
                        dateFormat.format(Date(dateMillis)),
                        type,
                        money(entry.amount),
                        interestDetails
                    )
                )
                transactionNotes.add(
                    entry.note.takeIf { it.isNotBlank() }?.let { "Note: $it" }
                )
            }

            writer.table(
                headers = listOf("Date", "Type", "Principal", "Interest"),
                rows = transactionRows,
                notes = transactionNotes
            )
        }


        val orderedRecords = records.sortedWith(
            compareByDescending<InterestAccountEntryEntity> {
                if (it.transactionDate > 0L) it.transactionDate else it.createdAt
            }.thenByDescending { it.id }
        )

        if (orderedRecords.isNotEmpty()) {
            writer.space(8f)
            writer.section("Interest Account Records (${orderedRecords.size})")

            val interestRows = mutableListOf<List<String>>()
            val interestNotes = mutableListOf<String?>()

            orderedRecords.forEach { record ->
                val recordDate = if (record.transactionDate > 0L) {
                    record.transactionDate
                } else {
                    record.createdAt
                }

                val recordType = if (record.type == "GAVE") {
                    "INTEREST $gaveLabel".uppercase()
                } else {
                    "INTEREST $partyGaveLabel".uppercase()
                }

                interestRows.add(
                    listOf(
                        dateFormat.format(Date(recordDate)),
                        recordType,
                        "—",
                        money(record.amount)
                    )
                )
                interestNotes.add(
                    record.note.takeIf { it.isNotBlank() }?.let { "Note: $it" }
                )
            }

            writer.table(
                headers = listOf("Date", "Type", "Principal", "Interest"),
                rows = interestRows,
                notes = interestNotes
            )
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
