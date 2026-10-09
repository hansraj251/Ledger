package com.ledger.app.ui

import androidx.compose.foundation.layout.width

import androidx.compose.material.icons.outlined.Search

import androidx.compose.material.icons.outlined.MoreVert

import androidx.compose.ui.draw.clip

import androidx.compose.foundation.background

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Category
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

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
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == "parties",
            onClick = onPartiesClick,
            icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
            label = { Text("Parties", fontWeight = if (selectedTab == "parties") FontWeight.SemiBold else FontWeight.Normal) },
                    )
        NavigationBarItem(
            selected = selectedTab == "expenses",
            onClick = onExpensesClick,
            icon = { Icon(Icons.Outlined.ReceiptLong, contentDescription = null) },
            label = { Text("Daily Expense", fontWeight = if (selectedTab == "expenses") FontWeight.SemiBold else FontWeight.Normal) },
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
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

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

    val visibleExpenses = remember(expenses, searchQuery) {
        val query = searchQuery.trim()
        if (query.isBlank()) {
            expenses
        } else {
            expenses.filter { expense ->
                expense.category.contains(query, ignoreCase = true) ||
                    expense.note.contains(query, ignoreCase = true) ||
                    expense.amount.toString().contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                color = androidx.compose.ui.graphics.Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        placeholder = { Text("Search") }
                    )

                    Spacer(Modifier.width(8.dp))

                    Box {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp
                        ) {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .width(250.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Today's records") },
                                onClick = {
                                    showMenu = false
                                    showAllDates = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("All records") },
                                onClick = {
                                    showMenu = false
                                    showAllDates = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Parties") },
                                onClick = {
                                    showMenu = false
                                    onPartiesClick()
                                }
                            )
                        }
                    }
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
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Text("Add Expense", fontWeight = FontWeight.SemiBold)
                }
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


            if (isLoading) {
                item { Text("Loading expenses…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (visibleExpenses.isEmpty()) {
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
                            Text(
                                if (searchQuery.isNotBlank() && expenses.isNotEmpty())
                                    "No matching expenses"
                                else
                                    "No expenses yet",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                if (searchQuery.isNotBlank() && expenses.isNotEmpty())
                                    "Try another search term."
                                else
                                    "Tap + to record your first expense.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(visibleExpenses, key = { it.id }) { expense ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                editingExpense = expense
                                showExpenseDialog = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        )
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
                                    imageVector = expenseCategoryIcon(expense.category),
                                    contentDescription = expense.category,
                                    modifier = Modifier.padding(11.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    expense.category,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                if (expense.note.isNotBlank()) {
                                    Text(
                                        expense.note,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    expenseDateText(expense.expenseDate),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                expenseMoney(expense.amount),
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                maxLines = 1
                            )
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
            onDelete = {
                deletingExpense = editingExpense
                showExpenseDialog = false
            },
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
    onDelete: () -> Unit,
    onSave: (Double, String, String, Long) -> Unit
) {
    val context = LocalContext.current
    var amount by remember(initial?.id) {
        mutableStateOf(
            initial?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var category by remember(initial?.id) {
        mutableStateOf(initial?.category ?: expenseCategories.first())
    }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var date by remember(initial?.id) {
        mutableStateOf(initial?.expenseDate ?: System.currentTimeMillis())
    }
    var showCategoryPage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.padding(11.dp).size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (initial == null) "Add expense" else "Edit expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Record your spending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Amount (₹)") },
                    leadingIcon = {
                        Text(
                            "₹",
                            modifier = Modifier.padding(start = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryPage = true },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Icon(
                                imageVector = expenseCategoryIcon(category),
                                contentDescription = null,
                                modifier = Modifier.padding(9.dp).size(22.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Category",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.ArrowForwardIos,
                            contentDescription = "Choose category",
                            modifier = Modifier.size(17.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
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
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(Modifier.size(9.dp))
                        Text("Date: ${expenseDateText(date)}")
                    }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Note") },
                        leadingIcon = {
                            Icon(Icons.Outlined.EditNote, contentDescription = null)
                        },
                        singleLine = true
                    )
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                if (initial != null) {
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
                            Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "Delete Expense",
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
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val parsed = amount.trim().replace(",", "").toDoubleOrNull()
                            if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                                error = "Enter a valid amount greater than zero."
                            } else {
                                onSave(parsed, category, note, date)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }

    if (showCategoryPage) {
        Dialog(
            onDismissRequest = { showCategoryPage = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(0.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showCategoryPage = false }) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                contentDescription = "Back to expense"
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Choose category",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Select where you spent",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(expenseCategories.chunked(2)) { categoryRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                categoryRow.forEach { item ->
                                    val selected = item == category
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable {
                                                category = item
                                                showCategoryPage = false
                                            },
                                        shape = RoundedCornerShape(18.dp),
                                        color = if (selected)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceContainerLow,
                                        tonalElevation = if (selected) 2.dp else 0.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(
                                                horizontal = 12.dp,
                                                vertical = 16.dp
                                            ),
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(13.dp),
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Icon(
                                                    imageVector = expenseCategoryIcon(item),
                                                    contentDescription = null,
                                                    modifier = Modifier.padding(11.dp).size(25.dp),
                                                    tint = if (selected)
                                                        MaterialTheme.colorScheme.onPrimary
                                                    else
                                                        MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Text(
                                                item,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (selected)
                                                    FontWeight.Bold
                                                else
                                                    FontWeight.Medium,
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                                else
                                                    MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                if (categoryRow.size == 1) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun expenseCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        "Groceries" -> Icons.Outlined.ShoppingCart
        "Restaurant" -> Icons.Outlined.Restaurant
        "Coffee" -> Icons.Outlined.Coffee
        "Shopping" -> Icons.Outlined.ShoppingBag
        "Public transit" -> Icons.Outlined.DirectionsBus
        "Car" -> Icons.Outlined.DirectionsCar
        "Utilities" -> Icons.Outlined.Bolt
        "Health" -> Icons.Outlined.FavoriteBorder
        "Entertainment" -> Icons.Outlined.Movie
        "Subscriptions" -> Icons.Outlined.CreditCard
        "Bills" -> Icons.Outlined.ReceiptLong
        "Clothing" -> Icons.Outlined.Checkroom
        else -> Icons.Outlined.Category
    }
}
