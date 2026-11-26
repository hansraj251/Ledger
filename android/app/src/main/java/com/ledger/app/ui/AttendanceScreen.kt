package com.ledger.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var selectedStaff by remember { mutableStateOf<AttendanceStaffRecord?>(null) }
    var staffPendingDelete by remember { mutableStateOf<AttendanceStaffRecord?>(null) }

    suspend fun loadOwnerReport() {
        busy = true
        error = null
        try {
            check(repository.ownerSignedIn()) {
                "Owner Google account is not connected. Connect it from Profile, then reopen Attendance Report."
            }
            ownerStaff = repository.fetchOwnerStaffForReport()
                .filter {
                    it.isActive &&
                        it.linkStatus == "linked" &&
                        it.approvalStatus == "accepted"
                }
                .sortedBy { it.displayName.lowercase() }
            ownerRecords = repository.fetchOwnerRecords()
            ownerLoaded = true
        } catch (e: Exception) {
            error = e.message ?: "Unable to load attendance report."
        } finally {
            busy = false
        }
    }

    LaunchedEffect(Unit) { loadOwnerReport() }

    val activeStaff = selectedStaff

    staffPendingDelete?.let { staffToDelete ->
        AlertDialog(
            onDismissRequest = { if (!busy) staffPendingDelete = null },
            title = { Text("Delete online attendance?") },
            text = {
                Text(
                    "This will permanently delete ${staffToDelete.displayName}'s online attendance registration and all attendance records from the cloud. The staff member will remain in your local Ledger staff list."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                repository.deleteOwnerAttendanceStaff(staffToDelete.id)
                                staffPendingDelete = null
                                selectedStaff = null
                                loadOwnerReport()
                            } catch (e: Exception) {
                                error = e.message ?: "Unable to delete online attendance staff."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { staffPendingDelete = null },
                    enabled = !busy
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (activeStaff != null) {
        OwnerStaffAttendanceScreen(
            staff = activeStaff,
            records = ownerRecords.filter { it.staffId == activeStaff.id },
            busy = busy,
            error = error,
            onBack = { selectedStaff = null },
            onRefresh = { scope.launch { loadOwnerReport() } },
            onSetStatus = { date, status ->
                scope.launch {
                    busy = true
                    error = null
                    try {
                        repository.setOwnerAttendance(activeStaff.id, date.toString(), status)
                        ownerRecords = repository.fetchOwnerRecords()
                    } catch (e: Exception) {
                        error = e.message ?: "Unable to update attendance."
                    } finally {
                        busy = false
                    }
                }
            }
        )
    } else {
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
                        IconButton(
                            onClick = { scope.launch { loadOwnerReport() } },
                            enabled = !busy
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh attendance report"
                            )
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF2B8B5))
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(14.dp),
                                color = Color(0xFFB42318)
                            )
                        }
                    }
                }

                if (ownerStaff.isEmpty() && !busy && ownerLoaded) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Text(
                                "No staff has accepted an online attendance invitation yet.",
                                modifier = Modifier.padding(18.dp),
                                color = Color(0xFF667085)
                            )
                        }
                    }
                }

                items(ownerStaff, key = { "owner-${it.id}" }) { staff ->
                    val staffRecords = ownerRecords.filter { it.staffId == staff.id }
                    val presentCount = staffRecords.count { it.status.equals("present", true) }
                    val absentCount = staffRecords.count { it.status.equals("absent", true) }
                    val todayRecord = staffRecords.firstOrNull {
                        it.attendanceDate == LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { selectedStaff = staff },
                                onLongClick = { staffPendingDelete = staff }
                            ),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(46.dp),
                                shape = CircleShape,
                                color = Color(0xFFEFF2FF)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        staff.displayName.trim().take(1).uppercase(),
                                        color = Color(0xFF3448C5),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    staff.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF202124)
                                )
                                Text(
                                    staff.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF667085)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        "Present $presentCount",
                                        color = Color(0xFF16803D),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Absent $absentCount",
                                        color = Color(0xFFB42318),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Text(
                                    when (todayRecord?.status?.lowercase()) {
                                        "present" -> "Today · Present"
                                        "absent" -> "Today · Absent"
                                        else -> "Today · Not marked"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = when (todayRecord?.status?.lowercase()) {
                                        "present" -> Color(0xFF16803D)
                                        "absent" -> Color(0xFFB42318)
                                        else -> Color(0xFF667085)
                                    }
                                )
                            }
                            Text(
                                "›",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color(0xFF98A2B3)
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OwnerStaffAttendanceScreen(
    staff: AttendanceStaffRecord,
    records: List<AttendanceRecord>,
    busy: Boolean,
    error: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSetStatus: (LocalDate, String) -> Unit
) {
    var selectedMonth by remember(staff.id) { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember(staff.id) {
        mutableStateOf(LocalDate.now(ZoneId.of("Asia/Kolkata")))
    }

    val selectedRecord = records.firstOrNull { it.attendanceDate == selectedDate.toString() }
    val monthRecords = records.filter {
        YearMonth.from(LocalDate.parse(it.attendanceDate)) == selectedMonth
    }
    val presentCount = monthRecords.count { it.status.equals("present", true) }
    val absentCount = monthRecords.count { it.status.equals("absent", true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(staff.displayName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onRefresh) { Text("Refresh") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            staff.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF202124)
                            )
                            val statusText = when (selectedRecord?.status?.lowercase()) {
                                "present" -> "Present"
                                "absent" -> "Absent"
                                else -> "Not marked"
                            }
                            Text(
                                statusText,
                                color = when (selectedRecord?.status?.lowercase()) {
                                    "present" -> Color(0xFF16803D)
                                    "absent" -> Color(0xFFB42318)
                                    else -> Color(0xFF667085)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onSetStatus(selectedDate, "present") },
                                enabled = !busy && selectedRecord?.status?.equals("present", true) != true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16803D),
                                    contentColor = Color.White
                                )
                            ) { Text("Mark Present") }

                            Button(
                                onClick = { onSetStatus(selectedDate, "absent") },
                                enabled = !busy && selectedRecord?.status?.equals("absent", true) != true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB42318),
                                    contentColor = Color.White
                                )
                            ) { Text("Mark Absent") }
                        }
                        if (busy) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) { CircularProgressIndicator() }
                        }
                        error?.let {
                            Text(it, color = Color(0xFFB42318))
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) {
                                Text("‹")
                            }
                            Text(
                                selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF202124)
                            )
                            TextButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) {
                                Text("›")
                            }
                        }
                        AttendanceMonthGrid(
                            selectedMonth = selectedMonth,
                            selectedDate = selectedDate,
                            records = records,
                            allowFutureSelection = true,
                            onDateSelected = { selectedDate = it }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            AttendanceCalendarLegend(Color(0xFFDDF4E4), Color(0xFF16803D), "Present")
                            AttendanceCalendarLegend(Color(0xFFFCE4E4), Color(0xFFB42318), "Absent")
                        }
                        Text(
                            "This month: $presentCount present · $absentCount absent",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF667085)
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
    val repository = remember { AttendanceRepository() }
    val scope = rememberCoroutineScope()

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var myStaff by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var myRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember {
        mutableStateOf(LocalDate.now(ZoneId.of("Asia/Kolkata")))
    }

    val today = LocalDate.now(ZoneId.of("Asia/Kolkata"))

    suspend fun loadStaffAttendance() {
        busy = true
        error = null
        try {
            val email = repository.currentOnlineAccountEmail()
                ?: kotlin.error("Connect your Google account from the Ledger Online card in Profile.")
            repository.claimStaffLinks()
            myStaff = repository.fetchMyStaff()
            myRecords = repository.fetchMyRecords()
            if (myStaff.isEmpty()) {
                error = "No accepted attendance registration found for $email. Check your invitation on Home or contact your employer."
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
                    IconButton(
                        onClick = { scope.launch { loadStaffAttendance() } },
                        enabled = !busy
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh attendance"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF2B8B5))
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFFB42318)
                        )
                    }
                }
            }

            items(myStaff, key = { "staff-attendance-${it.id}" }) { staff ->
                val staffRecords = myRecords.filter { it.staffId == staff.id }
                val recordsInMonth = staffRecords.filter {
                    YearMonth.from(LocalDate.parse(it.attendanceDate)) == selectedMonth
                }
                val selectedDateString = selectedDate.toString()
                val selectedDateRecord = staffRecords.firstOrNull {
                    it.attendanceDate == selectedDateString
                }
                val selectedDateMarked = selectedDateRecord != null
                val todayMarked = staffRecords.any { it.attendanceDate == today.toString() }

                // Mark Attendance card: date selection and today's mark action.
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                staff.displayName,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF202124)
                            )
                            Text(
                                staff.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF667085)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy")),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF202124)
                            )
                            val selectedStatus = when {
                                selectedDate.isAfter(today) -> "Future date"
                                selectedDateMarked && selectedDateRecord?.status.equals("present", true) -> "Present"
                                selectedDateMarked -> "Absent"
                                else -> "Not marked"
                            }
                            Text(
                                selectedStatus,
                                color = when (selectedStatus) {
                                    "Present" -> Color(0xFF16803D)
                                    "Absent" -> Color(0xFFB42318)
                                    else -> Color(0xFF667085)
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        when {
                            selectedDate.isAfter(today) -> {
                                // Future dates can be viewed but not marked by staff.
                            }
                            selectedDateMarked -> {
                                if (selectedDate == today) {
                                    OutlinedButton(
                                        onClick = {},
                                        enabled = false,
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Already Marked") }
                                }
                            }
                            selectedDate == today -> {
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
                                    enabled = !busy && !todayMarked,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(if (todayMarked) "Already Marked" else "Mark Attendance")
                                }
                            }
                            else -> {
                                Text(
                                    "No record",
                                    color = Color(0xFF667085),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "No attendance status has been recorded for this date.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF667085)
                                )
                            }
                        }

                        Text(
                            "Present this month: ${recordsInMonth.count { it.status.equals("present", true) }} day(s)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF667085)
                        )
                    }
                }

                // Calendar replaces the old Attendance History list.
                Spacer(Modifier.height(16.dp))
                StaffAttendanceCalendarCard(
                    selectedMonth = selectedMonth,
                    selectedDate = selectedDate,
                    records = staffRecords,
                    today = today,
                    onDateSelected = { date ->
                        selectedDate = date
                        selectedMonth = YearMonth.from(date)
                    },
                    onMonthChanged = { month ->
                        selectedMonth = month
                        selectedDate = if (month.atEndOfMonth().isAfter(today)) {
                            today
                        } else {
                            month.atEndOfMonth()
                        }
                    }
                )
            }

            item { Spacer(Modifier.height(18.dp)) }
        }
    }

}

