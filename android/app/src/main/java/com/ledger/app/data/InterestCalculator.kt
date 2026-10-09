package com.ledger.app.data

import java.util.Calendar

private const val MILLIS_PER_DAY = 86_400_000L
private const val DAYS_PER_YEAR = 365.0

fun transactionInterestDays(
    transactionDate: Long,
    createdAt: Long,
    asOf: Long = System.currentTimeMillis()
): Long {
    val effectiveDate = if (transactionDate > 0L) transactionDate else createdAt

    val start = Calendar.getInstance().apply {
        timeInMillis = effectiveDate
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val end = Calendar.getInstance().apply {
        timeInMillis = asOf
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return maxOf(
        0L,
        (end.timeInMillis - start.timeInMillis) / MILLIS_PER_DAY
    )
}

fun calculateTransactionInterest(
    amount: Double,
    annualRate: Double,
    transactionDate: Long,
    createdAt: Long,
    asOf: Long = System.currentTimeMillis()
): Double {
    if (amount <= 0.0 || annualRate <= 0.0) return 0.0

    val days = transactionInterestDays(
        transactionDate = transactionDate,
        createdAt = createdAt,
        asOf = asOf
    )

    return amount * annualRate * days / (DAYS_PER_YEAR * 100.0)
}

fun calculateEntryTotal(
    entry: LedgerEntryEntity,
    asOf: Long = System.currentTimeMillis()
): Double {
    return entry.amount + calculateTransactionInterest(
        amount = entry.amount,
        annualRate = entry.interestRate,
        transactionDate = entry.transactionDate,
        createdAt = entry.createdAt,
        asOf = asOf
    )
}

fun calculateLedgerBalance(
    entries: List<LedgerEntryEntity>,
    asOf: Long = System.currentTimeMillis()
): Double {
    return entries.sumOf { entry ->
        val total = calculateEntryTotal(entry, asOf)

        when (entry.type) {
            EntryType.CREDIT -> total
            EntryType.DEBIT -> -total
        }
    }
}


data class InterestPosition(
    val receivable: Double,
    val payable: Double
)

fun calculateInterestPosition(
    entries: List<LedgerEntryEntity>,
    asOf: Long = System.currentTimeMillis()
): InterestPosition {
    val net = entries.sumOf { entry ->
        val interest = calculateTransactionInterest(
            amount = entry.amount,
            annualRate = entry.interestRate,
            transactionDate = entry.transactionDate,
            createdAt = entry.createdAt,
            asOf = asOf
        )
        when (entry.type) {
            EntryType.CREDIT -> interest
            EntryType.DEBIT -> -interest
        }
    }
    return InterestPosition(
        receivable = if (net > 0.0) net else 0.0,
        payable = if (net < 0.0) -net else 0.0
    )
}

fun calculateInterestAccountPosition(
    transactions: List<LedgerEntryEntity>,
    records: List<InterestAccountEntryEntity>,
    asOf: Long = System.currentTimeMillis()
): InterestPosition {
    val accrued = calculateInterestPosition(transactions, asOf)

    val interestYouGot = records
        .filter { it.type == "GOT" }
        .sumOf { it.amount }

    val interestYouGave = records
        .filter { it.type == "GAVE" }
        .sumOf { it.amount }

    return InterestPosition(
        receivable = (accrued.receivable - interestYouGot).coerceAtLeast(0.0),
        payable = (accrued.payable - interestYouGave).coerceAtLeast(0.0)
    )
}
