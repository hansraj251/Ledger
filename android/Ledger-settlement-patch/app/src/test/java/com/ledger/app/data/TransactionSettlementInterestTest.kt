package com.ledger.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionSettlementInterestTest {
    @Test
    fun settlementReducesPrincipalForInterestAfterSettlementDate() {
        val start = 1_700_000_000_000L
        val oneYear = 365L * 86_400_000L
        val entry = LedgerEntryEntity(
            id = 7L, partyId = 1L, amount = 100_000.0, type = EntryType.CREDIT,
            transactionDate = start, createdAt = start, interestRate = 10.0
        )
        val settlement = TransactionSettlementEntity(
            id = 1L, transactionId = 7L, amount = 30_000.0,
            settlementDate = start + oneYear, createdAt = start + oneYear
        )
        val interest = calculateTransactionInterest(
            amount = entry.amount, annualRate = entry.interestRate,
            transactionDate = entry.transactionDate, createdAt = entry.createdAt,
            asOf = start + 2 * oneYear, settlements = listOf(settlement)
        )
        assertEquals(17_000.0, interest, 1.0)
        assertEquals(70_000.0, remainingPrincipal(entry, listOf(settlement), start + 2 * oneYear), 0.001)
    }

    @Test
    fun multipleSettlementsReducePrincipalSequentially() {
        val start = 1_700_000_000_000L
        val year = 365L * 86_400_000L
        val entry = LedgerEntryEntity(
            id = 9L, partyId = 1L, amount = 100_000.0, type = EntryType.CREDIT,
            transactionDate = start, createdAt = start, interestRate = 10.0
        )
        val settlements = listOf(
            TransactionSettlementEntity(id = 1, transactionId = 9, amount = 30_000.0, settlementDate = start + year),
            TransactionSettlementEntity(id = 2, transactionId = 9, amount = 20_000.0, settlementDate = start + 2 * year)
        )
        val interest = calculateTransactionInterest(entry.amount, entry.interestRate, entry.transactionDate, entry.createdAt, start + 3 * year, settlements)
        assertEquals(22_000.0, interest, 1.0)
        assertEquals(50_000.0, remainingPrincipal(entry, settlements, start + 3 * year), 0.001)
    }
}
