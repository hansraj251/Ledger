package com.ledger.app.data

class LedgerRepository(
    private val database: LedgerDatabase
) : LedgerDataSource {

    private val partyDao = database.partyDao()

    private val ledgerEntryDao = database.ledgerEntryDao()

    private val profileDao = database.profileDao()

    private val interestAccountEntryDao = database.interestAccountEntryDao()

    private val transactionSettlementDao = database.transactionSettlementDao()

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

    override fun updateInterestAccountEntry(entry: InterestAccountEntryEntity) {
        require(entry.amount > 0.0) { "Interest amount must be greater than zero" }
        require(entry.type == "GAVE" || entry.type == "GOT") { "Invalid interest entry type" }
        require(partyDao.getById(entry.partyId) != null) { "Party does not exist" }
        require(interestAccountEntryDao.update(entry) == 1) {
            "Interest record no longer exists"
        }
    }

    override fun deleteInterestAccountEntry(entryId: Long, partyId: Long) {
        require(partyDao.getById(partyId) != null) { "Party does not exist" }
        require(interestAccountEntryDao.deleteByIdAndPartyId(entryId, partyId) == 1) {
            "Interest record no longer exists"
        }
    }

    override fun getBalance(
        partyId: Long
    ): Double {
        return ledgerEntryDao.getBalance(partyId)
    }

    override fun getSettlementsForTransaction(transactionId: Long): List<TransactionSettlementEntity> =
        transactionSettlementDao.getByTransactionId(transactionId)

    override fun getSettlementsForParty(partyId: Long): List<TransactionSettlementEntity> =
        transactionSettlementDao.getByPartyId(partyId)

    override fun addSettlement(settlement: TransactionSettlementEntity): TransactionSettlementEntity {
        require(settlement.amount > 0.0 && settlement.amount.isFinite()) { "Settlement amount must be greater than zero" }
        val entry = ledgerEntryDao.getById(settlement.transactionId)
            ?: throw IllegalArgumentException("Transaction does not exist")
        require(settlement.settlementDate >= (if (entry.transactionDate > 0L) entry.transactionDate else entry.createdAt)) {
            "Settlement date cannot be before transaction date"
        }
        val existing = transactionSettlementDao.getByTransactionId(entry.id)
        val settled = existing.filter { it.settlementDate <= settlement.settlementDate }.sumOf { it.amount }
        require(settled + settlement.amount <= entry.amount + 0.000001) { "Settlement exceeds remaining principal" }
        val id = transactionSettlementDao.insert(settlement.copy(note = settlement.note.trim()))
        return settlement.copy(id = id, note = settlement.note.trim())
    }

    override fun updateSettlement(settlement: TransactionSettlementEntity): TransactionSettlementEntity {
        require(settlement.id > 0L) { "Invalid settlement" }
        require(settlement.amount > 0.0 && settlement.amount.isFinite()) {
            "Settlement amount must be greater than zero"
        }

        val entry = ledgerEntryDao.getById(settlement.transactionId)
            ?: throw IllegalArgumentException("Transaction does not exist")
        require(
            settlement.settlementDate >=
                (if (entry.transactionDate > 0L) entry.transactionDate else entry.createdAt)
        ) { "Settlement date cannot be before transaction date" }

        val existing = transactionSettlementDao.getByTransactionId(entry.id)
        require(existing.any { it.id == settlement.id }) { "Settlement no longer exists" }

        val revised = existing.map {
            if (it.id == settlement.id) settlement.copy(note = settlement.note.trim()) else it
        }.sortedWith(compareBy<TransactionSettlementEntity> { it.settlementDate }.thenBy { it.id })

        var cumulative = 0.0
        revised.forEach {
            cumulative += it.amount
            require(cumulative <= entry.amount + 0.000001) {
                "Settlement exceeds remaining principal"
            }
        }

        require(transactionSettlementDao.update(settlement.copy(note = settlement.note.trim())) == 1) {
            "Settlement no longer exists"
        }
        return settlement.copy(note = settlement.note.trim())
    }

    override fun deleteSettlement(settlementId: Long) {
        require(settlementId > 0L) { "Invalid settlement" }
        require(transactionSettlementDao.deleteById(settlementId) == 1) {
            "Settlement no longer exists"
        }
    }

}
