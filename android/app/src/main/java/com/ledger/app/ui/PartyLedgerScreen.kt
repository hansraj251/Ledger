package com.ledger.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.launch
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PartyLedgerScreen(
    party: PartyEntity,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf(EntryType.CREDIT)
    }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(party.id) {
        viewModel.loadEntries(party.id)
    }

    BackHandler {
        onBack()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                if (party.mobile.isNotBlank()) {
                    Text(
                        text = party.mobile,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        BalanceCard(
            balance = uiState.balance
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        AddTransactionCard(
            amount = amount,
            note = note,
            selectedType = selectedType,
            onAmountChange = {
                amount = it
            },
            onNoteChange = {
                note = it
            },
            onTypeChange = {
                selectedType = it
            },
            onAdd = {
                val parsedAmount = amount.toDoubleOrNull()

                if (parsedAmount != null) {
                    coroutineScope.launch {
                        viewModel.addEntry(
                            partyId = party.id,
                            amount = parsedAmount,
                            type = selectedType,
                            note = note
                        )
                    }

                    amount = ""
                    note = ""
                }
            }
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Text(
            text = "Transactions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        if (uiState.entries.isEmpty()) {
            Text(
                text = "No transactions yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.entries,
                    key = {
                        it.id
                    }
                ) { entry ->
                    TransactionRow(
                        entry = entry
                    )
                }
            }
        }

        if (uiState.errorMessage.isNotBlank()) {
            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = uiState.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun BalanceCard(
    balance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Current Balance",
                style = MaterialTheme.typography.labelLarge
            )

            Text(
                text = "₹%.2f".format(balance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AddTransactionCard(
    amount: String,
    note: String,
    selectedType: EntryType,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTypeChange: (EntryType) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Add Transaction",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == EntryType.CREDIT,
                    onClick = {
                        onTypeChange(EntryType.CREDIT)
                    },
                    label = {
                        Text("Credit")
                    }
                )

                FilterChip(
                    selected = selectedType == EntryType.DEBIT,
                    onClick = {
                        onTypeChange(EntryType.DEBIT)
                    },
                    label = {
                        Text("Debit")
                    }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Amount")
                },
                singleLine = true
            )

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Note")
                },
                singleLine = true
            )

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull()?.let {
                    it > 0.0
                } == true
            ) {
                Text("Add Transaction")
            }
        }
    }
}

internal fun transactionAmountLabel(
    entry: LedgerEntryEntity
): String {
    val prefix = if (entry.type == EntryType.CREDIT) {
        "+"
    } else {
        "-"
    }

    return "$prefix₹%.2f".format(abs(entry.amount))
}

internal fun transactionDateTimeLabel(
    entry: LedgerEntryEntity
): String {
    return SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.ENGLISH
    ).format(
        Date(entry.createdAt)
    )
}

@Composable
private fun TransactionRow(
    entry: LedgerEntryEntity
) {
    val isCredit = entry.type == EntryType.CREDIT

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isCredit) {
                        "Credit"
                    } else {
                        "Debit"
                    },
                    color = if (isCredit) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontWeight = FontWeight.Bold
                )

                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = transactionDateTimeLabel(entry),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = transactionAmountLabel(entry),
                color = if (isCredit) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

