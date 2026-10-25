package com.ledger.app.data

import java.util.Calendar

private const val MILLIS_PER_DAY = 86_400_000L
private const val DAYS_PER_YEAR = 365.0

fun transactionInterestDays(transactionDate: Long, createdAt: Long, asOf: Long = System.currentTimeMillis()): Long {
    val effectiveDate = if (transactionDate > 0L) transactionDate else createdAt
    val start = Calendar.getInstance().apply { timeInMillis = effectiveDate; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    val end = Calendar.getInstance().apply { timeInMillis = asOf; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    return maxOf(0L, (end.timeInMillis - start.timeInMillis) / MILLIS_PER_DAY)
}

/** Interest is calculated on each principal segment; a settlement reduces principal from its date onward. */
fun calculateTransactionInterest(
    amount: Double, annualRate: Double, transactionDate: Long, createdAt: Long,
    asOf: Long = System.currentTimeMillis(), settlements: List<TransactionSettlementEntity> = emptyList()
): Double {
    if (amount <= 0.0 || annualRate <= 0.0) return 0.0
    val startDate = if (transactionDate > 0L) transactionDate else createdAt

    fun dayStart(time: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    val startDay = dayStart(startDate)
    val asOfDay = dayStart(asOf)

    val relevant = settlements.asSequence()
        .filter {
            it.amount > 0.0 &&
                dayStart(it.settlementDate) >= startDay &&
                dayStart(it.settlementDate) < asOfDay
        }
        .sortedWith(compareBy<TransactionSettlementEntity> { dayStart(it.settlementDate) }.thenBy { it.id })
        .toList()
    var principal = amount
    var segmentStart = startDate
    var interest = 0.0

    for (settlement in relevant) {
        val settlementDay = dayStart(settlement.settlementDate)
        val days = transactionInterestDays(segmentStart, segmentStart, settlementDay)
        interest += principal * annualRate * days / (DAYS_PER_YEAR * 100.0)
        principal = (principal - settlement.amount).coerceAtLeast(0.0)
        segmentStart = settlementDay
        if (principal <= 0.0) break
    }

    if (principal > 0.0 && dayStart(segmentStart) < asOfDay) {
        val days = transactionInterestDays(segmentStart, segmentStart, asOfDay)
        interest += principal * annualRate * days / (DAYS_PER_YEAR * 100.0)
    }
    return interest
}

fun remainingPrincipal(entry: LedgerEntryEntity, settlements: List<TransactionSettlementEntity>, asOf: Long = System.currentTimeMillis()): Double =
    (entry.amount - settlements.filter { it.settlementDate <= asOf }.sumOf { it.amount }).coerceAtLeast(0.0)

fun calculateEntryTotal(entry: LedgerEntryEntity, asOf: Long = System.currentTimeMillis(), settlements: List<TransactionSettlementEntity> = emptyList()): Double =
    remainingPrincipal(entry, settlements, asOf) + calculateTransactionInterest(entry.amount, entry.interestRate, entry.transactionDate, entry.createdAt, asOf, settlements)

fun calculateLedgerBalance(entries: List<LedgerEntryEntity>, asOf: Long = System.currentTimeMillis(), settlements: List<TransactionSettlementEntity> = emptyList()): Double = entries.sumOf { entry ->
    val related = settlements.filter { it.transactionId == entry.id }
    val total = calculateEntryTotal(entry, asOf, related)
    when (entry.type) { EntryType.CREDIT -> total; EntryType.DEBIT -> -total }
}

data class InterestPosition(val receivable: Double, val payable: Double)

fun calculateInterestPosition(entries: List<LedgerEntryEntity>, asOf: Long = System.currentTimeMillis(), settlements: List<TransactionSettlementEntity> = emptyList()): InterestPosition {
    val net = entries.sumOf { entry ->
        val interest = calculateTransactionInterest(entry.amount, entry.interestRate, entry.transactionDate, entry.createdAt, asOf, settlements.filter { it.transactionId == entry.id })
        when (entry.type) { EntryType.CREDIT -> interest; EntryType.DEBIT -> -interest }
    }
    return InterestPosition(if (net > 0.0) net else 0.0, if (net < 0.0) -net else 0.0)
}

fun calculateInterestAccountPosition(transactions: List<LedgerEntryEntity>, records: List<InterestAccountEntryEntity>, asOf: Long = System.currentTimeMillis(), settlements: List<TransactionSettlementEntity> = emptyList()): InterestPosition {
    val accrued = calculateInterestPosition(transactions, asOf, settlements)
    val interestYouGot = records.filter { it.type == "GOT" }.sumOf { it.amount }
    val interestYouGave = records.filter { it.type == "GAVE" }.sumOf { it.amount }
    return InterestPosition((accrued.receivable - interestYouGot).coerceAtLeast(0.0), (accrued.payable - interestYouGave).coerceAtLeast(0.0))
}
