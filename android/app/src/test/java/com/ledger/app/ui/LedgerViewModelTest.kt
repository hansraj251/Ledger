package com.ledger.app.ui

import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerDataSource
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.ProfileEntity
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LedgerViewModelTest {

    private class FakeLedgerDataSource : LedgerDataSource {

        private val parties = mutableListOf(
            PartyEntity(
                id = 1,
                name = "Alpha Traders",
                mobile = "9876543210"
            ),
            PartyEntity(
                id = 2,
                name = "Beta Store",
                mobile = "9876500000"
            )
        )

        override fun getProfile(): ProfileEntity? {
            return null
        }

        override fun saveProfile(
            profile: ProfileEntity
        ) {
        }

        override fun addParty(
            name: String,
            mobile: String
        ): PartyEntity {
            val party = PartyEntity(
                id = (parties.size + 1).toLong(),
                name = name,
                mobile = mobile
            )
            parties.add(party)
            return party
        }

        override fun getParties(): List<PartyEntity> {
            return parties.toList()
        }

        override fun getParty(
            partyId: Long
        ): PartyEntity? {
            return parties.firstOrNull {
                it.id == partyId
            }
        }

        override fun updateParty(
            party: PartyEntity
        ) {
        }

        override fun deleteParty(
            party: PartyEntity
        ) {
        }

        override fun updateEntry(
            entry: LedgerEntryEntity
        ) {
            val index =
                entries.indexOfFirst {
                    it.id == entry.id
                }

            require(index >= 0) {
                "Entry does not exist"
            }

            entries[index] = entry
        }

        override fun addEntry(
            partyId: Long,
            amount: Double,
            type: EntryType,
            note: String
        ): LedgerEntryEntity {
            require(
                parties.any {
                    it.id == partyId
                }
            ) {
                "Party does not exist"
            }
            val entry = LedgerEntryEntity(
                id = (entries.size + 1).toLong(),
                partyId = partyId,
                amount = amount,
                type = type,
                note = note
            )
            entries.add(entry)
            return entry
        }

        private val entries = mutableListOf<LedgerEntryEntity>()

        override fun getEntries(
            partyId: Long
        ): List<LedgerEntryEntity> {
            return entries.filter {
                it.partyId == partyId
            }
        }

        override fun getBalance(
            partyId: Long
        ): Double {
            return entries
                .filter {
                    it.partyId == partyId
                }
                .sumOf {
                    when (it.type) {
                        EntryType.CREDIT -> it.amount
                        EntryType.DEBIT -> -it.amount
                    }
                }
        }
    }

    @Test
    fun loadParties_populatesPartyList() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        assertEquals(
            2,
            viewModel.uiState.value.parties.size
        )
    }

    @Test
    fun filterParties_matchesNameAndMobile() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        viewModel.setSearchQuery("beta")

        assertEquals(
            1,
            viewModel.uiState.value.filteredParties.size
        )

        assertEquals(
            "Beta Store",
            viewModel.uiState.value.filteredParties.first().name
        )

        viewModel.setSearchQuery("9876543210")

        assertEquals(
            1,
            viewModel.uiState.value.filteredParties.size
        )

        assertEquals(
            "Alpha Traders",
            viewModel.uiState.value.filteredParties.first().name
        )
    }

    @Test
    fun addParty_addsPartyAndClearsError() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        viewModel.addParty(
            name = "New Customer",
            mobile = "9999999999"
        )

        assertTrue(
            viewModel.uiState.value.parties.any {
                it.name == "New Customer"
            }
        )

        assertEquals(
            "",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun addParty_blankName_isRejected() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        viewModel.addParty(
            name = "   ",
            mobile = ""
        )

        assertEquals(
            "Party name is required",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun addEntry_addsTransactionAndRefreshesEntries() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 1,
            amount = 750.0,
            type = EntryType.CREDIT,
            note = "Sale"
        )

        assertEquals(
            1,
            viewModel.uiState.value.entries.size
        )

        assertEquals(
            750.0,
            viewModel.uiState.value.entries.first().amount,
            0.001
        )

        assertEquals(
            EntryType.CREDIT,
            viewModel.uiState.value.entries.first().type
        )
    }

    @Test
    fun loadEntries_populatesSelectedPartyTransactions() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        dataSource.addEntry(
            partyId = 1,
            amount = 750.0,
            type = EntryType.CREDIT,
            note = "Existing entry"
        )

        viewModel.loadEntries(1)

        assertEquals(
            1,
            viewModel.uiState.value.entries.size
        )

        assertEquals(
            750.0,
            viewModel.uiState.value.entries.first().amount,
            0.001
        )
    }

    @Test
    fun addEntry_blankAmount_isRejected() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 1,
            amount = 0.0,
            type = EntryType.DEBIT,
            note = ""
        )

        assertEquals(
            "Amount must be greater than zero",
            viewModel.uiState.value.errorMessage
        )
    }

    @Test
    fun addEntry_unknownParty_isRejected() = runBlocking {
        val viewModel = LedgerViewModel(
            FakeLedgerDataSource()
        )

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 999,
            amount = 500.0,
            type = EntryType.CREDIT,
            note = ""
        )

        assertEquals(
            "Party does not exist",
            viewModel.uiState.value.errorMessage
        )
    }


    @Test
    fun loadEntries_updatesCurrentBalance() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        dataSource.addEntry(
            partyId = 1,
            amount = 1000.0,
            type = EntryType.CREDIT,
            note = "Credit"
        )

        dataSource.addEntry(
            partyId = 1,
            amount = 250.0,
            type = EntryType.DEBIT,
            note = "Debit"
        )

        viewModel.loadEntries(1)

        assertEquals(
            750.0,
            viewModel.uiState.value.balance,
            0.001
        )
    }


    @Test
    fun addEntry_credit_updatesBalanceAndEntry() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 1,
            amount = 1000.0,
            type = EntryType.CREDIT,
            note = "Credit sale"
        )

        assertEquals(
            1000.0,
            viewModel.uiState.value.balance,
            0.001
        )

        assertEquals(
            EntryType.CREDIT,
            viewModel.uiState.value.entries.first().type
        )
    }

    @Test
    fun addEntry_debit_updatesBalanceAndEntry() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 1,
            amount = 400.0,
            type = EntryType.DEBIT,
            note = "Payment received"
        )

        assertEquals(
            -400.0,
            viewModel.uiState.value.balance,
            0.001
        )

        assertEquals(
            EntryType.DEBIT,
            viewModel.uiState.value.entries.first().type
        )
    }

    @Test
    fun addEntry_creditAndDebit_calculatesNetBalance() = runBlocking {
        val dataSource = FakeLedgerDataSource()
        val viewModel = LedgerViewModel(dataSource)

        viewModel.loadParties()

        viewModel.addEntry(
            partyId = 1,
            amount = 1500.0,
            type = EntryType.CREDIT,
            note = "Sale"
        )

        viewModel.addEntry(
            partyId = 1,
            amount = 500.0,
            type = EntryType.DEBIT,
            note = "Payment"
        )

        assertEquals(
            1000.0,
            viewModel.uiState.value.balance,
            0.001
        )

        assertEquals(
            2,
            viewModel.uiState.value.entries.size
        )
    }


    @Test
    fun updateEntry_preservesIdAndCreatedAt() = runBlocking {
        val dataSource =
            FakeLedgerDataSource()

        val viewModel =
            LedgerViewModel(
                dataSource
            )

        val originalCreatedAt =
            123456789L

        val entry =
            LedgerEntryEntity(
                id = 10L,
                partyId = 1L,
                amount = 500.0,
                type = EntryType.CREDIT,
                note = "Original",
                createdAt = originalCreatedAt
            )

        val entriesField =
            dataSource.javaClass
                .getDeclaredField("entries")

        entriesField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val entries =
            entriesField.get(
                dataSource
            ) as MutableList<LedgerEntryEntity>

        entries.add(
            entry
        )

        viewModel.updateEntry(
            entry = entry,
            amount = 750.0,
            type = EntryType.DEBIT,
            note = "Updated"
        )

        viewModel.loadEntries(
            1L
        )

        val updated =
            viewModel.uiState.value.entries.first()

        assertEquals(
            10L,
            updated.id
        )

        assertEquals(
            originalCreatedAt,
            updated.createdAt
        )

        assertEquals(
            750.0,
            updated.amount,
            0.0
        )

        assertEquals(
            EntryType.DEBIT,
            updated.type
        )

        assertEquals(
            "Updated",
            updated.note
        )
    }

    @Test
    fun updateEntry_updatesBalance() = runBlocking {
        val dataSource =
            FakeLedgerDataSource()

        val viewModel =
            LedgerViewModel(
                dataSource
            )

        val entry =
            dataSource.addEntry(
                partyId = 1L,
                amount = 500.0,
                type = EntryType.CREDIT,
                note = "Original"
            )

        viewModel.updateEntry(
            entry = entry,
            amount = 800.0,
            type = EntryType.CREDIT,
            note = "Changed"
        )

        viewModel.loadEntries(
            1L
        )

        assertEquals(
            800.0,
            viewModel.uiState.value.balance,
            0.0
        )
    }

}
