package com.ledger.app.data

class LedgerRepository(
    private val database: LedgerDatabase
) : LedgerDataSource {

    private val partyDao = database.partyDao()

    private val ledgerEntryDao = database.ledgerEntryDao()

    private val profileDao = database.profileDao()

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
        transactionDate: Long
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
            transactionDate = transactionDate
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

    override fun getBalance(
        partyId: Long
    ): Double {
        return ledgerEntryDao.getBalance(partyId)
    }
}
