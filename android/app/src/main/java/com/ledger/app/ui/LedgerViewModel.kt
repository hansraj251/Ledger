package com.ledger.app.ui

import com.ledger.app.data.CurrencySettings

import androidx.lifecycle.ViewModel
import com.ledger.app.data.LedgerDataSource
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import com.ledger.app.data.TransactionSettlementEntity
import com.ledger.app.data.remainingPrincipal
import com.ledger.app.data.calculateLedgerBalance
import com.ledger.app.data.calculateInterestAccountPosition
import com.ledger.app.data.calculateInterestPosition
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LedgerUiState(
    val parties: List<PartyEntity> = emptyList(),
    val partyBalances: Map<Long, Double> = emptyMap(),
    val entries: List<LedgerEntryEntity> = emptyList(),
    val interestAccountEntries: List<InterestAccountEntryEntity> = emptyList(),
    val settlements: List<TransactionSettlementEntity> = emptyList(),
    val selectedPartyId: Long? = null,
    val balance: Double = 0.0,
    val searchQuery: String = "",
    val errorMessage: String = "",
    val isLoading: Boolean = false
) {

    val filteredParties: List<PartyEntity>
        get() {
            val query = searchQuery.trim()

            if (query.isEmpty()) {
                return parties
            }

            return parties.filter {
                it.name.contains(
                    query,
                    ignoreCase = true
                ) ||
                it.mobile.contains(query)
            }
        }
}

