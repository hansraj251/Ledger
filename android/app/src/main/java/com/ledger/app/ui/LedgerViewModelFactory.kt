package com.ledger.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ledger.app.data.LedgerDataSource

class LedgerViewModelFactory(
    private val dataSource: LedgerDataSource
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(
                LedgerViewModel::class.java
            )
        ) {
            return LedgerViewModel(
                dataSource
            ) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class"
        )
    }
}
