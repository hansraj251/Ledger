package com.ledger.app.ui

import androidx.lifecycle.ViewModel
import com.ledger.app.data.LedgerDataSource
import com.ledger.app.data.LedgerEntryEntity
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
    private val dataSource: LedgerDataSource
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
                    party.id to dataSource.getBalance(party.id)
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

            val balance = withContext(Dispatchers.IO) {
                dataSource.getBalance(partyId)
            }

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
        note: String
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
                note = note
            )

            withContext(Dispatchers.IO) {
                dataSource.updateEntry(updatedEntry)
            }

            loadEntries(entry.partyId)
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
        note: String
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
                    note = note.trim()
                )
            }

            loadEntries(partyId)
        } catch (exception: Exception) {
            _uiState.value = _uiState.value.copy(
                errorMessage = exception.message
                    ?: "Unable to add transaction"
            )
        }
    }


    fun clearError() {
        _uiState.value = _uiState.value.copy(
            errorMessage = ""
        )
    }
}
