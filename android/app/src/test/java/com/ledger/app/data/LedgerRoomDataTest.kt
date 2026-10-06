package com.ledger.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerRoomDataTest {

    @Test
    fun partyEntity_preservesPartyData() {
        val party = PartyEntity(
            id = 1L,
            name = "ABC Traders",
            mobile = "9876543210"
        )

        assertEquals(1L, party.id)
        assertEquals("ABC Traders", party.name)
        assertEquals("9876543210", party.mobile)
    }

    @Test
    fun ledgerEntryEntity_preservesTransactionData() {
        val entry = LedgerEntryEntity(
            id = 1L,
            partyId = 7L,
            amount = 1500.0,
            type = EntryType.CREDIT,
            note = "Opening balance"
        )

        assertEquals(1L, entry.id)
        assertEquals(7L, entry.partyId)
        assertEquals(1500.0, entry.amount, 0.001)
        assertEquals(EntryType.CREDIT, entry.type)
        assertEquals("Opening balance", entry.note)
    }

    @Test
    fun ledgerEntryEntity_supportsCreditAndDebit() {
        val credit = LedgerEntryEntity(
            partyId = 1L,
            amount = 1000.0,
            type = EntryType.CREDIT
        )

        val debit = LedgerEntryEntity(
            partyId = 1L,
            amount = 250.0,
            type = EntryType.DEBIT
        )

        assertTrue(credit.type == EntryType.CREDIT)
        assertTrue(debit.type == EntryType.DEBIT)
    }
}