class LedgerViewModel(
    private val dataSource: LedgerDataSource,
    private val onDataChanged: () -> Unit = {},
    private val isPremium: () -> Boolean = { false }
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LedgerUiState()
    )

    val uiState: StateFlow<LedgerUiState> =
        _uiState.asStateFlow()

    private fun calculateAdjustedBalance(
        entries: List<LedgerEntryEntity>,
        records: List<InterestAccountEntryEntity>,
        asOf: Long = System.currentTimeMillis(),
        settlements: List<TransactionSettlementEntity> = emptyList()
    ): Double {
        val originalBalance = calculateLedgerBalance(entries, asOf, settlements)
        val accrued = calculateInterestPosition(entries, asOf, settlements)
        val remaining = calculateInterestAccountPosition(entries, records, asOf, settlements)

        val accruedNet = accrued.receivable - accrued.payable

        return originalBalance - accruedNet
    }

    suspend fun loadParties() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = ""
        )

        try {
            val parties = withContext(Dispatchers.IO) {
                dataSource.getParties()
            }

            val partyBalances = withContext(Dispatchers.IO) {
                parties.associate { party ->
                    val entries = dataSource.getEntries(party.id)
                    val records = dataSource.getInterestAccountEntries(party.id)
                    val settlements = dataSource.getSettlementsForParty(party.id)
                    val emiTransactionIds = dataSource.getEmiTransactionIds(party.id)
                    val nonEmiEntries = entries.filterNot { it.id in emiTransactionIds }

                    party.id to calculateAdjustedBalance(
                        nonEmiEntries,
                        records,
                        settlements = settlements.filter { it.transactionId !in emiTransactionIds }
                    )
                }
            }

            _uiState.value = _uiState.value.copy(
                parties = parties,
                partyBalances = partyBalances,
                isLoading = false,
                errorMessage = ""
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = exception.message
                    ?: "Unable to load parties"
            )
        }
    }

    suspend fun updateParty(
        party: PartyEntity,
        name: String,
        mobile: String
    ): Boolean {
        val cleanName = name.trim()
        val cleanMobile = mobile.trim()

        if (cleanName.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Party name is required"
            )
            return false
        }

        return try {
            val updatedParty = party.copy(
                name = cleanName,
                mobile = cleanMobile
            )

            withContext(Dispatchers.IO) {
                dataSource.updateParty(updatedParty)
            }

            loadParties()

            _uiState.value = _uiState.value.copy(
                errorMessage = ""
            )

            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to update party"
            )

            false
        }
    }

    suspend fun updatePartyVisibility(
        partyId: Long,
        hideInterestButton: Boolean,
        hideSellButton: Boolean
    ): Boolean {
        return try {
            val updatedParty = withContext(Dispatchers.IO) {
                val existing = dataSource.getParty(partyId)
                    ?: return@withContext null
                existing.copy(
                    hideInterestButton = hideInterestButton,
                    hideSellButton = hideSellButton
                )
            } ?: return false

            withContext(Dispatchers.IO) {
                dataSource.updateParty(updatedParty)
            }
            loadParties()
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to save party settings"
            )
            false
        }
    }

    fun setSearchQuery(
        query: String
    ) {
        _uiState.value = _uiState.value.copy(
            searchQuery = query
        )
    }


    suspend fun deleteParty(
        party: PartyEntity
    ) {
        withContext(Dispatchers.IO) {
            dataSource.deleteParty(party)
        }

        loadParties()
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )

        onDataChanged()
    }

    suspend fun addParty(
        name: String,
        mobile: String
    ) {
        val cleanName = name.trim()
        val cleanMobile = mobile.trim()

        if (cleanName.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Party name is required"
            )
            return
        }


        if (!isPremium() && _uiState.value.parties.size >= 10) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Free plan allows up to 10 parties. Subscribe to Premium to add more."
            )
            return
        }

        try {
            withContext(Dispatchers.IO) {
                dataSource.addParty(
                    name = cleanName,
                    mobile = cleanMobile
                )
            }

            loadParties()
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to add party"
            )
        }

        onDataChanged()
    }

    suspend fun loadEntries(
        partyId: Long
    ) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            selectedPartyId = partyId,
            errorMessage = ""
        )

        try {
            val (entries, records, settlements) = withContext(Dispatchers.IO) {
                Triple(dataSource.getEntries(partyId), dataSource.getInterestAccountEntries(partyId), dataSource.getSettlementsForParty(partyId))
            }

            val asOf = System.currentTimeMillis()
            val balance = calculateAdjustedBalance(entries, records, asOf, settlements)

            _uiState.value = _uiState.value.copy(
                entries = entries,
                interestAccountEntries = records,
                settlements = settlements,
                selectedPartyId = partyId,
                balance = balance,
                isLoading = false,
                errorMessage = ""
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = exception.message
                    ?: "Unable to load transactions"
            )
        }
    }

    suspend fun addSettlement(entry: LedgerEntryEntity, amount: Double, settlementDate: Long, note: String = ""): Boolean {
        if (!amount.isFinite() || amount <= 0.0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Settlement amount must be greater than zero")
            return false
        }
        return try {
            withContext(Dispatchers.IO) {
                val existing = dataSource.getSettlementsForTransaction(entry.id)
                val remaining = (entry.amount - existing.filter { it.settlementDate <= settlementDate }.sumOf { it.amount }).coerceAtLeast(0.0)
                require(amount <= remaining + 0.000001) { "Settlement exceeds remaining principal" }
                dataSource.addSettlement(TransactionSettlementEntity(transactionId = entry.id, amount = amount, settlementDate = settlementDate, note = note.trim()))
            }
            loadEntries(entry.partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(errorMessage = exception.message ?: "Unable to save settlement")
            false
        }
    }

    suspend fun updateSettlement(
        entry: LedgerEntryEntity,
        settlementId: Long,
        amount: Double,
        settlementDate: Long,
        note: String
    ): Boolean {
        if (!amount.isFinite() || amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Settlement amount must be greater than zero"
            )
            return false
        }
        return try {
            withContext(Dispatchers.IO) {
                dataSource.updateSettlement(
                    TransactionSettlementEntity(
                        id = settlementId,
                        transactionId = entry.id,
                        amount = amount,
                        settlementDate = settlementDate,
                        note = note.trim()
                    )
                )
            }
            loadEntries(entry.partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to update settlement"
            )
            false
        }
    }

    suspend fun deleteSettlement(
        entry: LedgerEntryEntity,
        settlementId: Long
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                dataSource.deleteSettlement(settlementId)
            }
            loadEntries(entry.partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to delete settlement"
            )
            false
        }
    }

    suspend fun deleteEntry(
        entry: LedgerEntryEntity
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                dataSource.deleteEntry(entry)
            }
            loadEntries(entry.partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to delete transaction"
            )
            false
        }
    }

    suspend fun updateEntry(
        entry: LedgerEntryEntity,
        amount: Double,
        type: com.ledger.app.data.EntryType,
        note: String,
        transactionDate: Long,
        interestRate: Double
    ): Boolean {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Amount must be greater than zero"
            )
            return false
        }

        return try {
            val updatedEntry = entry.copy(
                amount = amount,
                type = type,
                note = note,
                transactionDate = transactionDate,
                interestRate = interestRate
            )

            withContext(Dispatchers.IO) {
                dataSource.updateEntry(updatedEntry)
            }

            loadEntries(entry.partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to update transaction"
            )
            false
        }
    }

    suspend fun addEntry(
        partyId: Long,
        amount: Double,
        type: com.ledger.app.data.EntryType,
        note: String,
        transactionDate: Long = System.currentTimeMillis(),
        interestRate: Double = 0.0
    ): Boolean {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Amount must be greater than zero"
            )
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                dataSource.addEntry(
                    partyId = partyId,
                    amount = amount,
                    type = type,
                    note = note.trim(),
                    transactionDate = transactionDate,
                    interestRate = interestRate
                )
            }

            loadEntries(partyId)
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to add transaction"
            )
            false
        }
    }

    suspend fun loadInterestAccountEntries(partyId: Long) {
        try {
            val records = withContext(Dispatchers.IO) {
                dataSource.getInterestAccountEntries(partyId)
            }
            val current = _uiState.value
            val updatedBalance = if (current.selectedPartyId == partyId) {
                calculateAdjustedBalance(current.entries, records, settlements = current.settlements)
            } else {
                current.balance
            }
            _uiState.value = current.copy(
                interestAccountEntries = records,
                balance = updatedBalance
            )
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to load interest records"
            )
        }
    }

    suspend fun addInterestAccountEntry(
        partyId: Long,
        amount: Double,
        type: String,
        note: String,
        transactionDate: Long
    ): Boolean {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Interest amount must be greater than zero"
            )
            return false
        }
        if (type != "GAVE" && type != "GOT") {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Select Int. You Gave or Int. You Got"
            )
            return false
        }

        return try {
            val (transactions, records, settlements) = withContext(Dispatchers.IO) {
                Triple(dataSource.getEntries(partyId), dataSource.getInterestAccountEntries(partyId), dataSource.getSettlementsForParty(partyId))
            }

            // Match the Interest Account page's Receivable/Payable Int. card.
            val accruedPosition = calculateInterestPosition(transactions, settlements = settlements)
            val recordedInterestNet = records.sumOf {
                if (it.type == "GAVE") it.amount else -it.amount
            }
            val netPendingInterest =
                accruedPosition.receivable - accruedPosition.payable + recordedInterestNet
            val pendingLimit = if (type == "GAVE") {
                maxOf(0.0, -netPendingInterest)
            } else {
                maxOf(0.0, netPendingInterest)
            }

            val maxRecordableAmount =
                kotlin.math.floor(pendingLimit * 100.0 + 0.000001) / 100.0

            if (amount > maxRecordableAmount + 1e-9) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Amount exceeds pending interest. Maximum allowed: ${CurrencySettings.symbol}%.2f"
                        .format(java.util.Locale.US, maxRecordableAmount)
                )
                return false
            }

            withContext(Dispatchers.IO) {
                dataSource.addInterestAccountEntry(
                    partyId = partyId,
                    amount = amount,
                    type = type,
                    note = note.trim(),
                    transactionDate = transactionDate
                )
            }
            loadInterestAccountEntries(partyId)
            _uiState.value = _uiState.value.copy(errorMessage = "")
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to save interest record"
            )
            false
        }
    }

    suspend fun updateInterestAccountEntry(
        partyId: Long,
        entryId: Long,
        amount: Double,
        type: String,
        note: String,
        transactionDate: Long
    ): Boolean {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Interest amount must be greater than zero"
            )
            return false
        }
        if (type != "GAVE" && type != "GOT") {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Select Int. You Gave or Int. You Got"
            )
            return false
        }

        return try {
            val (transactions, records, settlements) = withContext(Dispatchers.IO) {
                Triple(dataSource.getEntries(partyId), dataSource.getInterestAccountEntries(partyId), dataSource.getSettlementsForParty(partyId))
            }
            val original = records.firstOrNull {
                it.id == entryId && it.partyId == partyId
            } ?: throw IllegalArgumentException("Interest record no longer exists")

            // Exclude the old version of this record before checking its replacement.
            val otherRecords = records.filterNot { it.id == entryId }
            // Exclude the original record, then use the same net-card calculation as the UI.
            val accruedPosition = calculateInterestPosition(transactions, settlements = settlements)
            val recordedInterestNet = otherRecords.sumOf {
                if (it.type == "GAVE") it.amount else -it.amount
            }
            val netPendingInterest =
                accruedPosition.receivable - accruedPosition.payable + recordedInterestNet
            val pendingLimit = if (type == "GAVE") {
                maxOf(0.0, -netPendingInterest)
            } else {
                maxOf(0.0, netPendingInterest)
            }
            val maxRecordableAmount =
                kotlin.math.floor(pendingLimit * 100.0 + 0.000001) / 100.0

            if (amount > maxRecordableAmount + 1e-9) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Amount exceeds pending interest. Maximum allowed: ${CurrencySettings.symbol}%.2f"
                        .format(java.util.Locale.US, maxRecordableAmount)
                )
                return false
            }

            withContext(Dispatchers.IO) {
                dataSource.updateInterestAccountEntry(
                    original.copy(
                        amount = amount,
                        type = type,
                        note = note.trim(),
                        transactionDate = transactionDate
                    )
                )
            }
            loadInterestAccountEntries(partyId)
            _uiState.value = _uiState.value.copy(errorMessage = "")
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to update interest record"
            )
            false
        }
    }

    suspend fun deleteInterestAccountEntry(
        partyId: Long,
        entryId: Long
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                dataSource.deleteInterestAccountEntry(
                    entryId = entryId,
                    partyId = partyId
                )
            }
            loadInterestAccountEntries(partyId)
            _uiState.value = _uiState.value.copy(errorMessage = "")
            onDataChanged()
            true
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message ?: "Unable to delete interest record"
            )
            false
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }
}
