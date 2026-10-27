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
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
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
import com.ledger.app.data.StaffEntity
import com.ledger.app.data.SalaryRevisionEntity
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun HomeScreen(
    viewModel: LedgerViewModel,
    onPartyClick: (PartyEntity) -> Unit,
    onProfileClick: () -> Unit,
    onReportsClick: () -> Unit,
    onDailyExpenseClick: () -> Unit,
    onDailyExpenseReportClick: () -> Unit,
    onGroupSharingClick: () -> Unit,
    onGroupClick: (Long) -> Unit,
    isPremium: Boolean,
    premiumPrice: String,
    onUpgradeClick: () -> Unit,

    onDataChanged: () -> Unit,
    onGroupLongClick: (Long) -> Unit = { onGroupClick(it) },
    openSidebarOnLaunch: Boolean = false,
    onSidebarLaunchHandled: () -> Unit = {},
    autoSyncOff: Boolean = false,
    onAutoSyncOff: () -> Unit = {},
    onAutoSyncOn: () -> Unit = {},
    onHelpClick: () -> Unit = {},

    onStaffClick: (Long) -> Unit = {},
    onStaffLongClick: (Long) -> Unit = { onStaffClick(it) },
) {
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
    var staffMembers by remember { mutableStateOf<List<StaffEntity>>(emptyList()) }
    var showAddStaffDialog by rememberSaveable { mutableStateOf(false) }
    var newStaffName by rememberSaveable { mutableStateOf("") }
    var newStaffMobile by rememberSaveable { mutableStateOf("") }
    var newStaffRole by rememberSaveable { mutableStateOf("") }
    var newStaffSalary by rememberSaveable { mutableStateOf("") }
    var newStaffJoiningDate by rememberSaveable { mutableStateOf(java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())) }
    var isSavingStaff by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val homeFilterPrefs = remember(context) {
        context.getSharedPreferences(
            "ledger_home_filter_preferences",
            android.content.Context.MODE_PRIVATE
        )
    }
    var homeFilter by remember(context) {
        mutableStateOf(homeFilterPrefs.getString("selected_filter", "All") ?: "All")
    }

    val sharingGroupDao = remember(context) {
        LedgerDatabaseProvider.get(context).sharingGroupDao()
    }
    var sharingGroups by remember {
        mutableStateOf(emptyList<com.ledger.app.data.SharingGroupEntity>())
    }

    val availableHomeFilters = buildList {
            if (uiState.parties.isNotEmpty()) add("Parties")
            if (sharingGroups.isNotEmpty()) add("Groups")
            if (staffMembers.isNotEmpty()) add("Staff")
        }
        val activeHomeFilter =
            if (homeFilter == "All" || homeFilter in availableHomeFilters) homeFilter
            else "All"

        val visibleGroups = if (
            activeHomeFilter == "All" || activeHomeFilter == "Groups"
        ) sharingGroups else emptyList()

        val visibleStaff = if (
            activeHomeFilter == "All" || activeHomeFilter == "Staff"
        ) staffMembers else emptyList()

        val visibleParties = if (
            activeHomeFilter == "All" || activeHomeFilter == "Parties"
        ) uiState.filteredParties else emptyList()



    var groupExpenseTotals by remember {
        mutableStateOf(emptyMap<Long, Double>())
    }
    var receivableInterestTotal by remember { mutableStateOf(0.0) }
    var payableInterestTotal by remember { mutableStateOf(0.0) }
    var groupReceivableTotal by remember { mutableStateOf(0.0) }
    var totalStaffAdvanceOutstanding by remember { mutableStateOf(0.0) }
    var groupPayableTotal by remember { mutableStateOf(0.0) }
    var totalRemainingStaffSalary by remember { mutableStateOf(0.0) }
    var receivableEmiTotal by remember { mutableStateOf(0.0) }
    var payableEmiTotal by remember { mutableStateOf(0.0) }
    var isReceivableExpanded by rememberSaveable { mutableStateOf(false) }
    var isPayableExpanded by rememberSaveable { mutableStateOf(false) }
    var showCreateGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showAddOptionsDialog by rememberSaveable { mutableStateOf(false) }
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

    var showTipsDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(openSidebarOnLaunch) {
        if (openSidebarOnLaunch) {
            showMenu = true
            onSidebarLaunchHandled()
        }
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
        staffMembers = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            LedgerDatabaseProvider.get(context).staffDao().getAllStaff()
        }
    }

    LaunchedEffect(Unit) {
        sharingGroups = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            sharingGroupDao.getAll()
        }
    }

    LaunchedEffect(sharingGroups) {
        groupExpenseTotals = kotlinx.coroutines.withContext(
            kotlinx.coroutines.Dispatchers.IO
        ) {
            val expenseDao = LedgerDatabaseProvider.get(context).groupExpenseDao()
            sharingGroups.associate { group ->
                group.id to expenseDao.getByGroup(group.id).sumOf { it.amount }
            }
        }
    }

    LaunchedEffect(isReceivableExpanded, staffMembers) {
        if (isReceivableExpanded) {
            totalStaffAdvanceOutstanding =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val dao = LedgerDatabaseProvider.get(context).staffDao()

                    dao.getAllStaff().sumOf { staff ->
                        val advances = dao.getStaffAdvanceTransactions(staff.id)
                        val given = advances
                            .filter { it.transactionType == "GIVE" }
                            .sumOf { it.amount }
                        val adjusted = advances
                            .filter { it.transactionType == "ADJUST" }
                            .sumOf { it.amount }

                        (given - adjusted).coerceAtLeast(0.0)
                    }
                }
        }
    }

    LaunchedEffect(isPayableExpanded, staffMembers) {
        if (isPayableExpanded) {
            totalRemainingStaffSalary =
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    val dao = LedgerDatabaseProvider.get(context).staffDao()
                    val month = java.time.YearMonth.now()
                    val monthKey = month.toString()
                    val zone = java.time.ZoneId.systemDefault()

                    fun monthOf(timestamp: Long): java.time.YearMonth =
                        java.time.Instant.ofEpochMilli(timestamp)
                            .atZone(zone)
                            .toLocalDate()
                            .let { java.time.YearMonth.from(it) }

                    fun dayOf(timestamp: Long): Int =
                        java.time.Instant.ofEpochMilli(timestamp)
                            .atZone(zone)
                            .dayOfMonth
                            .coerceAtMost(30)

                    dao.getAllStaff()
                        .filter { it.isActive }
                        .sumOf { staff ->
                            val revisions = dao.getSalaryRevisions(staff.id)
                            val payments = dao.getSalaryPayments(staff.id)
                            val advances = dao.getStaffAdvanceTransactions(staff.id)
                            val deductions = dao.getStaffSalaryDeductions(staff.id)

                            val joiningMonth = monthOf(staff.joiningDate)
                            val firstEligibleDay =
                                if (month == joiningMonth) dayOf(staff.joiningDate) else 1

                            val salaryForMonth =
                                if (month < joiningMonth || firstEligibleDay > 30) {
                                    0.0
                                } else {
                                    (firstEligibleDay..30).sumOf { payrollDay ->
                                        val revision = revisions
                                            .filter {
                                                val revisionMonth = monthOf(it.effectiveDate)
                                                revisionMonth < month ||
                                                    (revisionMonth == month &&
                                                        dayOf(it.effectiveDate) <= payrollDay)
                                            }
                                            .maxWithOrNull(
                                                compareBy<com.ledger.app.data.SalaryRevisionEntity> {
                                                    monthOf(it.effectiveDate)
                                                }.thenBy {
                                                    dayOf(it.effectiveDate)
                                                }.thenBy {
                                                    it.id
                                                }
                                            )
                                        (revision?.amount ?: 0.0) / 30.0
                                    }
                                }

                            val alreadyPaid = payments
                                .filter { it.salaryMonth == monthKey }
                                .sumOf { it.amount }

                            val adjustedAdvances = advances
                                .filter {
                                    it.transactionType == "ADJUST" &&
                                        it.salaryMonth == monthKey
                                }
                                .sumOf { it.amount }

                            val totalDeductions = deductions
                                .filter { it.salaryMonth == monthKey }
                                .sumOf { it.amount }

                            val remaining = salaryForMonth - alreadyPaid -
                                adjustedAdvances - totalDeductions

                            if (kotlin.math.abs(remaining) < 0.005) 0.0 else remaining
                        }
                }
        }
    }

    LaunchedEffect(context) {
        val totals = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val database = LedgerDatabaseProvider.get(context)
            val repository = LedgerRepository(database)
            val asOf = System.currentTimeMillis()

            val partyInterestPositions = repository.getParties().map { party ->
                val entries = repository.getEntries(party.id)
                val emiTransactionIds = database.emiScheduleDao()
                    .getByPartyId(party.id)
                    .mapTo(mutableSetOf()) { it.transactionId }
                val nonEmiEntries = entries.filterNot { it.id in emiTransactionIds }
                val partySettlements = database.transactionSettlementDao()
                    .getByPartyId(party.id)

                com.ledger.app.data.calculateInterestAccountPosition(
                    transactions = nonEmiEntries,
                    records = repository.getInterestAccountEntries(party.id),
                    asOf = asOf,
                    settlements = partySettlements
                )
            }
            val interestTotal = partyInterestPositions.sumOf { it.receivable }
            val payableInterest = partyInterestPositions.sumOf { it.payable }

            val memberDao = database.sharingMemberDao()
            val expenseDao = database.groupExpenseDao()
            val splitDao = database.groupExpenseSplitDao()
            val paymentDao = database.groupExpensePaymentDao()
            val settlementDao = database.groupSettlementDao()

            val groupTotals = database.sharingGroupDao().getAll().fold(0.0 to 0.0) { totals, group ->
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

                val balances = members.map { member ->
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
                    spent - paid - received
                }

                (totals.first + balances.sumOf { it.coerceAtLeast(0.0) }) to
                    (totals.second + balances.sumOf { (-it).coerceAtLeast(0.0) })
            }
            Triple(interestTotal, payableInterest, groupTotals.first) to groupTotals.second
        }

        receivableInterestTotal = totals.first.first
        payableInterestTotal = totals.first.second
        groupReceivableTotal = totals.first.third
        groupPayableTotal = totals.second
    }

    LaunchedEffect(uiState.parties, isReceivableExpanded, isPayableExpanded) {
        if (isReceivableExpanded || isPayableExpanded) {
            val totals = kotlinx.coroutines.withContext(
                kotlinx.coroutines.Dispatchers.IO
            ) {
                val database = LedgerDatabaseProvider.get(context)
                val repository = LedgerRepository(database)
                var receivableTotal = 0.0
                var payableTotal = 0.0

                fun remainingEmi(
                    entry: com.ledger.app.data.LedgerEntryEntity,
                    schedules: List<com.ledger.app.data.EmiScheduleEntity>,
                    settlements: List<com.ledger.app.data.EmiChargeSettlementEntity>
                ): Double {
                    val matching = schedules
                        .filter { it.transactionId == entry.id }
                        .sortedBy { it.installmentNumber }
                    if (matching.isEmpty()) return 0.0

                    val history = settlements.filter { it.transactionId == entry.id }
                    val settledEmiAmountTotal =
                        history.sumOf { it.emiAmount.coerceAtLeast(0.0) }
                    val settledInterestAmountTotal =
                        history.sumOf { it.interestAmount.coerceAtLeast(0.0) }

                    val baseDate = java.util.Calendar.getInstance().apply {
                        timeInMillis = entry.transactionDate
                        add(java.util.Calendar.MONTH, 1)
                    }
                    fun localDate(millis: Long): java.time.LocalDate {
                        val c = java.util.Calendar.getInstance().apply {
                            timeInMillis = millis
                        }
                        return java.time.LocalDate.of(
                            c.get(java.util.Calendar.YEAR),
                            c.get(java.util.Calendar.MONTH) + 1,
                            c.get(java.util.Calendar.DAY_OF_MONTH)
                        )
                    }

                    val delayDays = java.time.temporal.ChronoUnit.DAYS.between(
                        localDate(baseDate.timeInMillis),
                        localDate(matching.first().dueDate)
                    ).coerceAtLeast(0L)
                    val delayInterest = kotlin.math.round(
                        kotlin.math.abs(entry.amount) *
                            entry.interestRate.coerceAtLeast(0.0) *
                            delayDays / 36500.0 * 100.0
                    ) / 100.0

                    // Pending already contains the unpaid part of partial EMIs.
                    val pending = matching.filter { !it.isPaid }
                        .sumOf { (it.amount - it.paidAmount).coerceAtLeast(0.0) }
                    val partial = matching.filter {
                        it.paidAmount > 0.0 && it.paidAmount < it.amount
                    }
                    val partialRemaining = partial.sumOf {
                        (it.amount - it.paidAmount).coerceAtLeast(0.0)
                    }

                    val today = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis

                    val overdueInterest = partial.sumOf { emi ->
                        val dueStart = java.util.Calendar.getInstance().apply {
                            timeInMillis = emi.dueDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val days = if (today > dueStart) {
                            (today - dueStart) / 86_400_000L
                        } else 0L
                        (emi.amount - emi.paidAmount).coerceAtLeast(0.0) *
                            entry.interestRate.coerceAtLeast(0.0) * days / 36500.0
                    }

                    val settledEmiOverdueInterest = history.sumOf { settlement ->
                        val settlementStart = java.util.Calendar.getInstance().apply {
                            timeInMillis = settlement.settlementDate
                            set(java.util.Calendar.HOUR_OF_DAY, 0)
                            set(java.util.Calendar.MINUTE, 0)
                            set(java.util.Calendar.SECOND, 0)
                            set(java.util.Calendar.MILLISECOND, 0)
                        }.timeInMillis
                        val days = ((today - settlementStart) / 86_400_000L)
                            .coerceAtLeast(0L)
                        kotlin.math.round(
                            settlement.emiAmount.coerceAtLeast(0.0) *
                                entry.interestRate.coerceAtLeast(0.0) *
                                days / 36500.0 * 100.0
                        ) / 100.0
                    }

                    val adjustedOverdueInterest =
                        (overdueInterest - settledEmiOverdueInterest).coerceAtLeast(0.0)
                    val adjustedPartialRemaining =
                        (partialRemaining - settledEmiAmountTotal).coerceAtLeast(0.0)

                    return (
                        pending +
                            delayInterest.coerceAtLeast(0.0) +
                            adjustedPartialRemaining +
                            adjustedOverdueInterest -
                            settledInterestAmountTotal
                    ).coerceAtLeast(0.0)
                }
                uiState.parties.forEach { party ->
                    val entries = repository.getEntries(party.id)
                    val schedules = database.emiScheduleDao().getByPartyId(party.id)
                    val emiSettlements = schedules
                        .map { it.transactionId }
                        .distinct()
                        .flatMap { transactionId ->
                            database.emiChargeSettlementDao()
                                .getByTransactionId(transactionId)
                        }

                    entries.filter {
                        it.type == com.ledger.app.data.EntryType.CREDIT
                    }.forEach { entry ->
                        receivableTotal += remainingEmi(entry, schedules, emiSettlements)
                    }
                    entries.filter {
                        it.type == com.ledger.app.data.EntryType.DEBIT
                    }.forEach { entry ->
                        payableTotal += remainingEmi(entry, schedules, emiSettlements)
                    }
                }
                receivableTotal to payableTotal
            }
            receivableEmiTotal = totals.first
            payableEmiTotal = totals.second
        }
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

    
    var showAutoSyncOffDialog by remember { mutableStateOf(false) }

    if (showAutoSyncOffDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAutoSyncOffDialog = false },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    Modifier.border(
                        width = 1.dp,
                        color = Color(0xFFE5E7EB),
                        shape = RoundedCornerShape(28.dp)
                    )
                ),
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("☁️", fontSize = 30.sp)
                    Text(
                        text = "Turn Auto Sync Off?",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1F2937),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            text = {
                Text(
                    text = "You can turn Auto Sync off temporarily if you want to Add, Edit, and Delete many transactions without syncing again and again.",
                    color = Color(0xFF4B5563),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showAutoSyncOffDialog = false
                        onAutoSyncOff()
                    },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text(
                        "Turn Auto Sync Off",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showAutoSyncOffDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color(0xFFD1D5DB)
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF374151)
                    ),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                }
            },
            titleContentColor = Color(0xFF1F2937),
            textContentColor = Color(0xFF4B5563)
        )
    }

