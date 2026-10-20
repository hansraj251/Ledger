package com.ledger.app.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Download
import androidx.core.content.FileProvider
import android.content.Intent
import com.ledger.app.data.CurrencySettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Menu
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
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Celebration
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.outlined.ChildCare
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Train
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Icecream
import androidx.compose.material.icons.outlined.LocalPizza
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding

internal val expenseCategories = listOf(
    "Groceries", "Restaurant", "Coffee", "Shopping", "Public transit",
    "Car", "Utilities", "Health", "Entertainment", "Subscriptions",
    "Bills", "Clothing", "Other"
)

private data class ExpenseCategoryIconOption(
    val key: String,
    val label: String,
    val group: String,
    val icon: ImageVector
)

private val customExpenseCategoryIcons = listOf(
    // Food & shopping — includes the existing default category icons.
    ExpenseCategoryIconOption("groceries", "Groceries", "Food & Shopping", Icons.Outlined.ShoppingCart),
    ExpenseCategoryIconOption("restaurant", "Restaurant", "Food & Shopping", Icons.Outlined.Restaurant),
    ExpenseCategoryIconOption("coffee", "Coffee", "Food & Shopping", Icons.Outlined.Coffee),
    ExpenseCategoryIconOption("shopping", "Shopping", "Food & Shopping", Icons.Outlined.ShoppingBag),
    ExpenseCategoryIconOption("fastfood", "Fast Food", "Food & Shopping", Icons.Outlined.Fastfood),
    ExpenseCategoryIconOption("pizza", "Pizza", "Food & Shopping", Icons.Outlined.LocalPizza),
    ExpenseCategoryIconOption("icecream", "Ice Cream", "Food & Shopping", Icons.Outlined.Icecream),
    ExpenseCategoryIconOption("cafe", "Cafe", "Food & Shopping", Icons.Outlined.LocalCafe),
    ExpenseCategoryIconOption("store", "Store", "Food & Shopping", Icons.Outlined.Store),
    ExpenseCategoryIconOption("mall", "Mall", "Food & Shopping", Icons.Outlined.LocalMall),

    // Transport
    ExpenseCategoryIconOption("transit", "Public Transit", "Transport", Icons.Outlined.DirectionsBus),
    ExpenseCategoryIconOption("car", "Car", "Transport", Icons.Outlined.DirectionsCar),
    ExpenseCategoryIconOption("bike", "Bicycle", "Transport", Icons.Outlined.DirectionsBike),
    ExpenseCategoryIconOption("motorcycle", "Motorcycle", "Transport", Icons.Outlined.TwoWheeler),
    ExpenseCategoryIconOption("flight", "Flight", "Transport", Icons.Outlined.Flight),
    ExpenseCategoryIconOption("train", "Train", "Transport", Icons.Outlined.Train),
    ExpenseCategoryIconOption("fuel", "Fuel", "Transport", Icons.Outlined.LocalGasStation),

    // Home & utilities
    ExpenseCategoryIconOption("home", "Home", "Home & Utilities", Icons.Outlined.Home),
    ExpenseCategoryIconOption("utilities", "Utilities", "Home & Utilities", Icons.Outlined.Bolt),
    ExpenseCategoryIconOption("light", "Electricity", "Home & Utilities", Icons.Outlined.Lightbulb),
    ExpenseCategoryIconOption("water", "Water", "Home & Utilities", Icons.Outlined.WaterDrop),
    ExpenseCategoryIconOption("wifi", "Internet", "Home & Utilities", Icons.Outlined.Wifi),
    ExpenseCategoryIconOption("phone", "Mobile", "Home & Utilities", Icons.Outlined.PhoneAndroid),
    ExpenseCategoryIconOption("cleaning", "Cleaning", "Home & Utilities", Icons.Outlined.Category),
    ExpenseCategoryIconOption("tools", "Repairs", "Home & Utilities", Icons.Outlined.Build),

    // Health & fitness
    ExpenseCategoryIconOption("health", "Health", "Health & Fitness", Icons.Outlined.FavoriteBorder),
    ExpenseCategoryIconOption("hospital", "Hospital", "Health & Fitness", Icons.Outlined.LocalHospital),
    ExpenseCategoryIconOption("medicine", "Medicine", "Health & Fitness", Icons.Outlined.Medication),
    ExpenseCategoryIconOption("medical", "Medical", "Health & Fitness", Icons.Outlined.MedicalServices),
    ExpenseCategoryIconOption("fitness", "Fitness", "Health & Fitness", Icons.Outlined.FitnessCenter),

    // Lifestyle & entertainment
    ExpenseCategoryIconOption("entertainment", "Entertainment", "Lifestyle", Icons.Outlined.Movie),
    ExpenseCategoryIconOption("gaming", "Gaming", "Lifestyle", Icons.Outlined.SportsEsports),
    ExpenseCategoryIconOption("clothing", "Clothing", "Lifestyle", Icons.Outlined.Checkroom),
    ExpenseCategoryIconOption("gift", "Gifts", "Lifestyle", Icons.Outlined.CardGiftcard),
    ExpenseCategoryIconOption("pets", "Pets", "Lifestyle", Icons.Outlined.Pets),
    ExpenseCategoryIconOption("child", "Childcare", "Lifestyle", Icons.Outlined.ChildCare),
    ExpenseCategoryIconOption("hotel", "Hotel", "Lifestyle", Icons.Outlined.Hotel),
    ExpenseCategoryIconOption("celebration", "Celebration", "Lifestyle", Icons.Outlined.Celebration),
    ExpenseCategoryIconOption("event", "Events", "Lifestyle", Icons.Outlined.Event),

    // Work, bills & money
    ExpenseCategoryIconOption("subscriptions", "Subscriptions", "Work & Money", Icons.Outlined.CreditCard),
    ExpenseCategoryIconOption("bills", "Bills", "Work & Money", Icons.Outlined.ReceiptLong),
    ExpenseCategoryIconOption("wallet", "Wallet", "Work & Money", Icons.Outlined.AccountBalanceWallet),
    ExpenseCategoryIconOption("payments", "Payments", "Work & Money", Icons.Outlined.Payments),
    ExpenseCategoryIconOption("bank", "Bank", "Work & Money", Icons.Outlined.AccountBalanceWallet),
    ExpenseCategoryIconOption("savings", "Savings", "Work & Money", Icons.Outlined.Savings),
    ExpenseCategoryIconOption("work", "Work", "Work & Money", Icons.Outlined.Work),
    ExpenseCategoryIconOption("books", "Books", "Work & Money", Icons.Outlined.MenuBook),
    ExpenseCategoryIconOption("education", "Education", "Work & Money", Icons.Outlined.School),

    // General
    ExpenseCategoryIconOption("inventory", "Inventory", "General", Icons.Outlined.Inventory),
    ExpenseCategoryIconOption("folder", "Folder", "General", Icons.Outlined.FolderOpen),
    ExpenseCategoryIconOption("other", "Other", "General", Icons.Outlined.Category)
)

private fun expenseMoney(amount: Double): String = CurrencySettings.format(amount)

private fun expenseDateText(timestamp: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))

