package com.ledger.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.MaterialTheme
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import com.ledger.app.ui.HomeScreen
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PartyLedgerScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        val database = LedgerDatabaseProvider.get(
            applicationContext
        )

        val repository = LedgerRepository(
            database
        )

        setContent {
            MaterialTheme {
                var selectedParty by remember {
                    mutableStateOf<PartyEntity?>(null)
                }

                val viewModel = remember {
                    LedgerViewModel(
                        repository
                    )
                }

                if (selectedParty == null) {
                    HomeScreen(
                        viewModel = viewModel,
                        onPartyClick = { party ->
                            selectedParty = party
                        }
                    )
                } else {
                    PartyLedgerScreen(
                        party = selectedParty!!,
                        viewModel = viewModel,
                        onBack = {
                            selectedParty = null
                        }
                    )
                }
            }
        }
    }
}
