package com.ledger.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LedgerRepositoryInstrumentedTest {

    private lateinit var database: LedgerDatabase

    private lateinit var repository: LedgerRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LedgerDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()

        repository = LedgerRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun addParty_persistsParty() {
        val party = repository.addParty(
            name = "Test Party",
            mobile = "9999999999"
        )

        val parties = repository.getParties()

        assertEquals(1, parties.size)
        assertEquals(party.id, parties.first().id)
        assertEquals("Test Party", parties.first().name)
        assertEquals("9999999999", parties.first().mobile)
    }

    @Test
    fun addEntries_balanceIsCreditMinusDebit() {
        val party = repository.addParty(
            name = "Test Party"
        )

        repository.addEntry(
            partyId = party.id,
            amount = 1000.0,
            type = EntryType.CREDIT
        )

        repository.addEntry(
            partyId = party.id,
            amount = 250.0,
            type = EntryType.DEBIT
        )

        assertEquals(
            750.0,
            repository.getBalance(party.id),
            0.001
        )
    }

    @Test
    fun addEntry_persistsEntryForParty() {
        val party = repository.addParty(
            name = "Test Party"
        )

        val entry = repository.addEntry(
            partyId = party.id,
            amount = 500.0,
            type = EntryType.CREDIT,
            note = "Opening balance"
        )

        val entries = repository.getEntries(party.id)

        assertEquals(1, entries.size)
        assertEquals(entry.id, entries.first().id)
        assertEquals(500.0, entries.first().amount, 0.001)
        assertEquals(EntryType.CREDIT, entries.first().type)
        assertEquals("Opening balance", entries.first().note)
    }

    @Test
    fun addEntry_forUnknownParty_isRejected() {
        try {
            repository.addEntry(
                partyId = 999L,
                amount = 100.0,
                type = EntryType.CREDIT
            )

            throw AssertionError(
                "Expected unknown party entry to be rejected"
            )
        } catch (exception: IllegalArgumentException) {
            assertEquals(
                "Party does not exist",
                exception.message
            )
        }
    }

    @Test
    fun addEntry_zeroAmount_isRejected() {
        val party = repository.addParty(
            name = "Test Party"
        )

        try {
            repository.addEntry(
                partyId = party.id,
                amount = 0.0,
                type = EntryType.CREDIT
            )

            throw AssertionError(
                "Expected zero amount to be rejected"
            )
        } catch (exception: IllegalArgumentException) {
            assertEquals(
                "Amount must be greater than zero",
                exception.message
            )
        }
    }
}