if (showTipsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTipsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 25.sp)
                    Text(
                        text = "  Tips & Tricks",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("1.", fontWeight = FontWeight.Bold, color = Color(0xFFB7791F))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Create Group", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Long-press the Add Party button to Create Group.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("2.", fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Set Default Share", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Long-press a member in a Group to set their default share. It is automatically filled when you select the Share split type while adding a Group expense.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("3.", fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Daily Expenses Report", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Long-press the Add Expense button on the Daily Expense page to open the Daily Expenses Report options.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("4.", fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Edit or Delete Parties & Groups", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Long-press any Party or Group on the Home page to access the Edit and Delete options.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("5.", fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("View, Edit & Delete Group Expenses", fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "Tap an expense card in a Group page to view its full details. Long-press the card to access the Edit and Delete options.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showTipsDialog = false }
                ) {
                    Text("Got it")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
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
                            shadowElevation = 1.dp
                        ) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Open navigation sidebar"
                                )
                            }
                        }
                    }

                    if (showMenu) {
                        Dialog(
                            onDismissRequest = { showMenu = false },
                            properties = androidx.compose.ui.window.DialogProperties(
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.42f))
                            ) {
                                Row(modifier = Modifier.fillMaxSize()) {
                                    androidx.compose.animation.AnimatedVisibility(
                                        visible = true,
                                        modifier = Modifier
                                            .width(300.dp)
                                            .fillMaxSize(),
                                        enter = androidx.compose.animation.slideInHorizontally(
                                            initialOffsetX = { -it }
                                        ) + androidx.compose.animation.fadeIn(),
                                        exit = androidx.compose.animation.slideOutHorizontally(
                                            targetOffsetX = { -it }
                                        ) + androidx.compose.animation.fadeOut()
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clickable(enabled = false) {},
                                            shape = RoundedCornerShape(
                                                topEnd = 24.dp,
                                                bottomEnd = 24.dp
                                            ),
                                            color = Color(0xFFFFFEFA),
                                            shadowElevation = 12.dp
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .statusBarsPadding()
                                                    .navigationBarsPadding()
                                                    .padding(
                                                        horizontal = 18.dp,
                                                        vertical = 18.dp
                                                    ),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Surface(
                                                        modifier = Modifier.size(44.dp),
                                                        shape = RoundedCornerShape(14.dp),
                                                        color = Color(0xFFFFE7A3)
                                                    ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Storage,
                                                                contentDescription = null,
                                                                tint = Color(0xFF805B00)
                                                            )
                                                        }
                                                    }
                                                    Column(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .padding(start = 12.dp)
                                                    ) {
                                                        Text(
                                                            text = "Ledger",
                                                            style = MaterialTheme.typography.titleLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF242424)
                                                        )
                                                        Text(
                                                            text = profileName.ifBlank {
                                                                "Personal Finance"
                                                            },
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = Color(0xFF77736A),
                                                            maxLines = 1
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = { showMenu = false }
                                                    ) {
                                                        Text("✕", color = Color(0xFF686868))
                                                    }
                                                }

                                                androidx.compose.material3.HorizontalDivider(
                                                    color = Color(0xFFECE5D5)
                                                )





                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            onProfileClick()
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 14.dp,
                                                    vertical = 13.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AccountCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF6D6557)
                                                )
                                                Text(
                                                    text = "Profile & Settings",
                                                    modifier = Modifier.padding(start = 12.dp),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = Color(0xFF303030)
                                                )
                                            }
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            onReportsClick()
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 13.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Storage,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D6557)
                                                        )
                                                        Text(
                                                            text = "Reports",
                                                            modifier = Modifier.padding(start = 12.dp),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = Color(0xFF303030)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            onDailyExpenseReportClick()
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.ReceiptLong,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D6557)
                                                        )
                                                        Text(
                                                            text = "Daily Expenses Report",
                                                            modifier = Modifier.padding(start = 12.dp),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = Color(0xFF303030)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            requestAddParty()
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 13.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.PersonAdd,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D6557)
                                                        )
                                                        Text(
                                                            text = "Add Party",
                                                            modifier = Modifier.padding(start = 12.dp),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = Color(0xFF303030)
                                                        )
                                                    }
                                                }

                                                Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            if (isPremium || sharingGroups.size < 4) {
                                                                newGroupName = ""
                                                                showCreateGroupDialog = true
                                                            } else {
                                                                showGroupUpgradeDialog = true
                                                            }
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color.Transparent
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 14.dp,
                                                            vertical = 13.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Group,
                                                            contentDescription = null,
                                                            tint = Color(0xFF6D6557)
                                                        )
                                                        Text(
                                                            text = "Add Group",
                                                            modifier = Modifier.padding(start = 12.dp),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = Color(0xFF303030)
                                                        )
                                                    }
                                                }

                                                Surface(
    modifier = Modifier
        .fillMaxWidth()
        .clickable {
            showMenu = false
            newStaffName = ""
            newStaffMobile = ""
            newStaffRole = ""
            newStaffSalary = ""
            newStaffJoiningDate = java.text.SimpleDateFormat(
                "yyyy-MM-dd", java.util.Locale.US
            ).format(java.util.Date())
            showAddStaffDialog = true
        },
    shape = RoundedCornerShape(13.dp),
    color = Color.Transparent
) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.PersonAdd,
            contentDescription = null,
            tint = Color(0xFF6D6557)
        )
        Text(
            text = "Add Staff",
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF303030)
        )
    }
}

