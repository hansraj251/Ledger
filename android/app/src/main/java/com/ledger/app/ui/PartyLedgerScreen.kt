package com.ledger.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun PartyLedgerScreen(
    party: PartyEntity,
    viewModel: LedgerViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var currentParty by remember {
        mutableStateOf(party)
    }

    var showEditPartyDialog by remember {
        mutableStateOf(false)
    }

    var showDeletePartyDialog by remember {
        mutableStateOf(false)
    }

    var editPartyName by remember {
        mutableStateOf(party.name)
    }

    var editPartyMobile by remember {
        mutableStateOf(party.mobile)
    }

    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    var selectedType by remember {
        mutableStateOf(EntryType.CREDIT)
    }

    LaunchedEffect(party.id) {
        viewModel.loadEntries(party.id)
    }

    BackHandler {
        onBack()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Box(
                    modifier = Modifier.statusBarsPadding()
                ) {
                    PartyHeader(
                        party = currentParty,
                        onBack = onBack,
                    onEdit = {
                        editPartyName = currentParty.name
                        editPartyMobile = currentParty.mobile
                        showEditPartyDialog = true
                    }
                )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(
                bottom = 28.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(
                    modifier = Modifier.height(12.dp)
                )
            }

            item {
                BalanceHero(
                    balance = uiState.balance
                )
            }

            item {
                TransactionComposer(
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

                        if (parsedAmount != null && parsedAmount > 0.0) {
                            scope.launch {
                                viewModel.addEntry(
                                    partyId = party.id,
                                    amount = parsedAmount,
                                    type = selectedType,
                                    note = note
                                )

                                if (viewModel.uiState.value.errorMessage.isEmpty()) {
                                    amount = ""
                                    note = ""
                                }
                            }
                        }
                    }
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 4.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Transaction history",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = if (uiState.entries.isEmpty()) {
                                "No activity recorded yet"
                            } else {
                                "${uiState.entries.size} transaction${if (uiState.entries.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Ledger",
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (uiState.entries.isEmpty()) {
                item {
                    EmptyTransactionState()
                }
            } else {
                items(
                    items = uiState.entries.asReversed(),
                    key = {
                        it.id
                    }
                ) { entry ->
                    TransactionCard(
                        entry = entry,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }

            if (uiState.errorMessage.isNotBlank()) {
                item {
                    Text(
                        text = uiState.errorMessage,
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (showEditPartyDialog) {
            EditPartyDialog(
                name = editPartyName,
                mobile = editPartyMobile,
                onNameChange = {
                    editPartyName = it
                },
                onMobileChange = {
                    editPartyMobile = it
                },
                onDismiss = {
                    showEditPartyDialog = false
                },
                onSave = {
                    scope.launch {
                        val saved = viewModel.updateParty(
                            party = currentParty,
                            name = editPartyName,
                            mobile = editPartyMobile
                        )

                        if (saved) {
                            currentParty = currentParty.copy(
                                name = editPartyName.trim(),
                                mobile = editPartyMobile.trim()
                            )

                            showEditPartyDialog = false
                        }
                    }
                },
                onDelete = {
                    showEditPartyDialog = false
                    showDeletePartyDialog = true
                }
            )
        }

        if (showDeletePartyDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeletePartyDialog = false
                },
                title = {
                    Text("Delete Party?")
                },
                text = {
                    Text(
                        "This will permanently delete ${currentParty.name} and all ledger entries for this party."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.deleteParty(currentParty)
                                showDeletePartyDialog = false
                                onBack()
                            }
                        }
                    ) {
                        Text(
                            text = "Delete",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeletePartyDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun PartyHeader(
    party: PartyEntity,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowBack,
                contentDescription = "Back"
            )
        }

        val initials = party.name
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }

        Surface(
            modifier = Modifier.size(44.dp),
            shape = androidx.compose.foundation.shape.CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials.ifBlank { "P" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(
            modifier = Modifier.width(12.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onEdit)
        ) {
            Text(
                text = party.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = party.mobile.ifBlank { "Customer / supplier" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditPartyDialog(
    name: String,
    mobile: String,
    onNameChange: (String) -> Unit,
    onMobileChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Party",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Update party details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Party name")
                    },
                    singleLine = true
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = onMobileChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Mobile number")
                    },
                    singleLine = true
                )

                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Delete Party",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(0.85f)
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = onSave,
                        modifier = Modifier
                            .weight(1.15f)
                            .height(52.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Save Changes",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalanceHero(
    balance: Double
) {
    val positive = balance > 0
    val negative = balance < 0

    val accent = when {
        positive -> MaterialTheme.colorScheme.tertiary
        negative -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    val title = when {
        positive -> "Receivable"
        negative -> "Payable"
        else -> "Settled"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(22.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = accent.copy(alpha = 0.12f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (negative) {
                                Icons.Outlined.Payments
                            } else {
                                Icons.Outlined.ReceiptLong
                            },
                            contentDescription = null,
                            tint = accent
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.size(12.dp)
                )

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )

                    Text(
                        text = "Current balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(19.dp)
            )

            Text(
                text = "₹%.2f".format(abs(balance)),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = when {
                    positive -> "Amount to receive from this party"
                    negative -> "Amount to pay to this party"
                    else -> "No outstanding balance"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TransactionComposer(
    amount: String,
    note: String,
    selectedType: EntryType,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTypeChange: (EntryType) -> Unit,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Payments,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(
                    modifier = Modifier.size(9.dp)
                )

                Column {
                    Text(
                        text = "New transaction",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "Record money received or paid",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedType == EntryType.CREDIT,
                    onClick = {
                        onTypeChange(EntryType.CREDIT)
                    },
                    label = {
                        Text("You Gave")
                    }
                )

                FilterChip(
                    selected = selectedType == EntryType.DEBIT,
                    onClick = {
                        onTypeChange(EntryType.DEBIT)
                    },
                    label = {
                        Text("You Got")
                    }
                )
            }

            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Amount")
                },
                prefix = {
                    Text("₹ ")
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
            )

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = {
                    Text("Note (optional)")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
            )

            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = amount.toDoubleOrNull()?.let {
                    it > 0.0
                } == true,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null
                )

                Spacer(
                    modifier = Modifier.size(8.dp)
                )

                Text(
                    text = if (selectedType == EntryType.CREDIT) {
                        "Add"
                    } else {
                        "Add"
                    }
                )
            }
        }
    }
}

@Composable
private fun TransactionCard(
    entry: LedgerEntryEntity,
    modifier: Modifier = Modifier
) {
    val isCredit = entry.type == EntryType.CREDIT

    val accent = if (isCredit) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(19.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = accent.copy(alpha = 0.11f)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCredit) {
                            Icons.Outlined.ReceiptLong
                        } else {
                            Icons.Outlined.Payments
                        },
                        contentDescription = null,
                        tint = accent
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(12.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isCredit) "You Gave" else "You Got",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (entry.note.isNotBlank()) {
                    Spacer(
                        modifier = Modifier.height(3.dp)
                    )

                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = transactionDateTimeLabel(entry),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = transactionAmountLabel(entry),
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EmptyTransactionState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(60.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Text(
                text = "No transactions yet",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(
                modifier = Modifier.height(5.dp)
            )

            Text(
                text = "Add the first credit or debit above.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
