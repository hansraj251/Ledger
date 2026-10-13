package com.ledger.app.ui

import com.ledger.app.data.CurrencySettings
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.app.data.PartyEntity
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: LedgerViewModel,
    onPartyClick: (PartyEntity) -> Unit,
    onProfileClick: () -> Unit,
    onReportsClick: () -> Unit,
    onDailyExpenseClick: () -> Unit,
    onGroupSharingClick: () -> Unit,
    onGroupClick: (Long) -> Unit,
    isPremium: Boolean,
    premiumPrice: String,
    onUpgradeClick: () -> Unit,

    onDataChanged: () -> Unit) {
    var profileName by rememberSaveable {
        mutableStateOf("")
    }

    var partyName by rememberSaveable {
        mutableStateOf("")
    }

    var partyMobile by rememberSaveable {
        mutableStateOf("")
    }

    var isSavingParty by rememberSaveable {
        mutableStateOf(false)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharingGroupDao = remember(context) {
        LedgerDatabaseProvider.get(context).sharingGroupDao()
    }
    var sharingGroups by remember {
        mutableStateOf(emptyList<com.ledger.app.data.SharingGroupEntity>())
    }
    var receivableInterestTotal by remember { mutableStateOf(0.0) }
    var payableInterestTotal by remember { mutableStateOf(0.0) }
    var groupReceivableTotal by remember { mutableStateOf(0.0) }
    var isReceivableExpanded by rememberSaveable { mutableStateOf(false) }
    var isPayableExpanded by rememberSaveable { mutableStateOf(false) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var isSavingGroup by remember { mutableStateOf(false) }
    var showGroupUpgradeDialog by remember { mutableStateOf(false) }

    val receivable = uiState.partyBalances.values
        .filter { it > 0.0 }
        .sum()

    val payable = uiState.partyBalances.values
        .filter { it < 0.0 }
        .sumOf { kotlin.math.abs(it) }

    val headerNameColor = if (payable > receivable) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    var showAddPartyDialog by remember {
        mutableStateOf(false)
    }

    var editingParty by remember { mutableStateOf<PartyEntity?>(null) }
    var editPartyName by remember { mutableStateOf("") }
    var editPartyMobile by remember { mutableStateOf("") }
    var isSavingEditedParty by remember { mutableStateOf(false) }
    var deletingEditedParty by remember { mutableStateOf<PartyEntity?>(null) }
    var showDeleteEditedPartyDialog by remember { mutableStateOf(false) }

    var showMenu by remember {
        mutableStateOf(false)
    }

    var showUpgradeDialog by remember { mutableStateOf(false) }
    val requestAddParty: () -> Unit = {
        if (isPremium || uiState.parties.size < 10) {
            showAddPartyDialog = true
        } else {
            showUpgradeDialog = true
        }
    }


    LaunchedEffect(Unit) {
        sharingGroups = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            sharingGroupDao.getAll()
        }
    }

    LaunchedEffect(context) {
        val totals = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val database = LedgerDatabaseProvider.get(context)
            val repository = LedgerRepository(database)
            val asOf = System.currentTimeMillis()

            val partyInterestPositions = repository.getParties().map { party ->
                com.ledger.app.data.calculateInterestAccountPosition(
                    transactions = repository.getEntries(party.id),
                    records = repository.getInterestAccountEntries(party.id),
                    asOf = asOf
                )
            }
            val interestTotal = partyInterestPositions.sumOf { it.receivable }
            val payableInterest = partyInterestPositions.sumOf { it.payable }

            val memberDao = database.sharingMemberDao()
            val expenseDao = database.groupExpenseDao()
            val splitDao = database.groupExpenseSplitDao()
            val paymentDao = database.groupExpensePaymentDao()
            val settlementDao = database.groupSettlementDao()

            val groupsTotal = database.sharingGroupDao().getAll().sumOf { group ->
                val members = memberDao.getByGroup(group.id)
                    .filterNot { it.name.trim().equals("You", ignoreCase = true) }
                val expenses = expenseDao.getByGroup(group.id)
                val splitsByExpense = expenses.associate { expense ->
                    expense.id to splitDao.getByExpense(expense.id)
                }
                val paymentsByExpense = expenses.associate { expense ->
                    expense.id to paymentDao.getByExpense(expense.id)
                }
                val settlements = settlementDao.getByGroup(group.id)

                members.sumOf { member ->
                    val spent = splitsByExpense.values.asSequence()
                        .flatten()
                        .filter { it.memberId == member.id }
                        .sumOf { it.amount }

                    val paid = paymentsByExpense.values.asSequence()
                        .flatten()
                        .filter { it.memberId == member.id }
                        .sumOf { it.amount }

                    val received = settlements.asSequence()
                        .filter { it.memberId == member.id }
                        .sumOf { it.amount }

                    (spent - paid - received).coerceAtLeast(0.0)
                }
            }

            Triple(interestTotal, payableInterest, groupsTotal)
        }

        receivableInterestTotal = totals.first
        payableInterestTotal = totals.second
        groupReceivableTotal = totals.third
    }

    LaunchedEffect(Unit) {
        viewModel.loadParties()

        val database = LedgerDatabaseProvider.get(
            context.applicationContext as android.app.Application
        )
        val repository = LedgerRepository(database)
        profileName = kotlinx.coroutines.withContext(
            kotlinx.coroutines.Dispatchers.IO
        ) {
            repository.getProfile()?.name?.trim().orEmpty()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        modifier = Modifier.weight(1f).height(52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        placeholder = {
                            Text("Search", fontSize = 14.sp, lineHeight = 16.sp)
                        }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp
                        ) {
                            IconButton(
                                onClick = {
                                    showMenu = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            },
                            modifier = Modifier
                                .width(250.dp)
                                .clip(androidx.compose.ui.graphics.RectangleShape)
                                .background(Color.White)
                                    .border(
                                        1.dp,
                                        androidx.compose.ui.graphics.Color(0xFFE7D7AE),
                                        androidx.compose.ui.graphics.RectangleShape
                                    )
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Profile & settings",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onProfileClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Reports",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Storage,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onReportsClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Add Party",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PersonAdd,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    requestAddParty()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Add Group",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Group,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    if (isPremium || sharingGroups.size < 4) {
                                        newGroupName = ""
                                        showCreateGroupDialog = true
                                    } else {
                                        showGroupUpgradeDialog = true
                                    }
                                }
                            )
                        }
}
                }
            }
        },
        bottomBar = {
            LedgerBottomNavigation(
                selectedTab = "parties",
                isPremium = isPremium,
                onPartiesClick = {},
                onExpensesClick = onDailyExpenseClick,
                onGroupSharingClick = onGroupSharingClick
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    requestAddParty()
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = if (isPremium) Color(0xFFFFC247)
                    else MaterialTheme.colorScheme.primary,
                contentColor = if (isPremium) Color(0xFF302000)
                    else MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = "Add party"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 18.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {



item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BalanceSummaryCard(
                        title = "Receivable",
                        amount = receivable,
                        modifier = Modifier.weight(1f),
                        amountColor = if (isPremium) Color(0xFF1B5E20)
                            else MaterialTheme.colorScheme.primary,
                        expanded = isReceivableExpanded,
                        onClick = { isReceivableExpanded = !isReceivableExpanded },
                        details = listOf(
                            "Receivable Int." to receivableInterestTotal,
                            "Group Receivable" to groupReceivableTotal
                        )
                    )

                    BalanceSummaryCard(
                        title = "Payable",
                        amount = payable,
                        modifier = Modifier.weight(1f),
                        amountColor = MaterialTheme.colorScheme.error,
                        expanded = isPayableExpanded,
                        onClick = { isPayableExpanded = !isPayableExpanded },
                        details = listOf(
                            "Payable Int." to payableInterestTotal
                        )
                    )
                }
            }

            item {

            }


            items(
                items = sharingGroups,
                key = { "sharing-group-${it.id}" }
            ) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group.id) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Group,
                                    contentDescription = "Group",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Group",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ArrowForwardIos,
                            contentDescription = "Open group",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    LoadingState()
                }
            } else if (uiState.filteredParties.isEmpty() && sharingGroups.isEmpty()) {
                item {
                    EmptyPartyState(
                        hasSearch = uiState.searchQuery.isNotBlank(),
                        onAddParty = {
                            requestAddParty()
                        }
                    )
                }
            } else {
                items(
                    items = uiState.filteredParties,
                    key = {
                        it.id
                    }
                ) { party ->
                    PartyCard(
                        party = party,
                        balance = uiState.partyBalances[party.id] ?: 0.0,
                        isPremium = isPremium,
                        onClick = {
                            onPartyClick(party)
                        },
                        onLongClick = {
                            editPartyName = party.name
                            editPartyMobile = party.mobile
                            editingParty = party
                        }
                    )
                }
            }
        }
    }

    editingParty?.let { targetParty ->
        EditPartyDialog(
            name = editPartyName,
            mobile = editPartyMobile,
            onNameChange = { editPartyName = it },
            onMobileChange = { if (it.length <= 15) editPartyMobile = it },
            onDismiss = { editingParty = null },
            onSave = {
                scope.launch {
                    val saved = viewModel.updateParty(
                        party = targetParty,
                        name = editPartyName,
                        mobile = editPartyMobile
                    )
                    if (saved) editingParty = null
                }
            },
            onDelete = {
                deletingEditedParty = targetParty
                editingParty = null
                showDeleteEditedPartyDialog = true
            }
        )
    }

    if (showDeleteEditedPartyDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteEditedPartyDialog = false },
            title = { Text("Delete Party?") },
            text = {
                Text(
                    "This will permanently delete ${deletingEditedParty?.name.orEmpty()} and all ledger entries for this party."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val targetParty = deletingEditedParty
                        if (targetParty != null) {
                            scope.launch {
                                viewModel.deleteParty(targetParty)
                                showDeleteEditedPartyDialog = false
                                deletingEditedParty = null
                            }
                        }
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteEditedPartyDialog = false
                        deletingEditedParty = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    val contactPickerContext = androidx.compose.ui.platform.LocalContext.current

    val contactPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val contactUri = result.data?.data
        if (result.resultCode == android.app.Activity.RESULT_OK && contactUri != null) {
            var selectedName = ""
            var selectedMobile = ""

            try {
                val projection = arrayOf(
                    android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                )

                contactPickerContext.contentResolver.query(
                    contactUri,
                    projection,
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        selectedName = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                            )
                        ).orEmpty().trim()

                        selectedMobile = cursor.getString(
                            cursor.getColumnIndexOrThrow(
                                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER
                            )
                        ).orEmpty().trim()
                    }
                }
            } catch (_: SecurityException) {
                android.widget.Toast.makeText(
                    contactPickerContext,
                    "Selected contact details could not be read. Please try again.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } catch (_: IllegalArgumentException) {
                android.widget.Toast.makeText(
                    contactPickerContext,
                    "Selected contact details are unavailable.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            when {
                selectedName.isNotBlank() -> {
                    partyName = selectedName
                    partyMobile = selectedMobile
                }
                selectedMobile.isNotBlank() -> {
                    partyName = selectedMobile
                    partyMobile = selectedMobile
                }
            }
        }
    }

    if (showAddPartyDialog) {
        Dialog(
            onDismissRequest = {
                if (!isSavingParty) {
                    showAddPartyDialog = false
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = androidx.compose.ui.graphics.Color.White,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Add Party",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "PARTY DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            contactPicker.launch(
                                android.content.Intent(
                                    android.content.Intent.ACTION_PICK,
                                    android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        enabled = !isSavingParty,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add from Contacts", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = partyName,
                        onValueChange = {
                            partyName = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSavingParty,
                        label = {
                            Text("Party name")
                        },
                        placeholder = {
                            Text("e.g. Sharma Traders")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = partyMobile,
                        onValueChange = {
                            if (it.length <= 15) {
                                partyMobile = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSavingParty,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                        label = {
                            Text("Mobile number")
                        },
                        placeholder = {
                            Text("Optional")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAddPartyDialog = false
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(52.dp),
                            enabled = !isSavingParty,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (partyName.isNotBlank()) {
                                    isSavingParty = true

                                    scope.launch {
                                        viewModel.addParty(
                                            partyName.trim(),
                                            partyMobile.trim()
                                        )

                                        partyName = ""
                                        partyMobile = ""
                                        isSavingParty = false
                                        showAddPartyDialog = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.15f)
                                .height(52.dp),
                            enabled = partyName.isNotBlank() && !isSavingParty,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSavingParty) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Create",
                                    maxLines = 1,
                                    softWrap = false,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }


    if (showGroupUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { showGroupUpgradeDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👑", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Unlock Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF171B3A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "The Free plan allows up to 4 groups. Upgrade to create unlimited groups.",
                        color = Color(0xFF55566A)
                    )
                    Surface(
                        color = Color(0xFFFFFBF1),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD782))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "✦  PREMIUM BENEFITS",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5A08)
                            )
                            Text("✓  Unlimited groups", color = Color(0xFF202238))
                            Text("✓  Unlimited group members", color = Color(0xFF202238))
                            Text("✓  Google Drive backup & sync", color = Color(0xFF202238))
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showGroupUpgradeDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Not now", maxLines = 1, softWrap = false)
                    }
                    androidx.compose.material3.Button(
                        onClick = {
                            showGroupUpgradeDialog = false
                            onUpgradeClick()
                        },
                        modifier = Modifier.weight(1.7f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 6.dp,
                            vertical = 10.dp
                        ),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC247),
                            contentColor = Color(0xFF302000)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Unlock Premium",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showCreateGroupDialog) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                if (!isSavingGroup) showCreateGroupDialog = false
            },
            shape = RoundedCornerShape(28.dp),
            title = {
                Text(
                    text = "Create Group",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSavingGroup,
                    label = { Text("Group name") },
                    placeholder = { Text("e.g. Trip, Flatmates") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Group, contentDescription = null)
                    },
                    shape = RoundedCornerShape(15.dp)
                )
            },
            confirmButton = {
                Button(
                    enabled = newGroupName.isNotBlank() && !isSavingGroup,
                    onClick = {
                        val name = newGroupName.trim()
                        if (name.isNotEmpty()) {
                            isSavingGroup = true
                            scope.launch {
val latestGroups =
                                    kotlinx.coroutines.withContext(
                                        kotlinx.coroutines.Dispatchers.IO
                                    ) {
                                        val existing = sharingGroupDao.getAll()
                                        if (!isPremium && existing.size >= 4) {
                                            null
                                        } else {
                                            sharingGroupDao.insert(
                                                com.ledger.app.data.SharingGroupEntity(name = name)
                                            )
                                            sharingGroupDao.getAll()
                                        }
                                    }
                                if (latestGroups == null) {
                                    isSavingGroup = false
                                    showGroupUpgradeDialog = true
                                } else {
                                    onDataChanged()
                                    sharingGroups = latestGroups
                                    newGroupName = ""
                                    isSavingGroup = false
                                    showCreateGroupDialog = false
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSavingGroup) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Create Group")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateGroupDialog = false },
                    enabled = !isSavingGroup
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUpgradeDialog) {
        androidx.compose.material3.AlertDialog(
            containerColor = androidx.compose.ui.graphics.Color.White,
            onDismissRequest = { showUpgradeDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    androidx.compose.material3.Text("👑", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.Text(
                        "Unlock Premium",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF171B3A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    androidx.compose.material3.Text(
                        "You've reached the Free plan limit of 10 parties.",
                        color = Color(0xFF55566A)
                    )
                    Surface(
                        color = Color(0xFFFFFBF1),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD782))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                "✦  PREMIUM BENEFITS",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5A08)
                            )
                            androidx.compose.material3.Text("✓  Unlimited parties & expenses", color = Color(0xFF202238))
                            androidx.compose.material3.Text("✓  Unlimited groups & members", color = Color(0xFF202238))
                            androidx.compose.material3.Text("✓  Google Drive backup & sync", color = Color(0xFF202238))
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showUpgradeDialog = false
                        onUpgradeClick()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFC247),
                        contentColor = Color(0xFF302000)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    androidx.compose.material3.Text(
                        if (premiumPrice.isBlank()) "Unlock Premium"
                        else "Unlock Premium · $premiumPrice"
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showUpgradeDialog = false }
                ) {
                    androidx.compose.material3.Text("Not now")
                }
            }
        )
    }

}

@Composable
private fun BalanceSummaryCard(
    title: String,
    amount: Double,
    modifier: Modifier = Modifier,
    amountColor: Color,
    expanded: Boolean = false,
    onClick: (() -> Unit)? = null,
    details: List<Pair<String, Double>> = emptyList()
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${CurrencySettings.symbol}${formatAmount(amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )

            if (expanded && details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))

                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                details.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${CurrencySettings.symbol}${formatAmount(value)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = amountColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyCard(
    party: PartyEntity,
    balance: Double,
    isPremium: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
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
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = party.name
                .trim()
                .split(" ")
                .filter {
                    it.isNotBlank()
                }
                .take(2)
                .joinToString("") {
                    it.first().uppercase()
                }

            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifBlank { "P" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = party.mobile.ifBlank { "No mobile number added" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = "${CurrencySettings.symbol}${formatAmount(kotlin.math.abs(balance))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) {
                    if (isPremium) Color(0xFF1B5E20)
                    else MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun EmptyPartyState(
    hasSearch: Boolean,
    onAddParty: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
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
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasSearch) {
                            Icons.Outlined.Search
                        } else {
                            Icons.Outlined.Group
                        },
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = if (hasSearch) {
                    "No matching parties"
                } else {
                    "Your ledger is empty"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = if (hasSearch) {
                    "Try another name or mobile number."
                } else {
                    "Add your first customer or supplier to get started."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasSearch) {
                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = onAddParty,
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.size(7.dp)
                    )

                    Text("Add party")
                }
            }
        }
    }
}

@Composable
private fun AddPartyDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    errorMessage: String
) {
    var name by remember {
        mutableStateOf("")
    }

    var mobile by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = "Add Party",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Create a customer or supplier ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Party name")
                    },
                    shape = RoundedCornerShape(15.dp)
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        mobile = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    label = {
                        Text("Mobile number")
                    },
                    shape = RoundedCornerShape(15.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, mobile)
                },
                enabled = name.trim().isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create party")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
