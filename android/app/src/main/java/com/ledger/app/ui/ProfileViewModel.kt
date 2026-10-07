package com.ledger.app.ui

import androidx.lifecycle.ViewModel
import com.ledger.app.data.LedgerDataSource
import com.ledger.app.data.ProfileEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProfileUiState(
    val name: String = "",
    val mobile: String = "",
    val isLoading: Boolean = true
)

class ProfileViewModel(
    private val dataSource: LedgerDataSource
) : ViewModel() {

    private val _uiState =
        MutableStateFlow(ProfileUiState())

    val uiState:
        kotlinx.coroutines.flow.StateFlow<ProfileUiState> =
        _uiState.asStateFlow()

    suspend fun loadProfile() {
        val profile = withContext(Dispatchers.IO) {
            dataSource.getProfile()
        }

        _uiState.value = ProfileUiState(
            name = profile?.name ?: "",
            mobile = profile?.mobile ?: "",
            isLoading = false
        )
    }

    suspend fun saveProfile(
        name: String,
        mobile: String
    ): Boolean {
        val cleanName = name.trim()
        val cleanMobile = mobile.trim()

        if (cleanName.isBlank()) {
            return false
        }

        withContext(Dispatchers.IO) {
            dataSource.saveProfile(
                ProfileEntity(
                    id = 1,
                    name = cleanName,
                    mobile = cleanMobile
                )
            )
        }

        _uiState.value = ProfileUiState(
            name = cleanName,
            mobile = cleanMobile,
            isLoading = false
        )

        return true
    }
}
