package com.ledger.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.ledger.app.data.CurrencySettings
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.StaffEntity
import com.ledger.app.data.SalaryRevisionEntity
import com.ledger.app.data.SalaryPaymentEntity
import com.ledger.app.data.StaffAdvanceTransactionEntity
import com.ledger.app.data.StaffBonusEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material.icons.outlined.CalendarMonth

private fun staffDate(value: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))

private fun staffMoney(value: Double): String = CurrencySettings.format(value)

private fun staffDateInput(value: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        isLenient = false
    }.parse(value.trim())?.time
} catch (_: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffDetailScreen(
    staffId: Long,
    onBack: () -> Unit,
    onDataChanged: () -> Unit,
    isPremium: Boolean = false
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var staff by remember { mutableStateOf<StaffEntity?>(null) }
    var revisions by remember { mutableStateOf<List<SalaryRevisionEntity>>(emptyList()) }
    var payments by remember { mutableStateOf<List<SalaryPaymentEntity>>(emptyList()) }
    var advances by remember { mutableStateOf<List<StaffAdvanceTransactionEntity>>(emptyList()) }
    var bonuses by remember { mutableStateOf<List<StaffBonusEntity>>(emptyList()) }
    var showBonusDialog by remember { mutableStateOf(false) }
    var editingBonus by remember { mutableStateOf<StaffBonusEntity?>(null) }
    var showDeleteBonusConfirm by remember { mutableStateOf(false) }
    var bonusAmount by remember { mutableStateOf("") }
    var bonusDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var bonusNote by remember { mutableStateOf("") }
    var bonusError by remember { mutableStateOf("") }
    var selectedStaffTab by remember { mutableIntStateOf(1) }
    var selectedPaymentYear by remember {
        mutableStateOf(java.time.Year.now().value.toString())
    }
    var showPaymentYearMenu by remember { mutableStateOf(false) }
    var showAdvanceDialog by remember { mutableStateOf(false) }
    var showAdvancePage by remember { mutableStateOf(false) }
    var showBonusPage by remember { mutableStateOf(false) }
    val advanceScope = rememberCoroutineScope()
    var advanceDialogType by remember { mutableStateOf("GIVE") }
    var advanceAmount by remember { mutableStateOf("") }
    var advanceDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var advanceNote by remember { mutableStateOf("") }
    var advanceError by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var showStaffEditDialog by remember { mutableStateOf(false) }
    var showDeleteStaffConfirm by remember { mutableStateOf(false) }
    var isSavingStaffEdit by remember { mutableStateOf(false) }
    var showRevisionDialog by remember { mutableStateOf(false) }
    var editingRevision by remember { mutableStateOf<SalaryRevisionEntity?>(null) }
    var showDeleteRevisionConfirm by remember { mutableStateOf(false) }
    var showRevisionHistory by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var salaryAmount by remember { mutableStateOf("") }
    var salaryEffectiveDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var salaryNote by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentDate by remember {
        mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    }
    var paymentMode by remember { mutableStateOf("Cash") }
    var paymentNote by remember { mutableStateOf("") }

    var editingPayment by remember { mutableStateOf<SalaryPaymentEntity?>(null) }
    var showEditPaymentDialog by remember { mutableStateOf(false) }
    var showDeletePaymentConfirm by remember { mutableStateOf(false) }
    var editPaymentAmount by remember { mutableStateOf("") }
    var editPaymentDate by remember { mutableStateOf("") }
    var editPaymentMode by remember { mutableStateOf("Cash") }
    var editPaymentNote by remember { mutableStateOf("") }
    var editSalaryMonth by remember { mutableStateOf("") }

    var selectedSalaryMonth by remember {
        mutableStateOf(java.time.YearMonth.now().toString())
    }
    var showSalaryMonthMenu by remember { mutableStateOf(false) }
    var pickerYear by remember { mutableIntStateOf(java.time.YearMonth.now().year) }
    var pickerMonth by remember { mutableIntStateOf(java.time.YearMonth.now().monthValue) }
    var showYearDropdown by remember { mutableStateOf(false) }


    fun reload() {
        scope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                val dao = LedgerDatabaseProvider.get(context).staffDao()
                Triple(dao.getStaff(staffId), dao.getSalaryRevisions(staffId), dao.getSalaryPayments(staffId)) to (dao.getStaffAdvanceTransactions(staffId) to dao.getStaffBonusPayments(staffId))
            }
            staff = result.first.first
            revisions = result.first.second
            payments = result.first.third
            advances = result.second.first
            bonuses = result.second.second
            loading = false
        }
    }

    LaunchedEffect(staffId) { reload() }
    BackHandler {
        if (showBonusPage) showBonusPage = false else if (showAdvancePage) showAdvancePage = false else if (showRevisionHistory) showRevisionHistory = false else onBack()
    }
    var showStaffMenu by remember { mutableStateOf(false) }

    val currentSalary = revisions.firstOrNull()?.amount ?: 0.0
    val totalPaid = payments.sumOf { it.amount }
    val advanceOutstanding = (advances.filter { it.transactionType == "GIVE" }.sumOf { it.amount } -
        advances.filter { it.transactionType == "ADJUST" }.sumOf { it.amount }).coerceAtLeast(0.0)

    val salaryMonthOptions = remember {
        (-24L..24L)
            .map { java.time.YearMonth.now().minusMonths(it) }
            .sortedDescending()
    }
    val selectedYearMonth = remember(selectedSalaryMonth) {
        runCatching { java.time.YearMonth.parse(selectedSalaryMonth) }
            .getOrElse { java.time.YearMonth.now() }
    }
    val selectedSalaryMonthLabel = selectedYearMonth.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault())
    )
    val salaryMonthEnd = selectedYearMonth.atEndOfMonth()
        .atTime(java.time.LocalTime.MAX)
        .atZone(java.time.ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

    // Fixed 30-day payroll calculation. Day 31 is not an additional salary day.
    // A joining month is prorated from the joining day through normalized day 30.
    val salaryForSelectedMonth = run {
        val month = selectedYearMonth
        val zone = java.time.ZoneId.systemDefault()

        fun monthOf(timestamp: Long): java.time.YearMonth =
            java.time.Instant.ofEpochMilli(timestamp).atZone(zone).toLocalDate()
                .let { java.time.YearMonth.from(it) }

        fun dayOf(timestamp: Long): Int =
            java.time.Instant.ofEpochMilli(timestamp).atZone(zone).dayOfMonth.coerceAtMost(30)

        val joining = staff?.joiningDate
        if (joining == null || month < monthOf(joining)) {
            0.0
        } else {
            val joiningMonth = monthOf(joining)
            val firstEligibleDay = if (month == joiningMonth) dayOf(joining) else 1

            if (firstEligibleDay > 30) {
                0.0
            } else {
                (firstEligibleDay..30).sumOf { payrollDay ->
                    val applicableRevision = revisions
                        .filter { revision ->
                            val revisionMonth = monthOf(revision.effectiveDate)
                            revisionMonth < month ||
                                (revisionMonth == month && dayOf(revision.effectiveDate) <= payrollDay)
                        }
                        .maxWithOrNull(
                            compareBy<SalaryRevisionEntity> { monthOf(it.effectiveDate) }
                                .thenBy { dayOf(it.effectiveDate) }
                                .thenBy { it.id }
                        )
                    (applicableRevision?.amount ?: 0.0) / 30.0
                }
            }
        }
    }

    fun paymentRecordYear(timestamp: Long): Int {
        // Support timestamps stored in either milliseconds or seconds.
        val millis = if (kotlin.math.abs(timestamp) < 100_000_000_000L) {
            timestamp * 1000L
        } else {
            timestamp
        }
        return java.time.Instant.ofEpochMilli(millis)
            .atZone(java.time.ZoneId.systemDefault())
            .year
    }

    val paymentYears = remember(payments, advances) {
        val recordedYears = payments.flatMap { payment ->
            buildList {
                add(paymentRecordYear(payment.paymentDate))
                payment.salaryMonth.takeIf { it.length >= 4 }
                    ?.take(4)?.toIntOrNull()?.let { add(it) }
            }
        } + advances.map { paymentRecordYear(it.transactionDate) }

        val currentYear = java.time.Year.now().value
        val firstYear = (recordedYears + currentYear).minOrNull() ?: currentYear
        val lastYear = maxOf(currentYear, recordedYears.maxOrNull() ?: currentYear)
        (firstYear..lastYear).map { it.toString() }.reversed()
    }

    // If data reloads and the selected year no longer exists, select the latest year.
    LaunchedEffect(paymentYears) {
        if (selectedPaymentYear !in paymentYears && paymentYears.isNotEmpty()) {
            selectedPaymentYear = paymentYears.first()
        }
    }

    val filteredPayments = remember(payments, selectedPaymentYear) {
        payments.filter {
            paymentRecordYear(it.paymentDate).toString() == selectedPaymentYear
        }.sortedByDescending { it.paymentDate }
    }

    val filteredAdvanceTransactions = remember(advances, selectedPaymentYear) {
        advances.filter {
            paymentRecordYear(it.transactionDate).toString() == selectedPaymentYear
        }.sortedByDescending { it.transactionDate }
    }

    val combinedPaymentHistory = remember(filteredPayments, filteredAdvanceTransactions) {
        (
            filteredPayments.map { it as Any } +
                filteredAdvanceTransactions.map { it as Any }
        ).sortedByDescending { entry ->
            when (entry) {
                is SalaryPaymentEntity -> entry.paymentDate
                is StaffAdvanceTransactionEntity -> entry.transactionDate
                else -> 0L
            }
        }
    }

    val alreadyPaidForSelectedMonth = payments
        .filter { it.salaryMonth == selectedSalaryMonth }
        .sumOf { it.amount }
    val remainingSalary =
        (salaryForSelectedMonth - alreadyPaidForSelectedMonth)

    Scaffold(
        floatingActionButton = {
            if (!loading && staff != null && !showRevisionHistory && !showAdvancePage && !showBonusPage &&
                (selectedStaffTab == 0 || selectedStaffTab == 1)
            ) {
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            paymentAmount = ""
                            selectedSalaryMonth = java.time.YearMonth.now().toString()
                            showSalaryMonthMenu = false
                            paymentNote = ""
                            paymentMode = "Cash"
                            paymentDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                .format(Date())
                            showPaymentDialog = true
                        },
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
                        Text(CurrencySettings.symbol, style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            if (!loading && staff != null && !showRevisionHistory && !showAdvancePage && !showBonusPage) {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    containerColor = Color.White,
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets(0.dp)
                ) {
                    NavigationBarItem(
                        selected = selectedStaffTab == 0,
                        onClick = { selectedStaffTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                            selectedTextColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                            indicatorColor = if (isPremium) Color(0xFFFFF0C2) else Color(0xFFE3F2FD)
                        ),
                        icon = {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "Payment"
                            )
                        },
                        label = {
                            Text(
                                "Payment",
                                fontWeight = if (selectedStaffTab == 0)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                    NavigationBarItem(
                        selected = selectedStaffTab == 1,
                        onClick = { selectedStaffTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                            selectedTextColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                            indicatorColor = if (isPremium) Color(0xFFFFF0C2) else Color(0xFFE3F2FD)
                        ),
                        icon = {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Salary"
                            )
                        },
                        label = {
                            Text(
                                "Salary",
                                fontWeight = if (selectedStaffTab == 1)
                                    FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            } else if (!loading && staff != null && showRevisionHistory) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            editingRevision = null
                            salaryAmount = ""
                            salaryNote = ""
                            salaryEffectiveDate = SimpleDateFormat(
                                "yyyy-MM-dd", Locale.US
                            ).format(Date())
                            showRevisionDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Revise Salary")
                    }
                }
            } else if (!loading && staff != null && showAdvancePage) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            advanceDialogType = "GIVE"
                            advanceAmount = ""
                            advanceDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            advanceNote = ""
                            advanceError = ""
                            showAdvanceDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pay Advance")
                    }
                }
            } else if (!loading && staff != null && showBonusPage) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            bonusAmount = ""
                            bonusDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            editingBonus = null
                            bonusNote = ""
                            bonusError = ""
                            showBonusDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Bonus")
                    }
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showBonusPage) "Bonus Pay" else if (showAdvancePage) "Advance Pay" else if (showRevisionHistory) "Salary Revision History"
                        else (staff?.name ?: "Staff Details"),
                        modifier = if (showRevisionHistory || staff == null) Modifier
                        else Modifier.clickable { showStaffEditDialog = true }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (showBonusPage) showBonusPage = false else if (showAdvancePage) showAdvancePage = false else if (showRevisionHistory) showRevisionHistory = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back")
                    }
                },
                actions = {}
            )
        }
    ) { padding ->
        if (loading) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { androidx.compose.material3.CircularProgressIndicator() }
        } else if (staff == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text("Staff member not found.")
                TextButton(onClick = onBack) { Text("Back") }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedStaffTab == 1 && !showAdvancePage && !showBonusPage && !showRevisionHistory) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE9D9B5)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(13.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(58.dp),
                                        shape = RoundedCornerShape(19.dp),
                                        color = Color(0xFFFFF0C2),
                                        contentColor = Color(0xFF8A5B13)
                                    ) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Text(
                                                staff!!.name.trim().split(Regex("\\s+"))
                                                    .filter { it.isNotBlank() }.take(2)
                                                    .joinToString("") { it.take(1).uppercase() }
                                                    .ifBlank { "S" },
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            staff!!.name,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF202124)
                                        )
                                        Surface(shape = RoundedCornerShape(50.dp), color = Color(0xFFF5F1E8)) {
                                            Text(
                                                staff!!.role.ifBlank { "Staff member" },
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF795A27)
                                            )
                                        }
                                    }
                                }

                                androidx.compose.material3.HorizontalDivider(color = Color(0xFFF0E8D9))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "MOBILE NUMBER",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF8A8D93),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            staff!!.mobile.ifBlank { "Not provided" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF30343B)
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Text(
                                            "JOINING DATE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF8A8D93),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            staffDate(staff!!.joiningDate),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF30343B)
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(19.dp),
                                    color = Color(0xFFFFF9EB),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1E1BD))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "CURRENT MONTHLY SALARY",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF896A35),
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            staffMoney(currentSalary),
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF493719)
                                        )
                                        androidx.compose.material3.HorizontalDivider(color = Color(0xFFEADBB9))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Total recorded payments",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF786B55)
                                            )
                                            Text(
                                                staffMoney(totalPaid),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF493719)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showRevisionHistory) {
                    item {
                        Spacer(Modifier.height(4.dp))
                    }
                    if (revisions.isEmpty()) {
                        item {
                            Text(
                                "No salary revisions recorded yet.",
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    } else {
                        val latestRevisionId = revisions.maxWithOrNull(
                            compareBy<SalaryRevisionEntity> { it.effectiveDate }
                                .thenBy { it.id }
                        )?.id

                        items(revisions, key = { "revision-${it.id}" }) { revision ->
                            val isLatestRevision = revision.id == latestRevisionId
                            Card(
                                modifier = if (isLatestRevision) {
                                    Modifier.clickable {
                                        editingRevision = revision
                                        salaryAmount = revision.amount.toString()
                                        salaryEffectiveDate = SimpleDateFormat(
                                            "yyyy-MM-dd", Locale.US
                                        ).format(Date(revision.effectiveDate))
                                        salaryNote = revision.note
                                        showRevisionDialog = true
                                    }
                                } else {
                                    Modifier
                                },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text(
                                        staffMoney(revision.amount),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Effective ${staffDate(revision.effectiveDate)}")
                                    if (revision.note.isNotBlank()) Text(revision.note)
                                }
                            }
                        }
                    }
                 } else if (showAdvancePage) {
                    item {
                        
                    }
                    if (advances.isEmpty()) {
                        item { Text("No advance transactions recorded yet.") }
                    } else {
                        items(advances, key = { "advance-${it.id}" }) { advance ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text(
                                        "Advance given • ${staffMoney(advance.amount)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(staffDate(advance.transactionDate))
                                    if (advance.note.isNotBlank()) Text(advance.note)
                                }
                            }
                        }
                    }
                } else if (showBonusPage) {
                    item {
                        Spacer(Modifier.height(12.dp))
                    }

                    if (bonuses.isEmpty()) {
                        item {
                            Text("No bonus payments recorded yet.")
                        }
                    } else {
                        items(bonuses, key = { "bonus-${it.id}" }) { bonus ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingBonus = bonus
                                        bonusAmount = bonus.amount.toString()
                                        bonusDate = SimpleDateFormat(
                                            "yyyy-MM-dd", Locale.US
                                        ).format(Date(bonus.bonusDate))
                                        bonusNote = bonus.note
                                        bonusError = ""
                                        showBonusDialog = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "Bonus paid • ${staffMoney(bonus.amount)}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(staffDate(bonus.bonusDate))
                                    if (bonus.note.isNotBlank()) Text(bonus.note)
                                }
                            }
                        }
                    }

                } else if (selectedStaffTab == 1) {
                    item {
                        OutlinedButton(
                            onClick = {
                                val ym = runCatching {
                                    java.time.YearMonth.parse(selectedSalaryMonth)
                                }.getOrElse { java.time.YearMonth.now() }
                                pickerYear = ym.year
                                pickerMonth = ym.monthValue
                                showYearDropdown = false
                                showSalaryMonthMenu = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                runCatching {
                                    java.time.YearMonth.parse(selectedSalaryMonth)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH))
                                }.getOrDefault(selectedSalaryMonth),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.width(6.dp))

                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    Text("Monthly Entitlement", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        staffMoney(salaryForSelectedMonth),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Card(
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                                    if (remainingSalary >= -0.009) {
                                        Text("Remaining Salary", style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            staffMoney(remainingSalary),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (remainingSalary < -0.009) {
                                        Spacer(Modifier.height(6.dp))
                                        Text("Excess Paid", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            staffMoney(-remainingSalary),
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Button(
                            onClick = { showBonusPage = false; showAdvancePage = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Advance Pay  →")
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = { showRevisionHistory = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Revise Salary")
                        }
                    }
                    item {
                        OutlinedButton(
                            onClick = {
                                showAdvancePage = false
                                showBonusPage = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Bonus Pay")
                        }
                    }
                
} else {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(15.dp))
                                    .clickable { showPaymentYearMenu = true },
                                shape = RoundedCornerShape(15.dp),
                                color = Color(0xFFFFF4D7),
                                contentColor = Color(0xFF694710),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp, Color(0xFFEAD39A)
                                ),
                                shadowElevation = 1.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 15.dp, vertical = 11.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "Filter by year"
                                    )
                                    Text(
                                        selectedPaymentYear,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("▾", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (showPaymentYearMenu) {
                            AlertDialog(
                                onDismissRequest = { showPaymentYearMenu = false },
                                containerColor = Color.White,
                                shape = RoundedCornerShape(26.dp),
                                title = {
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(
                                            "Filter payments",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Select a year to view its payment history",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF777777)
                                        )
                                    }
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        paymentYears.chunked(3).forEach { yearRow ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                yearRow.forEach { year ->
                                                    val selected = year == selectedPaymentYear
                                                    Surface(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(13.dp))
                                                            .clickable {
                                                                selectedPaymentYear = year
                                                                showPaymentYearMenu = false
                                                            },
                                                        shape = RoundedCornerShape(13.dp),
                                                        color = if (selected) Color(0xFFFFC247)
                                                            else Color(0xFFF6F6F6),
                                                        contentColor = if (selected) Color(0xFF302000)
                                                            else Color(0xFF333333),
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            if (selected) Color(0xFFE0A92F)
                                                            else Color(0xFFEAEAEA)
                                                        )
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 13.dp),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Text(
                                                                year,
                                                                fontWeight = if (selected)
                                                                    FontWeight.Bold
                                                                else FontWeight.Medium
                                                            )
                                                        }
                                                    }
                                                }
                                                repeat(3 - yearRow.size) {
                                                    Spacer(Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = { showPaymentYearMenu = false }
                                    ) {
                                        Text("Done")
                                    }
                                }
                            )
                        }
                    }
                    if (combinedPaymentHistory.isEmpty()) {
                        item {
                            Text("No payment or advance transactions recorded for $selectedPaymentYear.")
                        }
                    } else {
                        items(
                            combinedPaymentHistory,
                            key = { entry ->
                                when (entry) {
                                    is SalaryPaymentEntity -> "payment-${entry.id}"
                                    is StaffAdvanceTransactionEntity -> "advance-history-${entry.id}"
                                    else -> entry.hashCode().toString()
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is SalaryPaymentEntity -> {
                                    Card(
                                        modifier = Modifier.clickable {
                                            editingPayment = entry
                                            editPaymentAmount = entry.amount.toString()
                                            editPaymentDate = SimpleDateFormat(
                                                "yyyy-MM-dd", Locale.US
                                            ).format(Date(entry.paymentDate))
                                            editPaymentMode = entry.paymentMode
                                            editPaymentNote = entry.note
                                            editSalaryMonth = entry.salaryMonth
                                            selectedSalaryMonth = entry.salaryMonth
                                            showPaymentDialog = true
                                        },
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            Modifier.fillMaxWidth().padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                "Salary Payment • ${staffMoney(entry.amount)}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text("${staffDate(entry.paymentDate)} • ${entry.paymentMode}")
                                            Text(
                                                "Salary Month: " + run {
                                                    val parsedMonth = try {
                                                        java.text.SimpleDateFormat(
                                                            "yyyy-MM", java.util.Locale.US
                                                        ).parse(entry.salaryMonth)
                                                    } catch (_: Exception) {
                                                        null
                                                    }
                                                    if (parsedMonth != null) {
                                                        java.text.SimpleDateFormat(
                                                            "MMM yyyy", java.util.Locale.US
                                                        ).format(parsedMonth)
                                                    } else {
                                                        entry.salaryMonth
                                                    }
                                                }
                                            )
                                            if (entry.note.isNotBlank()) Text(entry.note)
                                        }
                                    }
                                }

                                is StaffAdvanceTransactionEntity -> {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            Modifier.fillMaxWidth().padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                (if (entry.transactionType == "GIVE")
                                                    "Advance Paid" else "Advance Adjustment") +
                                                    " • ${staffMoney(entry.amount)}",
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(staffDate(entry.transactionDate))
                                            if (entry.note.isNotBlank()) Text(entry.note)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (showStaffEditDialog) {
        val currentStaff = staff
        if (currentStaff != null) {
            StaffFormDialog(
                title = "Edit Staff",
                initialName = currentStaff.name,
                initialMobile = currentStaff.mobile,
                initialRole = currentStaff.role,
                initialJoiningDate = currentStaff.joiningDate,
                showDelete = true,
                saving = isSavingStaffEdit,
                onDismiss = { if (!isSavingStaffEdit) showStaffEditDialog = false },
                onDelete = {
                    showStaffEditDialog = false
                    showDeleteStaffConfirm = true
                },
                onSave = { name, mobile, role, date, _ ->
                    isSavingStaffEdit = true
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val staffDao = LedgerDatabaseProvider.get(context).staffDao()
                                staffDao.updateStaff(
                                    currentStaff.copy(
                                        name = name,
                                        mobile = mobile,
                                        role = role,
                                        joiningDate = date
                                    )
                                )
                            }
                            staff = withContext(Dispatchers.IO) {
                                LedgerDatabaseProvider.get(context).staffDao().getStaff(staffId)
                            }
                            showStaffEditDialog = false
                            onDataChanged()
                        } catch (error: Exception) {
                            android.util.Log.e("LedgerStaff", "Unable to update staff", error)
                        } finally {
                            isSavingStaffEdit = false
                        }
                    }
                }
            )
        }
    }

    if (showDeleteStaffConfirm) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { showDeleteStaffConfirm = false },
            title = { Text("Delete Staff?") },
            text = {
                Text(
                    "This permanently deletes this staff member and their salary revisions " +
                        "and payment history. This action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteStaffConfirm = false
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    val staffDao = LedgerDatabaseProvider.get(context).staffDao()
                                    staffDao.deleteSalaryRevisionsForStaff(staffId)
                                    staffDao.deleteSalaryPaymentsForStaff(staffId)
                                    staffDao.deleteStaffAdvanceTransactionsForStaff(staffId)
                                    staffDao.deleteStaffById(staffId)
                                }
                                onDataChanged()
                                onBack()
                            } catch (error: Exception) {
                                android.util.Log.e("LedgerStaff", "Unable to delete staff", error)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteStaffConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showRevisionDialog) {
        val singleRevisionEdit = revisions.size == 1 && editingRevision != null
        val minimumRevisionDate = remember(revisions, editingRevision, staff) {
            if (singleRevisionEdit) {
                0L
            } else {
                val previousRevision = revisions
                    .filter { it.id != editingRevision?.id }
                    .maxWithOrNull(
                        compareBy<SalaryRevisionEntity> { it.effectiveDate }
                            .thenBy { it.id }
                    )
                val baseDate = previousRevision?.effectiveDate ?: staff?.joiningDate ?: 0L
                if (baseDate > 0L) {
                    java.util.Calendar.getInstance().apply {
                        timeInMillis = baseDate
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    0L
                }
            }
        }

        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                showRevisionDialog = false
                editingRevision = null
            },
            title = { Text("Revise Salary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        salaryAmount, { salaryAmount = it },
                        label = { Text("New monthly salary (${CurrencySettings.symbol})") },
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = salaryEffectiveDate,
                        onValueChange = {},
                        label = { Text("Effective date") },
                        shape = RoundedCornerShape(10.dp),
                        readOnly = true,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = {
                                val initialDate = staffDateInput(salaryEffectiveDate) ?: System.currentTimeMillis()
                                val calendar = java.util.Calendar.getInstance().apply {
                                    timeInMillis = initialDate
                                }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        salaryEffectiveDate = String.format(
                                            Locale.US, "%04d-%02d-%02d",
                                            year, month + 1, day
                                        )
                                    },
                                    calendar.get(java.util.Calendar.YEAR),
                                    calendar.get(java.util.Calendar.MONTH),
                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                ).apply {
                                    if (!singleRevisionEdit) datePicker.minDate = minimumRevisionDate
                                }.show()
                            }) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = "Choose effective date"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        salaryNote, { salaryNote = it },
                        label = { Text("Reason / note") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingRevision != null) {
                        androidx.compose.material3.OutlinedButton(
                            enabled = revisions.size > 1,
                            onClick = { showDeleteRevisionConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Delete", maxLines = 1, color = Color(0xFFB91C1C))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                showRevisionDialog = false
                                editingRevision = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", maxLines = 1)
                        }
                        Button(
                            enabled = (salaryAmount.toDoubleOrNull() ?: -1.0) >= 0.0 &&
                                (staffDateInput(salaryEffectiveDate)?.let {
                                    singleRevisionEdit || it >= minimumRevisionDate
                                } == true),
                            onClick = {
                                val amount = salaryAmount.toDoubleOrNull() ?: return@Button
                                val date = staffDateInput(salaryEffectiveDate) ?: return@Button
                                if (!singleRevisionEdit && date < minimumRevisionDate) return@Button
                                val original = editingRevision
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        val staffDao = LedgerDatabaseProvider.get(context).staffDao()
                                        if (original == null) {
                                            staffDao.insertSalaryRevision(
                                                SalaryRevisionEntity(
                                                    staffId = staffId,
                                                    amount = amount,
                                                    effectiveDate = date,
                                                    note = salaryNote.trim()
                                                )
                                            )
                                        } else {
                                            staffDao.updateSalaryRevision(
                                                original.copy(
                                                    amount = amount,
                                                    effectiveDate = date,
                                                    note = salaryNote.trim()
                                                )
                                            )
                                        }
                                    }
                                    showRevisionDialog = false
                                    editingRevision = null
                                    reload()
                                    onDataChanged()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save", maxLines = 1)
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }



    if (showDeleteRevisionConfirm && editingRevision != null) {
        val revisionToDelete = editingRevision!!
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { showDeleteRevisionConfirm = false },
            title = { Text("Delete Salary Revision?") },
            text = {
                Text("This salary revision will be permanently deleted.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                LedgerDatabaseProvider.get(context).staffDao()
                                    .deleteSalaryRevisionById(revisionToDelete.id, staffId)
                            }
                            showDeleteRevisionConfirm = false
                            showRevisionDialog = false
                            editingRevision = null
                            reload()
                            onDataChanged()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB91C1C)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = { showDeleteRevisionConfirm = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSalaryMonthMenu) {
        AlertDialog(
            onDismissRequest = {
                showSalaryMonthMenu = false
                showYearDropdown = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Select Salary Month",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Year",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF64748B)
                    )

                    Box {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showYearDropdown = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                pickerYear.toString(),
                                modifier = Modifier.weight(1f)
                            )
                            Text("⌄")
                        }

                        DropdownMenu(
                            expanded = showYearDropdown,
                            onDismissRequest = { showYearDropdown = false },
                            modifier = Modifier.heightIn(max = 280.dp),
                            containerColor = Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 8.dp
                        ) {
                            val thisYear = java.time.YearMonth.now().year
                            (thisYear - 24..thisYear + 24)
                                .toList()
                                .sortedDescending()
                                .forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year.toString()) },
                                        onClick = {
                                            pickerYear = year
                                            showYearDropdown = false
                                        }
                                    )
                                }
                        }
                    }

                    Text(
                        "Month",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF64748B)
                    )

                    val monthLabels = listOf(
                        "Jan", "Feb", "Mar",
                        "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep",
                        "Oct", "Nov", "Dec"
                    )

                    monthLabels.chunked(3).forEachIndexed { rowIndex, rowMonths ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEachIndexed { colIndex, label ->
                                val monthNumber = rowIndex * 3 + colIndex + 1
                                val selected = pickerMonth == monthNumber

                                if (selected) {
                                    Button(
                                        onClick = { pickerMonth = monthNumber },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 2.dp,
                                            vertical = 10.dp
                                        )
                                    ) {
                                        Text(label, maxLines = 1)
                                    }
                                } else {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { pickerMonth = monthNumber },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                            horizontal = 2.dp,
                                            vertical = 10.dp
                                        )
                                    ) {
                                        Text(label, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedSalaryMonth = java.time.YearMonth
                            .of(pickerYear, pickerMonth)
                            .toString()
                        paymentAmount = ""
                        showYearDropdown = false
                        showSalaryMonthMenu = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showYearDropdown = false
                        showSalaryMonthMenu = false
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }



    if (showBonusDialog) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                showBonusDialog = false
                editingBonus = null
                bonusError = ""
            },
            title = { Text(if (editingBonus == null) "Bonus Pay" else "Edit Bonus") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = bonusAmount,
                        onValueChange = { input ->
                            if (input.isEmpty() ||
                                input.matches(Regex("\\d*(\\.\\d{0,2})?"))) {
                                bonusAmount = input
                                bonusError = ""
                            }
                        },
                        label = { Text("Bonus Amount (${CurrencySettings.symbol})") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                    value = bonusDate,
                    onValueChange = {},
                    label = { Text("Bonus date") },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            val calendar = java.util.Calendar.getInstance()
                            runCatching {
                                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(bonusDate)
                            }.getOrNull()?.let { calendar.time = it }

                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    bonusDate = String.format(
                                        Locale.US, "%04d-%02d-%02d",
                                        year, month + 1, day
                                    )
                                    bonusError = ""
                                },
                                calendar.get(java.util.Calendar.YEAR),
                                calendar.get(java.util.Calendar.MONTH),
                                calendar.get(java.util.Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Select bonus date"
                            )
                        }
                    }
                )
                    OutlinedTextField(
                        value = bonusNote,
                        onValueChange = { bonusNote = it },
                        label = { Text("Note (optional)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (bonusError.isNotBlank()) {
                        Text(bonusError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingBonus != null) {
                        OutlinedButton(
                            onClick = { showDeleteBonusConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Delete Bonus",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    OutlinedButton(
                    onClick = {
                        showBonusDialog = false
                        editingBonus = null
                        bonusError = ""
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", maxLines = 1)
                }
                    Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    
                    onClick = {
                        val amount = bonusAmount.toDoubleOrNull()
                        val date = staffDateInput(bonusDate)
                        when {
                            amount == null || amount <= 0.0 ->
                                bonusError = "Enter a valid amount greater than zero."
                            date == null ->
                                bonusError = "Enter a valid date in yyyy-MM-dd format."
                            else -> {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        LedgerDatabaseProvider.get(context).staffDao()
                                            .let { staffDao ->
                                                val existing = editingBonus
                                                if (existing == null) {
                                                    staffDao.insertStaffBonusPayment(
                                                        StaffBonusEntity(
                                                            staffId = staffId,
                                                            amount = amount,
                                                            bonusDate = date,
                                                            note = bonusNote.trim()
                                                        )
                                                    )
                                                } else {
                                                    staffDao.updateStaffBonusPayment(
                                                        existing.copy(
                                                            amount = amount,
                                                            bonusDate = date,
                                                            note = bonusNote.trim()
                                                        )
                                                    )
                                                }
                                            }
                                    }
                                    showBonusDialog = false
                                    editingBonus = null
                                    bonusError = ""
                                    reload()
                                    onDataChanged()
                                }
                            }
                        }
                    },
                    enabled = !loading
                ) { Text("Save") }
                    }
                }
            },
            dismissButton = {}        )
    }


    if (showDeleteBonusConfirm) {
        val bonusToDelete = editingBonus
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = { showDeleteBonusConfirm = false },
            title = { Text("Delete bonus?") },
            text = { Text("This bonus payment will be permanently deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        if (bonusToDelete != null) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    LedgerDatabaseProvider.get(context).staffDao()
                                        .deleteStaffBonusPaymentById(
                                            bonusToDelete.id, staffId
                                        )
                                }
                                showDeleteBonusConfirm = false
                                showBonusDialog = false
                                editingBonus = null
                                bonusError = ""
                                reload()
                                onDataChanged()
                            }
                        } else {
                            showDeleteBonusConfirm = false
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteBonusConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdvanceDialog) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                showAdvanceDialog = false
                advanceError = ""
            },
            title = {
                Text(if (advanceDialogType == "GIVE") "Give Advance" else "Adjust Advance")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = advanceAmount,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("\\d*(\\.\\d{0,2})?"))) {
                                advanceAmount = input
                                advanceError = ""
                            }
                        },
                        label = { Text("Amount (${CurrencySettings.symbol})") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = advanceDate,
                        onValueChange = {},
                        label = { Text("Advance date") },
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = java.util.Calendar.getInstance()
                                runCatching {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        .parse(advanceDate)
                                }.getOrNull()?.let { calendar.time = it }

                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        advanceDate = String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            year,
                                            month + 1,
                                            day
                                        )
                                        advanceError = ""
                                    },
                                    calendar.get(java.util.Calendar.YEAR),
                                    calendar.get(java.util.Calendar.MONTH),
                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = "Select advance date"
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = advanceNote,
                        onValueChange = { advanceNote = it },
                        label = { Text("Note (optional)") },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (advanceDialogType == "ADJUST") {
                        Text("Outstanding: ${staffMoney(advanceOutstanding)}")
                    }
                    if (advanceError.isNotBlank()) {
                        Text(advanceError, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            showAdvanceDialog = false
                            advanceError = ""
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", maxLines = 1)
                    }
Button(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    onClick = {
                        val amount = advanceAmount.toDoubleOrNull()
                        val date = staffDateInput(advanceDate)
                        when {
                            amount == null || amount <= 0.0 -> {
                                advanceError = "Enter a valid amount greater than zero."
                            }
                            date == null -> {
                                advanceError = "Enter a valid date in yyyy-MM-dd format."
                            }
                            advanceDialogType == "ADJUST" && amount > advanceOutstanding + 0.001 -> {
                                advanceError = "Adjustment cannot exceed outstanding advance."
                            }
                            else -> {
                                advanceScope.launch {
                                    withContext(Dispatchers.IO) {
                                        LedgerDatabaseProvider.get(context).staffDao()
                                            .insertStaffAdvanceTransaction(
                                                StaffAdvanceTransactionEntity(
                                                    staffId = staffId,
                                                    transactionType = advanceDialogType,
                                                    amount = amount,
                                                    transactionDate = date,
                                                    note = advanceNote.trim()
                                                )
                                            )
                                    }
                                    showAdvanceDialog = false
                                    advanceError = ""
                                    reload()
                                }
                            }
                        }
                    },
                    enabled = !loading
                ) { Text(if (advanceDialogType == "GIVE") "Save" else "Save Adjustment") }
                }
            },
            dismissButton = {}

        )
    }

    if (showPaymentDialog) {
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                showPaymentDialog = false
                editingPayment = null
            },
            title = { Text(if (editingPayment == null) "Record Salary Payment" else "Edit Salary Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = paymentAmount,
                            onValueChange = { input ->
                                if (input.isEmpty() ||
                                    input.matches(Regex("\\d*(\\.\\d{0,2})?"))) {
                                    paymentAmount = input
                                }
                            },
                            label = { Text("Amount (${CurrencySettings.symbol})") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )

                        androidx.compose.material3.OutlinedButton(
                            onClick = {
                                val parsed = runCatching {
                                    java.time.YearMonth.parse(selectedSalaryMonth)
                                }.getOrElse { java.time.YearMonth.now() }

                                pickerYear = parsed.year
                                pickerMonth = parsed.monthValue
                                showYearDropdown = false
                                showSalaryMonthMenu = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 8.dp
                            )
                        ) {
                            Text(
                                text = selectedSalaryMonthLabel,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Select salary month"
                            )
                        }
                    }

                    Text(
                        text = if (remainingSalary < -0.009) {
                            "Excess paid: ${staffMoney(-remainingSalary)}"
                        } else {
                            "Remaining salary: ${staffMoney(remainingSalary)}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (remainingSalary > 0.0) {
                            Color(0xFF15803D)
                        } else {
                            Color(0xFFB91C1C)
                        }
                    )

                    OutlinedTextField(
                        value = paymentDate,
                        onValueChange = {},
                        label = { Text("Payment date") },
                            shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = {
                                val calendar = java.util.Calendar.getInstance()
                                runCatching {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        .parse(paymentDate)
                                }.getOrNull()?.let { calendar.time = it }

                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        paymentDate = String.format(
                                            Locale.US,
                                            "%04d-%02d-%02d",
                                            year, month + 1, day
                                        )
                                    },
                                    calendar.get(java.util.Calendar.YEAR),
                                    calendar.get(java.util.Calendar.MONTH),
                                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = "Choose payment date"
                                )
                            }
                        }
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Payment mode",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (paymentMode == "Cash") {
                                Button(
                                    onClick = { paymentMode = "Cash" },
                                    modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cash")
                                }
                            } else {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { paymentMode = "Cash" },
                                    modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Cash")
                                }
                            }

                            if (paymentMode == "Online") {
                                Button(
                                    onClick = { paymentMode = "Online" },
                                    modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Online")
                                }
                            } else {
                                androidx.compose.material3.OutlinedButton(
                                    onClick = { paymentMode = "Online" },
                                    modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Online")
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        paymentNote, { paymentNote = it },
                        label = { Text("Note") },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (editingPayment != null) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { showDeletePaymentConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Delete", maxLines = 1, color = Color(0xFFB91C1C))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showPaymentDialog = false
                            editingPayment = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel", maxLines = 1)
                    }
                    Button(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        enabled = (paymentAmount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                            staffDateInput(paymentDate) != null,
                        onClick = {
                            val amount = paymentAmount.toDoubleOrNull() ?: return@Button
                            val original = editingPayment
                            if (amount <= 0.0) {
                                return@Button
                            }
                            val date = staffDateInput(paymentDate) ?: return@Button
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    val dao = LedgerDatabaseProvider.get(context).staffDao()
                                    if (original == null) {
                                        dao.insertSalaryPayment(
                                            SalaryPaymentEntity(
                                                staffId = staffId,
                                                amount = amount,
                                                paymentDate = date,
                                                paymentMode = paymentMode.trim().ifBlank { "Cash" },
                                                salaryMonth = selectedSalaryMonth,
                                                note = paymentNote.trim()
                                            )
                                        )
                                    } else {
                                        dao.updateSalaryPayment(
                                            original.copy(
                                                amount = amount,
                                                paymentDate = date,
                                                paymentMode = paymentMode.trim().ifBlank { "Cash" },
                                                salaryMonth = selectedSalaryMonth,
                                                note = paymentNote.trim()
                                            )
                                        )
                                    }
                                }
                                showPaymentDialog = false
                                editingPayment = null
                                reload()
                                onDataChanged()
                            }
                        }
                    ) {
                        Text(
                            if (editingPayment == null) "Save Payment" else "Save Changes",
                            maxLines = 1,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showDeletePaymentConfirm && editingPayment != null) {
        AlertDialog(
            onDismissRequest = {
                showDeletePaymentConfirm = false
                showPaymentDialog = true
            },
            containerColor = Color.White,
            title = { Text("Delete Payment?") },
            text = { Text("This payment record will be permanently deleted. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        val target = editingPayment ?: return@Button
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                LedgerDatabaseProvider.get(context).staffDao()
                                    .deleteSalaryPaymentById(target.id, staffId)
                            }
                            showDeletePaymentConfirm = false
                            showPaymentDialog = false
                            editingPayment = null
                            reload()
                            onDataChanged()
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB91C1C)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Delete") }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showDeletePaymentConfirm = false
                        showPaymentDialog = true
                    },
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Cancel") }
            }
        )
    }

}