Surface(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            showMenu = false
                                                            showTipsDialog = true
                                                        },
                                                    shape = RoundedCornerShape(13.dp),
                                                    color = Color(0xFFFFF3D6)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text("💡", fontSize = 20.sp)
                                                        Text(
                                                            text = "Tips & Tricks",
                                                            modifier = Modifier.padding(start = 12.dp),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF805B00)
                                                        )
                                                    }
                                                }

                                                
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                showMenu = false
                                                if (autoSyncOff) {
                                                    onAutoSyncOn()
                                                } else {
                                                    showAutoSyncOffDialog = true
                                                }
                                            },
                                        shape = RoundedCornerShape(13.dp),
                                        color = if (autoSyncOff)
                                            Color(0xFFE4F7E9)
                                        else
                                            Color(0xFFEAF2FF)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 14.dp,
                                                vertical = 13.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (autoSyncOff) "🔄" else "☁️",
                                                fontSize = 20.sp
                                            )
                                            Text(
                                                text = if (autoSyncOff)
                                                    "Auto sync on"
                                                else
                                                    "Auto sync off",
                                                modifier = Modifier.padding(start = 12.dp),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (autoSyncOff)
                                                    Color(0xFF176B35)
                                                else
                                                    Color(0xFF234E8A)
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(8.dp))
Surface(
    modifier = Modifier
        .fillMaxWidth()
        .clickable {
            showMenu = false
            onHelpClick()
        },
    shape = RoundedCornerShape(13.dp),
    color = Color(0xFFF1F5F9)
) {
    Row(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("❔", fontSize = 20.sp)
        Text(
            text = "Help",
            modifier = Modifier.padding(start = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF303030)
        )
    }
}

