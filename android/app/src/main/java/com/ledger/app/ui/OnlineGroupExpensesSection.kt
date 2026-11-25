package com.ledger.app.ui

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.Instant
import java.time.format.DateTimeFormatter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledger.app.cloudgroup.CloudGroupExpense
import com.ledger.app.cloudgroup.CloudGroupExpenseData
import com.ledger.app.cloudgroup.CloudGroupExpenseDraftCalculator
import com.ledger.app.cloudgroup.CloudGroupExpensePaymentDraft
import com.ledger.app.cloudgroup.CloudGroupExpenseSplitDraft
import com.ledger.app.cloudgroup.CloudGroupMember
import com.ledger.app.cloudgroup.CloudGroupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnlineGroupExpensesSection(
    groupId: String,
    members: List<CloudGroupMember>,
    currentUserId: String?,
    repository: CloudGroupRepository,
    enabled: Boolean,
    expenseData: CloudGroupExpenseData,
    expenseDataLoaded: Boolean,
    onExpenseDataChange: (CloudGroupExpenseData) -> Unit,
    onAddExpenseRequestConsumed: () -> Unit,
    addExpenseRequest: Int = 0
) {
    val scope = rememberCoroutineScope()

    val data = expenseData
    var loading by remember(groupId) {
        mutableStateOf(!expenseDataLoaded)
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedMonth by remember(groupId) { mutableStateOf(YearMonth.now()) }
    var monthMenuExpanded by remember { mutableStateOf(false) }

    val monthFormatter = remember {
        DateTimeFormatter.ofPattern("MMMM yyyy")
    }
    val availableMonths = remember(data.expenses, selectedMonth) {
        (data.expenses.map {
            YearMonth.from(
                Instant.ofEpochMilli(it.expenseDateMs).atZone(ZoneOffset.UTC)
            )
        } + selectedMonth).distinct().sortedDescending()
    }
    val filteredExpenses = remember(data.expenses, selectedMonth) {
        data.expenses.filter { expense ->
            YearMonth.from(
                Instant.ofEpochMilli(expense.expenseDateMs).atZone(ZoneOffset.UTC)
            ) == selectedMonth
        }.sortedByDescending { it.expenseDateMs }
    }
    var showEditor by remember { mutableStateOf(false) }
    var settlementMode by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPaidByDialog by remember { mutableStateOf(false) }
    var showMemberSearch by remember { mutableStateOf(false) }
    var memberSearch by remember { mutableStateOf("") }
    var expenseDateMs by remember(groupId) {
        mutableStateOf(
            LocalDate.now().atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli()
        )
    }
    var editingExpense by remember { mutableStateOf<CloudGroupExpense?>(null) }
    var deletingExpense by remember { mutableStateOf<CloudGroupExpense?>(null) }
    var expandedExpenseIds by remember(groupId) { mutableStateOf<Set<String>>(emptySet()) }

    var title by remember { mutableStateOf("") }
    val settlementTitlePattern = remember {
        Regex(
            """\b(?:settle(?:d|ment|ments)?|settling)\b""",
            RegexOption.IGNORE_CASE
        )
    }

    val hasReservedSettlementTitle = !settlementMode &&
        Regex(
            """\bsettle(?:d|ment|ments|ing)?\b""",
            RegexOption.IGNORE_CASE
        ).containsMatchIn(title)

    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Cash") }
    var splitMode by remember { mutableStateOf("Equal") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var shareUnits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var paidAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var settlementPayerId by remember { mutableStateOf<String?>(null) }

    val joinedMembers = members.filter { it.status == "joined" }
    val youMember = joinedMembers.firstOrNull {
        currentUserId != null && it.userId == currentUserId
    }

    suspend fun refreshExpenses() {
        loading = data.expenses.isEmpty() && !expenseDataLoaded
        error = null
        try {
            val loadedData = withContext(Dispatchers.IO) {
                repository.loadGroupExpenses(groupId)
            }
            onExpenseDataChange(loadedData)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Could not load expenses."
        } finally {
            loading = false
        }
    }

    fun runAction(action: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = null
            try {
                withContext(Dispatchers.IO) { action() }
                refreshExpenses()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Expense operation failed."
            } finally {
                busy = false
            }
        }
    }

    fun openNewExpense() {
        settlementMode = false
        editingExpense = null
        expenseDateMs = LocalDate.now().atStartOfDay(ZoneOffset.UTC)
            .toInstant().toEpochMilli()
        title = ""
        amount = ""
        note = ""
        category = "Cash"
        splitMode = "Equal"
        selectedIds = joinedMembers.map { it.id }.toSet()
        customAmounts = joinedMembers.associate { it.id to "" }
        shareUnits = joinedMembers.associate { it.id to "1" }
        paidAmounts = joinedMembers
            .filter { it.id != youMember?.id }
            .associate { it.id to "" }
        settlementPayerId = null
        showEditor = true
    }

    fun openEditExpense(expense: CloudGroupExpense) {
        editingExpense = expense
        settlementMode = expense.title.trim().equals("Settled", ignoreCase = true)
        expenseDateMs = expense.expenseDateMs
        title = expense.title
        amount = expense.amount.toString()
        note = expense.note
        category = expense.category
        splitMode = expense.splitMode

        val oldSplits = data.splits.filter { it.expenseId == expense.id }
        selectedIds = oldSplits.map { it.memberId }.toSet()
        customAmounts = oldSplits.associate {
            it.memberId to it.amount.toString()
        }
        shareUnits = oldSplits.associate {
            it.memberId to it.shareUnits.toString()
        }

        val oldPayments = data.payments.filter { it.expenseId == expense.id }
        paidAmounts = oldPayments
            .filter { it.memberId != youMember?.id }
            .associate { it.memberId to it.amount.toString() }
        settlementPayerId = if (settlementMode) {
            oldPayments.singleOrNull()?.memberId
        } else null

        showEditor = true
    }

    LaunchedEffect(groupId) {
        if (!expenseDataLoaded) {
            refreshExpenses()
        }
    }

    LaunchedEffect(addExpenseRequest) {
        if (addExpenseRequest > 0) {
            openNewExpense()
            onAddExpenseRequestConsumed()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { monthMenuExpanded = true },
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF263449)
                    )
                ) {
                    Text(
                        "▦",
                        color = Color(0xFF3975C6),
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        selectedMonth.format(monthFormatter),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("⌄", color = Color(0xFF64748B))
                }

                DropdownMenu(
                    expanded = monthMenuExpanded,
                    onDismissRequest = { monthMenuExpanded = false },
                    modifier = Modifier.background(Color.White),
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color.White,
                    tonalElevation = 5.dp,
                    shadowElevation = 8.dp
                ) {
                    availableMonths.forEach { month ->
                        val isSelected = month == selectedMonth

                        DropdownMenuItem(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .background(
                                    color = if (isSelected) Color(0xFFEAF2FF)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            text = {
                                Text(
                                    month.format(monthFormatter),
                                    color = if (isSelected) Color(0xFF245DA8)
                                    else Color(0xFF263449),
                                    fontWeight = if (isSelected) FontWeight.Bold
                                    else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Text(
                                    if (isSelected) "✓" else " ",
                                    color = Color(0xFF2872C8),
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            onClick = {
                                selectedMonth = month
                                monthMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }

        if (loading) {
            CircularProgressIndicator()
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (!loading && filteredExpenses.isEmpty()) {
            val emptyMessage = if (data.expenses.isEmpty()) {
                "No expenses added yet."
            } else {
                "No transactions for ${selectedMonth.format(monthFormatter)}."
            }
            Text(
                emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (filteredExpenses.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    filteredExpenses,
                    key = { it.id }
                ) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                enabled = enabled && !busy,
                                onClick = {
                                    expandedExpenseIds =
                                        if (expense.id in expandedExpenseIds) {
                                            expandedExpenseIds - expense.id
                                        } else {
                                            expandedExpenseIds + expense.id
                                        }
                                },
                                onLongClick = { openEditExpense(expense) }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFE8B64A)),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(
                            defaultElevation = 3.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    expense.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "₹${"%.2f".format(expense.amount)}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Text(
                                "${expense.splitMode} • ${expense.category}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF626B78)
                            )
                            Text(
                                java.text.SimpleDateFormat(
                                    "dd MMM yyyy",
                                    java.util.Locale.getDefault()
                                ).format(java.util.Date(expense.expenseDateMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF737B87)
                            )
                            if (expense.id in expandedExpenseIds) {
                                if (expense.note.isNotBlank()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFF5F7FA),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = expense.note,
                                            modifier = Modifier.padding(10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF424A55)
                                        )
                                    }
                                }

                                val expenseSplits = data.splits.filter {
                                    it.expenseId == expense.id
                                }
                                val expensePayments = data.payments.filter {
                                    it.expenseId == expense.id
                                }
                                val displayedMemberIds = (
                                    expenseSplits.map { it.memberId } +
                                    expensePayments.map { it.memberId }
                                ).distinct()

                                displayedMemberIds.forEach { memberId ->
                                    val split = expenseSplits.firstOrNull {
                                        it.memberId == memberId
                                    }
                                    val memberPayments = expensePayments.filter {
                                        it.memberId == memberId
                                    }
                                    val memberName = split?.memberNameSnapshot
                                        ?: memberPayments.firstOrNull()?.memberNameSnapshot
                                        ?: ""
                                    val shareAmount = split?.amount ?: 0.0
                                    val memberPaid = memberPayments.sumOf { it.amount }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                memberName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = if (memberPaid > 0.009)
                                                    Color(0xFF166534)
                                                else Color(0xFF202124)
                                            )
                                            Text(
                                                if (memberPaid > 0.009)
                                                    "Paid ₹${"%.2f".format(memberPaid)}"
                                                else "Not paid",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (memberPaid > 0.009)
                                                    Color(0xFF166534)
                                                else Color(0xFF737B87)
                                            )
                                        }
                                        Text(
                                            "₹${"%.2f".format(shareAmount)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF202124)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 2.dp),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Hold to Edit",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF8A9099)
                                    )
                                }
                            } else {
                                Text(
                                    "Tap to view details",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF8A9099)
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }

    fun updateSettlementMode(checked: Boolean) {
        if (busy || editingExpense != null) return

        settlementMode = checked
        title = if (checked) "Settled" else ""
        splitMode = if (checked) "Custom" else "Equal"
        selectedIds = if (checked) emptySet()
            else joinedMembers.map { it.id }.toSet()
        customAmounts = if (checked) emptyMap()
            else joinedMembers.associate { it.id to "" }
        shareUnits = joinedMembers.associate { it.id to "1" }
        paidAmounts = if (checked) emptyMap()
            else joinedMembers.filter { it.id != youMember?.id }
                .associate { it.id to "" }
        settlementPayerId = null
    }

    if (showEditor) {
        AlertDialog(
            modifier = Modifier.fillMaxSize(),
            properties = DialogProperties(usePlatformDefaultWidth = false),
            onDismissRequest = { if (!busy) showEditor = false },
            shape = androidx.compose.ui.graphics.RectangleShape,
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.padding(11.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            when {
                                settlementMode -> "Settlement"
                                editingExpense == null -> "Add group expense"
                                else -> "Edit group expense"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (settlementMode) "Settle amounts between group members"
                            else "Split and track group spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settlementMode,
                        onCheckedChange = { checked ->
                            updateSettlementMode(checked)
                        },
                        enabled = !busy && editingExpense == null
                    )
                }
            },
            text = {
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { newTitle ->
                                if (!settlementMode) {
                                    if (settlementTitlePattern.containsMatchIn(newTitle)) {
                                        if (editingExpense == null) {
                                            updateSettlementMode(true)
                                        }
                                    } else {
                                        title = newTitle
                                    }
                                }
                            },
                            label = { Text("Title") },
                            singleLine = true,
                            readOnly = settlementMode,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(15.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.weight(1f).height(56.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(15.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Payment type",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Cash", "Online").forEach { option ->
                                val selected = category == option
                                Surface(
                                    modifier = Modifier.weight(1f).clickable(
                                        enabled = !busy
                                    ) { category = option },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) Color(0xFFB8C9F5)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            option,
                                            fontWeight = if (selected) FontWeight.SemiBold
                                            else FontWeight.Medium,
                                            color = if (selected)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.weight(0.9f).clickable {
                                if (!busy) showDatePicker = true
                            }
                        ) {
                            val dateText = Instant.ofEpochMilli(expenseDateMs)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            OutlinedTextField(
                                value = dateText,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("Date") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(15.dp),
                                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledContainerColor = MaterialTheme.colorScheme.surface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("Note") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(15.dp)
                        )
                    }

                    if (!settlementMode) Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("Equal", "Custom", "Share").forEach { option ->
                            val selected = splitMode == option
                            Surface(
                                modifier = Modifier.weight(1f).clickable(enabled = !busy) {
                                    splitMode = option
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
                                contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface,
                                border = if (selected) null else BorderStroke(
                                    1.dp, MaterialTheme.colorScheme.primary
                                ),
                                tonalElevation = if (selected) 2.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        option,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold
                                        else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Members",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        IconButton(onClick = {
                            showMemberSearch = !showMemberSearch
                            if (!showMemberSearch) memberSearch = ""
                        }) {
                            Icon(Icons.Outlined.Search, contentDescription = "Search members")
                        }
                    }

                    if (showMemberSearch) {
                        OutlinedTextField(
                            value = memberSearch,
                            onValueChange = { memberSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search members") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        (if (settlementMode) {
                            joinedMembers.filter {
                                it.allowSettlementByOthers ||
                                    (currentUserId != null && it.userId == currentUserId) ||
                                    it.id in selectedIds
                            }
                        } else {
                            joinedMembers
                        }).filter {
                            it.memberName.contains(memberSearch, ignoreCase = true)
                        }.forEach { member ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = member.id in selectedIds,
                                    enabled = !busy && (
                                        member.allowSettlementByOthers ||
                                            (currentUserId != null && member.userId == currentUserId) ||
                                            member.id in selectedIds
                                        ),
                                    onCheckedChange = { checked ->
                                        if (settlementMode) {
                                            selectedIds = if (checked) setOf(member.id) else emptySet()
                                            if (checked && settlementPayerId == member.id) {
                                                settlementPayerId = null
                                                paidAmounts = emptyMap()
                                            }
                                        } else {
                                            selectedIds = if (checked) selectedIds + member.id
                                            else selectedIds - member.id
                                            if (checked) paidAmounts = paidAmounts - member.id
                                        }
                                    }
                                )
                                Text(member.memberName, modifier = Modifier.weight(1f))
                                if (settlementMode && !member.allowSettlementByOthers &&
                                    !(currentUserId != null && member.userId == currentUserId)
                                ) {
                                    Text(
                                        "Permission off",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                if (!settlementMode && member.id in selectedIds && splitMode == "Custom") {
                                    OutlinedTextField(
                                        value = customAmounts[member.id] ?: "",
                                        onValueChange = {
                                            customAmounts = customAmounts + (member.id to it)
                                        },
                                        label = {
                                            Text("Amount", fontSize = 10.sp, maxLines = 1)
                                        },
                                        singleLine = true,
                                        enabled = !busy,
                                        modifier = Modifier.height(52.dp).width(112.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp, lineHeight = 14.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                } else if (member.id in selectedIds && splitMode == "Share") {
                                    OutlinedTextField(
                                        value = shareUnits[member.id] ?: "1",
                                        onValueChange = {
                                            shareUnits = shareUnits + (member.id to it)
                                        },
                                        label = {
                                            Text("Shares", fontSize = 10.sp, maxLines = 1)
                                        },
                                        singleLine = true,
                                        enabled = !busy,
                                        modifier = Modifier.height(52.dp).width(112.dp),
                                        textStyle = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.sp, lineHeight = 14.sp
                                        ),
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    error?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (editingExpense != null) {
                        OutlinedButton(
                            onClick = { deletingExpense = editingExpense },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Delete expense")
                        }
                    }

                    val paidTotal = paidAmounts.values.sumOf {
                        it.trim().toDoubleOrNull()?.takeIf { v -> v.isFinite() && v > 0.0 } ?: 0.0
                    }
                    val totalForPaidBy = amount.toDoubleOrNull()
                        ?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
                    val youRemaining = (totalForPaidBy - paidTotal).coerceAtLeast(0.0)

                    OutlinedButton(
                        onClick = { showPaidByDialog = true },
                        enabled = !busy && totalForPaidBy > 0.0 &&
                            (if (settlementMode) selectedIds.size == 1 &&
                                joinedMembers.any { it.id !in selectedIds }
                            else youMember != null),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (settlementMode) {
                                val payerName = joinedMembers.firstOrNull {
                                    it.id == settlementPayerId
                                }?.memberName ?: "Select payer"
                                "Paid By · $payerName · ₹${"%.2f".format(totalForPaidBy)}"
                            } else {
                                "Paid By · ₹${"%.2f".format(paidTotal + youRemaining)}"
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditor = false },
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) { Text("Cancel") }

                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !busy && title.isNotBlank() &&
                                !hasReservedSettlementTitle &&
                                (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                selectedIds.isNotEmpty() &&
                                (if (settlementMode) {
                                    selectedIds.size == 1 && settlementPayerId != null &&
                                        settlementPayerId !in selectedIds &&
                                        joinedMembers.any { member ->
                                            member.id in selectedIds && (
                                                member.allowSettlementByOthers ||
                                                    (currentUserId != null && member.userId == currentUserId)
                                                )
                                        }
                                } else youMember != null),
                            onClick = {
                                if (hasReservedSettlementTitle) return@Button
                                val total = amount.toDoubleOrNull() ?: return@Button
                                val selectedMembers = joinedMembers.filter { it.id in selectedIds }
                                try {
                                    val splitDrafts = CloudGroupExpenseDraftCalculator.buildSplits(
                                        totalAmount = total,
                                        mode = if (settlementMode) "Custom" else splitMode,
                                        selectedMembers = selectedMembers,
                                        customAmounts = if (settlementMode) {
                                            mapOf(selectedMembers.single().id to total)
                                        } else customAmounts.mapNotNull { (id, value) ->
                                            value.toDoubleOrNull()?.let { id to it }
                                        }.toMap(),
                                        shareUnits = shareUnits.mapNotNull { (id, value) ->
                                            value.toDoubleOrNull()?.let { id to it }
                                        }.toMap()
                                    )
                                    val paymentDrafts = if (settlementMode) {
                                        CloudGroupExpenseDraftCalculator.buildManualPayments(
                                            totalAmount = total,
                                            members = joinedMembers,
                                            payerAmounts = mapOf(requireNotNull(settlementPayerId) to total),
                                            excludedMemberIds = selectedIds,
                                            paymentType = category
                                        )
                                    } else {
                                        CloudGroupExpenseDraftCalculator.buildPayments(
                                            totalAmount = total,
                                            members = joinedMembers,
                                            youMemberId = youMember!!.id,
                                            otherMemberAmounts = paidAmounts.mapValues { (_, value) ->
                                                value.toDoubleOrNull() ?: 0.0
                                            },
                                            paymentType = category
                                        )
                                    }
                                    val existing = editingExpense
                                    showEditor = false
                                    runAction {
                                        repository.saveGroupExpense(
                                            groupId = groupId,
                                            expenseId = existing?.id,
                                            title = title.trim(),
                                            amount = total,
                                            category = category,
                                            note = note.trim(),
                                            expenseDateMs = expenseDateMs,
                                            splitMode = splitMode,
                                            splits = splitDrafts,
                                            payments = paymentDrafts
                                        )
                                    }
                                } catch (e: Exception) {
                                    error = e.message ?: "Invalid expense details."
                                }
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(if (editingExpense == null) "Save" else "Update")
                        }
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showPaidByDialog) {
        val total = amount.toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        val otherPaid = paidAmounts.values.sumOf {
            it.trim().toDoubleOrNull()?.takeIf { v -> v.isFinite() && v >= 0.0 } ?: 0.0
        }
        val remaining = (total - otherPaid).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showPaidByDialog = false },
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = { Text("Paid By") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (settlementMode) {
                        Text("Select the member who paid the full settlement amount.")

                        joinedMembers.filter { it.id !in selectedIds }.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        settlementPayerId = member.id
                                        paidAmounts = mapOf(member.id to total.toString())
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settlementPayerId == member.id,
                                    onClick = {
                                        settlementPayerId = member.id
                                        paidAmounts = mapOf(member.id to total.toString())
                                    }
                                )
                                Text(member.memberName)
                            }
                        }
                    } else {
                    Text("Enter how much each member paid. Use 0 if they paid nothing.")
                    joinedMembers.filter { !settlementMode || it.id !in selectedIds }
                        .forEach { member ->
                        val isYou = !settlementMode && member.id == youMember?.id
                        val value = if (isYou) String.format(java.util.Locale.US, "%.2f", remaining)
                        else paidAmounts[member.id].orEmpty()

                        OutlinedTextField(
                            value = value,
                            onValueChange = { newValue ->
                                if (!isYou) {
                                    val parsed = newValue.trim().toDoubleOrNull()
                                    if (newValue.isBlank() || (parsed != null &&
                                            parsed.isFinite() && parsed >= 0.0)) {
                                        val otherTotal = paidAmounts
                                            .filterKeys { it != member.id }
                                            .values.sumOf {
                                                it.trim().toDoubleOrNull()
                                                    ?.takeIf { v -> v.isFinite() && v >= 0.0 }
                                                    ?: 0.0
                                            }
                                        if ((parsed ?: 0.0) + otherTotal <= total + 0.000001) {
                                            paidAmounts = paidAmounts + (member.id to newValue)
                                        }
                                    }
                                }
                            },
                            label = {
                                Text(if (isYou) "${member.memberName} · Remaining (Auto)"
                                else member.memberName)
                            },
                            singleLine = true,
                            readOnly = isYou,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPaidByDialog = false }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showPaidByDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val selectedDate = Instant.ofEpochMilli(expenseDateMs)
            .atZone(ZoneOffset.UTC)
            .toLocalDate()

        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                expenseDateMs = LocalDate.of(year, month + 1, day)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
                showDatePicker = false
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { if (!busy) deletingExpense = null },
            containerColor = androidx.compose.ui.graphics.Color.White,
            title = { Text("Delete Expense?") },
            text = { Text("Delete '${expense.title}' and its split/payment records?") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        deletingExpense = null
                        showEditor = false
                        runAction {
                            repository.deleteGroupExpense(groupId, expense.id)
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !busy,
                    onClick = { deletingExpense = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