private fun startOfExpenseMonth(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun startOfExpenseYear(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.MONTH, Calendar.JANUARY)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

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
        isPremium: Boolean = false,
onPartiesClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onGroupSharingClick: () -> Unit = {}
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        containerColor = Color.White,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets(0.dp)
    ) {
        NavigationBarItem(
            selected = selectedTab == "parties",
            onClick = onPartiesClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                selectedTextColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                indicatorColor = if (isPremium) Color(0xFFFFF0C2) else Color(0xFFE3F2FD)
            ),
            icon = { Icon(Icons.Outlined.Group, contentDescription = null) },
            label = {
                Text(
                    "Parties",
                    fontWeight = if (selectedTab == "parties") FontWeight.SemiBold else FontWeight.Normal
                )
            }
        )

        NavigationBarItem(
            selected = selectedTab == "expenses",
            onClick = onExpensesClick,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                selectedTextColor = if (isPremium) Color(0xFFB8862B) else Color(0xFF1976D2),
                indicatorColor = if (isPremium) Color(0xFFFFF0C2) else Color(0xFFE3F2FD)
            ),
            icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) },
            label = {
                Text(
                    "Daily Expense",
                    fontWeight = if (selectedTab == "expenses") FontWeight.SemiBold else FontWeight.Normal
                )
            }
        )


    }
}

private const val FREE_EXPENSE_LIMIT = 100

@Composable
fun DailyExpenseScreen(
    onPartiesClick: () -> Unit,
    onGroupSharingClick: () -> Unit,
    onProfileClick: () -> Unit,
    onExpenseDataChanged: () -> Unit,
    isPremium: Boolean,
    onUpgradeClick: () -> Unit = {

},
    openReportOnLaunch: Boolean = false,
    onReportLaunchHandled: () -> Unit = {},
    onOpenHomeSidebar: () -> Unit = { onPartiesClick() }
) {
    val context = LocalContext.current
    val database = remember(context) {
        LedgerDatabaseProvider.get(context.applicationContext)
    }
    val dao = remember(database) { database.dailyExpenseDao() }
    val scope = rememberCoroutineScope()

    var expenses by remember { mutableStateOf<List<DailyExpenseEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showMonthRecords by remember { mutableStateOf(true) }
    var showYearRecords by remember { mutableStateOf(false) }
    var showSelectedDayRecords by remember { mutableStateOf(false) }
    var selectedDayStart by remember { mutableStateOf(startOfToday()) }
    var selectedExpenseYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedMonthStart by remember {
        mutableStateOf(startOfExpenseMonth(System.currentTimeMillis()))
    }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }
    var yearPickerValue by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var pickerYear by remember {
        mutableStateOf(Calendar.getInstance().get(Calendar.YEAR))
    }
    var refreshKey by remember { mutableStateOf(0) }
    var showExpenseDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<DailyExpenseEntity?>(null) }

    var showUpgradeDialog by remember { mutableStateOf(false) }
