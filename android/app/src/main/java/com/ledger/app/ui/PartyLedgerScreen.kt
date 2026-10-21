package com.ledger.app.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import com.ledger.app.backup.GoogleDriveAuthorization
import com.ledger.app.backup.GoogleDriveTemporaryReport
import com.ledger.app.data.LedgerDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.material.icons.outlined.Message
import android.telephony.PhoneNumberUtils
import android.content.Intent
import android.net.Uri

import com.ledger.app.data.CurrencySettings

import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.outlined.DeleteOutline

import androidx.compose.foundation.BorderStroke
import com.ledger.app.data.calculateEntryTotal
import com.ledger.app.data.transactionInterestDays

import android.app.DatePickerDialog
import android.content.Context

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ledger.app.data.EntryType
import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import com.ledger.app.data.calculateInterestPosition
import com.ledger.app.data.calculateInterestAccountPosition
import com.ledger.app.data.calculateTransactionInterest
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private fun transactionBalanceMessage(balance: Double): String {
    val amount = "%.2f".format(Locale.getDefault(), abs(balance))
    return when {
        balance > 0.0 ->
            "Outstanding Balance — You Will Pay: ${CurrencySettings.symbol}$amount"
        balance < 0.0 ->
            "Outstanding Balance — You Will Get: ${CurrencySettings.symbol}$amount"
        else ->
            "Balance: Settled"
    }
}

