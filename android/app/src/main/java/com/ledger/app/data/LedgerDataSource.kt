package com.ledger.app.data

interface LedgerDataSource {

    fun addParty(
        name: String,
        mobile: String = ""
    ): PartyEntity

    fun getParties(): List<PartyEntity>

    fun getParty(
        partyId: Long
    ): PartyEntity?

    fun updateParty(
        party: PartyEntity
    )

    fun updateEntry(
        entry: LedgerEntryEntity
    )

    fun addEntry(
        partyId: Long,
        amount: Double,
        type: EntryType,
        note: String = ""
    ): LedgerEntryEntity

    fun getEntries(
        partyId: Long
    ): List<LedgerEntryEntity>

    fun getBalance(
        partyId: Long
    ): Double
}