var isReportLimitReached by remember { mutableStateOf(false) }

    var showAllTimeRecords by remember { mutableStateOf(false) }

    val openExpenseDatePicker: () -> Unit = {
    val selected = Calendar.getInstance().apply {
        timeInMillis = selectedDayStart
    }
    DatePickerDialog(
        context,
        { _, year, month, day ->
            selectedDayStart = Calendar.getInstance().apply {
                clear()
                set(year, month, day, 0, 0, 0)
            }.timeInMillis
            showAllTimeRecords = false
            showSelectedDayRecords = true
            showMonthRecords = false
            showYearRecords = false
            refreshKey++
        },
        selected.get(Calendar.YEAR),
        selected.get(Calendar.MONTH),
        selected.get(Calendar.DAY_OF_MONTH)
    ).show()
}
    val requestAddExpense: () -> Unit = {
        scope.launch {
            val expenseCount = withContext(Dispatchers.IO) {
                dao.getAll().size
            }
            if (isPremium || expenseCount < FREE_EXPENSE_LIMIT) {
                editingExpense = null
                showExpenseDialog = true
            } else {
                showUpgradeDialog = true
            }
        }
    }

    var deletingExpense by remember { mutableStateOf<DailyExpenseEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showReportOptions by remember { mutableStateOf(false) }

    LaunchedEffect(openReportOnLaunch) {
        if (openReportOnLaunch) {
            showMenu = false
            showReportOptions = true
            onReportLaunchHandled()
        }
    }
    var dailyReportPreviewFile by remember { mutableStateOf<java.io.File?>(null) }
    var dailyReportPreviewYear by remember { mutableStateOf<Int?>(null) }
    var dailyReportPreviewPeriod by remember { mutableStateOf<String?>(null) }
    var showCategoryReportPicker by remember { mutableStateOf(false) }
    var showCategoryReportMonthPicker by remember { mutableStateOf(false) }
    var reportCategoryOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedReportCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var reportMonthYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var reportMonthIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }



    val generateDailyExpenseReport: (Int?) -> Unit = { reportYear ->
        scope.launch {
            try {
                // DAILY_REPORT_FREE_LIMIT_V1: three successful reports for free users.
                val reportLimitPrefs = context.getSharedPreferences(
                    "daily_expense_report_limits", 0
                )
                if (!isPremium &&
                    reportLimitPrefs.getInt("generated_count", 0) >= 3
                ) {
                    showReportOptions = false
                    isReportLimitReached = true
                    showUpgradeDialog = true
                    return@launch
                }

                val reportExpenses = withContext(Dispatchers.IO) {
                    val all = dao.getAll()
                    if (reportYear == null) {
                        all
                    } else {
                        val start = Calendar.getInstance().apply {
                            clear()
                            set(Calendar.YEAR, reportYear)
                            set(Calendar.MONTH, Calendar.JANUARY)
                            set(Calendar.DAY_OF_MONTH, 1)
                        }.timeInMillis
                        val end = Calendar.getInstance().apply {
                            timeInMillis = start
                            add(Calendar.YEAR, 1)
                        }.timeInMillis
                        all.filter {
                            it.expenseDate >= start && it.expenseDate < end
                        }
                    }.sortedWith(
                        compareByDescending<DailyExpenseEntity> { it.expenseDate }
                            .thenByDescending { it.id }
                    )
                }

                val pdf = withContext(Dispatchers.IO) {
                    createDailyExpenseReportPdf(context, reportExpenses, reportYear)
                }

                // Count only after PDF creation succeeds; premium reports are unlimited.
                if (!isPremium) {
                    val currentCount = reportLimitPrefs.getInt("generated_count", 0)
                    reportLimitPrefs.edit()
                        .putInt("generated_count", currentCount + 1)
                        .apply()
                }

                dailyReportPreviewPeriod = null
                dailyReportPreviewYear = reportYear


                dailyReportPreviewFile = pdf
            } catch (e: Exception) {
                android.widget.Toast.makeText(
                    context,
                    "Report could not be created: ${e.message ?: "Unknown error"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val generateCategoryWiseDailyExpenseReport: (Set<String>, Int, Int) -> Unit =
        { categories, reportYear, reportMonth ->
            scope.launch {
                try {
                    if (categories.isEmpty()) {
                        android.widget.Toast.makeText(
                            context, "Select at least one category.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }

                    val reportLimitPrefs = context.getSharedPreferences(
                        "daily_expense_report_limits", 0
                    )
                    if (!isPremium &&
                        reportLimitPrefs.getInt("generated_count", 0) >= 3
                    ) {
                        showCategoryReportMonthPicker = false
                        isReportLimitReached = true
                        showUpgradeDialog = true
                        return@launch
                    }

                    val monthStart = Calendar.getInstance().apply {
                        clear()
                        set(reportYear, reportMonth, 1, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val monthEnd = Calendar.getInstance().apply {
                        timeInMillis = monthStart
                        add(Calendar.MONTH, 1)
                    }.timeInMillis

                    val reportExpenses = withContext(Dispatchers.IO) {
                        dao.getAll().filter { expense ->
                            val category = expense.category.ifBlank { "Uncategorized" }
                            category in categories &&
                                expense.expenseDate >= monthStart &&
                                expense.expenseDate < monthEnd
                        }.sortedWith(
                            compareByDescending<DailyExpenseEntity> { it.expenseDate }
                                .thenByDescending { it.id }
                        )
                    }

                    val monthLabel = listOf(
                        "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December"
                    )[reportMonth]
                    val periodLabel =
                        "Period: $monthLabel $reportYear | Categories: ${categories.sorted().joinToString(", ")}"

                    val pdf = withContext(Dispatchers.IO) {
                        createDailyExpenseReportPdf(
                            context, reportExpenses, reportYear, periodLabel
                        )
                    }

                    if (!isPremium) {
                        val currentCount = reportLimitPrefs.getInt("generated_count", 0)
                        reportLimitPrefs.edit()
                            .putInt("generated_count", currentCount + 1)
                            .apply()
                    }

                    dailyReportPreviewYear = reportYear
                    dailyReportPreviewPeriod =
                        "Category Wise • $monthLabel $reportYear • ${categories.size} categories"
                    dailyReportPreviewFile = pdf
                    showCategoryReportMonthPicker = false
                } catch (e: Exception) {
                    android.widget.Toast.makeText(
                        context,
                        "Report could not be created: ${e.message ?: "Unknown error"}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    var showCategoryChart by remember { mutableStateOf(false) }

    LaunchedEffect(showAllTimeRecords, showMonthRecords, showYearRecords, showSelectedDayRecords, selectedDayStart, selectedMonthStart, selectedExpenseYear, refreshKey) {
        isLoading = true
        expenses = withContext(Dispatchers.IO) {
            val all = dao.getAll()
            val now = System.currentTimeMillis()
            val start = when {
                showYearRecords -> Calendar.getInstance().apply {
                    set(Calendar.YEAR, selectedExpenseYear)
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                showMonthRecords -> selectedMonthStart
                showSelectedDayRecords -> selectedDayStart
                else -> startOfToday()
            }
            val end = Calendar.getInstance().apply {
                timeInMillis = start
                when {
                    showYearRecords -> add(Calendar.YEAR, 1)
                    showMonthRecords -> add(Calendar.MONTH, 1)
                    else -> add(Calendar.DAY_OF_MONTH, 1)
                }
            }.timeInMillis

            if (showAllTimeRecords) all
            else all.filter { it.expenseDate >= start && it.expenseDate < end }
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


    if (showReportOptions) {
        AlertDialog(
            onDismissRequest = { showReportOptions = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFF2D5)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF946200),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Text(
                        text = "Expense Report",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF202124)
                    )
                    Text(
                        text = "Choose the period for your report",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF737780)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showReportOptions = false
                                showAllTimeRecords = true
                                showSelectedDayRecords = false
                                showMonthRecords = false
                                showYearRecords = false
                                refreshKey++
                                generateDailyExpenseReport(null)
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF3FAF4),
                        border = BorderStroke(1.dp, Color(0xFFD8EBDD))
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 16.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE0F1E3)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        tint = Color(0xFF287746),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "All Time",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF202124)
                                )
                                Text(
                                    text = "Complete expense history",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF68716A)
                                )
                            }
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF287746)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showReportOptions = false
                                yearPickerValue = Calendar.getInstance()
                                    .get(Calendar.YEAR)
                                showYearPicker = true
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF3F6FF),
                        border = BorderStroke(1.dp, Color(0xFFDCE4FF))
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 14.dp,
                                vertical = 16.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFE3EAFF)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color(0xFF3158B8),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Select Year",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF202124)
                                )
                                Text(
                                    text = "View expenses for a specific year",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF69728A)
                                )
                            }
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF3158B8)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showReportOptions = false
                                scope.launch {
                                    val categories = withContext(Dispatchers.IO) {
                                        dao.getAll()
                                            .map { it.category.ifBlank { "Uncategorized" } }
                                            .distinct()
                                            .sorted()
                                    }
                                    reportCategoryOptions = categories
                                    selectedReportCategories = emptySet()
                                    reportMonthYear = Calendar.getInstance().get(Calendar.YEAR)
                                    reportMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
                                    showCategoryReportPicker = true
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFFFF8EB),
                        border = BorderStroke(1.dp, Color(0xFFEEDCB6))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(13.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFFFEBC2)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Outlined.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF946200),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Category Wise",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF202124)
                                )
                                Text(
                                    text = "Select multiple categories and a month",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF786747)
                                )
                            }
                            Text(
                                text = "›",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color(0xFF946200)
                            )
                        }
                    }

                }
            },
            confirmButton = {
                TextButton(onClick = { showReportOptions = false }) {
                    Text(
                        text = "Close",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }


    if (showCategoryReportPicker) {
        AlertDialog(
            onDismissRequest = { showCategoryReportPicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Select Categories", fontWeight = FontWeight.Bold)
                    Text(
                        "Choose one or more categories for the report.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (reportCategoryOptions.isEmpty()) {
                        Text("No expense categories found. Add expenses first.")
                    } else {
                        reportCategoryOptions.forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedReportCategories =
                                            if (category in selectedReportCategories) {
                                                selectedReportCategories - category
                                            } else {
                                                selectedReportCategories + category
                                            }
                                    }
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = category in selectedReportCategories,
                                    onCheckedChange = { checked ->
                                        selectedReportCategories =
                                            if (checked) selectedReportCategories + category
                                            else selectedReportCategories - category
                                    }
                                )
                                Text(category)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = selectedReportCategories.isNotEmpty(),
                    onClick = {
                        showCategoryReportPicker = false
                        showCategoryReportMonthPicker = true
                    }
                ) { Text("Select Month") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryReportPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCategoryReportMonthPicker) {
        val reportMonthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        AlertDialog(
            onDismissRequest = { showCategoryReportMonthPicker = false },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Select Month", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { reportMonthYear-- }) { Text("‹ Previous") }
                        Text(
                            reportMonthYear.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { reportMonthYear++ }) { Text("Next ›") }
                    }
                    (0..9 step 3).forEach { rowStart ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (rowStart until rowStart + 3).forEach { month ->
                                val monthButtonModifier = Modifier.weight(1f)
                                if (reportMonthIndex == month) {
                                    Button(
                                        onClick = { reportMonthIndex = month },
                                        modifier = monthButtonModifier,
                                        contentPadding = PaddingValues(horizontal = 0.dp)
                                    ) { Text(reportMonthNames[month]) }
                                } else {
                                    OutlinedButton(
                                        onClick = { reportMonthIndex = month },
                                        modifier = monthButtonModifier,
                                        contentPadding = PaddingValues(horizontal = 0.dp)
                                    ) { Text(reportMonthNames[month]) }
                                }
                            }
                        }
                    }
                    Text(
                        "Categories: ${selectedReportCategories.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        generateCategoryWiseDailyExpenseReport(
                            selectedReportCategories, reportMonthYear, reportMonthIndex
                        )
                    }
                ) { Text("Generate Report") }
            },
            dismissButton = {
                TextButton(onClick = { showCategoryReportMonthPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = { showYearPicker = false },
            containerColor = androidx.compose.ui.graphics.Color.White,
            title = { Text("Select year") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { yearPickerValue-- }) {
                        Text("‹")
                    }
                    Text(
                        yearPickerValue.toString(),
                        style = MaterialTheme.typography.titleLarge
                    )
                    TextButton(onClick = { yearPickerValue++ }) {
                        Text("›")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedExpenseYear = yearPickerValue
                        showAllTimeRecords = false
                        showSelectedDayRecords = false
                        showYearRecords = true
                        showMonthRecords = false
                        showYearPicker = false
                        generateDailyExpenseReport(yearPickerValue)
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showYearPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showMonthPicker) {
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )

        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            containerColor = androidx.compose.ui.graphics.Color.White,
            title = { Text("Select month") },
            text = {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { pickerYear-- }) {
                            Text("‹")
                        }
                        Text(pickerYear.toString())
                        TextButton(onClick = { pickerYear++ }) {
                            Text("›")
                        }
                    }

                    (0..11 step 3).forEach { rowStart ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (rowStart until rowStart + 3).forEach { month ->
                                val calendar = Calendar.getInstance().apply {
                                    timeInMillis = selectedMonthStart
                                }
                                val selected =
                                    calendar.get(Calendar.YEAR) == pickerYear &&
                                    calendar.get(Calendar.MONTH) == month

                                val chooseMonth = {
                                    selectedMonthStart = Calendar.getInstance().apply {
                                        clear()
                                        set(pickerYear, month, 1, 0, 0, 0)
                                    }.timeInMillis
                                    showMonthRecords = true
                                    showMonthPicker = false
                                }

                                if (selected) {
                                    Button(
                                        onClick = { chooseMonth() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(
                                            horizontal = 2.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Text(monthNames[month])
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { chooseMonth() },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(
                                            horizontal = 2.dp,
                                            vertical = 8.dp
                                        )
                                    ) {
                                        Text(monthNames[month])
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showCategoryChart) {
        ExpenseCategoryChartScreen(
            expenses = expenses,
            onBack = { showCategoryChart = false }
        )
    } else {
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
                        .padding(top = 6.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f).height(52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search"
                            )
                        },
                        placeholder = { Text("Search", fontSize = 14.sp, lineHeight = 16.sp) }
                    )

                    Spacer(Modifier.width(8.dp))

                    Box {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = androidx.compose.ui.graphics.Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp
                        ) {
                            IconButton(onClick = onOpenHomeSidebar) {
                                Icon(
                                    imageVector = Icons.Outlined.Menu,
                                    contentDescription = "Open navigation sidebar"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier
                                .width(250.dp)
                                .clip(androidx.compose.ui.graphics.RectangleShape)
                                .background(androidx.compose.ui.graphics.Color.White)
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
                                            text = "Add Expense",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    requestAddExpense()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Report",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showReportOptions = true
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
                isPremium = isPremium,
                onPartiesClick = onPartiesClick,
                onExpensesClick = {},
                onGroupSharingClick = onGroupSharingClick
            )
        },
        floatingActionButton = {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .combinedClickable(
                        onClick = {
                            requestAddExpense()
                        },
                        onLongClick = {
                            showMenu = false
                            showReportOptions = true
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
                        imageVector = Icons.Outlined.ReceiptLong,
                        contentDescription = "Add expense"
                    )
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
                    modifier = Modifier.fillMaxWidth().clickable { showCategoryChart = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF7E5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFFE8B64A)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            when {
                                showAllTimeRecords -> "ALL-TIME EXPENSE"
                                showYearRecords -> "YEARLY EXPENSE"
                                showMonthRecords -> "MONTHLY EXPENSE"
                                else -> "TODAY\'S EXPENSE"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF77500A)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            expenseMoney(total),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9A6700)
                        )
                        Text(
                            "${expenses.size} ${if (expenses.size == 1) "record" else "records"}",
                            color = Color(0xFF77500A)
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!showMonthRecords && !showYearRecords) {
                        Button(
                            shape = RoundedCornerShape(12.dp),
                            onClick = { openExpenseDatePicker() },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.onPrimary
                            )
                        ) { Text(
    if (!showMonthRecords && !showYearRecords) {
        java.text.SimpleDateFormat("dd-MMM-yy", java.util.Locale.getDefault())
            .format(java.util.Date(
                if (showSelectedDayRecords) selectedDayStart else startOfToday()
            ))
    } else {
        "Today"
    },
    maxLines = 1
) }
                    } else {
                        OutlinedButton(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                selectedDayStart = startOfToday()
                                showSelectedDayRecords = false
                                showMonthRecords = false
                                showYearRecords = false
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isPremium) Color(0xFFFFF7E5) else Color.White,
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.primary
                            )
                        ) { Text(
    if (!showMonthRecords && !showYearRecords) {
        java.text.SimpleDateFormat("dd-MMM-yy", java.util.Locale.getDefault())
            .format(java.util.Date(
                if (showSelectedDayRecords) selectedDayStart else startOfToday()
            ))
    } else {
        "Today"
    },
    maxLines = 1
) }
                    }

                    if (showMonthRecords) {
                        Button(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                pickerYear = Calendar.getInstance()
                                    .apply { timeInMillis = selectedMonthStart }
                                    .get(Calendar.YEAR)
                                showMonthPicker = true
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(
                                SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                    .format(Date(selectedMonthStart)),
                                maxLines = 1
                            )
                        }
                    } else {
                        OutlinedButton(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                selectedMonthStart =
                                    startOfExpenseMonth(System.currentTimeMillis())
                                showMonthRecords = true
                                showYearRecords = false
                            },
                            modifier = Modifier.weight(1.2f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.primary
                            )
                        ) { Text("This Month", maxLines = 1) }
                    }

                    if (showYearRecords) {
                        Button(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                yearPickerValue = selectedExpenseYear
                                showYearPicker = true
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary,
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(selectedExpenseYear.toString(), maxLines = 1)
                        }
                    } else {
                        OutlinedButton(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                selectedExpenseYear = Calendar.getInstance().get(Calendar.YEAR)
                                showYearRecords = true
                                showMonthRecords = false
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isPremium) Color(0xFFFFC247) else MaterialTheme.colorScheme.primary),
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isPremium) Color(0xFF302000) else MaterialTheme.colorScheme.primary
                            )
                        ) { Text("This Year", maxLines = 1) }
                    }
                }
            }

            if (isLoading) {
                item { Text("Loading expenses…", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else if (visibleExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                requestAddExpense()
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        )
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
                                    "Tap to record your expense.",
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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, Color(0xFFE8B64A)
                        ),
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
                    if (old == null && !isPremium) {
                        val expenseCount = withContext(Dispatchers.IO) {
                            dao.getAll().size
                        }
                        if (expenseCount >= FREE_EXPENSE_LIMIT) {
                            showExpenseDialog = false
                            showUpgradeDialog = true
                            return@launch
                        }
                    }
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
                    onExpenseDataChanged()
                    showExpenseDialog = false
                    refreshKey++
                }
            }
        )
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Delete expense?",
                    color = Color(0xFF202124),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Delete ${expense.category} expense of ${expenseMoney(expense.amount)}?",
                    color = Color(0xFF444746)
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 0.dp, end = 0.dp, top = 8.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { deletingExpense = null },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFB8B8B8)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF424242)
                        )
                    ) {
                        Text("Cancel")
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            deletingExpense = null
                            scope.launch {
                                withContext(Dispatchers.IO) { dao.delete(expense) }
                                onExpenseDataChanged()
                                refreshKey++
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFD32F2F)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFD32F2F)
                        )
                    ) {
                        Text("Delete")
                    }
                }
            }
        )
    }
    }

    if (showUpgradeDialog) {
        androidx.compose.material3.AlertDialog(
            containerColor = androidx.compose.ui.graphics.Color.White,
            onDismissRequest = { showUpgradeDialog = false },
            shape = RoundedCornerShape(28.dp),
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        if (isReportLimitReached)
                            "You've reached the free plan limit of 3 Daily Expense Reports. Upgrade to Premium for unlimited reports."
                        else
                            "You've reached the Free plan limit of 100 expenses.",
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
                            androidx.compose.material3.Text("✓  Unlimited expenses & parties", color = Color(0xFF202238))
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
                    androidx.compose.material3.Text("Unlock Premium")
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





    dailyReportPreviewFile?.let { previewFile ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { dailyReportPreviewFile = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { dailyReportPreviewFile = null }) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Daily Expense Report Preview",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                dailyReportPreviewPeriod ?: (if (dailyReportPreviewYear == null) "All Time"
                                else "Year: ${dailyReportPreviewYear}"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = {
                                scope.launch {
                                    try {
                                        val saved = withContext(Dispatchers.IO) {
                                            saveDailyExpenseReportPdf(
                                                context, previewFile, dailyReportPreviewYear
                                            )
                                        }
                                        android.widget.Toast.makeText(
                                            context, "Report saved: $saved",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(
                                            context, "Download failed: ${e.message}",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Download, contentDescription = "Download report")
                        }
                        IconButton(
                            onClick = {
                                try {
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", previewFile
                                    )
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Daily Expense Report")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(
                                        Intent.createChooser(intent, "Share daily expense report")
                                    )
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(
                                        context, "Share failed: ${e.message}",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Share, contentDescription = "Share report")
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        PdfPreview(file = previewFile, modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
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
    val categoryPrefs = remember(context) {
        context.getSharedPreferences(
            "daily_expense_categories",
            android.content.Context.MODE_PRIVATE
        )
    }
    val categoryDao = remember(context) {
        LedgerDatabaseProvider.get(context.applicationContext).customExpenseCategoryDao()
    }
    val categoryScope = rememberCoroutineScope()

    var customCategories by remember(categoryPrefs) {
        mutableStateOf<List<String>>(emptyList())
    }

    LaunchedEffect(categoryDao, categoryPrefs) {
        try {
            val existingRows = withContext(Dispatchers.IO) {
                categoryDao.getAll()
            }

            // Import existing preferences only when Room has no categories yet.
            if (existingRows.isEmpty()) {
                val legacyNames = categoryPrefs
                    .getStringSet("names", emptySet<String>())
                    ?.toList()
                    .orEmpty()
                    .distinct()

                if (legacyNames.isNotEmpty()) {
                    val legacyRows = legacyNames.map { name ->
                        com.ledger.app.data.CustomExpenseCategoryEntity(
                            name = name,
                            icon = categoryPrefs.getString("icon:$name", "folder")
                                ?: "folder",
                            quantityType = categoryPrefs.getString(
                                "quantity_type:$name", "Unit"
                            ) ?: "Unit"
                        )
                    }
                    withContext(Dispatchers.IO) {
                        categoryDao.insertAll(legacyRows)
                    }
                }
            }

            val rows = withContext(Dispatchers.IO) {
                categoryDao.getAll()
            }

            customCategories = rows.map { it.name }

            // Keep legacy preference keys synchronized for existing calculator/note code.
            val editor = categoryPrefs.edit()
                .putStringSet("names", rows.map { it.name }.toSet())
            rows.forEach { row ->
                editor.putString("icon:${row.name}", row.icon)
                editor.putString("quantity_type:${row.name}", row.quantityType)
            }
            editor.apply()
        } catch (e: Exception) {
            android.widget.Toast.makeText(
                context,
                "Could not load saved categories: ${e.message ?: "Database error"}",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val customCategoryIcons: Map<String, String> =
        remember(categoryPrefs, customCategories) {
            customCategories.associateWith { name ->
                categoryPrefs.getString("icon:$name", "folder") ?: "folder"
            }
        }
    val allCategories: List<String> = remember(customCategories) {
        (expenseCategories + customCategories).distinct()
    }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var editingCategoryTitle by remember { mutableStateOf<String?>(null) }
    var newCategoryTitle by remember { mutableStateOf("") }
    var newCategoryIcon by remember { mutableStateOf("folder") }
    var newCategoryQuantityType by remember { mutableStateOf("Unit") }
    var newCategoryDefaultPrice by remember { mutableStateOf("") }
    var showQuantityTypeMenu by remember { mutableStateOf(false) }
    val quantityTypeOptions = listOf(
        "Kilogram (kg)", "Gram (g)", "Milligram (mg)", "Tonne",
        "Litre (L)", "Millilitre (ml)",
        "Piece (pcs)", "Dozen", "Pair", "Pack", "Box",
        "Metre (m)", "Centimetre (cm)", "Kilometre (km)",
        "Unit", "Bag", "Bottle", "Carton"
    )
    var categoryFormError by remember { mutableStateOf("") }

    var amount by remember(initial?.id) {
        mutableStateOf(
            initial?.amount?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var showRateQuantity by remember(initial?.id) {
        mutableStateOf(false)
    }
    var rate by remember(initial?.id) {
        mutableStateOf(categoryPrefs.getString("calculator_last_rate", "") ?: "")
    }
    var quantity by remember(initial?.id) {
        mutableStateOf(categoryPrefs.getString("calculator_last_quantity", "") ?: "")
    }

    fun updateCalculatedAmount(rateText: String, quantityText: String) {
        val parsedRate = rateText.trim().replace(",", "").toDoubleOrNull()
        val parsedQuantity = quantityText.trim().replace(",", "").toDoubleOrNull()

        if (parsedRate != null && parsedQuantity != null &&
            parsedRate.isFinite() && parsedQuantity.isFinite() &&
            parsedRate >= 0.0 && parsedQuantity >= 0.0
        ) {
            val total = parsedRate * parsedQuantity
            if (total.isFinite()) {
                amount = java.math.BigDecimal.valueOf(total)
                    .stripTrailingZeros()
                    .toPlainString()
            }
        }
    }

    var category by remember(initial?.id) {
        mutableStateOf(
            initial?.category
                ?: categoryPrefs.getString("last_selected_category", null)
                    ?.takeIf { it in allCategories }
                ?: expenseCategories.first()
        )
    }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }

    fun updateQuantityNote(quantityText: String, categoryName: String = category) {
        val cleanQuantity = quantityText.trim()
        if (cleanQuantity.isBlank()) {
            note = ""
            return
        }

        val savedType = categoryPrefs.getString(
            "quantity_type:$categoryName",
            "Unit"
        ) ?: "Unit"

        // Keep the readable unit name in Note, without the abbreviation in brackets.
        val displayType = savedType.substringBefore(" (").trim()
        note = "$cleanQuantity $displayType"
    }
    var date by remember(initial?.id) {
        mutableStateOf(initial?.expenseDate ?: System.currentTimeMillis())
    }
    var showCategoryPage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp
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

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryPage = true },
                    shape = RoundedCornerShape(14.dp),
                    color = androidx.compose.ui.graphics.Color.White,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it; error = "" },
                        modifier = Modifier.weight(1f),
                        label = { Text("Amount (${CurrencySettings.symbol})") },
                        leadingIcon = {
                            Text(
                                "${CurrencySettings.symbol}",
                                modifier = Modifier.padding(start = 4.dp),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(15.dp)
                    )

                    androidx.compose.material3.IconButton(
                        onClick = {
                            if (!showRateQuantity) {
                                val selectedDefaultPrice =
                                    categoryPrefs.getString("default_price:$category", "") ?: ""
                                rate = selectedDefaultPrice
                                categoryPrefs.edit()
                                    .putString("calculator_last_rate", selectedDefaultPrice)
                                    .apply()
                                showRateQuantity = true
                                updateQuantityNote(quantity)
                                updateCalculatedAmount(selectedDefaultPrice, quantity)
                            } else {
                                showRateQuantity = false
                                amount = ""
                                note = ""
                                error = ""
                            }
                        },
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(
                                id = com.ledger.app.R.drawable.ic_calculator
                            ),
                            contentDescription = "Calculate amount",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (showRateQuantity) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = rate,
                            onValueChange = {
                                rate = it
                                if (it.isBlank()) {
                                    amount = ""
                                } else {
                                    updateCalculatedAmount(it, quantity)
                                }
                                categoryPrefs.edit()
                                    .putString("calculator_last_rate", it)
                                    .apply()
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Rate") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            shape = RoundedCornerShape(15.dp)
                        )

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = {
                                quantity = it
                                updateQuantityNote(it)
                                if (it.isBlank()) {
                                    amount = ""
                                } else {
                                    updateCalculatedAmount(rate, it)
                                }
                                categoryPrefs.edit()
                                    .putString("calculator_last_quantity", it)
                                    .apply()
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Quantity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            shape = RoundedCornerShape(15.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = expenseDateText(date),
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            singleLine = true,
                            label = {
                                Text("Date")
                            },
                            shape = RoundedCornerShape(15.dp)
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
                                    val c = Calendar.getInstance().apply {
                                        timeInMillis = date
                                    }
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
                                }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Note") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Edit, contentDescription = null)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(15.dp)
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
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {

                        items(allCategories) { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                    val selected = item == category
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    category = item

                                                    // Load default rate for the newly selected category.
                                                    val selectedDefaultPrice =
                                                        categoryPrefs.getString("default_price:$item", "") ?: ""
                                                    rate = selectedDefaultPrice
                                                    categoryPrefs.edit()
                                                        .putString("calculator_last_rate", selectedDefaultPrice)
                                                        .putString("last_selected_category", item)
                                                        .apply()


                                                    // Recalculate immediately when the calculator is already open.
                                                    if (showRateQuantity) {
                                                        // Refresh the generated Note using the newly selected
                                                        // category's quantity type immediately.
                                                        updateQuantityNote(quantity, item)

                                                        if (selectedDefaultPrice.isBlank()) {
                                                            amount = ""
                                                        } else {
                                                            updateCalculatedAmount(selectedDefaultPrice, quantity)
                                                        }
                                                    }

                                                    showCategoryPage = false
                                                },
                                                onLongClick = {
                                                    if (item in customCategories) {
                                                        editingCategoryTitle = item
                                                        newCategoryTitle = item
                                                        newCategoryQuantityType = categoryPrefs.getString("quantity_type:$item", "Unit") ?: "Unit"
                                                newCategoryDefaultPrice = categoryPrefs.getString("default_price:$item", "") ?: ""
                                                        showQuantityTypeMenu = false
                                                        newCategoryIcon = customCategoryIcons[item]
                                                            ?.takeIf { key ->
                                                                customExpenseCategoryIcons.any { it.key == key }
                                                            } ?: "folder"
                                                        categoryFormError = ""
                                                        showAddCategoryDialog = true
                                                    }
                                                }
                                            ),
                                        shape = RoundedCornerShape(18.dp),
                                        color = androidx.compose.ui.graphics.Color.White,
                                        tonalElevation = if (selected) 2.dp else 0.dp
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(13.dp),
                                                color = if (selected)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                val savedIcon = customCategoryIcons[item]
                                                val iconOption = customExpenseCategoryIcons
                                                    .firstOrNull { it.key == savedIcon }

                                                if (savedIcon != null && iconOption == null) {
                                                    // Continue displaying icons saved by the older emoji picker.
                                                    Text(
                                                        text = savedIcon,
                                                        modifier = Modifier.padding(11.dp),
                                                        fontSize = 25.sp
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = iconOption?.icon ?: expenseCategoryIcon(item),
                                                        contentDescription = iconOption?.label ?: item,
                                                        modifier = Modifier.padding(11.dp).size(25.dp),
                                                        tint = if (selected)
                                                            MaterialTheme.colorScheme.onPrimary
                                                        else
                                                            MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                }
                                            }
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                if (item in customCategories) {
                                                    Text(
                                                        text = "Hold to Edit",
                                                        modifier = Modifier.fillMaxWidth(),
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.7f
                                                        ),
                                                        maxLines = 1
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

                                                if (item in customCategories) {
                                                    Text(
                                                        text = categoryPrefs.getString(
                                                            "quantity_type:$item",
                                                            "Unit"
                                                        ) ?: "Unit",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (selected)
                                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }

                    Spacer(Modifier.size(12.dp))
                    Button(
                        onClick = {
                            editingCategoryTitle = null
                            newCategoryTitle = ""
                            newCategoryIcon = "folder"
                            newCategoryQuantityType = "Unit"
                            newCategoryDefaultPrice = ""
                            showQuantityTypeMenu = false
                            categoryFormError = ""
                            showAddCategoryDialog = true
                        },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Add Category")
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddCategoryDialog = false
                editingCategoryTitle = null
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = androidx.compose.ui.graphics.Color.White,
            title = {
                Text(
                    if (editingCategoryTitle == null) "Add Category" else "Edit Category",
                    color = androidx.compose.ui.graphics.Color(0xFF172033),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.Top
) {
    OutlinedTextField(
                            value = newCategoryTitle,
                            onValueChange = {
                                newCategoryTitle = it
                                categoryFormError = ""
                            },
                            modifier = Modifier.weight(1f),
                            label = { Text("Title") },
                            placeholder = {
                                Text(
                                    "e.g. Office Supplies",
                                    color = androidx.compose.ui.graphics.Color(0xFF8A94A6)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            isError = categoryFormError.isNotBlank(),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFD9DEE8),
                                focusedContainerColor = androidx.compose.ui.graphics.Color.White,
                                unfocusedContainerColor = androidx.compose.ui.graphics.Color.White
                            )
                        )
    Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = newCategoryQuantityType,
                                onValueChange = {},
                                modifier = Modifier.fillMaxWidth().clickable {
                                    showQuantityTypeMenu = true
                                },
                                readOnly = true,
                                label = { Text("Quantity Type") },
                                trailingIcon = { Text("▾") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { showQuantityTypeMenu = true }
                            )
                            DropdownMenu(
                                expanded = showQuantityTypeMenu,
                                onDismissRequest = { showQuantityTypeMenu = false },
                                modifier = Modifier.heightIn(max = 280.dp),
                                containerColor = androidx.compose.ui.graphics.Color.White
                            ) {
                                quantityTypeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            newCategoryQuantityType = option
                                            showQuantityTypeMenu = false
                                        }
                                    )
                                }
                            }
                        }
}


                    OutlinedTextField(
                        value = newCategoryDefaultPrice,
                        onValueChange = { input ->
                            newCategoryDefaultPrice = input.filter {
                                it.isDigit() || it == '.' || it == ','
                            }
                            categoryFormError = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default Price (${CurrencySettings.symbol})") },
                        placeholder = { Text("e.g. 100") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        customExpenseCategoryIcons
                            .groupBy { it.group }
                            .forEach { (groupName, options) ->
                                Text(
                                    groupName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                options.chunked(5).forEach { iconRow ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        iconRow.forEach { option ->
                                            val selectedIcon = newCategoryIcon == option.key
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { newCategoryIcon = option.key },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (selectedIcon)
                                                    MaterialTheme.colorScheme.secondaryContainer
                                                else
                                                    MaterialTheme.colorScheme.surface,
                                                border = BorderStroke(
                                                    if (selectedIcon) 2.dp else 1.dp,
                                                    if (selectedIcon)
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(vertical = 10.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = option.icon,
                                                        contentDescription = option.label,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = if (selectedIcon)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                        repeat(5 - iconRow.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                    }

                    if (categoryFormError.isNotBlank()) {
                        Text(
                            categoryFormError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                
if (editingCategoryTitle != null) {
    androidx.compose.material3.OutlinedButton(
        onClick = {
            val titleToDelete = editingCategoryTitle
                ?: return@OutlinedButton
            categoryScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        categoryDao.deleteByName(titleToDelete)
                    }
                    val updated = customCategories.filterNot {
                        it == titleToDelete
                    }
                    categoryPrefs.edit()
                        .putStringSet("names", updated.toSet())
                        .remove("icon:$titleToDelete")
                        .remove("quantity_type:$titleToDelete")
                        .remove("default_price:$titleToDelete")
                        .apply()
                    customCategories = updated
                    if (category == titleToDelete) {
                        category = expenseCategories.firstOrNull() ?: ""
                    }
                    editingCategoryTitle = null
                    showAddCategoryDialog = false
                    showCategoryPage = false
                } catch (e: Exception) {
                    categoryFormError =
                        "Could not delete category: ${e.message ?: "Database error"}"
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Color.Red
        )
    ) {
        Text(
            "Delete",
            color = androidx.compose.ui.graphics.Color.Red,
            fontWeight = FontWeight.SemiBold
        )
    }
}

Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showAddCategoryDialog = false
                            editingCategoryTitle = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            androidx.compose.ui.graphics.Color(0xFFD9DEE8)
                        )
                    ) {
                        Text(
                            "Cancel",
                            color = androidx.compose.ui.graphics.Color(0xFF475467)
                        )
                    }

                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            val title = newCategoryTitle.trim()
                            val oldTitle = editingCategoryTitle

                            when {
                                title.isBlank() -> {
                                    categoryFormError = "Enter a category title."
                                }
                                allCategories.any {
                                    it.equals(title, ignoreCase = true) &&
                                        !it.equals(oldTitle, ignoreCase = true)
                                } -> {
                                    categoryFormError = "This category already exists."
                                }
                                else -> {
                                    categoryScope.launch {
                                        try {
                                            val updated = if (oldTitle == null) {
                                                (customCategories + title).distinct()
                                            } else {
                                                customCategories.map {
                                                    if (it == oldTitle) title else it
                                                }.distinct()
                                            }

                                            withContext(Dispatchers.IO) {
                                                if (oldTitle != null && oldTitle != title) {
                                                    categoryDao.deleteByName(oldTitle)
                                                }
                                                categoryDao.upsert(
                                                    com.ledger.app.data.CustomExpenseCategoryEntity(
                                                        name = title,
                                                        icon = newCategoryIcon,
                                                        quantityType = newCategoryQuantityType
                                                    )
                                                )
                                            }

                                            val editor = categoryPrefs.edit()
                                                .putStringSet("names", updated.toSet())
                                                .putString("icon:$title", newCategoryIcon)
                                                .putString(
                                                    "quantity_type:$title",
                                                    newCategoryQuantityType
                                                )
                                                .putString(
                                                    "default_price:$title",
                                                    newCategoryDefaultPrice.trim()
                                                )

                                            if (oldTitle != null && oldTitle != title) {
                                                editor.remove("icon:$oldTitle")
                                                editor.remove("quantity_type:$oldTitle")
                                                editor.remove("default_price:$oldTitle")
                                            }
                                            editor.apply()

                                            customCategories = updated
                                            if (oldTitle == null || category == oldTitle) {
                                                category = title
                                            }
                                            editingCategoryTitle = null
                                            showAddCategoryDialog = false
                                            showCategoryPage = false
                                        } catch (e: Exception) {
                                            categoryFormError =
                                                "Could not save category: ${e.message ?: "Database error"}"
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Save",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }

}

@Composable
internal fun expenseCategoryIcon(category: String): ImageVector {
    val context = LocalContext.current
    val savedKey = context.getSharedPreferences(
        "daily_expense_categories",
        android.content.Context.MODE_PRIVATE
    ).getString("icon:$category", null)

    customExpenseCategoryIcons.firstOrNull { it.key == savedKey }?.let {
        return it.icon
    }

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

@Composable
private fun ExpenseCategoryChartScreen(
    expenses: List<DailyExpenseEntity>,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val categoryTotals = remember(expenses) {
        expenses
            .groupBy { it.category.trim().ifBlank { "Uncategorized" } }
            .map { (category, entries) ->
                category to entries.sumOf { it.amount }
            }
            .sortedByDescending { it.second }
    }

    val totalAmount = categoryTotals.sumOf { it.second }

    val chartColors = listOf(
        androidx.compose.ui.graphics.Color(0xFF3978F6),
        androidx.compose.ui.graphics.Color(0xFF19A974),
        androidx.compose.ui.graphics.Color(0xFFFF9F43),
        androidx.compose.ui.graphics.Color(0xFF9B59B6),
        androidx.compose.ui.graphics.Color(0xFFE05260),
        androidx.compose.ui.graphics.Color(0xFF00A8A8),
        androidx.compose.ui.graphics.Color(0xFF6C7A89),
        androidx.compose.ui.graphics.Color(0xFFD4A017)
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Expense Breakdown",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Category-wise report",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF7E5)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFFE8B64A)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "TOTAL EXPENSE",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF77500A)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            expenseMoney(totalAmount),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF9A6700)
                        )
                        Text(
                            "${expenses.size} ${if (expenses.size == 1) "record" else "records"} · ${categoryTotals.size} categories",
                            color = Color(0xFF77500A)
                        )
                    }
                }
            }

            if (categoryTotals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        )
                    ) {
                        Text(
                            "No expenses recorded for this period.",
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "CATEGORY DETAILS",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(categoryTotals) { entry ->
                    val index = categoryTotals.indexOf(entry)
                    val percentage = if (totalAmount > 0.0) {
                        entry.second / totalAmount * 100.0
                    } else {
                        0.0
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(chartColors[index % chartColors.size])
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.first,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    String.format(
                                        Locale.getDefault(),
                                        "%.1f%% of total",
                                        percentage
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                expenseMoney(entry.second),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun createDailyExpenseReportPdf(
    context: android.content.Context,
    expenses: List<DailyExpenseEntity>,
    year: Int?,
    periodLabel: String? = null
): java.io.File {
    val pdf = android.graphics.pdf.PdfDocument()
    val ordered = expenses.sortedWith(
        compareByDescending<DailyExpenseEntity> { it.expenseDate }
            .thenByDescending { it.id }
    )
    val total = ordered.sumOf { it.amount }
    val categorySummaries = ordered
        .groupBy { it.category.ifBlank { "Uncategorized" } }
        .map { (category, items) ->
            Triple(category, items.size, items.sumOf { it.amount })
        }
        .sortedByDescending { it.third }

    val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
    val generatedAt = dateFormat.format(java.util.Date())
    val pageWidth = 595
    val pageHeight = 842
    val left = 36f
    val tableWidth = 523f
    val widths = listOf(82f, 100f, 235f, 106f)
    val headers = listOf("Date", "Category", "Note", "Amount")
    val rowHeight = 34f

    var page: android.graphics.pdf.PdfDocument.Page? = null
    var y = 0f
    var pageNumber = 0
    var categorySummaryDrawn = false

    fun paint(
        color: Int = android.graphics.Color.BLACK,
        size: Float = 10f,
        bold: Boolean = false
    ) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            if (bold) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

    fun fitText(value: String, maxWidth: Float, textPaint: android.graphics.Paint): String {
        var result = value.replace('\n', ' ').replace('\r', ' ').trim()
        if (textPaint.measureText(result) <= maxWidth) return result
        while (result.isNotEmpty() && textPaint.measureText("$result…") > maxWidth) {
            result = result.dropLast(1)
        }
        return "$result…"
    }

    fun drawTableHeader() {
        val canvas = page!!.canvas
        val bg = paint(android.graphics.Color.WHITE, 9f, true)
        var x = left
        headers.forEachIndexed { index, label ->
            canvas.drawRect(
                x, y, x + widths[index], y + rowHeight,
                paint(android.graphics.Color.rgb(35, 73, 112))
            )
            canvas.drawText(label, x + 7f, y + 21f, bg)
            canvas.drawRect(
                x, y, x + widths[index], y + rowHeight,
                paint(android.graphics.Color.LTGRAY).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = 0.6f
                }
            )
            x += widths[index]
        }
        y += rowHeight
    }

    fun startPage() {
        page?.let { pdf.finishPage(it) }
        pageNumber++
        page = pdf.startPage(
            android.graphics.pdf.PdfDocument.PageInfo.Builder(
                pageWidth, pageHeight, pageNumber
            ).create()
        )
        val canvas = page!!.canvas
        y = 36f

        canvas.drawText(
            "DAILY EXPENSE REPORT",
            left, y + 10f, paint(android.graphics.Color.rgb(35, 73, 112), 18f, true)
        )
        y += 24f
        canvas.drawText(
            periodLabel ?: (if (year == null) "Period: All Time" else "Period: 1 Jan $year - 31 Dec $year"),
            left, y + 5f, paint(android.graphics.Color.DKGRAY, 10f)
        )
        y += 18f
        canvas.drawText(
            "Generated: $generatedAt",
            left, y + 4f, paint(android.graphics.Color.DKGRAY, 9f)
        )
        y += 25f

        canvas.drawRoundRect(
            left, y, left + tableWidth, y + 47f, 6f, 6f,
            paint(android.graphics.Color.rgb(235, 242, 249))
        )
        canvas.drawText(
            "TOTAL EXPENSE",
            left + 12f, y + 17f, paint(android.graphics.Color.DKGRAY, 9f, true)
        )
        canvas.drawText(
            CurrencySettings.format(total),
            left + 12f, y + 37f,
            paint(android.graphics.Color.rgb(35, 73, 112), 14f, true)
        )
        canvas.drawText(
            "${ordered.size} records",
            left + tableWidth - 90f, y + 28f,
            paint(android.graphics.Color.DKGRAY, 9f)
        )
        y += 61f
        if (!categorySummaryDrawn) {
            categorySummaryDrawn = true
            val canvas = page!!.canvas
            canvas.drawText(
                "CATEGORY-WISE EXPENSE",
                left, y + 12f,
                paint(android.graphics.Color.rgb(35, 73, 112), 11f, true)
            )
            y += 20f

            val summaryWidths = listOf(220f, 130f, 173f)
            val summaryHeaders = listOf("Category", "No. of Expenses", "Total Amount")

            fun drawSummaryRow(cells: List<String>, header: Boolean = false, totalRow: Boolean = false) {
                val height = 25f
                var x = left
                cells.forEachIndexed { index, value ->
                    val bgColor = if (header) {
                        android.graphics.Color.rgb(35, 73, 112)
                    } else if (totalRow) {
                        android.graphics.Color.rgb(235, 242, 249)
                    } else {
                        android.graphics.Color.WHITE
                    }
                    canvas.drawRect(
                        x, y, x + summaryWidths[index], y + height,
                        paint(bgColor)
                    )
                    canvas.drawRect(
                        x, y, x + summaryWidths[index], y + height,
                        paint(android.graphics.Color.LTGRAY).apply {
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 0.6f
                        }
                    )
                    val textColor = if (header) android.graphics.Color.WHITE
                        else android.graphics.Color.rgb(38, 43, 50)
                    val textPaint = paint(textColor, 8.5f, header || totalRow || index == 2)
                    canvas.drawText(
                        fitText(value, summaryWidths[index] - 12f, textPaint),
                        x + 6f, y + 16f, textPaint
                    )
                    x += summaryWidths[index]
                }
                y += height
            }

            drawSummaryRow(summaryHeaders, header = true)
            categorySummaries.forEach { (category, count, amount) ->
                drawSummaryRow(
                    listOf(category, count.toString(), CurrencySettings.format(amount))
                )
            }
            drawSummaryRow(
                listOf("Grand Total", ordered.size.toString(), CurrencySettings.format(total)),
                totalRow = true
            )
            y += 12f
        }
        drawTableHeader()
    }

    try {
        startPage()

        if (ordered.isEmpty()) {
            page!!.canvas.drawText(
                "No expenses recorded for this period.",
                left + 6f, y + 24f, paint(android.graphics.Color.DKGRAY, 11f)
            )
            y += rowHeight
        } else {
            ordered.forEach { expense ->
                if (y + rowHeight > 790f) startPage()

                val cells = listOf(
                    dateFormat.format(java.util.Date(expense.expenseDate)),
                    expense.category.ifBlank { "Uncategorized" },
                    expense.note.ifBlank { "-" },
                    CurrencySettings.format(expense.amount)
                )
                var x = left
                cells.forEachIndexed { index, value ->
                    val canvas = page!!.canvas
                    val cellPaint = paint(
                        android.graphics.Color.rgb(38, 43, 50),
                        8.5f,
                        index == 3
                    )
                    canvas.drawRect(
                        x, y, x + widths[index], y + rowHeight,
                        paint(android.graphics.Color.WHITE)
                    )
                    canvas.drawRect(
                        x, y, x + widths[index], y + rowHeight,
                        paint(android.graphics.Color.LTGRAY).apply {
                            style = android.graphics.Paint.Style.STROKE
                            strokeWidth = 0.6f
                        }
                    )
                    canvas.drawText(
                        fitText(value, widths[index] - 12f, cellPaint),
                        x + 6f, y + 21f, cellPaint
                    )
                    x += widths[index]
                }
                y += rowHeight
            }
        }

        page!!.canvas.drawText(
            "Page $pageNumber",
            pageWidth - 75f, 820f,
            paint(android.graphics.Color.GRAY, 8f)
        )

        page?.let { pdf.finishPage(it) }
        page = null

        val directory = java.io.File(context.cacheDir, "daily_reports").apply { mkdirs() }
        val output = java.io.File(directory, "daily_expense_report.pdf")
        output.outputStream().use { pdf.writeTo(it) }
        return output
    } finally {
        page?.let {
            try { pdf.finishPage(it) } catch (_: Exception) { }
        }
        pdf.close()
    }
}

private fun saveDailyExpenseReportPdf(
    context: android.content.Context,
    source: java.io.File,
    year: Int?
): String {
    val stamp = java.text.SimpleDateFormat(
        "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
    ).format(java.util.Date())
    val period = year?.toString() ?: "all_time"
    val fileName = "daily_expense_report_${period}_$stamp.pdf"

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(
                android.provider.MediaStore.Downloads.RELATIVE_PATH,
                "${android.os.Environment.DIRECTORY_DOWNLOADS}/Ledger"
            )
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
        ) ?: error("Could not create report in Downloads")

        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("Could not write report file")

            values.clear()
            values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return "Downloads/Ledger/$fileName"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    val directory = java.io.File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
        "Ledger"
    ).apply { mkdirs() }
    val output = java.io.File(directory, fileName)
    source.inputStream().use { input ->
        output.outputStream().use { out -> input.copyTo(out) }
    }
    return output.absolutePath
}
