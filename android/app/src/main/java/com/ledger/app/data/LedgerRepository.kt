package com.ledger.app.data

class LedgerRepository(
    private val database: LedgerDatabase
) : LedgerDataSource {

    private val partyDao = database.partyDao()

    private val ledgerEntryDao = database.ledgerEntryDao()

    private val profileDao = database.profileDao()

    private val interestAccountEntryDao = database.interestAccountEntryDao()

    override fun getProfile(): ProfileEntity? {
        return profileDao.getProfile()
    }

    override fun saveProfile(
        profile: ProfileEntity
    ) {
        profileDao.saveProfile(profile)
    }

    override fun addParty(
        name: String,
        mobile: String
    ): PartyEntity {
        val party = PartyEntity(
            name = name,
            mobile = mobile
        )

        val id = partyDao.insert(party)

        return party.copy(
            id = id
        )
    }

    override fun getParties(): List<PartyEntity> {
        return partyDao.getAll()
    }

    override fun getParty(
        partyId: Long
    ): PartyEntity? {
        return partyDao.getById(partyId)
    }

    override fun updateParty(
        party: PartyEntity
    ) {
        partyDao.update(party)
    }

    override fun deleteParty(
        party: PartyEntity
    ) {
        partyDao.delete(party)
    }

    override fun updateEntry(
        entry: LedgerEntryEntity
    ) {
        ledgerEntryDao.update(entry)
    }

    override fun deleteEntry(
        entry: LedgerEntryEntity
    ) {
        ledgerEntryDao.delete(entry)
    }

    override fun addEntry(
        partyId: Long,
        amount: Double,
        type: EntryType,
        note: String,
        transactionDate: Long,
        interestRate: Double
    ): LedgerEntryEntity {
        require(amount > 0) {
            "Amount must be greater than zero"
        }

        require(partyDao.getById(partyId) != null) {
            "Party does not exist"
        }

        val entry = LedgerEntryEntity(
            partyId = partyId,
            amount = amount,
            type = type,
            note = note,
            transactionDate = transactionDate,
            interestRate = interestRate
        )

        val id = ledgerEntryDao.insert(entry)

        return entry.copy(
            id = id
        )
    }

    override fun getEntries(
        partyId: Long
    ): List<LedgerEntryEntity> {
        return ledgerEntryDao.getByPartyId(partyId)
    }

    override fun getInterestAccountEntries(
        partyId: Long
    ): List<InterestAccountEntryEntity> {
        return interestAccountEntryDao.getByPartyId(partyId)
    }

    override fun addInterestAccountEntry(
        partyId: Long,
        amount: Double,
        type: String,
        note: String,
        transactionDate: Long
    ): InterestAccountEntryEntity {
        require(amount > 0.0) { "Interest amount must be greater than zero" }
        require(type == "GAVE" || type == "GOT") { "Invalid interest entry type" }
        require(partyDao.getById(partyId) != null) { "Party does not exist" }

        val entry = InterestAccountEntryEntity(
            partyId = partyId,
            amount = amount,
            type = type,
            note = note.trim(),
            transactionDate = transactionDate
        )
        val id = interestAccountEntryDao.insert(entry)
        return entry.copy(id = id)
    }

    override fun getBalance(
        partyId: Long
    ): Double {
        return ledgerEntryDao.getBalance(partyId)
    }
}
