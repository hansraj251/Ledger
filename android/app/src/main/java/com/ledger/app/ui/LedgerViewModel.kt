package com.ledger.app.ui

import androidx.lifecycle.ViewModel
import com.ledger.app.data.LedgerDataSource
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import com.ledger.app.data.calculateLedgerBalance
import com.ledger.app.data.calculateInterestAccountPosition
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
    private val onDataChanged: () -> Unit = {}
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LedgerUiState()
    )

    val uiState: StateFlow<LedgerUiState> =
        _uiState.asStateFlow()

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
                    party.id to calculateLedgerBalance(
                        dataSource.getEntries(party.id)
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
            val entries = withContext(Dispatchers.IO) {
                dataSource.getEntries(partyId)
            }

            val balance = calculateLedgerBalance(entries)

            _uiState.value = _uiState.value.copy(
                entries = entries,
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

    suspend fun deleteEntry(
        entry: LedgerEntryEntity
    ) {
        try {
            withContext(Dispatchers.IO) {
                dataSource.deleteEntry(entry)
            }

            loadEntries(entry.partyId)
            onDataChanged()
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to delete transaction"
            )
        }
    }

    suspend fun updateEntry(
        entry: LedgerEntryEntity,
        amount: Double,
        type: com.ledger.app.data.EntryType,
        note: String,
        transactionDate: Long,
        interestRate: Double
    ) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Amount must be greater than zero"
            )
            return
        }

        try {
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
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to update transaction"
            )
        }
    }

    suspend fun addEntry(
        partyId: Long,
        amount: Double,
        type: com.ledger.app.data.EntryType,
        note: String,
        transactionDate: Long = System.currentTimeMillis(),
        interestRate: Double = 0.0
    ) {
        if (amount <= 0.0) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Amount must be greater than zero"
            )
            return
        }

        try {
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
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to add transaction"
            )
        }
    }


    suspend fun loadInterestAccountEntries(partyId: Long) {
        try {
            val records = withContext(Dispatchers.IO) {
                dataSource.getInterestAccountEntries(partyId)
            }
            _uiState.value = _uiState.value.copy(
                interestAccountEntries = records
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
            val (transactions, records) = withContext(Dispatchers.IO) {
                dataSource.getEntries(partyId) to
                    dataSource.getInterestAccountEntries(partyId)
            }

            val position = calculateInterestAccountPosition(transactions, records)
            val pendingLimit = if (type == "GAVE") {
                position.payable
            } else {
                position.receivable
            }

            val maxRecordableAmount =
                kotlin.math.floor(pendingLimit * 100.0 + 0.000001) / 100.0

            if (amount > maxRecordableAmount + 1e-9) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Amount exceeds pending interest. Maximum allowed: ₹%.2f"
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }
}