@Composable
fun PartyLedgerScreen(
    party: PartyEntity,
    viewModel: LedgerViewModel,
    onBack: () -> Unit,
    isPremium: Boolean = false
,
    onUpgradeClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val visibilityPrefs = remember(context) {
        context.getSharedPreferences("ledger_party_ui_visibility", android.content.Context.MODE_PRIVATE)
    }
    var hideInterestButton by remember(party.id) { mutableStateOf(false) }
    var hideSellButton by remember(party.id) { mutableStateOf(false) }

    LaunchedEffect(party.id) {
        val storedParty = withContext(Dispatchers.IO) {
            val database = LedgerDatabaseProvider.get(context)
            val stored = database.partyDao().getById(party.id)
                ?: return@withContext null

            val intKey = "hide_int_${party.id}"
            val sellKey = "hide_sell_${party.id}"
            val hasLegacyInt = visibilityPrefs.contains(intKey)
            val hasLegacySell = visibilityPrefs.contains(sellKey)

            val migrated = stored.copy(
                hideInterestButton = if (hasLegacyInt)
                    visibilityPrefs.getBoolean(intKey, false)
                else stored.hideInterestButton,
                hideSellButton = if (hasLegacySell)
                    visibilityPrefs.getBoolean(sellKey, false)
                else stored.hideSellButton
            )

            if (migrated != stored) {
                database.partyDao().update(migrated)
            }

            if (hasLegacyInt || hasLegacySell) {
                visibilityPrefs.edit()
                    .remove(intKey)
                    .remove(sellKey)
                    .apply()
            }
            migrated
        }

        storedParty?.let {
            hideInterestButton = it.hideInterestButton
            hideSellButton = it.hideSellButton
        }
    }


    // Net interest for the party header SMS reminder.
    val headerGaveTransactionInterest = uiState.entries
        .filter { it.type == EntryType.CREDIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }
    val headerGotTransactionInterest = uiState.entries
        .filter { it.type == EntryType.DEBIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }
    val headerRecordedInterest = uiState.interestAccountEntries.sumOf {
        if (it.type == "GAVE") it.amount else -it.amount
    }
    val headerNetInterest =
        headerGaveTransactionInterest - headerGotTransactionInterest + headerRecordedInterest


    var transactionDate by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var interestRateText by remember {
        mutableStateOf("")
    }
    val interestRate = interestRateText.toDoubleOrNull()
        ?.takeIf { it in 0.0..100.0 } ?: 0.0

    var showInterestInput by remember {
        mutableStateOf(false)
    }


    var currentParty by remember {
        mutableStateOf(party)
    }

    var showEditPartyDialog by remember {
        mutableStateOf(false)
    }

    var showReport by remember {
        mutableStateOf(false)
    }


    var showInterestAccount by remember { mutableStateOf(false) }
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

    var editingEntry by remember {
        mutableStateOf<LedgerEntryEntity?>(null)
    }

    var showDeleteEntryDialog by remember {
        mutableStateOf(false)
    }

    var pendingTransactionSms by remember {
        mutableStateOf<Pair<String, String>?>(null)
    }

    val smsPreferences = remember(context) {
        context.getSharedPreferences("ledger_ui_preferences", android.content.Context.MODE_PRIVATE)
    }

    var askSmsAfterTransaction by remember(party.id) {
        mutableStateOf(
            smsPreferences.getBoolean("ask_sms_after_transaction_${party.id}", false)
        )
    }


    var isSharingPartyReport by remember(party.id) {
        mutableStateOf(false)
    }
    val reportDriveAuthorization = remember(context) {
        GoogleDriveAuthorization(context)
    }

    val uploadPartyReport: (String) -> Unit = { accessToken ->
        if (!isSharingPartyReport) {
            isSharingPartyReport = true
            scope.launch {
                try {
                    val reportLink = withContext(Dispatchers.IO) {
                        val db = LedgerDatabaseProvider.get(context)
                        val profileName = db.profileDao()
                            .getProfile()?.name?.trim().orEmpty()

                        val report = createPartyReportPdf(
                            context = context,
                            party = currentParty,
                            entries = uiState.entries,
                            interestRecords = uiState.interestAccountEntries,
                            profileName = profileName
                        )
                        try {
                            GoogleDriveTemporaryReport().uploadAndShare(
                                source = report,
                                partyName = currentParty.name,
                                accessToken = accessToken
                            )
                        } finally {
                            report.delete()
                        }
                    }

                    val phone = currentParty.mobile.trim()
                    val amountText =
                        "${CurrencySettings.symbol}${formatAmount(kotlin.math.abs(uiState.balance))}"
                    val interestText = if (headerNetInterest > 0.0) {
                        " Additional interest pending: " +
                            "${CurrencySettings.symbol}${formatAmount(headerNetInterest)}."
                    } else {
                        ""
                    }
                    val message =
                        "Dear ${currentParty.name}, outstanding balance: $amountText." +
                            interestText +
                            "\nView your party ledger report (link available for 24 hours):\n" +
                            reportLink

                    context.startActivity(
                        Intent(
                            Intent.ACTION_SENDTO,
                            Uri.parse("smsto:" + Uri.encode(phone))
                        ).putExtra("sms_body", message)
                    )
                    Toast.makeText(
                        context,
                        "Report uploaded. Link added to SMS; it expires after 24 hours.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (error: Exception) {
                    Toast.makeText(
                        context,
                        "Report upload failed: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    isSharingPartyReport = false
                }
            }
        }
    }

    val reportAuthorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        if (activityResult.resultCode == Activity.RESULT_OK) {
            reportDriveAuthorization.authorize()
                .addOnSuccessListener { result ->
                    val token = result.accessToken
                    if (!result.hasResolution() && !token.isNullOrBlank()) {
                        uploadPartyReport(token)
                    } else {
                        Toast.makeText(
                            context,
                            "Google Drive authorization is still required. Try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        context,
                        "Google Drive authorization failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        } else {
            Toast.makeText(
                context,
                "Google Drive authorization cancelled.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val requestPartyReport: () -> Unit = {
        if (!isSharingPartyReport) {
            reportDriveAuthorization.authorize()
                .addOnSuccessListener { result ->
                    val token = result.accessToken
                    if (result.hasResolution()) {
                        val sender = result.pendingIntent?.intentSender
                        if (sender != null) {
                            reportAuthorizationLauncher.launch(
                                IntentSenderRequest.Builder(sender).build()
                            )
                        } else {
                            Toast.makeText(
                                context,
                                "Google Drive authorization could not be opened.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else if (!token.isNullOrBlank()) {
                        uploadPartyReport(token)
                    } else {
                        Toast.makeText(
                            context,
                            "Google Drive access token is unavailable.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(
                        context,
                        "Google Drive authorization failed: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    LaunchedEffect(party.id) {
        viewModel.loadEntries(party.id)
        viewModel.loadInterestAccountEntries(party.id)
    }

    BackHandler {
        if (showInterestAccount) {
            showInterestAccount = false
        } else {
            onBack()
        }
    }

    if (showReport) {
        PartyReportScreen(
            party = currentParty,
            entries = uiState.entries,
            records = uiState.interestAccountEntries,
            onBack = {
                showReport = false
            }
        )
        return
    }

    if (showInterestAccount) {
        InterestAccountScreen(
            party = currentParty,
            transactions = uiState.entries,
            records = uiState.interestAccountEntries,
            errorMessage = uiState.errorMessage,
            onBack = { showInterestAccount = false },
            onAdd = { value, type, recordNote, date ->
                scope.launch {
                    viewModel.addInterestAccountEntry(
                        partyId = currentParty.id,
                        amount = value,
                        type = type,
                        note = recordNote,
                        transactionDate = date
                    )
                }
            },
            onEdit = { recordId, value, type, recordNote, date ->
                scope.launch {
                    viewModel.updateInterestAccountEntry(
                        partyId = currentParty.id,
                        entryId = recordId,
                        amount = value,
                        type = type,
                        note = recordNote,
                        transactionDate = date
                    )
                }
            },
            onDelete = { recordId ->
                scope.launch {
                    viewModel.deleteInterestAccountEntry(
                        partyId = currentParty.id,
                        entryId = recordId
                    )
                }
            }
        )
        return
    }

    
    val transactionLimitContext = androidx.compose.ui.platform.LocalContext.current
    var showTransactionLimitDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var transactionLimitMessage by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }

    if (showTransactionLimitDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTransactionLimitDialog = false },
            title = { androidx.compose.material3.Text("Free Plan Limit Reached") },
            text = { androidx.compose.material3.Text(transactionLimitMessage) },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        showTransactionLimitDialog = false
                        onUpgradeClick()
                    }
                ) {
                    androidx.compose.material3.Text("Upgrade to Premium")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showTransactionLimitDialog = false }
                ) {
                    androidx.compose.material3.Text("Not now")
                }
            }
        )
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
                        netInterest = headerNetInterest,
                        balance = uiState.balance,
                        onBack = onBack,
                    onEdit = {
                        editPartyName = currentParty.name
                        editPartyMobile = currentParty.mobile
                        showEditPartyDialog = true
                    },
                    onReport = {
                        showReport = true
                    },
                        onSendReport = { requestPartyReport() },
                    hideInterestButton = hideInterestButton,
                    hideSellButton = hideSellButton,
                    onHideInterestButtonChange = { enabled ->
                        hideInterestButton = enabled
                        scope.launch {
                            viewModel.updatePartyVisibility(
                                partyId = currentParty.id,
                                hideInterestButton = enabled,
                                hideSellButton = hideSellButton
                            )
                        }
                        if (enabled) showInterestInput = false
                    },
                    onHideSellButtonChange = { enabled ->
                        hideSellButton = enabled
                        scope.launch {
                            viewModel.updatePartyVisibility(
                                partyId = currentParty.id,
                                hideInterestButton = hideInterestButton,
                                hideSellButton = enabled
                            )
                        }
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
                val interestPosition = calculateInterestAccountPosition(uiState.entries, uiState.interestAccountEntries)

            val gaveTransactionInterest = uiState.entries
                .filter { it.type == EntryType.CREDIT }
                .sumOf {
                    calculateTransactionInterest(
                        amount = it.amount,
                        annualRate = it.interestRate,
                        transactionDate = it.transactionDate,
                        createdAt = it.createdAt
                    )
                }
            val gotTransactionInterest = uiState.entries
                .filter { it.type == EntryType.DEBIT }
                .sumOf {
                    calculateTransactionInterest(
                        amount = it.amount,
                        annualRate = it.interestRate,
                        transactionDate = it.transactionDate,
                        createdAt = it.createdAt
                    )
                }
            val totalRecordedInterest = uiState.interestAccountEntries.sumOf {
                if (it.type == "GAVE") it.amount else -it.amount
            }
            val netInterest =
                gaveTransactionInterest - gotTransactionInterest + totalRecordedInterest

                            BalanceHero(
                    party = currentParty,
                    balance = uiState.balance,
                    isPremium = isPremium,
                    netInterest = netInterest,
                    receivableInterest = interestPosition.receivable,
                    payableInterest = interestPosition.payable,
                    hasInterest = uiState.entries.any { it.interestRate > 0.0 } ||
                        uiState.interestAccountEntries.isNotEmpty(),
                    onInterestClick = { showInterestAccount = true },
                    isSharingReport = isSharingPartyReport,
                    onSendReport = { requestPartyReport() }
                )
            }

            item {
                TransactionComposer(
                    isPremium = isPremium,
                    partyId = party.id.toLong(),
                    amount = amount,
                    note = note,
                    transactionDate = transactionDate,
                    interestRate = interestRate,
                    interestRateText = interestRateText,
                    showInterestInput = showInterestInput,
                    hideInterestButton = hideInterestButton,
                    hideSellButton = hideSellButton,
                    onInterestClick = {
                        showInterestInput = !showInterestInput
                    },
                    onInterestRateChange = {
                        interestRateText = it
                    },
                    selectedType = selectedType,
                    onAmountChange = {
                        amount = it
                    },
                    onNoteChange = {
                        note = it
                    },
                    onTransactionDateChange = {
                        transactionDate = it
                    },
                    onTypeChange = {
                        selectedType = it
                    },
                    smsEnabled = askSmsAfterTransaction,
                    onSmsEnabledChange = { enabled ->
                        askSmsAfterTransaction = enabled
                        smsPreferences.edit()
                            .putBoolean("ask_sms_after_transaction_${party.id}", enabled)
                            .apply()
                    },
                    onAdd = {
                        val parsedAmount = amount.toDoubleOrNull()

                        if (parsedAmount != null && parsedAmount > 0.0) {
                            val transactionType = selectedType
                            val transactionNote = note.trim()
                            val transactionTimestamp = transactionDate
                            val transactionRate = interestRate

                            scope.launch {
                                if (!isPremium) {
                    val limitMessage = kotlinx.coroutines.withContext(
                        kotlinx.coroutines.Dispatchers.IO
                    ) {
                        val dao = com.ledger.app.data.LedgerDatabaseProvider
                            .get(transactionLimitContext).ledgerEntryDao()
                        when {
                            dao.count() >= 100 ->
                                "The Free plan allows up to 100 transactions across all parties. Upgrade to Premium for unlimited transactions."
                            dao.countByPartyId(party.id.toLong()) >= 25 ->
                                "The Free plan allows up to 25 transactions per party. Upgrade to Premium for unlimited transactions."
                            else -> null
                        }
                    }
                    if (limitMessage != null) {
                        transactionLimitMessage = limitMessage
                        showTransactionLimitDialog = true
                        return@launch
                    }
                }

                val succeeded = viewModel.addEntry(
                                    partyId = party.id,
                                    amount = parsedAmount,
                                    type = transactionType,
                                    note = transactionNote,
                                    transactionDate = transactionTimestamp,
                                    interestRate = transactionRate
                                )

                                if (succeeded) {
                                    amount = ""
                                    note = ""
                                    interestRateText = ""
                                    showInterestInput = false

                                    val phone = currentParty.mobile.trim()
                                    if (askSmsAfterTransaction && phone.isNotBlank()) {
                                        val typeLabel =
                                            if (transactionType == EntryType.CREDIT) "You Got"
                                            else "You Gave"
                                        val amountText = "%.2f".format(
                                            Locale.getDefault(), parsedAmount
                                        )
                                        val noteText = transactionNote
                                            .takeIf { it.isNotBlank() }
                                            ?.let { "\nNote: $it" } ?: ""

                                        val message =
                                            "Hello ${currentParty.name}, a transaction has been added to your ledger.\n" +
                                            "Type: $typeLabel\n" +
                                            "Amount: ${CurrencySettings.symbol}$amountText$noteText"

                                        pendingTransactionSms = Pair(
                                            phone,
                                            "$message\n${transactionBalanceMessage(viewModel.uiState.value.balance)}"
                                        )
                                    }
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
                            text = if (uiState.entries.isEmpty()) {
                                "No activity recorded yet"
                            } else {
                                "${uiState.entries.size} transaction${if (uiState.entries.size == 1) "" else "s"}"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .clickable {
                                editingEntry = entry
                            }
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


        editingEntry?.let { entry ->
            EditTransactionDialog(
                entry = entry,
                onDismiss = {
                    editingEntry = null
                },
                onDelete = {
                    showDeleteEntryDialog = true
                },
                onSave = { newAmount, newType, newNote, newTransactionDate, newInterestRate ->
                    scope.launch {
                        val succeeded = viewModel.updateEntry(
                            entry = entry,
                            amount = newAmount,
                            type = newType,
                            note = newNote,
                            transactionDate = newTransactionDate,
                            interestRate = newInterestRate
                        )

                        if (succeeded) {
                            editingEntry = null

                            val phone = currentParty.mobile.trim()
                            if (askSmsAfterTransaction && phone.isNotBlank()) {
                                val oldTypeLabel =
                                    if (entry.type == EntryType.CREDIT) "You Got"
                                    else "You Gave"
                                val newTypeLabel =
                                    if (newType == EntryType.CREDIT) "You Got"
                                    else "You Gave"
                                val oldAmountText = "%.2f".format(
                                    Locale.getDefault(), entry.amount
                                )
                                val newAmountText = "%.2f".format(
                                    Locale.getDefault(), newAmount
                                )
                                val noteText = newNote.trim()
                                    .takeIf { it.isNotBlank() }
                                    ?.let { "\nNote: $it" } ?: ""

                                val message =
                                    "Hello ${currentParty.name}, a ledger transaction has been updated.\n" +
                                    "Previous: $oldTypeLabel ${CurrencySettings.symbol}$oldAmountText\n" +
                                    "Updated: $newTypeLabel ${CurrencySettings.symbol}$newAmountText$noteText"

                                pendingTransactionSms = Pair(
                                            phone,
                                            "$message\n${transactionBalanceMessage(viewModel.uiState.value.balance)}"
                                        )
                            }
                        }
                    }
                }
            )
        }

        if (showDeleteEntryDialog) {
            val entry = editingEntry

            if (entry != null) {
                AlertDialog(
                    onDismissRequest = {
                        showDeleteEntryDialog = false
                    },
                                containerColor = androidx.compose.ui.graphics.Color.White,
title = {
                        Text("Delete Transaction?")
                    },
                    text = {
                        Text(
                            "This transaction will be permanently deleted."
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    val succeeded = viewModel.deleteEntry(entry)
                                    showDeleteEntryDialog = false
                                    editingEntry = null

                                    if (succeeded) {
                                        val phone = currentParty.mobile.trim()
                                        if (askSmsAfterTransaction && phone.isNotBlank()) {
                                            val typeLabel =
                                                if (entry.type == EntryType.CREDIT) "You Got"
                                                else "You Gave"
                                            val amountText = "%.2f".format(
                                                Locale.getDefault(), entry.amount
                                            )
                                            val message =
                                                "Hello ${currentParty.name}, a ledger transaction has been deleted.\n" +
                                                "Deleted transaction: $typeLabel ${CurrencySettings.symbol}$amountText\n" +
                                                ""

                                            pendingTransactionSms = Pair(
                                            phone,
                                            "$message\n${transactionBalanceMessage(viewModel.uiState.value.balance)}"
                                        )
                                        }
                                    }
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
                                showDeleteEntryDialog = false
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }

        pendingTransactionSms?.let { (phone, message) ->
            AlertDialog(
                onDismissRequest = {
                    pendingTransactionSms = null
                },
                title = {
                    Text("Send transaction message?")
                },
                text = {
                    Text(
                        "Transaction saved successfully. Would you like to open SMS " +
                            "with a message for ${currentParty.name}?"
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:" + Uri.encode(phone))
                                putExtra("sms_body", message)
                            }

                            try {
                                context.startActivity(smsIntent)
                                pendingTransactionSms = null
                            } catch (exception: Exception) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Unable to open SMS app",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                                pendingTransactionSms = null
                            }
                        }
                    ) {
                        Text("Send Message")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            pendingTransactionSms = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeletePartyDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDeletePartyDialog = false
                },
                            containerColor = androidx.compose.ui.graphics.Color.White,
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
    netInterest: Double,
    balance: Double,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onReport: () -> Unit,
    onSendReport: () -> Unit,
    hideInterestButton: Boolean,
    hideSellButton: Boolean,
    onHideInterestButtonChange: (Boolean) -> Unit,
    onHideSellButtonChange: (Boolean) -> Unit) {
    var showMenu by remember {
        mutableStateOf(false)
    }
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
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Text(
                text = party.mobile.ifBlank { "Customer / supplier" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val context = LocalContext.current

        Box {
            IconButton(
                onClick = { showMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,

                    contentDescription = "More options"
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier
                    .clip(androidx.compose.ui.graphics.RectangleShape)
                    .background(Color.White)
                    .border(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0xFFE7D7AE),
                        androidx.compose.ui.graphics.RectangleShape
                    )
                    .clip(androidx.compose.ui.graphics.RectangleShape)
            ) {
                DropdownMenuItem(
                    text = { Text("Call") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Call,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        val phone = party.mobile.trim()
                        if (phone.isNotBlank()) {
                            val intent = Intent(
                                Intent.ACTION_DIAL,
                                Uri.parse("tel:" + Uri.encode(phone))
                            )
                            context.startActivity(intent)
                        }
                    },
                    enabled = party.mobile.isNotBlank()
                )
                DropdownMenuItem(
                    text = { Text("Message") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Message,
                            contentDescription = null
                        )
                    },
                    onClick = {
                    showMenu = false
                    onSendReport()
                },
                    enabled = party.mobile.isNotBlank() && balance > 0.0
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = hideInterestButton,
                                onCheckedChange = null
                            )
                            Text("Hide Int.")
                        }
                    },
                    onClick = { onHideInterestButtonChange(!hideInterestButton) }
                )
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.Checkbox(
                                checked = hideSellButton,
                                onCheckedChange = null
                            )
                            Text("Hide Sell")
                        }
                    },
                    onClick = { onHideSellButtonChange(!hideSellButton) }
                )
                DropdownMenuItem(
                    text = {
                        Text("Reports")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        showMenu = false
                        onReport()
                    }
                )
            }
        }
    }
}

@Composable
private fun EditTransactionDialog(
    entry: LedgerEntryEntity,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (Double, EntryType, String, Long, Double) -> Unit
) {
    val editDialogContext = LocalContext.current
    var hideInterestButton by remember(entry.partyId) { mutableStateOf(false) }
    LaunchedEffect(entry.partyId) {
        hideInterestButton = withContext(Dispatchers.IO) {
            LedgerDatabaseProvider.get(editDialogContext)
                .partyDao()
                .getById(entry.partyId)
                ?.hideInterestButton ?: false
        }
    }

    var amount by remember(entry.id) {
        mutableStateOf(entry.amount.toString())
    }

    var note by remember(entry.id) {
        mutableStateOf(entry.note)
    }

    var selectedType by remember(entry.id) {
        mutableStateOf(entry.type)
    }

    var interestRate by remember(entry.id) {
        mutableStateOf(entry.interestRate)
    }

    var showInterestInput by remember(entry.id) {
        mutableStateOf(entry.interestRate > 0.0)
    }
    var interestRateText by remember(entry.id) {
        mutableStateOf(
            if (entry.interestRate > 0.0) formatInterestRate(entry.interestRate) else ""
        )
    }

    var transactionDate by remember(entry.id) {
        mutableStateOf(
            if (entry.transactionDate > 0L) {
                entry.transactionDate
            } else {
                entry.createdAt
            }
        )
    }

    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Update transaction details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        modifier = Modifier.weight(if (showInterestInput) 1.15f else 1f),
                        singleLine = true,
                        label = { Text("Amount") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        prefix = { Text("${CurrencySettings.symbol} ") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                    )

                    if (showInterestInput) {
                        OutlinedTextField(
                            value = interestRateText,
                            onValueChange = { value ->
                                val normalized = value.replace(",", ".")
                                val dotIndex = normalized.indexOf('.')
                                val valid = normalized.isEmpty() ||
                                    (
                                        normalized.count { it == '.' } <= 1 &&
                                        normalized.all { it.isDigit() || it == '.' } &&
                                        (dotIndex == -1 ||
                                            normalized.length - dotIndex - 1 <= 2) &&
                                        (dotIndex == -1 || dotIndex <= 3) &&
                                        (normalized.toDoubleOrNull() ?: 0.0) <= 100.0
                                    )
                                if (valid) {
                                    interestRateText = normalized
                                    interestRate = normalized.toDoubleOrNull() ?: 0.0
                                }
                            },
                            modifier = Modifier.weight(0.85f),
                            singleLine = true,
                            label = { Text("Rate (%)") },
                            suffix = { Text("%") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedType == EntryType.CREDIT,
                        onClick = {
                            selectedType = EntryType.CREDIT
                        },
                        label = {
                            Text("You Gave")
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
                            labelColor = androidx.compose.ui.graphics.Color(0xFF475569),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == EntryType.CREDIT,
                            borderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    FilterChip(
                        selected = selectedType == EntryType.DEBIT,
                        onClick = {
                            selectedType = EntryType.DEBIT
                        },
                        label = {
                            Text("You Got")
                        },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFF1F5F9),
                            labelColor = androidx.compose.ui.graphics.Color(0xFF475569),
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = androidx.compose.material3.FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedType == EntryType.DEBIT,
                            borderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1),
                            selectedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    if (!hideInterestButton) {
FilterChip(
                        selected = interestRate > 0.0,
                        onClick = { showInterestInput = !showInterestInput },
                        label = {
                            Text(
                                if (interestRate > 0.0) {
                                    "Int. ${formatInterestRate(interestRate)}%"
                                } else {
                                    "Int."
                                }
                            )
                        }
                    )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = formatTransactionDate(transactionDate),
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = true,
                        singleLine = true,
                        label = {
                            Text("Date")
                        },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                indication = null,
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) {
                                showTransactionDatePicker(
                                    context = context,
                                    initialDate = transactionDate,
                                    onDateSelected = { transactionDate = it }
                                )
                            }
                    )
                }


                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.weight(1.15f),
                    singleLine = true,
                    label = { Text("Note") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null
                        )
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                )
                }



                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "Delete Transaction",
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
                            .height(52.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amount.toDoubleOrNull()

                            if (parsedAmount != null && parsedAmount > 0.0) {
                                onSave(
                                    parsedAmount,
                                    selectedType,
                                    note.trim(),
                                    transactionDate,
                                    interestRate
                                )
                            }
                        },
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
fun EditPartyDialog(
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
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(26.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp
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
                    label = { Text("Party name") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = onMobileChange,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),
                    label = { Text("Mobile number") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.size(8.dp))
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
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp)
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
                        shape = RoundedCornerShape(16.dp)
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
    party: PartyEntity,
    balance: Double,
    isPremium: Boolean,
    netInterest: Double,
    hasInterest: Boolean,
    receivableInterest: Double,
    payableInterest: Double,
    onInterestClick: () -> Unit,
    onSendReport: () -> Unit,
    isSharingReport: Boolean
) {
    val context = LocalContext.current
    val positive = balance > 0
    val negative = balance < 0

    val accent = when {
        positive -> MaterialTheme.colorScheme.error
        negative -> MaterialTheme.colorScheme.tertiary
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

                Spacer(modifier = Modifier.weight(1f))

                if (hasInterest) {
                    Surface(
                        modifier = Modifier.clickable(onClick = onInterestClick),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 13.dp,
                                vertical = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Int.",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }


            }

            Spacer(
                modifier = Modifier.height(19.dp)
            )

            Text(
                text = "${CurrencySettings.symbol}${formatAmount(abs(balance))}",
                style = MaterialTheme.typography.displaySmall,
                color = accent,
                fontWeight = FontWeight.Bold
            )


if (kotlin.math.abs(netInterest) > 0.000001) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (netInterest > 0.0) "Receivable Int." else "Payable Int.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (netInterest > 0.0) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                    Text(
                        text = "${CurrencySettings.symbol}${formatAmount(kotlin.math.abs(netInterest))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
Spacer(
                modifier = Modifier.height(2.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val phone = party.mobile.trim()
                        if (phone.isNotBlank()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:" + Uri.encode(phone))
                                )
                            )
                        }
                    },
                    enabled = party.mobile.isNotBlank(),
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Call,
                        contentDescription = "Call party",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Call", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onSendReport,
                    enabled = balance > 0.0 &&
                        party.mobile.isNotBlank() &&
                        !isSharingReport,
                    modifier = Modifier.weight(1f).height(34.dp),
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Message,
                        contentDescription = "Send party report link",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        if (isSharingReport) "Preparing..." else "Message",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }

        }
    }
}@Composable
private fun InterestAccountScreen(
    party: PartyEntity,
    transactions: List<LedgerEntryEntity>,
    records: List<InterestAccountEntryEntity>,
    errorMessage: String,
    onBack: () -> Unit,
    onAdd: (Double, String, String, Long) -> Unit,
    onEdit: (Long, Double, String, String, Long) -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val position = calculateInterestAccountPosition(transactions, records)
    val totalPaidInterest = records.sumOf {
        if (it.type == "GAVE") it.amount else -it.amount
    }
    val totalPaidInterestLabel = if (totalPaidInterest >= 0.0) {
        "Total Int. Paid"
    } else {
        "Total Int. Received"
    }
    val totalPaidInterestDisplay = kotlin.math.abs(totalPaidInterest)

    val gaveTransactionInterest = transactions
        .filter { it.type == EntryType.CREDIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val gotTransactionInterest = transactions
        .filter { it.type == EntryType.DEBIT }
        .sumOf {
            calculateTransactionInterest(
                amount = it.amount,
                annualRate = it.interestRate,
                transactionDate = it.transactionDate,
                createdAt = it.createdAt
            )
        }

    val totalInterest = gaveTransactionInterest - gotTransactionInterest
    val totalInterestLabel = if (totalInterest >= 0.0) {
        "Total Receivable Int."
    } else {
        "Total Payable Int."
    }
    val totalInterestDisplay = kotlin.math.abs(totalInterest)

    val totalReceivableInterest = totalInterest + totalPaidInterest
    val receivableInterestLabel = if (totalReceivableInterest >= 0.0) {
        "Receivable Int."
    } else {
        "Payable Int."
    }
    val receivableInterestDisplay = kotlin.math.abs(totalReceivableInterest)
    val totalPayableInterest = position.payable + gotTransactionInterest
    var editingRecordId by remember { mutableStateOf<Long?>(null) }
    var recordToDelete by remember { mutableStateOf<InterestAccountEntryEntity?>(null) }
    val validationRecords = records.filterNot { it.id == editingRecordId }
    val validationPosition = calculateInterestAccountPosition(transactions, validationRecords)
    var selectedType by remember { mutableStateOf("GAVE") }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var recordDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var validationError by remember { mutableStateOf("") }

    val validationPaidInterest = validationRecords.sumOf {
        if (it.type == "GAVE") it.amount else -it.amount
    }

    // Match the third card: Receivable Int. / Payable Int.
    val validationNetInterest = totalInterest + validationPaidInterest

    val pendingLimit = if (selectedType == "GAVE") {
        maxOf(0.0, -validationNetInterest)
    } else {
        maxOf(0.0, validationNetInterest)
    }
    val maxRecordableAmount =
        kotlin.math.floor(pendingLimit * 100.0 + 0.000001) / 100.0
    val formattedMaxAmount = String.format(
        java.util.Locale.US, "%.2f", maxRecordableAmount
    )
    val enteredAmount = amountText.trim().toDoubleOrNull()
    val exceedsPending = enteredAmount != null &&
        enteredAmount > maxRecordableAmount + 1e-9


    recordToDelete?.let { record ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { recordToDelete = null },
                        containerColor = androidx.compose.ui.graphics.Color.White,
title = { Text("Delete interest record?") },
            text = {
                Text(
                    "Are you sure you want to delete this " +
                        "${if (record.type == "GAVE") "You Gave" else "You Got"} " +
                        "interest record of ${CurrencySettings.symbol}${formatAmount(record.amount)}? " +
                        "This action cannot be undone."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        onDelete(record.id)
                        if (editingRecordId == record.id) {
                            editingRecordId = null
                            amountText = ""
                            noteText = ""
                            validationError = ""
                        }
                        recordToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { recordToDelete = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }


    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.White,
        topBar = {
            Surface(
                color = androidx.compose.ui.graphics.Color.White,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Interest Account",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            party.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(totalInterestLabel, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${CurrencySettings.symbol}${formatAmount(totalInterestDisplay)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(totalPaidInterestLabel, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${CurrencySettings.symbol}${formatAmount(totalPaidInterestDisplay)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(receivableInterestLabel, style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${CurrencySettings.symbol}${formatAmount(receivableInterestDisplay)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            if (editingRecordId == null) "Add Interest Record" else "Edit Interest Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = selectedType == "GAVE",
                                onClick = {
                                    selectedType = "GAVE"
                                    validationError = ""
                                },
                                label = { Text("Int. You Gave") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedType == "GOT",
                                onClick = {
                                    selectedType = "GOT"
                                    validationError = ""
                                },
                                label = { Text("Int. You Got") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = amountText,
                            onValueChange = {
                                amountText = it
                                validationError = ""
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                            label = { Text("Interest amount (${CurrencySettings.symbol})") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
                            isError = exceedsPending,
                            supportingText = {
                                Text("Maximum allowed: ${CurrencySettings.symbol}$formattedMaxAmount")
                            }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = SimpleDateFormat(
                                        "dd MMM yyyy",
                                        Locale.getDefault()
                                    ).format(Date(recordDate)),
                                    onValueChange = {},
                                    modifier = Modifier.fillMaxWidth(),
                                    readOnly = true,
                                    singleLine = true,
                                    label = { Text("Date") },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable(
                                            indication = null,
                                            interactionSource = remember {
                                                androidx.compose.foundation.interaction.MutableInteractionSource()
                                            }
                                        ) {
                                            val cal = Calendar.getInstance().apply {
                                                timeInMillis = recordDate
                                            }
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, day ->
                                                    recordDate = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, year)
                                                        set(Calendar.MONTH, month)
                                                        set(Calendar.DAY_OF_MONTH, day)
                                                        set(Calendar.HOUR_OF_DAY, 12)
                                                        set(Calendar.MINUTE, 0)
                                                        set(Calendar.SECOND, 0)
                                                        set(Calendar.MILLISECOND, 0)
                                                    }.timeInMillis
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                )
                            }

                            OutlinedTextField(
                                value = noteText,
                                onValueChange = { noteText = it },
                                modifier = Modifier.weight(1.15f),
                                singleLine = true,
                                label = { Text("Note") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = null
                                    )
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                            )
                        }
                        val visibleError = validationError.ifBlank { errorMessage }
                        if (visibleError.isNotBlank()) {
                            Text(
                                visibleError,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                            onClick = {
                                val value = amountText.trim().toDoubleOrNull()
                                when {
                                    value == null || value <= 0.0 -> {
                                        validationError = "Enter an amount greater than zero."
                                    }
                                    value > maxRecordableAmount + 1e-9 -> {
                                        validationError =
                                            "Amount exceeds pending interest. Maximum allowed: ${CurrencySettings.symbol}$formattedMaxAmount"
                                    }
                                    else -> {
                                        validationError = ""
                                        val recordId = editingRecordId
                                        if (recordId == null) {
                                            onAdd(value, selectedType, noteText.trim(), recordDate)
                                        } else {
                                            onEdit(
                                                recordId, value, selectedType,
                                                noteText.trim(), recordDate
                                            )
                                        }
                                        editingRecordId = null
                                        amountText = ""
                                        noteText = ""
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                            Text(
                                if (editingRecordId == null) "Save Interest Record"
                                else "Update"
                            )
                        }
                            if (editingRecordId != null) {
                                OutlinedButton(
                                    onClick = {
                                        recordToDelete = records.firstOrNull {
                                            it.id == editingRecordId
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }



            if (records.isEmpty()) {
                item {
                    Text(
                        "No interest records added yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            } else {
                items(records, key = { it.id }) { record ->
                    Card(
                    modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { listState.animateScrollToItem(0) }
                                editingRecordId = record.id
                                selectedType = record.type
                                amountText = record.amount.toString()
                                noteText = record.note
                                recordDate = record.transactionDate
                                validationError = ""
                            },
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (record.type == "GAVE") "Int. You Gave" else "Int. You Got",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (record.type == "GAVE") {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        Color(0xFF2E7D32)
                                    }
                                )
                                Text(
                                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                        .format(Date(record.transactionDate)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (record.note.isNotBlank()) {
                                    Text(record.note, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${CurrencySettings.symbol}${formatAmount(record.amount)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (record.type == "GAVE") {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        Color(0xFF2E7D32)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PartySellItem(
    val title: String,
    val rate: Double,
    val quantityType: String
)

@Composable
private fun TransactionComposer(
    isPremium: Boolean,
    partyId: Long,
    amount: String,
    note: String,
    transactionDate: Long,
    interestRate: Double,
    interestRateText: String,
    showInterestInput: Boolean,
    hideInterestButton: Boolean,
    hideSellButton: Boolean,
    onInterestClick: () -> Unit,
    onInterestRateChange: (String) -> Unit,
    selectedType: EntryType,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onTransactionDateChange: (Long) -> Unit,
    onTypeChange: (EntryType) -> Unit,
    smsEnabled: Boolean,
    onSmsEnabledChange: (Boolean) -> Unit,
    onAdd: () -> Unit
) {
    val context = LocalContext.current
    val sellPreferences = remember(context) {
        context.getSharedPreferences("ledger_party_sell_items", Context.MODE_PRIVATE)
    }
    val sellItemsKey = "party_sell_items_$partyId"
    val sellMigrationKey = "party_sell_items_room_migrated_$partyId"
    val sellItemsScope = rememberCoroutineScope()

    var sellItems by remember(partyId) {
        mutableStateOf<List<PartySellItem>>(emptyList())
    }

    LaunchedEffect(partyId) {
        val dao = LedgerDatabaseProvider.get(context).partySellItemDao()

        withContext(Dispatchers.IO) {
            var stored = dao.getByPartyId(partyId)

            // Import legacy SharedPreferences data once, without overwriting Room data.
            if (!sellPreferences.getBoolean(sellMigrationKey, false)) {
                if (stored.isEmpty()) {
                    val legacyItems = runCatching {
                        val json = org.json.JSONArray(
                            sellPreferences.getString(sellItemsKey, "[]") ?: "[]"
                        )
                        List(json.length()) { index ->
                            val item = json.getJSONObject(index)
                            PartySellItem(
                                title = item.getString("title"),
                                rate = item.getDouble("rate"),
                                quantityType = item.getString("quantityType")
                            )
                        }
                    }.getOrDefault(emptyList())

                    if (legacyItems.isNotEmpty()) {
                        dao.replaceForParty(
                            partyId,
                            legacyItems.map {
                                com.ledger.app.data.PartySellItemEntity(
                                    partyId = partyId,
                                    title = it.title,
                                    rate = it.rate,
                                    quantityType = it.quantityType
                                )
                            }
                        )
                    }
                }

                sellPreferences.edit()
                    .putBoolean(sellMigrationKey, true)
                    .remove(sellItemsKey)
                    .commit()

                stored = dao.getByPartyId(partyId)
            }

            stored.map {
                PartySellItem(
                    title = it.title,
                    rate = it.rate,
                    quantityType = it.quantityType
                )
            }
        }.also { loadedItems ->
            sellItems = loadedItems
        }
    }

    var selectedSellItem by remember(partyId) {
        mutableStateOf<PartySellItem?>(null)
    }
    var sellQuantity by remember(partyId) { mutableStateOf("") }
    var showAddSellItemDialog by remember { mutableStateOf(false) }
    var editingSellItemIndex by remember { mutableStateOf<Int?>(null) }
    var newSellTitle by remember { mutableStateOf("") }
    var newSellRate by remember { mutableStateOf("") }
    var newSellQuantityType by remember { mutableStateOf("") }

    fun saveSellItems(items: List<PartySellItem>) {
        // Update the UI immediately; persist the complete list in Room.
        sellItems = items
        sellItemsScope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    LedgerDatabaseProvider.get(context)
                        .partySellItemDao()
                        .replaceForParty(
                            partyId,
                            items.map {
                                com.ledger.app.data.PartySellItemEntity(
                                    partyId = partyId,
                                    title = it.title,
                                    rate = it.rate,
                                    quantityType = it.quantityType
                                )
                            }
                        )
                }
            }.onFailure {
                android.util.Log.e(
                    "PartySellItems",
                    "Could not save Sell Items for party $partyId",
                    it
                )
                Toast.makeText(
                    context,
                    "Sell Items database save failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

                Spacer(modifier = Modifier.weight(1f))

                Switch(
                    checked = smsEnabled,
                    colors = androidx.compose.material3.SwitchDefaults.colors(
                        checkedThumbColor = if (isPremium) Color(0xFFB8862B)
                            else MaterialTheme.colorScheme.primary,
                        checkedTrackColor = if (isPremium) Color(0xFFFFF0C2)
                            else MaterialTheme.colorScheme.primaryContainer,
                        checkedBorderColor = if (isPremium) Color(0xFFB8862B)
                            else MaterialTheme.colorScheme.primary
                    ),
                    onCheckedChange = { enabled ->
                        onSmsEnabledChange(enabled)
                        android.widget.Toast.makeText(
                            context,
                            if (enabled) "SMS On" else "SMS Off",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier
                        .scale(0.80f)
                        .height(36.dp)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                FilterChip(
                    modifier = Modifier.weight(1.45f),
                    selected = selectedType == EntryType.CREDIT,
                    onClick = {
                        onTypeChange(EntryType.CREDIT)
                    },
                    label = {
                        Text("You Gave", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                )

                FilterChip(
                    modifier = Modifier.weight(1.45f),
                    selected = selectedType == EntryType.DEBIT,
                    onClick = {
                        onTypeChange(EntryType.DEBIT)
                    },
                    label = {
                        Text("You Got", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                    }
                )

                if (!hideInterestButton) {
FilterChip(
                    modifier = Modifier.weight(0.8f),
                    selected = interestRate > 0.0,
                    onClick = onInterestClick,
                    label = {
                        Text(
                            if (interestRate > 0.0) {
                                "Int. ${formatInterestRate(interestRate)}%"
                            } else {
                                "Int."
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                )
                }


                if (selectedType == EntryType.CREDIT && !hideSellButton) {
                    Box(
                        modifier = Modifier
                            .weight(0.8f)
                            .align(Alignment.CenterVertically)
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                editingSellItemIndex = null
                                newSellTitle = ""
                                newSellRate = ""
                                newSellQuantityType = ""
                                showAddSellItemDialog = true
                            }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sell",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                }

            }

            }

            if (selectedType == EntryType.CREDIT && sellItems.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 0.dp)
                        .horizontalScroll(
                            androidx.compose.foundation.rememberScrollState()
                        ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    sellItems.forEach { item ->
                        FilterChip(
                            selected = selectedSellItem == item,
                            onClick = {
                                if (selectedSellItem == item) {
                                    selectedSellItem = null
                                } else {
                                    selectedSellItem = item
                                }
                                sellQuantity = ""
                                onAmountChange("")
                                onNoteChange("")
                            },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall, maxLines = 1) }
                        )
                    }
                }
            }

            if (selectedType == EntryType.CREDIT && selectedSellItem != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = sellQuantity,
                        onValueChange = { value ->
                            val normalized = value.replace(",", ".")
                            if (
                                normalized.isEmpty() ||
                                (normalized.count { it == '.' } <= 1 &&
                                    normalized.all { it.isDigit() || it == '.' })
                            ) {
                                sellQuantity = normalized
                                val quantity = normalized.toDoubleOrNull()
                                val item = selectedSellItem
                                if (quantity != null && quantity > 0.0 && item != null) {
                                    onAmountChange(
                                        String.format(
                                            Locale.US,
                                            "%.2f",
                                            item.rate * quantity
                                        )
                                    )
                                    val quantityText = normalized.trimEnd('.')
                                    onNoteChange(
                                        "$quantityText ${item.quantityType} ${item.title}"
                                    )
                                } else {
                                    onAmountChange("")
                                    onNoteChange("")
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Quantity") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                val item = selectedSellItem
                                val index = item?.let { sellItems.indexOf(it) } ?: -1
                                if (item != null && index >= 0) {
                                    editingSellItemIndex = index
                                    newSellTitle = item.title
                                    newSellRate = item.rate.toString()
                                    newSellQuantityType = item.quantityType
                                    showAddSellItemDialog = true
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Rate: ${CurrencySettings.symbol} ${
                                String.format(Locale.getDefault(), "%.2f", selectedSellItem!!.rate)
                            }",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Per ${selectedSellItem!!.quantityType}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = amount,
                    enabled = selectedSellItem == null,
                    onValueChange = onAmountChange,
                    modifier = Modifier.weight(if (showInterestInput) 1.15f else 1f),
                    singleLine = true,
                    label = { Text("Amount") },
                    prefix = { Text("${CurrencySettings.symbol} ") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                )

                if (showInterestInput) {
                    OutlinedTextField(
                        value = interestRateText,
                        onValueChange = { value ->
                            val normalized = value.replace(",", ".")
                            val dotIndex = normalized.indexOf('.')
                            val valid = normalized.isEmpty() ||
                                (
                                    normalized.count { it == '.' } <= 1 &&
                                    normalized.all { it.isDigit() || it == '.' } &&
                                    (dotIndex == -1 ||
                                        normalized.length - dotIndex - 1 <= 2) &&
                                    (dotIndex == -1 || dotIndex <= 3) &&
                                    (normalized.toDoubleOrNull() ?: 0.0) <= 100.0
                                )
                            if (valid) onInterestRateChange(normalized)
                        },
                        modifier = Modifier.weight(0.85f),
                        singleLine = true,
                        label = { Text("Rate (%)") },
                        suffix = { Text("%") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Box(
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = formatTransactionDate(transactionDate),
                    onValueChange = {},
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = true,
                    singleLine = true,
                    label = {
                        Text("Date")
                    },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            indication = null,
                            interactionSource = remember {
                                androidx.compose.foundation.interaction.MutableInteractionSource()
                            }
                        ) {
                            showTransactionDatePicker(
                                context = context,
                                initialDate = transactionDate,
                                onDateSelected = onTransactionDateChange
                            )
                        }
                )
            }

            OutlinedTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier.weight(1.15f),
                singleLine = true,
                label = {
                    Text("Note")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null
                    )
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp)
            )
            }

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

    if (showAddSellItemDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddSellItemDialog = false
                editingSellItemIndex = null
            },
            containerColor = Color.White,
            title = {
                Text(if (editingSellItemIndex != null) "Edit Sell Item" else "Add Sell Item")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSellTitle,
                        onValueChange = { newSellTitle = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = newSellRate,
                        onValueChange = { value ->
                            val normalized = value.replace(",", ".")
                            if (
                                normalized.isEmpty() ||
                                (normalized.count { it == '.' } <= 1 &&
                                    normalized.all { it.isDigit() || it == '.' })
                            ) newSellRate = normalized
                        },
                        label = { Text("Rate") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                    )
                    OutlinedTextField(
                        value = newSellQuantityType,
                        onValueChange = { newSellQuantityType = it },
                        label = { Text("Quantity Type (e.g. Litre, Kg, Piece)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingSellItemIndex != null) {
                        OutlinedButton(
                            onClick = {
                                val index = editingSellItemIndex
                                if (index != null && index in sellItems.indices) {
                                    val removed = sellItems[index]
                                    saveSellItems(sellItems.filterIndexed { i, _ -> i != index })
                                    if (selectedSellItem == removed) {
                                        selectedSellItem = null
                                        sellQuantity = ""
                                        onAmountChange("")
                                        onNoteChange("")
                                    }
                                }
                                editingSellItemIndex = null
                                showAddSellItemDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Red)
                        ) {
                            Text("Delete", maxLines = 1, color = Color.Red)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            showAddSellItemDialog = false
                            editingSellItemIndex = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Text("Cancel", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                        val title = newSellTitle.trim()
                        val rate = newSellRate.toDoubleOrNull()
                        val quantityType = newSellQuantityType.trim()
                        if (title.isNotBlank() && rate != null && rate > 0.0 &&
                            quantityType.isNotBlank()
                        ) {
                            val item = PartySellItem(title, rate, quantityType)
                            val editIndex = editingSellItemIndex
                            val updated = if (editIndex != null && editIndex in sellItems.indices) {
                                sellItems.toMutableList().also { it[editIndex] = item }
                            } else {
                                val existingIndex = sellItems.indexOfFirst {
                                    it.title.equals(title, ignoreCase = true)
                                }
                                if (existingIndex >= 0) {
                                    sellItems.toMutableList().also { it[existingIndex] = item }
                                } else {
                                    sellItems + item
                                }
                            }
                            saveSellItems(updated)
                            selectedSellItem = item
                            sellQuantity = ""
                            onAmountChange("")
                            onNoteChange("")
                            editingSellItemIndex = null
                            showAddSellItemDialog = false
                        } else {
                            Toast.makeText(
                                context,
                                "Enter Title, valid Rate and Quantity Type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    ,
                        modifier = Modifier.weight(1f),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.Red)
                    ) {
                        Text(if (editingSellItemIndex != null) "Save" else "Add", maxLines = 1)
                    }
                }
            },
            dismissButton = {}
        )
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

internal fun formatAmount(amount: Double): String = CurrencySettings.formatNumber(amount)

internal fun transactionAmountLabel(
    entry: LedgerEntryEntity
): String {
    val prefix = when (entry.type) {
        EntryType.CREDIT -> "+"
        EntryType.DEBIT -> "-"
    }
    return "$prefix${CurrencySettings.symbol}${formatAmount(abs(entry.amount))}"
}

internal fun transactionDateTimeLabel(
    entry: LedgerEntryEntity
): String {
    val date = if (entry.transactionDate > 0L) {
        entry.transactionDate
    } else {
        entry.createdAt
    }

    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.ENGLISH
    ).format(Date(date))
}

internal fun formatTransactionDate(timestamp: Long): String {
    return SimpleDateFormat(
        "dd MMM yyyy",
        Locale.ENGLISH
    ).format(Date(timestamp))
}

@Composable
private fun InterestRateDialog(
    initialRate: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var rateText by remember(initialRate) {
        mutableStateOf(
            if (initialRate == 0.0) "" else formatInterestRate(initialRate)
        )
    }

    val rate = rateText.toDoubleOrNull()
    val validRate = rate != null && rate >= 0.0 && rate <= 100.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Interest Rate")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Annual interest rate. Interest is calculated daily from the transaction date to today.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = rateText,
                    onValueChange = { value ->
                        val normalized = value.replace(",", ".")
                        val dotIndex = normalized.indexOf('.')

                        val valid =
                            normalized.isEmpty() ||
                            (
                                normalized.count { it == '.' } <= 1 &&
                                normalized.all { it.isDigit() || it == '.' } &&
                                (
                                    dotIndex == -1 ||
                                    normalized.length - dotIndex - 1 <= 2
                                ) &&
                                (
                                    dotIndex == -1 ||
                                    dotIndex <= 3
                                ) &&
                                (normalized.toDoubleOrNull() ?: 0.0) <= 100.0
                            )

                        if (valid) {
                            rateText = normalized
                        }
                    },
                    singleLine = true,
                    label = {
                        Text("Rate per year")
                    },
                    suffix = {
                        Text("% p.a.")
                    },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    )
                )

                Text(
                    text = "Default: 0%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(rate ?: 0.0)
                },
                enabled = validRate
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

internal fun formatInterestRate(rate: Double): String {
    return if (rate % 1.0 == 0.0) {
        rate.toInt().toString()
    } else {
        String.format(Locale.ENGLISH, "%.2f", rate)
    }
}

private fun showTransactionDatePicker(
    context: Context,
    initialDate: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = initialDate
    }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selected = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selected.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
private fun TransactionCard(
    entry: LedgerEntryEntity,
    modifier: Modifier = Modifier
) {
    val isCredit = entry.type == EntryType.CREDIT

    val accent = if (isCredit) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.tertiary
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

            val accruedInterest = calculateTransactionInterest(
                amount = entry.amount,
                annualRate = entry.interestRate,
                transactionDate = entry.transactionDate,
                createdAt = entry.createdAt
            )

            val interestDays = transactionInterestDays(
                transactionDate = entry.transactionDate,
                createdAt = entry.createdAt
            )

            val total = calculateEntryTotal(entry)

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${CurrencySettings.symbol}${formatAmount(kotlin.math.abs(entry.amount))}",
                    color = accent,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (entry.interestRate > 0.0) {
                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = "Int. ${formatInterestRate(entry.interestRate)}% p.a.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "$interestDays days • ${CurrencySettings.symbol}${formatAmount(accruedInterest)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }


}

