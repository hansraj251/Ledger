package com.ledger.app.ui

import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.layout.width

import androidx.compose.foundation.layout.size

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import java.time.LocalDate
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
    addExpenseRequest: Int = 0
) {
    val scope = rememberCoroutineScope()

    var data by remember(groupId) {
        mutableStateOf(CloudGroupExpenseData(emptyList(), emptyList(), emptyList()))
    }
    var loading by remember(groupId) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var showEditor by remember { mutableStateOf(false) }
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

    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Cash") }
    var splitMode by remember { mutableStateOf("Equal") }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var customAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var shareUnits by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var paidAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    val joinedMembers = members.filter { it.status == "joined" }
    val youMember = joinedMembers.firstOrNull {
        currentUserId != null && it.userId == currentUserId
    }

    suspend fun refreshExpenses() {
        loading = true
        error = null
        try {
            data = withContext(Dispatchers.IO) {
                repository.loadGroupExpenses(groupId)
            }
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
        showEditor = true
    }

    fun openEditExpense(expense: CloudGroupExpense) {
        editingExpense = expense
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

        showEditor = true
    }

    LaunchedEffect(groupId) {
        refreshExpenses()
    }

    LaunchedEffect(addExpenseRequest) {
        if (addExpenseRequest > 0) openNewExpense()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Expenses (${data.expenses.size})",
                style = MaterialTheme.typography.titleMedium
            )
            // Add Expense action is provided by the floating button.
        }

        if (loading) {
            CircularProgressIndicator()
        }

        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (!loading && data.expenses.isEmpty()) {
            Text(
                "No expenses added yet.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (data.expenses.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    data.expenses.sortedByDescending { it.expenseDateMs },
                    key = { it.id }
                ) { expense ->
                    Card(modifier = Modifier.fillMaxWidth()) {
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (expense.note.isNotBlank()) {
                                Text(expense.note, style = MaterialTheme.typography.bodySmall)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    enabled = enabled && !busy,
                                    onClick = { openEditExpense(expense) }
                                ) {
                                    Text("Edit")
                                }
                                TextButton(
                                    enabled = enabled && !busy,
                                    onClick = { deletingExpense = expense }
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
        }
        }
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
                            if (editingExpense == null) "Add group expense" else "Edit group expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Split and track group spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                            onValueChange = { title = it },
                            label = { Text("Title") },
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            singleLine = true,
                            enabled = !busy,
                            modifier = Modifier.weight(1f),
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

                    Row(
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
                        joinedMembers.filter {
                            it.memberName.contains(memberSearch, ignoreCase = true)
                        }.forEach { member ->
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = member.id in selectedIds,
                                    enabled = !busy,
                                    onCheckedChange = { checked ->
                                        selectedIds = if (checked) selectedIds + member.id
                                        else selectedIds - member.id
                                    }
                                )
                                Text(member.memberName, modifier = Modifier.weight(1f))
                                if (member.id in selectedIds && splitMode == "Custom") {
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
                        enabled = !busy && totalForPaidBy > 0.0 && youMember != null,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Paid By · ₹${"%.2f".format(paidTotal + youRemaining)}")
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
                                (amount.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                selectedIds.isNotEmpty() && youMember != null,
                            onClick = {
                                val total = amount.toDoubleOrNull() ?: return@Button
                                val selectedMembers = joinedMembers.filter { it.id in selectedIds }
                                try {
                                    val splitDrafts = CloudGroupExpenseDraftCalculator.buildSplits(
                                        totalAmount = total,
                                        mode = splitMode,
                                        selectedMembers = selectedMembers,
                                        customAmounts = customAmounts.mapNotNull { (id, value) ->
                                            value.toDoubleOrNull()?.let { id to it }
                                        }.toMap(),
                                        shareUnits = shareUnits.mapNotNull { (id, value) ->
                                            value.toDoubleOrNull()?.let { id to it }
                                        }.toMap()
                                    )
                                    val paymentDrafts = CloudGroupExpenseDraftCalculator.buildPayments(
                                        totalAmount = total,
                                        members = joinedMembers,
                                        youMemberId = youMember!!.id,
                                        otherMemberAmounts = paidAmounts.mapValues { (_, value) ->
                                            value.toDoubleOrNull() ?: 0.0
                                        },
                                        paymentType = category
                                    )
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
                    Text("Enter how much each member paid. Use 0 if they paid nothing.")
                    joinedMembers.forEach { member ->
                        val isYou = member.id == youMember?.id
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expenseDateMs
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            expenseDateMs = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { if (!busy) deletingExpense = null },
            title = { Text("Delete Expense?") },
            text = { Text("Delete '${expense.title}' and its split/payment records?") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        deletingExpense = null
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
