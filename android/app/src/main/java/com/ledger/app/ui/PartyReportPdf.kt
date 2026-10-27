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
import com.ledger.app.data.EmiScheduleEntity
import com.ledger.app.data.EmiChargeSettlementEntity
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import com.ledger.app.data.calculateInterestPosition
import com.ledger.app.data.calculateInterestAccountPosition
import com.ledger.app.data.PartyEntity
import com.ledger.app.data.TransactionSettlementEntity
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

private fun calculateReportEmiTotalInterest(
    entry: LedgerEntryEntity,
    schedules: List<EmiScheduleEntity>,
    asOf: Long,
    settlements: List<EmiChargeSettlementEntity> = emptyList()
): Double {
    if (schedules.isEmpty()) return 0.0

    val sorted = schedules.sortedBy { it.installmentNumber }
    val periodsPerYear = when (sorted.firstOrNull()?.frequency) {
        "Weekly" -> 52.0
        "Quarterly" -> 4.0
        "Half-Yearly" -> 2.0
        else -> 12.0
    }
    val periodicRate = entry.interestRate.coerceAtLeast(0.0) / 100.0 / periodsPerYear
    var outstandingPrincipal = kotlin.math.abs(entry.amount)
    val installmentInterest = sorted.sumOf { emi ->
        val interest = kotlin.math.round(outstandingPrincipal * periodicRate * 100.0) / 100.0
        val principal = (emi.amount - interest).coerceAtLeast(0.0)
        outstandingPrincipal = (outstandingPrincipal - principal).coerceAtLeast(0.0)
        interest
    }

    val baseEmiDate = java.util.Calendar.getInstance().apply {
        timeInMillis = entry.transactionDate
        add(java.util.Calendar.MONTH, 1)
    }
    val actualStartDate = sorted.firstOrNull()?.dueDate ?: baseEmiDate.timeInMillis
    fun localDate(millis: Long): java.time.LocalDate {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        return java.time.LocalDate.of(
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }
    val delayDays = java.time.temporal.ChronoUnit.DAYS.between(
        localDate(baseEmiDate.timeInMillis),
        localDate(actualStartDate)
    ).coerceAtLeast(0L)
    val delayInterest = kotlin.math.round(
        kotlin.math.abs(entry.amount) * entry.interestRate.coerceAtLeast(0.0) *
            delayDays / 36500.0 * 100.0
    ) / 100.0

    val todayStart = java.util.Calendar.getInstance().apply {
        timeInMillis = asOf
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val overdueInterest = sorted.filter {
        it.paidAmount > 0.0 && it.paidAmount < it.amount
    }.sumOf { emi ->
        val dueStart = java.util.Calendar.getInstance().apply {
            timeInMillis = emi.dueDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val overdueDays = if (todayStart > dueStart) {
            (todayStart - dueStart) / 86_400_000L
        } else 0L
        val remaining = (emi.amount - emi.paidAmount).coerceAtLeast(0.0)
        remaining * entry.interestRate.coerceAtLeast(0.0) * overdueDays / (365.0 * 100.0)
    }

    val settledEmiOverdueInterest = settlements.sumOf { settlement ->
        val settlementStart = java.util.Calendar.getInstance().apply {
            timeInMillis = settlement.settlementDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val days = ((todayStart - settlementStart) / 86_400_000L).coerceAtLeast(0L)
        kotlin.math.round(
            settlement.emiAmount.coerceAtLeast(0.0) *
                entry.interestRate.coerceAtLeast(0.0) * days / 36500.0 * 100.0
        ) / 100.0
    }
    val adjustedOverdueInterest =
        (overdueInterest - settledEmiOverdueInterest).coerceAtLeast(0.0)

    return installmentInterest + delayInterest + adjustedOverdueInterest
}

internal fun createPartyReportPdf(
    context: Context,
    party: PartyEntity,
    entries: List<LedgerEntryEntity>,
    interestRecords: List<InterestAccountEntryEntity> = emptyList(),
    settlements: List<TransactionSettlementEntity> = emptyList(),
    profileName: String = "",
    emiSchedules: List<EmiScheduleEntity> = emptyList(),
    emiChargeSettlements: List<EmiChargeSettlementEntity> = emptyList()
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

    val emiTransactionIdsForSummary =
        emiSchedules.mapTo(mutableSetOf()) { it.transactionId }

    val gave = orderedEntries
        .filter { it.type == EntryType.CREDIT && it.id !in emiTransactionIdsForSummary }
        .sumOf { it.amount }

    val got = orderedEntries
        .filter { it.type == EntryType.DEBIT && it.id !in emiTransactionIdsForSummary }
        .sumOf { it.amount }

    val gaveInterest = orderedEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { entry ->
            if (entry.id in emiTransactionIdsForSummary) {
                calculateReportEmiTotalInterest(
                    entry = entry,
                    schedules = emiSchedules.filter { it.transactionId == entry.id },
                    asOf = today,
                    settlements = emiChargeSettlements.filter { it.transactionId == entry.id }
                )
            } else {
                calculateTransactionInterest(
                    amount = entry.amount,
                    annualRate = entry.interestRate,
                    transactionDate = entry.transactionDate,
                    createdAt = entry.createdAt,
                    asOf = today,
                    settlements = settlements.filter { it.transactionId == entry.id }
                )
            }
        }

    val gotInterest = orderedEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { entry ->
            if (entry.id in emiTransactionIdsForSummary) {
                calculateReportEmiTotalInterest(
                    entry = entry,
                    schedules = emiSchedules.filter { it.transactionId == entry.id },
                    asOf = today,
                    settlements = emiChargeSettlements.filter { it.transactionId == entry.id }
                )
            } else {
                calculateTransactionInterest(
                    amount = entry.amount,
                    annualRate = entry.interestRate,
                    transactionDate = entry.transactionDate,
                    createdAt = entry.createdAt,
                    asOf = today,
                    settlements = settlements.filter { it.transactionId == entry.id }
                )
            }
        }

    val principalBalance = orderedEntries
        .filter { it.id !in emiTransactionIdsForSummary }
        .sumOf { entry ->
        val related = settlements.filter {
            it.transactionId == entry.id && it.settlementDate <= today
        }
        val remaining = (entry.amount - related.sumOf { it.amount })
            .coerceAtLeast(0.0)
        if (entry.type == EntryType.CREDIT) remaining else -remaining
    }

    val nonEmiEntries = orderedEntries.filter {
        it.id !in emiTransactionIdsForSummary
    }

    val accruedInterest = calculateInterestPosition(
        entries = nonEmiEntries,
        asOf = today,
        settlements = settlements
    )

    val remainingInterest = calculateInterestAccountPosition(
        transactions = nonEmiEntries,
        records = interestRecords,
        asOf = today,
        settlements = settlements
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
    summaryRows.add(listOf("Total ${party.name.trim()} Gave", money(got)))

    val gaveTransactionIds = nonEmiEntries
        .filter { it.type == EntryType.CREDIT }
        .map { it.id }
        .toSet()

    val gotTransactionIds = nonEmiEntries
        .filter { it.type == EntryType.DEBIT }
        .map { it.id }
        .toSet()

    val totalSettledOnGave = settlements
        .filter {
            it.settlementDate <= today && it.transactionId in gaveTransactionIds
        }
        .sumOf { it.amount }

    val totalSettledOnGot = settlements
        .filter {
            it.settlementDate <= today && it.transactionId in gotTransactionIds
        }
        .sumOf { it.amount }

    if (totalSettledOnGave > 0.0) {
        summaryRows.add(
            listOf("Total Settled (${party.name.trim()} Paid)", money(totalSettledOnGave))
        )
    }

    if (totalSettledOnGot > 0.0) {
        summaryRows.add(
            listOf(
                "Total Settled (${profileName.trim().ifEmpty { "You" }} Paid)",
                money(totalSettledOnGot)
            )
        )
    }

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

    val emiEntries = orderedEntries.filter { it.id in emiTransactionIdsForSummary }
    val emiGave = emiEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { it.amount }
    val emiGot = emiEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { it.amount }
    val emiGaveInterest = emiEntries
        .filter { it.type == EntryType.CREDIT }
        .sumOf { entry ->
            calculateReportEmiTotalInterest(
                entry = entry,
                schedules = emiSchedules.filter { it.transactionId == entry.id },
                asOf = today,
                    settlements = emiChargeSettlements.filter { it.transactionId == entry.id }
            )
        }
    val emiGotInterest = emiEntries
        .filter { it.type == EntryType.DEBIT }
        .sumOf { entry ->
            calculateReportEmiTotalInterest(
                entry = entry,
                schedules = emiSchedules.filter { it.transactionId == entry.id },
                asOf = today,
                    settlements = emiChargeSettlements.filter { it.transactionId == entry.id }
            )
        }
    val emiGaveTransactionIds = emiEntries
        .filter { it.type == EntryType.CREDIT }
        .map { it.id }
        .toSet()
    val emiGotTransactionIds = emiEntries
        .filter { it.type == EntryType.DEBIT }
        .map { it.id }
        .toSet()

    fun calculateReportEmiRemaining(entry: LedgerEntryEntity): Double {
    val schedules = emiSchedules
        .filter { it.transactionId == entry.id }
        .sortedBy { it.installmentNumber }
    if (schedules.isEmpty()) return 0.0

    val baseEmiDate = java.util.Calendar.getInstance().apply {
        timeInMillis = entry.transactionDate
        add(java.util.Calendar.MONTH, 1)
    }
    val actualEmiStartDate = schedules.first().dueDate

    fun localEmiDate(millis: Long): java.time.LocalDate {
        val c = java.util.Calendar.getInstance().apply { timeInMillis = millis }
        return java.time.LocalDate.of(
            c.get(java.util.Calendar.YEAR),
            c.get(java.util.Calendar.MONTH) + 1,
            c.get(java.util.Calendar.DAY_OF_MONTH)
        )
    }

    val delayDays = java.time.temporal.ChronoUnit.DAYS.between(
        localEmiDate(baseEmiDate.timeInMillis),
        localEmiDate(actualEmiStartDate)
    ).coerceAtLeast(0L)

    val delayInterest = kotlin.math.round(
        kotlin.math.abs(entry.amount) *
            entry.interestRate.coerceAtLeast(0.0) *
            delayDays / 36500.0 * 100.0
    ) / 100.0

    val pendingTotal = schedules.filter { !it.isPaid }
        .sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }

    val partialEmis = schedules.filter {
        it.paidAmount > 0.0 && it.paidAmount < it.amount
    }
    val partialRemaining = partialEmis.sumOf {
        (it.amount - it.paidAmount).coerceAtLeast(0.0)
    }

    val todayStart = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    val overdueInterest = partialEmis.sumOf { emi ->
        val dueStart = java.util.Calendar.getInstance().apply {
            timeInMillis = emi.dueDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        val days = if (todayStart > dueStart) {
            (todayStart - dueStart) / 86_400_000L
        } else 0L

        (emi.amount - emi.paidAmount).coerceAtLeast(0.0) *
            entry.interestRate.coerceAtLeast(0.0) * days / 36500.0
    }

    val history = emiChargeSettlements.filter { it.transactionId == entry.id }
    val settledEmiAmountTotal = history.sumOf { it.emiAmount.coerceAtLeast(0.0) }
    val settledInterestAmountTotal = history.sumOf { it.interestAmount.coerceAtLeast(0.0) }

    val settledEmiOverdueInterest = history.sumOf { settlement ->
        val settlementStart = java.util.Calendar.getInstance().apply {
            timeInMillis = settlement.settlementDate
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val days = ((todayStart - settlementStart) / 86_400_000L).coerceAtLeast(0L)
        kotlin.math.round(
            settlement.emiAmount.coerceAtLeast(0.0) *
                entry.interestRate.coerceAtLeast(0.0) * days / 36500.0 * 100.0
        ) / 100.0
    }

    val adjustedOverdueInterest =
        (overdueInterest - settledEmiOverdueInterest).coerceAtLeast(0.0)
    val adjustedPartialRemaining =
        (partialRemaining - settledEmiAmountTotal).coerceAtLeast(0.0)

    return (
        pendingTotal +
            delayInterest.coerceAtLeast(0.0) +
            adjustedPartialRemaining +
            adjustedOverdueInterest -
            settledInterestAmountTotal
    ).coerceAtLeast(0.0)
}

val emiGaveRemaining = emiEntries
    .filter { it.type == EntryType.CREDIT }
    .sumOf { calculateReportEmiRemaining(it) }

val emiGotRemaining = emiEntries
    .filter { it.type == EntryType.DEBIT }
    .sumOf { calculateReportEmiRemaining(it) }

    if (emiEntries.isNotEmpty()) {
        writer.space(14f)
        writer.section("EMI Summary")

        val profileLabel = profileName.trim().ifEmpty { "You" }
        val emiSummaryRows = mutableListOf<List<String>>()
        if (emiGave > 0.0) {
            emiSummaryRows.add(
                listOf("Total $profileLabel Gave", money(emiGave))
            )
        }
        if (emiGot > 0.0) {
            emiSummaryRows.add(
                listOf("Total $profileLabel Got", money(emiGot))
            )
        }
        if (emiGaveInterest > 0.0) {
            emiSummaryRows.add(
                listOf("Total EMI Int. of ${party.name.trim()}", money(emiGaveInterest))
            )
        }
        if (emiGotInterest > 0.0) {
            emiSummaryRows.add(
                listOf("Total EMI Int. of $profileLabel", money(emiGotInterest))
            )
        }
        if (emiGaveRemaining > 0.0) {
            emiSummaryRows.add(
                listOf("Remaining Balance of ${party.name.trim()}", money(emiGaveRemaining))
            )
        }
        if (emiGotRemaining > 0.0) {
            emiSummaryRows.add(
                listOf("Remaining Balance of $profileLabel", money(emiGotRemaining))
            )
        }

        writer.table(
            headers = listOf("EMI Summary", "Amount"),
            rows = emiSummaryRows,
            notes = List(emiSummaryRows.size) { null },
            widths = listOf(360f, 163f)
        )
    }

    writer.space(14f)
    writer.section("Transactions (${orderedEntries.size})")

    if (orderedEntries.isEmpty()) {
        writer.text("No transactions recorded.", 10f, false)
    } else {
        val transactionRows = mutableListOf<List<String>>()
        val transactionNotes = mutableListOf<String?>()
        var hasTransactionInterest = false
        val emiTransactionIds = emiSchedules.mapTo(mutableSetOf()) { it.transactionId }

        orderedEntries.forEach { entry ->
            val transactionDate =
                if (entry.transactionDate > 0L) entry.transactionDate
                else entry.createdAt

            val days = transactionInterestDays(
                transactionDate = entry.transactionDate,
                createdAt = entry.createdAt
            )
            val isEmiTransaction = entry.id in emiTransactionIds
            val interest = if (isEmiTransaction) {
                0.0
            } else {
                calculateTransactionInterest(
                    amount = entry.amount,
                    annualRate = entry.interestRate,
                    transactionDate = entry.transactionDate,
                    createdAt = entry.createdAt,
                    asOf = today,
                    settlements = settlements.filter { it.transactionId == entry.id }
                )
            }
            val emiTotalInterest = if (isEmiTransaction) {
                calculateReportEmiTotalInterest(
                    entry = entry,
                    schedules = emiSchedules.filter { it.transactionId == entry.id },
                    asOf = today,
                    settlements = emiChargeSettlements.filter { it.transactionId == entry.id }
                )
            } else 0.0
            if (interest > 0.0 || emiTotalInterest > 0.0) hasTransactionInterest = true

            val type = if (entry.type == EntryType.CREDIT) {
                gaveLabel.uppercase()
            } else {
                gotLabel.uppercase()
            }

            val interestDetails = if (isEmiTransaction) {
                "EMI Total Int.\n${money(emiTotalInterest)}"
            } else if (entry.interestRate > 0.0) {
                "${money(interest)}\n${formatRate(entry.interestRate)}% p.a."
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


    val entryById = orderedEntries.associateBy { it.id }
    val visibleSettlements = settlements
        .filter { it.settlementDate <= today && entryById.containsKey(it.transactionId) }
        .sortedWith(compareBy<TransactionSettlementEntity> { it.settlementDate }.thenBy { it.id })

    if (visibleSettlements.isNotEmpty()) {
        writer.space(14f)
        writer.section("Transaction Settlements (${visibleSettlements.size})")

        val remainingById = orderedEntries.associate { it.id to it.amount }.toMutableMap()
        val rows = mutableListOf<List<String>>()
        val notes = mutableListOf<String?>()

        visibleSettlements.forEach { settlement ->
            val entry = entryById.getValue(settlement.transactionId)
            val remaining = (
                (remainingById[entry.id] ?: entry.amount) - settlement.amount
            ).coerceAtLeast(0.0)
            remainingById[entry.id] = remaining

            val transactionDate = if (entry.transactionDate > 0L) {
                entry.transactionDate
            } else {
                entry.createdAt
            }
            val type = if (entry.type == EntryType.CREDIT) gaveLabel else gotLabel

            rows.add(
                listOf(
                    dateFormat.format(Date(settlement.settlementDate)),
                    "${dateFormat.format(Date(transactionDate))} - $type ${money(entry.amount)}",
                    money(settlement.amount),
                    money(remaining)
                )
            )
            notes.add(settlement.note.takeIf { it.isNotBlank() }?.let { "Note: $it" })
        }

        writer.table(
            headers = listOf("Date", "Transaction", "Settled", "Remaining Principal"),
            rows = rows,
            notes = notes
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


    // Append a separate installment table for every EMI transaction.
    val emiReportEntries = orderedEntries.mapNotNull { entry ->
        val transactionEmis = emiSchedules
            .filter { it.transactionId == entry.id }
            .sortedBy { it.installmentNumber }

        if (transactionEmis.isEmpty()) null else entry to transactionEmis
    }

    if (emiReportEntries.isNotEmpty()) {
        writer.space(14f)
        writer.section("EMI Installment Details (${emiReportEntries.size} transactions)")

        val todayStart = java.util.Calendar.getInstance().apply {
            timeInMillis = today
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        emiReportEntries.forEachIndexed { index, (entry, transactionEmis) ->
            writer.space(6f)

            val transactionDate = if (entry.transactionDate > 0L) {
                entry.transactionDate
            } else {
                entry.createdAt
            }

            val transactionType = if (entry.type == EntryType.CREDIT) {
                gaveLabel.uppercase()
            } else {
                gotLabel.uppercase()
            }

            val transactionTitle = buildString {
                append("${index + 1}. $transactionType")
                if (entry.note.isNotBlank()) append(" - ${entry.note}")
                append(" (${dateFormat.format(Date(transactionDate))})")
            }

            writer.subtitle(transactionTitle)

            // Transaction Summary: mirror the EMI Details screen's top card.
            val summaryTodayStart = java.util.Calendar.getInstance().apply {
                timeInMillis = today
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val summaryPaidCount = transactionEmis.count { it.isPaid }
            val summaryPendingCount = transactionEmis.count {
                !it.isPaid && it.dueDate >= summaryTodayStart
            }
            val summaryOverdueCount = transactionEmis.count {
                !it.isPaid && it.dueDate < summaryTodayStart
            }

            val summaryPeriodsPerYear = when (transactionEmis.first().frequency) {
                "Weekly" -> 52.0
                "Quarterly" -> 4.0
                "Half-Yearly" -> 2.0
                else -> 12.0
            }
            val summaryPeriodicRate =
                entry.interestRate.coerceAtLeast(0.0) / 100.0 /
                    summaryPeriodsPerYear

            var summaryOutstandingPrincipal = kotlin.math.abs(entry.amount)
            val summaryInstallmentInterest = transactionEmis.sumOf { emi ->
                val interest = kotlin.math.round(
                    summaryOutstandingPrincipal * summaryPeriodicRate * 100.0
                ) / 100.0
                val principal = (emi.amount - interest).coerceAtLeast(0.0)
                summaryOutstandingPrincipal =
                    (summaryOutstandingPrincipal - principal).coerceAtLeast(0.0)
                interest
            }

            val summaryBaseEmiDate = java.util.Calendar.getInstance().apply {
                timeInMillis = entry.transactionDate
                add(java.util.Calendar.MONTH, 1)
            }
            val summaryActualStartDate = transactionEmis.first().dueDate

            fun summaryLocalDate(millis: Long): java.time.LocalDate {
                val c = java.util.Calendar.getInstance().apply {
                    timeInMillis = millis
                }
                return java.time.LocalDate.of(
                    c.get(java.util.Calendar.YEAR),
                    c.get(java.util.Calendar.MONTH) + 1,
                    c.get(java.util.Calendar.DAY_OF_MONTH)
                )
            }

            val summaryDelayDays = java.time.temporal.ChronoUnit.DAYS.between(
                summaryLocalDate(summaryBaseEmiDate.timeInMillis),
                summaryLocalDate(summaryActualStartDate)
            ).coerceAtLeast(0L)

            val summaryDelayInterest = kotlin.math.round(
                kotlin.math.abs(entry.amount) *
                    entry.interestRate.coerceAtLeast(0.0) *
                    summaryDelayDays / 36500.0 * 100.0
            ) / 100.0

            val summaryPartialEmis = transactionEmis.filter {
                it.paidAmount > 0.0 && it.paidAmount < it.amount
            }
            val summaryPartialRemaining = summaryPartialEmis.sumOf {
                (it.amount - it.paidAmount).coerceAtLeast(0.0)
            }

            val summaryOverdueInterest = summaryPartialEmis.sumOf { emi ->
                val dueStart = java.util.Calendar.getInstance().apply {
                    timeInMillis = emi.dueDate
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis

                val days = if (summaryTodayStart > dueStart) {
                    (summaryTodayStart - dueStart) / 86_400_000L
                } else 0L

                (emi.amount - emi.paidAmount).coerceAtLeast(0.0) *
                    entry.interestRate.coerceAtLeast(0.0) *
                    days / (365.0 * 100.0)
            }

            val summaryHistory = emiChargeSettlements.filter {
                it.transactionId == entry.id
            }
            val summarySettledEmiAmount = summaryHistory.sumOf {
                it.emiAmount.coerceAtLeast(0.0)
            }
            val summarySettledOverdueInterest = summaryHistory.sumOf { settlement ->
                val settledEmi = settlement.emiAmount.coerceAtLeast(0.0)
                if (settledEmi <= 0.0) {
                    0.0
                } else {
                    val settlementStart = java.util.Calendar.getInstance().apply {
                        timeInMillis = settlement.settlementDate
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val days = (
                        (summaryTodayStart - settlementStart) / 86_400_000L
                    ).coerceAtLeast(0L)

                    kotlin.math.round(
                        settledEmi * entry.interestRate.coerceAtLeast(0.0) *
                            days / 36500.0 * 100.0
                    ) / 100.0
                }
            }

            val summaryAdjustedPartialRemaining =
                (summaryPartialRemaining - summarySettledEmiAmount)
                    .coerceAtLeast(0.0)
            val summaryAdjustedOverdueInterest =
                (summaryOverdueInterest - summarySettledOverdueInterest)
                    .coerceAtLeast(0.0)
            val summarySettledInterest = summaryHistory.sumOf {
                it.interestAmount.coerceAtLeast(0.0)
            }
            val summaryPendingEmisTotal = transactionEmis
                .filter { !it.isPaid }
                .sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }

            val summaryTotalInterest =
                summaryInstallmentInterest + summaryDelayInterest +
                    summaryAdjustedOverdueInterest

            val summaryRemaining =
                summaryPendingEmisTotal + (
                    summaryDelayInterest.coerceAtLeast(0.0) +
                        summaryAdjustedPartialRemaining.coerceAtLeast(0.0) +
                        summaryAdjustedOverdueInterest.coerceAtLeast(0.0) -
                        summarySettledInterest
                    ).coerceAtLeast(0.0)

            writer.subtitle("Transaction Summary")

            val summaryRows = mutableListOf<List<String>>()
            summaryRows.add(
                listOf("Transaction amount", money(entry.amount))
            )
            summaryRows.add(
                listOf(
                    "Total Installments (${transactionEmis.first().frequency})",
                    transactionEmis.size.toString()
                )
            )
            summaryRows.add(
                listOf(
                    "Paid / Pending / Overdue",
                    "$summaryPaidCount / $summaryPendingCount / $summaryOverdueCount"
                )
            )
            summaryRows.add(
                listOf("EMIs Int.", money(summaryInstallmentInterest))
            )

            if (summaryDelayDays > 0L) {
                summaryRows.add(
                    listOf(
                        "EMI Delay Int. ($summaryDelayDays days)",
                        money(summaryDelayInterest)
                    )
                )
            }
            if (summaryAdjustedPartialRemaining > 0.0) {
                summaryRows.add(
                    listOf(
                        "Partial Paid EMI Remaining",
                        money(summaryAdjustedPartialRemaining)
                    )
                )
            }
            if (summaryAdjustedOverdueInterest > 0.0) {
                summaryRows.add(
                    listOf(
                        "Partial Paid EMI Overdue Int.",
                        money(summaryAdjustedOverdueInterest)
                    )
                )
            }
            if (summaryTotalInterest > 0.0) {
                summaryRows.add(
                    listOf("Total Int.", money(summaryTotalInterest))
                )
            }
            summaryRows.add(
                listOf("Remaining", money(summaryRemaining))
            )

            writer.table(
                headers = listOf("Transaction Summary", "Details"),
                rows = summaryRows,
                notes = List(summaryRows.size) { null },
                widths = listOf(300f, 223f)
            )

            // Settlement history for this EMI transaction.
            writer.subtitle("Settlement History")

            val settlementHistoryRows = summaryHistory
                .sortedBy { it.settlementDate }
                .map { settlement ->
                    val emiSettled = settlement.emiAmount.coerceAtLeast(0.0)
                    val interestSettled =
                        settlement.interestAmount.coerceAtLeast(0.0)

                    listOf(
                        dateFormat.format(Date(settlement.settlementDate)),
                        money(emiSettled),
                        money(interestSettled),
                        money(emiSettled + interestSettled)
                    )
                }

            if (settlementHistoryRows.isEmpty()) {
                writer.text("No settlement history recorded.", 9f, false)
            } else {
                writer.table(
                    headers = listOf(
                        "Settlement Date",
                        "EMI Settled",
                        "Interest Settled",
                        "Total Settled"
                    ),
                    rows = settlementHistoryRows,
                    notes = List(settlementHistoryRows.size) { null },
                    widths = listOf(105f, 125f, 135f, 158f)
                )
            }

            val periodsPerYear = when (transactionEmis.first().frequency) {
                "Weekly" -> 52.0
                "Quarterly" -> 4.0
                "Half-Yearly" -> 2.0
                else -> 12.0
            }

            val periodicRate =
                entry.interestRate.coerceAtLeast(0.0) / 100.0 / periodsPerYear

            var outstandingPrincipal = kotlin.math.abs(entry.amount)

            val rows = transactionEmis.map { emi ->
                val interest = kotlin.math.round(
                    outstandingPrincipal * periodicRate * 100.0
                ) / 100.0

                val principal = (emi.amount - interest).coerceAtLeast(0.0)
                outstandingPrincipal =
                    (outstandingPrincipal - principal).coerceAtLeast(0.0)

                val remaining = (emi.amount - emi.paidAmount)
                    .coerceAtLeast(0.0)

                val status = when {
                    emi.paidAmount > 0.0 &&
                        emi.paidAmount < emi.amount - 0.001 -> "Partial Paid"
                    emi.isPaid -> "Paid"
                    emi.dueDate < todayStart -> "Overdue"
                    else -> "Pending"
                }

                listOf(
                    "${emi.installmentNumber}/${emi.installmentCount}",
                    dateFormat.format(Date(emi.dueDate)),
                    money(principal),
                    money(interest),
                    money(emi.amount),
                    money(emi.paidAmount.coerceAtLeast(0.0)),
                    money(remaining),
                    status
                )
            }

            writer.table(
                headers = listOf(
                    "EMI No.",
                    "Due Date",
                    "Principal",
                    "Interest",
                    "EMI Amount",
                    "Paid",
                    "Remaining",
                    "Status"
                ),
                rows = rows,
                notes = List(rows.size) { null },
                widths = listOf(
                    48f, 64f, 62f, 57f, 64f, 64f, 64f, 100f
                )
            )
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
