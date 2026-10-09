package com.ledger.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ledger.app.data.DailyExpenseEntity
import com.ledger.app.data.LedgerDatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val expenseCategories = listOf(
    "Groceries", "Restaurant", "Coffee", "Shopping", "Public transit",
    "Car", "Utilities", "Health", "Entertainment", "Subscriptions",
    "Bills", "Clothing", "Other"
)

private fun expenseMoney(amount: Double): String =
    NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)

private fun expenseDateText(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))

private fun startOfToday(): Long =
    Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

@Composable
fun LedgerBottomNavigation(
    selectedTab: String,
    onPartiesClick: () -> Unit,
    onExpensesClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == "parties",
            onClick = onPartiesClick,
            icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
            label = { Text("Parties") }
        )
        NavigationBarItem(
            selected = selectedTab == "expenses",
            onClick = onExpensesClick,
            icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null) },
            label = { Text("Daily Expense") }
        )
    }
}

@Composable
fun DailyExpenseScreen(
    onPartiesClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) {
        LedgerDatabaseProvider.get(context.applicationContext)
    }
    val dao = remember(database) { database.dailyExpenseDao() }
    val scope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf<List<DailyExpenseEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAllDates by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableStateOf(0) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<DailyExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<DailyExpenseEntity?>(null) }

    LaunchedEffect(showAllDates, refreshKey) {
        isLoading = true
        expenses = withContext(Dispatchers.IO) {
            val all = dao.getAll()
            if (showAllDates) all else {
                val start = startOfToday()
                val end = start + 24L * 60 * 60 * 1000
                all.filter { it.expenseDate >= start && it.expenseDate < end }
            }
        }
        isLoading = false
    }

    val total = expenses.sumOf { it.amount }

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 15.dp)
                ) {
                    Text(
                        "Daily Expense",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Track your everyday spending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        bottomBar = {
            LedgerBottomNavigation(
                selectedTab = "expenses",
                onPartiesClick = onPartiesClick,
                onExpensesClick = {}
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingExpense = null
                    showExpenseDialog = true
                },
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add expense")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            if (showAllDates) "TOTAL EXPENSES" else "TODAY'S EXPENSE",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            expenseMoney(total),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "${expenses.size} ${if (expenses.size == 1) "record" else "records"}",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!showAllDates) {
                        Button(
                            onClick = { showAllDates = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Today") }
                    } else {
                        OutlinedButton(
                            onClick = { showAllDates = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Today") }
                    }
                    if (showAllDates) {
                        Button(
                            onClick = { showAllDates = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("All records") }
                    } else {
                        OutlinedButton(
                            onClick = { showAllDates = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("All records") }
                    }
                }
            }

            item {
                Text(
                    if (showAllDates) "All expenses" else "Today's records",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoading) {
                item { Text("Loading expenses…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (expenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                modifier = Modifier.size(42.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No expenses yet", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Tap + to record your first expense.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(13.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Icon(
                                    Icons.Outlined.AccountBalanceWallet,
                                    contentDescription = null,
                                    modifier = Modifier.padding(11.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            ) {
                                Text(expense.category, fontWeight = FontWeight.SemiBold)
                                Text(
                                    expense.note.ifBlank { expenseDateText(expense.expenseDate) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (expense.note.isNotBlank()) {
                                    Text(
                                        expenseDateText(expense.expenseDate),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    expenseMoney(expense.amount),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Column {
                                IconButton(onClick = {
                                    editingExpense = expense
                                    showExpenseDialog = true
                                }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = "Edit expense")
                                }
                                IconButton(onClick = { deletingExpense = expense }) {
                                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete expense")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showExpenseDialog) {
        ExpenseEditorDialog(
            initial = editingExpense,
            onDismiss = { showExpenseDialog = false },
            onSave = { amount, category, note, date ->
                val old = editingExpense
                scope.launch {
                    withContext(Dispatchers.IO) {
                        if (old == null) {
                            dao.insert(
                                DailyExpenseEntity(
                                    amount = amount,
                                    category = category,
                                    note = note.trim(),
                                    expenseDate = date
                                )
                            )
                        } else {
                            dao.update(
                                old.copy(
                                    amount = amount,
                                    category = category,
                                    note = note.trim(),
                                    expenseDate = date
                                )
                            )
                        }
                    }
                    showExpenseDialog = false
                    refreshKey++
                }
            }
        )
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete expense?") },
            text = { Text("Delete ${expense.category} expense of ${expenseMoney(expense.amount)}?") },
            confirmButton = {
                Button(onClick = {
                    deletingExpense = null
                    scope.launch {
                        withContext(Dispatchers.IO) { dao.delete(expense) }
                        refreshKey++
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ExpenseEditorDialog(
    initial: DailyExpenseEntity?,
    onDismiss: () -> Unit,
    onSave: (Double, String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var amount by remember(initial?.id) {
        mutableStateOf(initial?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "")
    }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: expenseCategories.first()) }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var date by remember(initial?.id) { mutableStateOf(initial?.expenseDate ?: System.currentTimeMillis()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add expense" else "Edit expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Box {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().clickable { categoryExpanded = true },
                        readOnly = true,
                        label = { Text("Category") }
                    )
                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        expenseCategories.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    category = item
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val c = Calendar.getInstance().apply { timeInMillis = date }
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val selected = Calendar.getInstance()
                                selected.set(year, month, day, 12, 0, 0)
                                selected.set(Calendar.MILLISECOND, 0)
                                date = selected.timeInMillis
                            },
                            c.get(Calendar.YEAR),
                            c.get(Calendar.MONTH),
                            c.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Date: ${expenseDateText(date)}") }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Note (optional)") },
                    singleLine = true
                )
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val parsed = amount.trim().replace(",", "").toDoubleOrNull()
                if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                    error = "Enter a valid amount greater than zero."
                } else {
                    onSave(parsed, category, note, date)
                }
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
