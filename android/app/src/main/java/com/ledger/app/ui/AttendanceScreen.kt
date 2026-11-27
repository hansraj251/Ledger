package com.ledger.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Surface
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.material3.Slider
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ledger.app.attendance.AttendanceRecord
import com.ledger.app.attendance.AttendanceMarkRequest
import com.ledger.app.attendance.AttendanceRepository
import com.ledger.app.attendance.AttendanceStaffRecord
import com.ledger.app.attendance.readAttendanceLocation
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
    val context = LocalContext.current

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var ownerStaff by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var ownerPendingInvites by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var ownerRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var ownerPendingRequests by remember { mutableStateOf<List<AttendanceMarkRequest>>(emptyList()) }
    var showVerificationSettings by remember { mutableStateOf(false) }
    var ownerVerificationMode by remember { mutableStateOf("owner_approval") }
    var gpsLatitude by remember { mutableStateOf<Double?>(null) }
    var gpsLongitude by remember { mutableStateOf<Double?>(null) }
    var gpsRadiusMeters by remember { mutableStateOf(200f) }
    var ownerLoaded by remember { mutableStateOf(false) }
    var selectedStaff by remember { mutableStateOf<AttendanceStaffRecord?>(null) }
    var staffPendingDelete by remember { mutableStateOf<AttendanceStaffRecord?>(null) }
    var pendingInviteToCancel by remember { mutableStateOf<AttendanceStaffRecord?>(null) }

    suspend fun loadOwnerReport() {
        busy = true
        error = null
        try {
            check(repository.ownerSignedIn()) {
                "Owner Google account is not connected. Connect it from Profile, then reopen Attendance Report."
            }
            val allOwnerStaff = repository.fetchOwnerStaffForReport()
                .filter { it.isActive }
            val ownerWorkplace = repository.fetchOwnerWorkplace()
            val loadedVerificationMode = ownerWorkplace?.verificationMode ?: "owner_approval"
            gpsLatitude = ownerWorkplace?.gpsLatitude
            gpsLongitude = ownerWorkplace?.gpsLongitude
            gpsRadiusMeters = (ownerWorkplace?.attendanceRadiusM ?: 200).toFloat()
            ownerVerificationMode = loadedVerificationMode

            ownerStaff = allOwnerStaff
                .filter {
                    it.linkStatus == "linked" &&
                        it.approvalStatus == "accepted"
                }
                .sortedBy { it.displayName.lowercase() }

            ownerPendingInvites = allOwnerStaff
                .filter {
                    it.linkStatus == "pending" &&
                        it.approvalStatus == "pending"
                }
                .sortedBy { it.displayName.lowercase() }
            ownerRecords = repository.fetchOwnerRecords()
            ownerPendingRequests = repository.fetchOwnerAttendanceRequests()
            ownerLoaded = true
        } catch (e: Exception) {
            error = e.message ?: "Unable to load attendance report."
        } finally {
            busy = false
        }
    }

    suspend fun saveOwnerGpsLocation() {
        busy = true
        error = null
        try {
            val fix = readAttendanceLocation(context)
                ?: error("Unable to get workplace location. Enable Location and try again.")
            check(!fix.isMock) { "Mock location cannot be saved as the workplace location." }
            check(fix.accuracyMeters <= 50.0) {
                "GPS accuracy is ${fix.accuracyMeters.toInt()} m. Move to an open area and try again."
            }
            repository.setOwnerGpsSettings(
                fix.latitude,
                fix.longitude,
                gpsRadiusMeters.toInt().coerceIn(50, 300)
            )
            gpsLatitude = fix.latitude
            gpsLongitude = fix.longitude
            error = null
        } catch (e: Exception) {
            error = e.message ?: "Unable to save workplace location."
        } finally {
            busy = false
        }
    }

    suspend fun saveOwnerGpsRadius() {
        val latitude = gpsLatitude
        val longitude = gpsLongitude
        check(latitude != null && longitude != null) {
            "Set the workplace location first."
        }
        repository.setOwnerGpsSettings(
            latitude,
            longitude,
            gpsRadiusMeters.toInt().coerceIn(50, 300)
        )
    }

    val ownerGpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasLocationPermission) {
            scope.launch { saveOwnerGpsLocation() }
        } else {
            error = "Location permission is required to set the workplace location."
        }
    }

    fun requestOwnerGpsLocation() {
        val required = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val missing = required.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            scope.launch { saveOwnerGpsLocation() }
        } else {
            ownerGpsPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    suspend fun applyOwnerVerificationMode(mode: String) {
        require(mode == "owner_approval" || mode == "gps_auto")
        busy = true
        error = null
        try {
            if (mode == "gps_auto") {
                val workplace = repository.fetchOwnerWorkplace()
                    ?: error("Attendance workplace was not found for this owner account.")
                val workplaceLatitude = workplace.gpsLatitude
                    ?: error("Set the workplace GPS location before enabling GPS Attendance.")
                val workplaceLongitude = workplace.gpsLongitude
                    ?: error("Set the workplace GPS location before enabling GPS Attendance.")
                check(workplace.attendanceRadiusM in 50..300) {
                    "Set a valid attendance radius between 50 and 300 metres."
                }
                repository.setOwnerGpsSettings(
                    workplaceLatitude,
                    workplaceLongitude,
                    gpsRadiusMeters.toInt().coerceIn(50, 300)
                )
            }
            repository.setOwnerAttendanceVerificationMode(mode)
            ownerVerificationMode = mode
        } catch (e: Exception) {
            error = e.message ?: "Unable to update attendance verification."
        } finally {
            busy = false
        }
    }

    fun selectOwnerVerificationMode(mode: String) {
        scope.launch { applyOwnerVerificationMode(mode) }
    }

    LaunchedEffect(Unit) { loadOwnerReport() }

    val activeStaff = selectedStaff

    BackHandler(enabled = selectedStaff != null) {
        selectedStaff = null
    }

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
            },
            containerColor = Color.White
        )
    }

    pendingInviteToCancel?.let { inviteToCancel ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingInviteToCancel = null },
            title = { Text("Cancel attendance invitation?") },
            text = {
                Text(
                    "This will cancel the pending online attendance invitation for ${inviteToCancel.displayName}. No attendance records will be deleted."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            error = null
                            try {
                                repository.cancelOwnerAttendanceInvite(inviteToCancel.id)
                                pendingInviteToCancel = null
                                loadOwnerReport()
                            } catch (e: Exception) {
                                error = e.message ?: "Unable to cancel attendance invitation."
                            } finally {
                                busy = false
                            }
                        }
                    },
                    enabled = !busy
                ) {
                    Text("Cancel invitation", color = Color(0xFFB42318))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingInviteToCancel = null },
                    enabled = !busy
                ) {
                    Text("Keep invitation")
                }
            },
            containerColor = Color.White
        )
    }

    if (showVerificationSettings) {
        AlertDialog(
            onDismissRequest = { if (!busy) showVerificationSettings = false },
            title = { Text("Attendance Verification") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Workplace GPS Location", fontWeight = FontWeight.Bold)
                    Text(
                        if (gpsLatitude != null && gpsLongitude != null)
                            "Location saved · ${"%.5f".format(gpsLatitude)}, ${"%.5f".format(gpsLongitude)}"
                        else "Workplace location not set",
                        color = Color(0xFF667085),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { requestOwnerGpsLocation() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Set / Update Current Workplace Location") }
                    Text("Attendance radius: ${gpsRadiusMeters.toInt()} metres", fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = gpsRadiusMeters,
                        onValueChange = { gpsRadiusMeters = it },
                        valueRange = 50f..300f,
                        steps = 24,
                        enabled = !busy
                    )
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                error = null
                                try {
                                    saveOwnerGpsRadius()
                                } catch (e: Exception) {
                                    error = e.message ?: "Unable to save attendance radius."
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy && gpsLatitude != null && gpsLongitude != null,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save GPS Settings") }

                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            selectOwnerVerificationMode("owner_approval")
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ownerVerificationMode == "owner_approval",
                            onClick = { selectOwnerVerificationMode("owner_approval") },
                            enabled = !busy
                        )
                        Column {
                            Text("Owner Approval", fontWeight = FontWeight.Bold)
                            Text(
                                "Staff requests attendance. It is recorded only after you approve it.",
                                color = Color(0xFF667085),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(enabled = !busy) {
                            selectOwnerVerificationMode("gps_auto")
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = ownerVerificationMode == "gps_auto",
                            onClick = { selectOwnerVerificationMode("gps_auto") },
                            enabled = !busy
                        )
                        Column {
                            Text("GPS – Auto Approved", fontWeight = FontWeight.Bold)
                            Text(
                                "Inside the saved workplace radius: automatic Present. Outside or uncertain GPS: Owner Approval.",
                                color = Color(0xFF667085),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (ownerVerificationMode == "gps_auto") {
                        Text(
                            "Staff must allow location access while marking attendance. Poor accuracy, unavailable GPS or mock location goes to Owner Approval.",
                            color = Color(0xFF667085),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVerificationSettings = false }, enabled = !busy) {
                    Text("Done")
                }
            },
            containerColor = Color.White
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
                        if (status == "unmarked") {
                            repository.clearOwnerAttendance(activeStaff.id, date.toString())
                        } else {
                            repository.setOwnerAttendance(activeStaff.id, date.toString(), status)
                        }
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
                            onClick = { showVerificationSettings = true },
                            enabled = !busy
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Attendance verification settings"
                            )
                        }
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

                if (ownerPendingRequests.isNotEmpty()) {
                    item {
                        Text(
                            "Attendance Approval Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF202124)
                        )
                    }
                    items(ownerPendingRequests, key = { "attendance-request-${it.id}" }) { request ->
                        val requestStaff = ownerStaff.firstOrNull { it.id == request.staffId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF2D19B))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            requestStaff?.displayName ?: "Staff member",
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF202124)
                                        )
                                        Text(
                                            requestStaff?.email.orEmpty(),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF667085)
                                        )
                                    }
                                    Text("Pending", color = Color(0xFFB54708), fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    "Attendance request · ${request.attendanceDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF667085)
                                )
                                request.locationReason?.let { reason ->
                                    val reasonLabel = when (reason) {
                                        "outside_geofence" -> "Staff location is outside the workplace radius."
                                        "near_boundary" -> "GPS position is too close to the radius boundary."
                                        "low_accuracy" -> "GPS accuracy is not sufficient for automatic attendance."
                                        "location_unavailable" -> "Staff location could not be obtained."
                                        "location_permission_denied" -> "Staff denied location permission."
                                        "mock_location_detected" -> "Mock location was detected."
                                        else -> "GPS verification requires owner review."
                                    }
                                    Text(
                                        reasonLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB54708)
                                    )
                                }
                                if (request.locationDistanceM != null || request.locationAccuracyM != null) {
                                    Text(
                                        "Distance: ${request.locationDistanceM?.toInt()?.let { "$it m" } ?: "Unavailable"} · " +
                                            "Accuracy: ${request.locationAccuracyM?.toInt()?.let { "$it m" } ?: "Unavailable"} · " +
                                            "Allowed: ${request.locationRadiusM?.let { "$it m" } ?: "—"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF667085)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                busy = true
                                                error = null
                                                try {
                                                    repository.approveOwnerAttendanceRequest(request.id)
                                                    loadOwnerReport()
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Unable to approve attendance."
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Approve") }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                busy = true
                                                error = null
                                                try {
                                                    repository.rejectOwnerAttendanceRequest(request.id)
                                                    loadOwnerReport()
                                                } catch (e: Exception) {
                                                    error = e.message ?: "Unable to reject attendance."
                                                } finally {
                                                    busy = false
                                                }
                                            }
                                        },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) { Text("Reject") }
                                }
                            }
                        }
                    }
                }

                if (ownerPendingInvites.isNotEmpty()) {

                    items(ownerPendingInvites, key = { "pending-${it.id}" }) { invite ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = Color(0xFFFFF4E5)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            invite.displayName.trim().take(1).uppercase(),
                                            color = Color(0xFFB54708),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        invite.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF202124)
                                    )
                                    Text(
                                        invite.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF667085)
                                    )
                                    Text(
                                        "Pending",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFB54708)
                                    )
                                }

                                TextButton(
                                    onClick = { pendingInviteToCancel = invite },
                                    enabled = !busy
                                ) {
                                    Text("Cancel", color = Color(0xFFB42318))
                                }
                            }
                        }
                    }
                }

                if (ownerStaff.isNotEmpty()) {
                    val today = LocalDate.now(ZoneId.of("Asia/Kolkata")).toString()
                    val todayStaffRecords = ownerStaff.mapNotNull { staff ->
                        ownerRecords.firstOrNull {
                            it.staffId == staff.id && it.attendanceDate == today
                        }
                    }

                    val presentToday = todayStaffRecords.count {
                        it.status.equals("present", true)
                    }
                    val absentToday = todayStaffRecords.count {
                        it.status.equals("absent", true)
                    }
                    val notMarkedToday = ownerStaff.size - presentToday - absentToday
                    val presentPercentage = presentToday * 100 / ownerStaff.size

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    "Today's Attendance",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF202124)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Total Staff",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF667085)
                                        )
                                        Text(
                                            "${ownerStaff.size}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF3448C5)
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            "Present %",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF667085)
                                        )
                                        Text(
                                            "$presentPercentage%",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16803D)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Present",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF16803D)
                                        )
                                        Text(
                                            "$presentToday",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16803D)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Absent",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFFB42318)
                                        )
                                        Text(
                                            "$absentToday",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFB42318)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Not Marked",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color(0xFF667085)
                                        )
                                        Text(
                                            "$notMarkedToday",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF667085)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (ownerStaff.isEmpty() && ownerPendingInvites.isEmpty() && !busy && ownerLoaded) {
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
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Refresh"
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
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onSetStatus(selectedDate, "present") },
                                enabled = !busy && selectedRecord?.status?.equals("present", true) != true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 2.dp,
                                    vertical = 10.dp
                                ),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16803D),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Present", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { onSetStatus(selectedDate, "absent") },
                                enabled = !busy && selectedRecord?.status?.equals("absent", true) != true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 2.dp,
                                    vertical = 10.dp
                                ),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB42318),
                                    contentColor = Color.White
                                )
                            ) {
                                Text("Absent", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                            }

                            OutlinedButton(
                                onClick = { onSetStatus(selectedDate, "unmarked") },
                                enabled = !busy && selectedRecord != null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    horizontal = 2.dp,
                                    vertical = 10.dp
                                )
                            ) {
                                Text("Unmarked", maxLines = 1, style = MaterialTheme.typography.labelLarge)
                            }
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
    val context = LocalContext.current

    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var myStaff by remember { mutableStateOf<List<AttendanceStaffRecord>>(emptyList()) }
    var myRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var myRequests by remember { mutableStateOf<List<AttendanceMarkRequest>>(emptyList()) }
    var myVerificationModes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var pendingStaffAttendanceId by remember { mutableStateOf<String?>(null) }
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
            myVerificationModes = myStaff.associate { staff ->
                staff.id to runCatching { repository.fetchMyAttendanceVerificationMode(staff.id) }
                    .getOrDefault("owner_approval")
            }
            myRecords = repository.fetchMyRecords()
            myRequests = repository.fetchMyAttendanceRequests()
            if (myStaff.isEmpty()) {
                error = "No accepted attendance registration found for $email. Check your invitation on Home or contact your employer."
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to load staff attendance."
        } finally {
            busy = false
        }
    }

    suspend fun markAttendanceForStaff(staffId: String) {
        busy = true
        error = null
        info = null
        try {
            val mode = myVerificationModes[staffId] ?: "owner_approval"
            if (mode == "gps_auto") {
                info = null
                val hasLocationPermission =
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val fix = if (hasLocationPermission) {
                    runCatching { readAttendanceLocation(context) }.getOrNull()
                } else null
                val locationStatus = when {
                    !hasLocationPermission -> "permission_denied"
                    fix == null -> "location_unavailable"
                    else -> "captured"
                }
                val result = repository.submitMyGpsAttendance(
                    staffId = staffId,
                    latitude = fix?.latitude,
                    longitude = fix?.longitude,
                    accuracyMeters = fix?.accuracyMeters,
                    locationStatus = locationStatus,
                    isMockLocation = fix?.isMock == true
                )
                myRecords = repository.fetchMyRecords()
                myRequests = repository.fetchMyAttendanceRequests()
                info = if (result.result == "present") {
                    "Attendance marked Present. GPS accuracy: ${result.accuracyM?.toInt() ?: 0} m."
                } else {
                    val reasonText = when (result.reason) {
                        "outside_geofence" -> "Your location is outside the workplace radius."
                        "near_boundary" -> "Your location is near the radius boundary."
                        "low_accuracy" -> "GPS accuracy is insufficient."
                        "location_unavailable" -> "Location could not be obtained."
                        "location_permission_denied" -> "Location permission was denied."
                        "mock_location_detected" -> "Mock location was detected."
                        else -> "GPS verification needs owner review."
                    }
                    "$reasonText Attendance approval request has been sent to the owner."
                }
            } else {
                repository.requestMyAttendance(staffId)
                myRequests = repository.fetchMyAttendanceRequests()
            }
        } catch (e: Exception) {
            error = e.message ?: "Unable to mark attendance or send approval request."
        } finally {
            busy = false
        }
    }

    val staffLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val staffId = pendingStaffAttendanceId
        pendingStaffAttendanceId = null
        if (staffId != null) {
            // Even when permission is denied, submit the attempt so the owner
            // receives an explicit approval request with the correct reason.
            scope.launch { markAttendanceForStaff(staffId) }
        }
    }

    fun startStaffAttendance(staffId: String) {
        if (myVerificationModes[staffId] == "gps_auto") {
            val required = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            val missing = required.filter {
                ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
            }
            pendingStaffAttendanceId = staffId
            if (missing.isEmpty()) {
                pendingStaffAttendanceId = null
                scope.launch { markAttendanceForStaff(staffId) }
            } else {
                staffLocationPermissionLauncher.launch(missing.toTypedArray())
            }
        } else {
            scope.launch { markAttendanceForStaff(staffId) }
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

            info?.let { message ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF7EE)),
                        border = BorderStroke(1.dp, Color(0xFFB7E4C7))
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(14.dp),
                            color = Color(0xFF16703A)
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
                val selectedDateRequest = myRequests
                    .filter { it.staffId == staff.id && it.attendanceDate == selectedDateString }
                    .maxByOrNull { it.requestedAt }
                val selectedRequestPending = selectedDateRequest?.status == "pending"
                val selectedRequestRejected = selectedDateRequest?.status == "rejected"

                // Staff can submit a request; only the owner can create final attendance.
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
                                selectedRequestPending -> "Approval pending"
                                selectedRequestRejected -> "Request rejected"
                                else -> "Not marked"
                            }
                            Text(
                                selectedStatus,
                                color = when (selectedStatus) {
                                    "Present" -> Color(0xFF16803D)
                                    "Absent" -> Color(0xFFB42318)
                                    "Approval pending" -> Color(0xFFB54708)
                                    "Request rejected" -> Color(0xFFB42318)
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
                            selectedDate == today && selectedRequestPending -> {
                                OutlinedButton(
                                    onClick = {},
                                    enabled = false,
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Awaiting Owner Approval") }
                            }
                            selectedDate == today -> {
                                Button(
                                    onClick = { startStaffAttendance(staff.id) },
                                    enabled = !busy && !todayMarked,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        when {
                                            todayMarked -> "Already Marked"
                                            myVerificationModes[staff.id] == "gps_auto" -> "Mark Attendance"
                                            selectedRequestRejected -> "Request Approval Again"
                                            else -> "Request Attendance Approval"
                                        }
                                    )
                                }
                                if (selectedRequestRejected) {
                                    Text(
                                        "Your previous request was rejected. You can submit a new request.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFB42318)
                                    )
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

