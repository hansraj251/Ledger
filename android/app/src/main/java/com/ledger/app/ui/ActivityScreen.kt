package com.ledger.app.ui

import com.ledger.app.data.CurrencySettings

import android.app.DatePickerDialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class ActivityLogRow(
    val id: Long,
    val tableName: String,
    val operation: String,
    val recordId: Long,
    val occurredAt: Long,
    val details: String
)

@Composable
fun ActivityScreen(
    database: SupportSQLiteDatabase,
    onBack: () -> Unit
) {
    val activityCurrencyCode = CurrencySettings.currentCode.value

    BackHandler { onBack() }

    var logs by remember { mutableStateOf<List<ActivityLogRow>>(emptyList()) }
    var loadError by remember { mutableStateOf(false) }
    var selectedActivityDate by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        )
    }
    val activityContext = LocalContext.current
    val activityDayFormat = remember {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    }


    LaunchedEffect(database, selectedActivityDate) {
        try {
            logs = withContext(Dispatchers.IO) {
                val dayCalendar = Calendar.getInstance().apply {
                    timeInMillis = selectedActivityDate
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayStart = dayCalendar.timeInMillis
                dayCalendar.add(Calendar.DAY_OF_MONTH, 1)
                val nextDayStart = dayCalendar.timeInMillis

                val result = mutableListOf<ActivityLogRow>()
                val cursor = database.query(
                    "SELECT id, tableName, operation, recordId, occurredAt, details " +
                        "FROM activity_logs WHERE LOWER(tableName) NOT IN ('custom_expense_categories', 'expense_categories') AND  LOWER(tableName) NOT IN ('staff_members', 'staff') AND  LOWER(tableName) NOT IN ('sharing_groups', 'sharing_group') AND  LOWER(tableName) NOT IN ('user_profile', 'profile') AND  LOWER(tableName) NOT IN ('app_settings', 'parties') ORDER BY id DESC LIMIT 500"
                )
                cursor.use {
                    while (it.moveToNext()) {
                        result.add(
                            ActivityLogRow(
                                id = it.getLong(0),
                                tableName = it.getString(1),
                                operation = it.getString(2),
                                recordId = it.getLong(3),
                                occurredAt = it.getLong(4),
                                details = it.getString(5) ?: ""
                            )
                        )
                    }
                }
                result.filter {
                    it.occurredAt >= dayStart && it.occurredAt < nextDayStart
                }.map { log ->
                    if (log.tableName.equals("emi_schedules", ignoreCase = true)) {
                        log.copy(
                            details = calculateEmiActivityDetails(
                                database = database,
                                recordId = log.recordId,
                                details = log.details
                            )
                        )
                    } else {
                        log
                    }
                }
            }
        } catch (_: Exception) {
            loadError = true
        }
    }

    val dateFormat = remember {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Activity",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Filter by date",
                style = MaterialTheme.typography.titleSmall
            )
            TextButton(
                onClick = {
                    val current = Calendar.getInstance().apply {
                        timeInMillis = selectedActivityDate
                    }
                    DatePickerDialog(
                        activityContext,
                        { _, year, month, dayOfMonth ->
                            selectedActivityDate = Calendar.getInstance().apply {
                                set(year, month, dayOfMonth, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        },
                        current.get(Calendar.YEAR),
                        current.get(Calendar.MONTH),
                        current.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
            ) {
                Text("📅 ${activityDayFormat.format(Date(selectedActivityDate))}  ▾")
            }
        }

        when {
            loadError -> Text(
                "Unable to load activity history.",
                color = MaterialTheme.colorScheme.error
            )

            logs.isEmpty() -> Text(
                "No activity recorded for ${activityDayFormat.format(Date(selectedActivityDate))}.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 20.dp)
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    val actionColor = when (log.operation) {
                        "ADD" -> androidx.compose.ui.graphics.Color(0xFF176B35)
                        "EDIT" -> androidx.compose.ui.graphics.Color(0xFF234E8A)
                        else -> MaterialTheme.colorScheme.error
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                text = "${log.operation}: ${friendlyTableName(log.tableName)}",
                                fontWeight = FontWeight.SemiBold,
                                color = actionColor
                            )
                            if (log.details.isNotBlank()) {
                                Text(
                                    text = log.details.replace("₹", CurrencySettings.symbol).let { _ ->
                                    activityCurrencyCode // Observe Profile currency changes.
                                    log.details.replace("₹", CurrencySettings.symbol)
                                },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "Record ID: ${log.recordId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = dateFormat.format(Date(log.occurredAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun calculateEmiActivityDetails(
    database: SupportSQLiteDatabase,
    recordId: Long,
    details: String
): String {
    data class ScheduleRow(val number: Int, val amount: Double)

    val schedule = database.query(
        "SELECT transactionId, installmentNumber, frequency " +
            "FROM emi_schedules WHERE id = ? LIMIT 1",
        arrayOf(recordId)
    ).use { cursor ->
        if (!cursor.moveToFirst()) return details
        Triple(
            cursor.getLong(0),
            cursor.getInt(1),
            cursor.getString(2).orEmpty()
        )
    }

    val transactionId = schedule.first
    val targetNumber = schedule.second
    val frequency = schedule.third

    val entry = database.query(
        "SELECT amount, interestRate FROM ledger_entries WHERE id = ? LIMIT 1",
        arrayOf(transactionId)
    ).use { cursor ->
        if (!cursor.moveToFirst()) return details
        Pair(
            kotlin.math.abs(cursor.getDouble(0)),
            cursor.getDouble(1).coerceAtLeast(0.0)
        )
    }

    val periodsPerYear = when (frequency) {
        "Weekly" -> 52.0
        "Quarterly" -> 4.0
        "Half-Yearly" -> 2.0
        else -> 12.0
    }
    val periodicRate = entry.second / 100.0 / periodsPerYear

    val schedules = mutableListOf<ScheduleRow>()
    database.query(
        "SELECT installmentNumber, amount FROM emi_schedules " +
            "WHERE transactionId = ? AND installmentNumber <= ? " +
            "ORDER BY installmentNumber",
        arrayOf(transactionId, targetNumber)
    ).use { cursor ->
        while (cursor.moveToNext()) {
            schedules += ScheduleRow(cursor.getInt(0), cursor.getDouble(1))
        }
    }

    var outstanding = entry.first
    var principal = 0.0
    var interest = 0.0

    for (item in schedules) {
        val installmentInterest =
            kotlin.math.round(outstanding * periodicRate * 100.0) / 100.0
        val installmentPrincipal =
            (item.amount - installmentInterest).coerceAtLeast(0.0)

        if (item.number == targetNumber) {
            principal = installmentPrincipal
            interest = installmentInterest
        }

        outstanding = (outstanding - installmentPrincipal).coerceAtLeast(0.0)
    }

    val principalText = String.format(Locale.US, "%.2f", principal)
    val interestText = String.format(Locale.US, "%.2f", interest)

    val resultWithoutEmiAmounts = details
        .replace(Regex(""" · Principal: ₹[0-9,.]+"""), "")
        .replace(Regex(""" · Int\.: ₹[0-9,.]+"""), "")

    val result = resultWithoutEmiAmounts +
        " · Principal: ₹$principalText" +
        " · Int.: ₹$interestText"

    return result
}

private fun friendlyTableName(table: String): String = when (table) {
    "parties" -> "Party"
    "ledger_entries" -> "Party Transaction"
    "user_profile" -> "Profile"
    "interest_account_entries" -> "Interest Account"
    "daily_expenses" -> "Daily Expense"
    "custom_expense_categories" -> "Expense Category"
    "sharing_groups" -> "Sharing Group"
    "sharing_members" -> "Group Member"
    "group_expenses" -> "Group Expense"
    "group_expense_splits" -> "Expense Split"
    "group_expense_payments" -> "Group Payment"
    "group_settlements" -> "Group Settlement"
    "app_settings" -> "App Settings"
    "staff_members" -> "Staff"
    "salary_revisions" -> "Salary Revision"
    "salary_payments" -> "Salary Payment"
    "staff_advance_transactions" -> "Staff Advance"
    "staff_bonuses" -> "Staff Bonus"
    "staff_salary_deductions" -> "Salary Deduction"
    "party_sell_items" -> "Party Sell Item"
    "emi_schedules" -> "EMI Installment"
    "transaction_settlements" -> "Transaction Settlement"
    "emi_charge_settlements" -> "EMI Settlement"
    else -> table.replace('_', ' ').replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
    }
}
