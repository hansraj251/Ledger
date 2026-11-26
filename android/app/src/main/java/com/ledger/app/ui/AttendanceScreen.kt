package com.ledger.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import com.ledger.app.attendance.AttendanceAuthManager
import com.ledger.app.attendance.AttendanceRecord
import com.ledger.app.attendance.AttendanceRepository
import com.ledger.app.attendance.AttendanceStaffRecord
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(onBack: () -> Unit) {
    val repository = remember { AttendanceRepository() }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ownerStaff by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var ownerRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var ownerLoaded by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    val today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()
    val monthStart = selectedMonth.atDay(1).toString()
    val monthEnd = selectedMonth.atEndOfMonth().toString()
    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    suspend fun loadOwnerReport() {
        busy = true
        error = null
        try {
            check(repository.ownerSignedIn()) {
                "Owner Google account is not connected. Connect it from Profile, then reopen Attendance Report."
            }
            ownerStaff = repository.fetchOwnerStaff()
                .filter { it.isActive && it.linkStatus != "disabled" }
            ownerRecords = repository.fetchOwnerRecords()
            ownerLoaded = true
        } catch (e: Exception) {
            error = e.message ?: "Unable to load attendance report."
        } finally {
            busy = false
        }
    }

    LaunchedEffect(selectedMonth) { loadOwnerReport() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Report") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { scope.launch { loadOwnerReport() } }) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            }

            error?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Monthly attendance",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                                Text("‹ Previous")
                            }
                            Text(monthLabel, fontWeight = FontWeight.SemiBold)
                            TextButton(
                                onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                                enabled = selectedMonth < YearMonth.now()
                            ) { Text("Next ›") }
                        }
                        Text(
                            "${ownerStaff.size} active staff · ${
                                ownerRecords.count { it.attendanceDate in monthStart..monthEnd }
                            } attendance marks",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (ownerStaff.isEmpty() && !busy && ownerLoaded) {
                item { Text("No active staff registered for online attendance.") }
            }

            items(ownerStaff, key = { "owner-${it.id}" }) { staff ->
                val staffRecords = ownerRecords.filter { it.staffId == staff.id }
                val recordsInMonth = staffRecords.filter {
                    it.attendanceDate in monthStart..monthEnd
                }
                val markedToday = staffRecords.any { it.attendanceDate == today }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(staff.displayName, fontWeight = FontWeight.SemiBold)
                        Text(staff.email, style = MaterialTheme.typography.bodySmall)
                        Text(
                            if (markedToday) "Today: Marked" else "Today: No mark recorded",
                            color = if (markedToday) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Marked ${recordsInMonth.size} day(s) in $monthLabel",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffAttendanceScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val authManager = remember(context) { AttendanceAuthManager(context) }
    val repository = remember { AttendanceRepository() }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var myStaff by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var myRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }

    val today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()
    val monthStart = selectedMonth.atDay(1).toString()
    val monthEnd = selectedMonth.atEndOfMonth().toString()
    val monthLabel = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    suspend fun loadStaffAttendance() {
        busy = true
        error = null
        try {
            val user = authManager.restoreSession()
                ?: kotlin.error("Staff login session not found. Please sign in again from Profile.")
            repository.claimStaffLinks()
            myStaff = repository.fetchMyStaff()
            myRecords = repository.fetchMyRecords()
            if (myStaff.isEmpty()) {
                error = "No active staff registration found for ${user.email}. Contact your employer."
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load staff attendance."
        } finally {
            busy = false
        }
    }

    LaunchedEffect(Unit) { loadStaffAttendance() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staff Attendance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { scope.launch { loadStaffAttendance() } }) {
                        Text("Refresh")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (busy) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) { CircularProgressIndicator() }
                }
            }

            error?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (myStaff.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Welcome to Attendance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${myStaff.size} linked workplace registration(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(myStaff, key = { "staff-attendance-${it.id}" }) { staff ->
                val staffRecords = myRecords
                    .filter { it.staffId == staff.id }
                    .sortedByDescending { it.attendanceDate }
                val markedToday = staffRecords.any { it.attendanceDate == today }
                val recordsInMonth = staffRecords.filter {
                    it.attendanceDate in monthStart..monthEnd
                }

                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Text(
                            staff.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            staff.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (markedToday) {
                            Text(
                                "Attendance marked for today",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Already Marked") }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        busy = true
                                        error = null
                                        try {
                                            repository.markMyAttendance(staff.id)
                                            myRecords = repository.fetchMyRecords()
                                        } catch (e: Exception) {
                                            error = e.message ?: "Unable to mark attendance."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text("Mark Attendance") }
                        }
                        Text(
                            "This Month: ${recordsInMonth.size} day(s) marked",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (myStaff.isNotEmpty()) {
                item {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Attendance History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                                    Text("‹ Previous")
                                }
                                Text(monthLabel, fontWeight = FontWeight.SemiBold)
                                TextButton(
                                    onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                                    enabled = selectedMonth < YearMonth.now()
                                ) { Text("Next ›") }
                            }
                            val history = myRecords
                                .filter { it.attendanceDate in monthStart..monthEnd }
                                .sortedByDescending { it.attendanceDate }
                            if (history.isEmpty()) {
                                Text(
                                    "No attendance records for this month.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                history.forEach { record ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            record.attendanceDate,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            "Present",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}
