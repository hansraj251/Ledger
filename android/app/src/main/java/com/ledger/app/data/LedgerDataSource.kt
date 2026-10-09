package com.ledger.app.data

interface LedgerDataSource {

    fun getProfile(): ProfileEntity?

    fun saveProfile(
        profile: ProfileEntity
    )

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

    fun deleteParty(
        party: PartyEntity
    )

    fun updateEntry(
        entry: LedgerEntryEntity
    )

    fun deleteEntry(
        entry: LedgerEntryEntity
    )

    fun addEntry(
        partyId: Long,
        amount: Double,
        type: EntryType,
        note: String = "",
        transactionDate: Long = System.currentTimeMillis(),
        interestRate: Double = 0.0
    ): LedgerEntryEntity

    fun getEntries(
        partyId: Long
    ): List<LedgerEntryEntity>

    fun getInterestAccountEntries(
        partyId: Long
    ): List<InterestAccountEntryEntity>

    fun addInterestAccountEntry(
        partyId: Long,
        amount: Double,
        type: String,
        note: String = "",
        transactionDate: Long = System.currentTimeMillis()
    ): InterestAccountEntryEntity

    fun getBalance(
        partyId: Long
    ): Double
}
