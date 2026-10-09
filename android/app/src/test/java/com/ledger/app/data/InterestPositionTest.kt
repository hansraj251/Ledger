package com.ledger.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class InterestPositionTest {
    @Test
    fun creditInterestExceedingDebitInterestIsReceivable() {
        val asOf = 10L * 86_400_000L
        val entries = listOf(
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.CREDIT,
                transactionDate = 0L,
                createdAt = 0L,
                interestRate = 10.0
            ),
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.DEBIT,
                transactionDate = 0L,
                createdAt = 5L * 86_400_000L,
                interestRate = 10.0
            )
        )
        val result = calculateInterestPosition(entries, asOf)
        assertEquals(5.0, result.receivable, 0.001)
        assertEquals(0.0, result.payable, 0.001)
    }

    @Test
    fun debitInterestExceedingCreditInterestIsPayable() {
        val asOf = 10L * 86_400_000L
        val entries = listOf(
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.CREDIT,
                transactionDate = 0L,
                createdAt = 8L * 86_400_000L,
                interestRate = 10.0
            ),
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.DEBIT,
                transactionDate = 0L,
                createdAt = 0L,
                interestRate = 10.0
            )
        )
        val result = calculateInterestPosition(entries, asOf)
        assertEquals(0.0, result.receivable, 0.001)
        assertEquals(8.0, result.payable, 0.001)
    }

    @Test
    fun recordedInterest_isDeductedFromReceivableBalance() {
        val asOf = 10L * 86_400_000L
        val transactions = listOf(
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.CREDIT,
                transactionDate = 0L,
                createdAt = 0L,
                interestRate = 10.0
            )
        )
        val records = listOf(
            InterestAccountEntryEntity(
                partyId = 1L,
                amount = 3.0,
                type = "GOT",
                transactionDate = asOf
            ),
            InterestAccountEntryEntity(
                partyId = 1L,
                amount = 4.0,
                type = "GAVE",
                transactionDate = asOf
            )
        )

        val result = calculateInterestAccountPosition(
            transactions = transactions,
            records = records,
            asOf = asOf
        )

        assertEquals(7.0, result.receivable, 0.001)
        assertEquals(0.0, result.payable, 0.001)
    }

    @Test
    fun recordedInterest_isDeductedFromPayableBalance() {
        val asOf = 10L * 86_400_000L
        val transactions = listOf(
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.DEBIT,
                transactionDate = 0L,
                createdAt = 0L,
                interestRate = 10.0
            )
        )
        val records = listOf(
            InterestAccountEntryEntity(
                partyId = 1L,
                amount = 4.0,
                type = "GAVE",
                transactionDate = asOf
            )
        )

        val result = calculateInterestAccountPosition(
            transactions = transactions,
            records = records,
            asOf = asOf
        )

        assertEquals(0.0, result.receivable, 0.001)
        assertEquals(6.0, result.payable, 0.001)
    }

    @Test
    fun recordedInterest_cannotMakeCardBalanceNegative() {
        val asOf = 10L * 86_400_000L
        val transactions = listOf(
            LedgerEntryEntity(
                partyId = 1L,
                amount = 3650.0,
                type = EntryType.CREDIT,
                transactionDate = 0L,
                createdAt = 0L,
                interestRate = 10.0
            )
        )
        val records = listOf(
            InterestAccountEntryEntity(
                partyId = 1L,
                amount = 100.0,
                type = "GOT",
                transactionDate = asOf
            )
        )

        val result = calculateInterestAccountPosition(
            transactions = transactions,
            records = records,
            asOf = asOf
        )

        assertEquals(0.0, result.receivable, 0.001)
        assertEquals(0.0, result.payable, 0.001)
    }

}
