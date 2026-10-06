package com.ledger.app.ui

import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerEntryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class PartyLedgerPresentationTest {

    @Test
    fun creditAmount_hasPositivePrefix() {
        val entry = LedgerEntryEntity(
            id = 1,
            partyId = 1,
            amount = 750.0,
            type = EntryType.CREDIT,
            note = "Sale"
        )

        assertEquals(
            "+₹750.00",
            transactionAmountLabel(entry)
        )
    }

    @Test
    fun debitAmount_hasNegativePrefix() {
        val entry = LedgerEntryEntity(
            id = 2,
            partyId = 1,
            amount = 500.0,
            type = EntryType.DEBIT,
            note = "Purchase"
        )

        assertEquals(
            "-₹500.00",
            transactionAmountLabel(entry)
        )
    }

    @Test
    fun transactionDateTime_usesCreatedAt() {
        val entry = LedgerEntryEntity(
            id = 3,
            partyId = 1,
            amount = 250.0,
            type = EntryType.CREDIT,
            note = "Payment",
            createdAt = 0L
        )

        assertEquals(
            "01 Jan 1970, 05:30 AM",
            transactionDateTimeLabel(entry)
        )
    }
}
