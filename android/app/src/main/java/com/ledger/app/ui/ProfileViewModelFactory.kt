package com.ledger.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository

class ProfileViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            val database =
                LedgerDatabaseProvider.get(application)

            val repository =
                LedgerRepository(database)

            @Suppress("UNCHECKED_CAST")
            return ProfileViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