@Composable
private fun StaffAttendanceCalendarCard(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    records: List<AttendanceRecord>,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onMonthChanged: (YearMonth) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { onMonthChanged(selectedMonth.minusMonths(1)) }) {
                    Text("‹")
                }
                Text(
                    selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF202124)
                )
                TextButton(
                    onClick = { onMonthChanged(selectedMonth.plusMonths(1)) },
                    enabled = selectedMonth < YearMonth.from(today)
                ) { Text("›") }
            }
            AttendanceMonthGrid(
                selectedMonth = selectedMonth,
                selectedDate = selectedDate,
                records = records,
                allowFutureSelection = false,
                today = today,
                onDateSelected = onDateSelected
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AttendanceCalendarLegend(Color(0xFFDDF4E4), Color(0xFF16803D), "Present")
                AttendanceCalendarLegend(Color(0xFFFCE4E4), Color(0xFFB42318), "Absent")
            }
        }
    }
}

@Composable
private fun AttendanceMonthGrid(
    selectedMonth: YearMonth,
    selectedDate: LocalDate,
    records: List<AttendanceRecord>,
    allowFutureSelection: Boolean,
    today: LocalDate = LocalDate.now(ZoneId.of("Asia/Kolkata")),
    onDateSelected: (LocalDate) -> Unit
) {
    val statusByDate = remember(records) {
        records.associate { it.attendanceDate to it.status.lowercase() }
    }
    val leadingBlanks = selectedMonth.atDay(1).dayOfWeek.value - 1
    val calendarCells: List<LocalDate?> = buildList {
        repeat(leadingBlanks) { add(null) }
        for (day in 1..selectedMonth.lengthOfMonth()) add(selectedMonth.atDay(day))
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    day,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF667085),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    calendarCells.chunked(7).forEach { week ->
        Row(modifier = Modifier.fillMaxWidth()) {
            week.forEach { date ->
                Box(
                    modifier = Modifier.weight(1f).aspectRatio(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (date == null) {
                        Spacer(Modifier.size(34.dp))
                    } else {
                        val status = statusByDate[date.toString()]
                        val cellColor = when (status) {
                            "present" -> Color(0xFFDDF4E4)
                            "absent" -> Color(0xFFFCE4E4)
                            else -> Color(0xFFF2F4F7)
                        }
                        val textColor = when (status) {
                            "present" -> Color(0xFF16803D)
                            "absent" -> Color(0xFFB42318)
                            else -> Color(0xFF667085)
                        }
                        Surface(
                            modifier = Modifier
                                .size(34.dp)
                                .clickable(enabled = allowFutureSelection || !date.isAfter(today)) {
                                    onDateSelected(date)
                                },
                            shape = CircleShape,
                            color = cellColor,
                            border = if (date == selectedDate) {
                                BorderStroke(2.dp, Color(0xFF3448C5))
                            } else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    color = textColor,
                                    fontWeight = if (date == selectedDate || date == today) {
                                        FontWeight.Bold
                                    } else FontWeight.Medium,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
            repeat(7 - week.size) {
                Spacer(Modifier.weight(1f).aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun AttendanceCalendarLegend(
    background: Color,
    foreground: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape,
            color = background
        ) {}
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            fontWeight = FontWeight.SemiBold
        )
    }
}