Spacer(Modifier.height(8.dp))
Spacer(Modifier.weight(1f))
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clickable { showMenu = false }
                                    )
                                }
                            }
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
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .combinedClickable(
                        onClick = {
                            requestAddParty()
                        },
                        onLongClick = {
                            showAddOptionsDialog = true
                        }
                    ),
                shape = RoundedCornerShape(18.dp),
                color = if (isPremium) Color(0xFFFFC247)
                    else MaterialTheme.colorScheme.primary,
                contentColor = if (isPremium) Color(0xFF302000)
                    else MaterialTheme.colorScheme.onPrimary,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = "Add party"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 6.dp,
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
                        amountColor = Color(0xFFC62828),
                        expanded = isReceivableExpanded,
                        onClick = { isReceivableExpanded = !isReceivableExpanded },
                        details = listOf(
                            "Receivable Int." to receivableInterestTotal,
                            "Receivable EMI" to receivableEmiTotal,
                            "Group Receivable" to groupReceivableTotal,
                            "Advanced Salary" to totalStaffAdvanceOutstanding
                        ).filter { kotlin.math.abs(it.second) >= 0.01 }
                    )

                    BalanceSummaryCard(
                        title = "Payable",
                        amount = payable,
                        modifier = Modifier.weight(1f),
                        amountColor = Color(0xFF2E7D32),
                        expanded = isPayableExpanded,
                        onClick = { isPayableExpanded = !isPayableExpanded },
                        details = listOf(
                            "Payable Int." to payableInterestTotal,
                            "Payable EMI" to payableEmiTotal,
                            "Group Payable" to groupPayableTotal,
                            "Salaries" to totalRemainingStaffSalary
                        ).filter { kotlin.math.abs(it.second) >= 0.01 }
                    )
                }
            }

            item {

            }


            item {
                if (availableHomeFilters.size >= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        (listOf("All") + availableHomeFilters).forEach { filter ->
                            val selected = activeHomeFilter == filter
                            val label = when (filter) {
                                "All" -> "All"
                                "Parties" -> "Parties"
                                "Groups" -> "Groups"
                                else -> "Staff"
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        homeFilter = filter
                                        homeFilterPrefs.edit()
                                            .putString("selected_filter", filter)
                                            .apply()
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFFFFC247)
                                    else MaterialTheme.colorScheme.surface,
                                contentColor = if (selected) Color(0xFF302000)
                                    else MaterialTheme.colorScheme.onSurface,
                                border = androidx.compose.foundation.BorderStroke(
                                    if (selected) 1.5.dp else 1.dp,
                                    if (selected) Color(0xFFE2A323)
                                    else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shadowElevation = if (selected) 2.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 2.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold
                                            else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(
                items = visibleGroups,
                key = { "sharing-group-${it.id}" }
            ) { group ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .combinedClickable(
                            onClick = { onGroupClick(group.id) },
                            onLongClick = { onGroupLongClick(group.id) }
                        ),
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
                            Spacer(Modifier.height(5.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Expense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = java.text.NumberFormat.getCurrencyInstance(
                                        java.util.Locale("en", "IN")
                                    ).format(groupExpenseTotals[group.id] ?: 0.0).substringBefore("."),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1565C0)
                                )
                            }
                        }
                    }
                }
            }

            if (staffMembers.isNotEmpty()) {
                items(items = visibleStaff, key = { "staff-${it.id}" }) { staff ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onStaffClick(staff.id) },
                                onLongClick = { onStaffLongClick(staff.id) }
                            ),
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
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountCircle,
                                        contentDescription = "Staff"
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(staff.name, fontWeight = FontWeight.SemiBold)
                                Text(
                                    listOf(staff.role, staff.mobile)
                                        .filter { it.isNotBlank() }
                                        .joinToString(" • ").ifBlank { "Staff member" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                item {
                    LoadingState()
                }
            } else if (visibleParties.isEmpty() && visibleGroups.isEmpty() && visibleStaff.isEmpty()) {
                item {
                    EmptyPartyState(
                        hasSearch = uiState.searchQuery.isNotBlank(),
                        onAddParty = {
                            requestAddParty()
                        }
                    )
                }
            } else {
                items(items = visibleParties,
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

    if (showAddOptionsDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAddOptionsDialog = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    "What would you like to add?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF20243A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showAddOptionsDialog = false
                            requestAddParty()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add Party")
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showAddOptionsDialog = false
                            if (isPremium || sharingGroups.size < 4) {
                                newGroupName = ""
                                showCreateGroupDialog = true
                            } else {
                                showGroupUpgradeDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Group,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Create Group")
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showAddOptionsDialog = false
                            newStaffName = ""
                            newStaffMobile = ""
                            newStaffRole = ""
                            newStaffSalary = ""
                            newStaffJoiningDate =
                                java.text.SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    java.util.Locale.US
                                ).format(java.util.Date())
                            showAddStaffDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Add Staff")
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showAddOptionsDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddStaffDialog) {
        StaffFormDialog(
            title = "Add Staff",
            initialName = newStaffName,
            initialMobile = newStaffMobile,
            initialRole = newStaffRole,
            initialSalary = newStaffSalary,
            initialJoiningDate = runCatching {
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                    .apply { isLenient = false }
                    .parse(newStaffJoiningDate)?.time
            }.getOrNull() ?: System.currentTimeMillis(),
            showSalary = true,
            saving = isSavingStaff,
            onDismiss = { if (!isSavingStaff) showAddStaffDialog = false },
            onSave = { name, mobile, role, date, salaryText ->
                isSavingStaff = true
                val salary = salaryText.toDoubleOrNull() ?: 0.0
                scope.launch {
                    try {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val dao = LedgerDatabaseProvider.get(context).staffDao()
                            val id = dao.insertStaff(
                                StaffEntity(
                                    name = name,
                                    mobile = mobile,
                                    role = role,
                                    joiningDate = date
                                )
                            )
                            if (salary > 0.0) {
                                dao.insertSalaryRevision(
                                    SalaryRevisionEntity(
                                        staffId = id,
                                        amount = salary,
                                        effectiveDate = date,
                                        note = "Starting salary"
                                    )
                                )
                            }
                        }
                        staffMembers = kotlinx.coroutines.withContext(
                            kotlinx.coroutines.Dispatchers.IO
                        ) {
                            LedgerDatabaseProvider.get(context).staffDao().getAllStaff()
                        }
                        showAddStaffDialog = false
                        newStaffName = ""
                        newStaffMobile = ""
                        newStaffRole = ""
                        newStaffSalary = ""
                        newStaffJoiningDate = java.text.SimpleDateFormat(
                            "yyyy-MM-dd", java.util.Locale.US
                        ).format(java.util.Date())
                        onDataChanged()
                    } catch (error: Exception) {
                        android.util.Log.e("LedgerStaff", "Unable to save staff", error)
                    } finally {
                        isSavingStaff = false
                    }
                }
            }
        )
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
                        containerColor = androidx.compose.ui.graphics.Color.White,
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
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isPremium) Color(0xFFFFC247)
                                    else MaterialTheme.colorScheme.primary,
                                contentColor = if (isPremium) Color(0xFF302000)
                                    else MaterialTheme.colorScheme.onPrimary
                            ),
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
                        "Subscribe to Premium",
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
                            "Subscribe to Premium",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { showCreateGroupDialog = false },
                        enabled = !isSavingGroup,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFCBD5E1)
                        )
                    ) {
                        Text("Cancel", maxLines = 1)
                    }

                    Button(
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (isPremium) Color(0xFFFFC247)
                        else MaterialTheme.colorScheme.primary,
                    contentColor = if (isPremium) Color(0xFF302000)
                        else MaterialTheme.colorScheme.onPrimary
                ),

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
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingGroup) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Create Group",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
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
                        "Subscribe to Premium",
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
                        if (premiumPrice.isBlank()) "Subscribe to Premium"
                        else "Subscribe yearly · $premiumPrice/year"
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
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (title == "Receivable") Color(0xFFE8A09A)
            else Color(0xFF91C9A0)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (title == "Receivable") {
                Color(0xFFFFF1F0)
            } else {
                Color(0xFFF0F9F1)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
    val cardBackground = Color.White
    val cardBorder = when {
        balance > 0.0 -> Color(0xFFEBCACA)
        balance < 0.0 -> Color(0xFFC8E5CE)
        else -> Color(0xFFE9D8A8)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBackground
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
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

                if (balance > 0.0) {
                    Text(
                        text = "You will get",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC62828)
                    )
                } else if (balance < 0.0) {
                    Text(
                        text = "You will give",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Text(
                        text = "Settled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (balance != 0.0) {
                Text(
                    text = "${CurrencySettings.symbol}${formatAmount(kotlin.math.abs(balance))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (balance > 0.0) {
                        Color(0xFFC62828)
                    } else {
                        Color(0xFF2E7D32)
                    }
                )
            }
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
