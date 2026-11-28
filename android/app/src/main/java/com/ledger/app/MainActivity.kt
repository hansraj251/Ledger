package com.ledger.app

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.InstallStateUpdatedListener

import com.ledger.app.data.LedgerEntryEntity
import com.ledger.app.data.InterestAccountEntryEntity
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.ledger.app.ui.PartyReportScreen
import com.ledger.app.ui.PartyReportSelectionScreen
import com.ledger.app.ui.ReportTypeSelectionScreen
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn

import com.ledger.app.data.CurrencySettings

import kotlinx.coroutines.launch
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.collectAsState
import androidx.compose.material.icons.filled.ArrowBack

import androidx.compose.foundation.background

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.draw.clip

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone

import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.android.gms.auth.api.identity.Identity
import java.io.File
import com.ledger.app.backup.DriveAccountBinding
import com.ledger.app.backup.GoogleDriveAuthorization
import com.ledger.app.backup.GoogleDriveBackup
import com.ledger.app.backup.GoogleDriveBackupCopies
import com.ledger.app.backup.GoogleDriveRestore
import com.ledger.app.backup.GoogleDriveSyncCoordinator
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.LedgerRepository
import com.ledger.app.data.PartyEntity
import com.ledger.app.ui.HomeScreen
import com.ledger.app.ui.CalculateScreen
import com.ledger.app.ui.WriteScreen
import com.ledger.app.ui.HelpScreen
import com.ledger.app.ui.ActivityScreen
import com.ledger.app.ui.DailyExpenseScreen
import com.ledger.app.ui.GroupSharingScreen
import com.ledger.app.ui.OnlineGroupDetailScreen
import com.ledger.app.ui.GroupExpenseReportScreen
import com.ledger.app.ui.GroupMemberReportSelectionScreen
import com.ledger.app.ui.GroupReportModeSelectionScreen
import com.ledger.app.ui.GroupReportSelectionScreen
import com.ledger.app.ui.AllPartiesReportScreen
import com.ledger.app.ui.LedgerTheme
import com.ledger.app.ui.LedgerViewModel
import com.ledger.app.ui.PartyLedgerScreen
import com.ledger.app.ui.StaffDetailScreen
import com.ledger.app.ui.AttendanceScreen
import com.ledger.app.ui.StaffAttendanceScreen
import com.ledger.app.attendance.AttendanceRepository
import com.ledger.app.attendance.AttendanceInviteRecord
import com.ledger.app.billing.PremiumBillingManager

class MainActivity : ComponentActivity() {

    private val notificationGroupIdState =

        mutableStateOf<String?>(null)


    private val notificationApprovalRequestIdState =

        mutableStateOf<String?>(null)


    
    private val notificationTypeState = mutableStateOf<String?>(null)
    private val notificationStaffIdState = mutableStateOf<String?>(null)
    private val notificationWorkplaceIdState = mutableStateOf<String?>(null)
override fun onNewIntent(intent: Intent) {

        super.onNewIntent(intent)

        setIntent(intent)

        notificationGroupIdState.value = intent.getStringExtra("group_id")

        notificationApprovalRequestIdState.value =

            intent.getStringExtra("request_id")
        notificationTypeState.value = intent.getStringExtra("notification_type")
        notificationStaffIdState.value = intent.getStringExtra("staff_id")
        notificationWorkplaceIdState.value = intent.getStringExtra("workplace_id")
    }

    private lateinit var googleDriveAuthorization: GoogleDriveAuthorization
    private lateinit var googleDriveBackup: GoogleDriveBackup
    private lateinit var googleDriveRestore: GoogleDriveRestore
    private lateinit var googleDriveSyncCoordinator: GoogleDriveSyncCoordinator
    private lateinit var premiumBillingManager: PremiumBillingManager

    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var updateFlowLauncher: ActivityResultLauncher<IntentSenderRequest>

    private var isCheckingForAppUpdate = false
    private var isUpdateFlowInProgress = false
    private var isFlexibleUpdateAccepted = false
    private var lastPromptedUpdateVersionCode: Int? = null
    private var isUpdateCompletionDialogShowing = false

    private val appUpdateInstallListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            isFlexibleUpdateAccepted = false
            runOnUiThread {
                showUpdateDownloadedDialog()
            }
        }
    }

    private lateinit var ledgerSqliteDatabase: SupportSQLiteDatabase

    private var initialDriveSetupCompletion: (() -> Unit)? = null
    private var pendingDriveEmailChange: ((String) -> Unit)? = null
    private var pendingDriveRestoreOnlyConnection = false
    private var pendingBackupDownloadFile: java.io.File? = null


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        notificationGroupIdState.value = intent?.getStringExtra("group_id")

        notificationApprovalRequestIdState.value =

            intent?.getStringExtra("request_id")
        notificationTypeState.value = intent?.getStringExtra("notification_type")
        notificationStaffIdState.value = intent?.getStringExtra("staff_id")
        notificationWorkplaceIdState.value = intent?.getStringExtra("workplace_id")
        if (
            android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }.launch(
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }

        updateFlowLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            isUpdateFlowInProgress = false

            if (result.resultCode == RESULT_OK) {
                isFlexibleUpdateAccepted = true
            } else {
                Toast.makeText(
                    this,
                    "Update was not completed. You can update later.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        appUpdateManager = AppUpdateManagerFactory.create(this)
        appUpdateManager.registerListener(appUpdateInstallListener)

        CurrencySettings.load(this)

        googleDriveAuthorization = GoogleDriveAuthorization(
            this
        )

        googleDriveBackup = GoogleDriveBackup(
            applicationContext
        )

        googleDriveRestore = GoogleDriveRestore(
            applicationContext
        )

        val database = LedgerDatabaseProvider.get(
            applicationContext
        )

        ledgerSqliteDatabase =
            database.openHelper.writableDatabase

        googleDriveSyncCoordinator =
            GoogleDriveSyncCoordinator(
                context = applicationContext,
                authorization = googleDriveAuthorization,
                backup = googleDriveBackup,
                database = ledgerSqliteDatabase,
                onAuthorizationRequired = { intentSender ->
                    try {
                        startIntentSenderForResult(
                            intentSender,
                            GOOGLE_DRIVE_SYNC_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (error: Exception) {
                        googleDriveSyncCoordinator.onAuthorizationCancelled()
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onSyncStarted = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Syncing backup...",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSyncSuccess = {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Backup synced to Google Drive.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onSyncFailure = { error ->
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            "Backup failed: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onRemoteBackupNewer = { fileId, modifiedTime, accessToken, driveAccountId ->
                    runOnUiThread {
                        showRemoteBackupChoice(
                            fileId = fileId,
                            modifiedTime = modifiedTime,
                            accessToken = accessToken,
                            driveAccountId = driveAccountId
                        )
                    }
                }
            )

        val repository = LedgerRepository(
            database
        )

        premiumBillingManager = PremiumBillingManager(applicationContext)

        setContent {
            var showHelpScreen by remember { mutableStateOf(false) }
                var showActivityScreen by remember { mutableStateOf(false) }
            // Temporary UI state only: app restart defaults Auto Sync to ON.
            val autoSyncOffState = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(false)
            }

            val attendanceRepository = remember { AttendanceRepository() }
            val attendanceAuthScope = androidx.compose.runtime.rememberCoroutineScope()
            val attendanceCachePrefs = remember {
                getSharedPreferences("ledger_attendance_home_cache", MODE_PRIVATE)
            }
            // Render the last account's cached card visibility immediately. Once the
            // persisted Ledger Online session is restored, its user ID selects the
            // correct account-specific cache before a background refresh starts.
            val lastAttendanceCacheUserId = remember {
                attendanceCachePrefs.getString("last_user_id", null)
            }
            var attendanceCacheUserId by remember {
                mutableStateOf(lastAttendanceCacheUserId)
            }
            var attendanceHasOwnerReportEntries by remember {
                mutableStateOf(
                    lastAttendanceCacheUserId?.let {
                        attendanceCachePrefs.getBoolean("owner_$it", false)
                    } ?: false
                )
            }
            var attendanceHasLinkedStaff by remember {
                mutableStateOf(
                    lastAttendanceCacheUserId?.let {
                        attendanceCachePrefs.getBoolean("staff_$it", false)
                    } ?: false
                )
            }
            var attendanceTodayPresent by remember {
                mutableStateOf(lastAttendanceCacheUserId?.let {
                    attendanceCachePrefs.getInt("present_$it", 0)
                } ?: 0)
            }
            var attendanceTodayAbsent by remember {
                mutableStateOf(lastAttendanceCacheUserId?.let {
                    attendanceCachePrefs.getInt("absent_$it", 0)
                } ?: 0)
            }
            var attendanceTodayTotal by remember {
                mutableStateOf(lastAttendanceCacheUserId?.let {
                    attendanceCachePrefs.getInt("total_$it", 0)
                } ?: 0)
            }
            var attendancePendingInvites by remember {
                mutableStateOf<List<AttendanceInviteRecord>>(emptyList())
            }
            var attendanceAuthBusy by remember { mutableStateOf(false) }
            var attendanceRefreshQueued by remember { mutableStateOf(false) }
            var attendanceAuthError by remember { mutableStateOf<String?>(null) }

            suspend fun refreshAttendanceForOnlineAccount(expectedUserId: String) {
                if (attendanceAuthBusy) {
                    attendanceRefreshQueued = true
                    return
                }

                attendanceAuthBusy = true
                attendanceAuthError = null
                try {
                    // Do not allow a response for a previous account to overwrite
                    // the currently selected account's cards or local cache.
                    if (attendanceRepository.currentOnlineAccountUserId() != expectedUserId) {
                        return
                    }

                    val email = attendanceRepository.currentOnlineAccountEmail()
                    if (email.isNullOrBlank()) {
                        if (attendanceRepository.currentOnlineAccountUserId() == expectedUserId) {
                            attendanceHasOwnerReportEntries = false
                            attendanceHasLinkedStaff = false
                            attendanceTodayPresent = 0
                            attendanceTodayAbsent = 0
                            attendanceTodayTotal = 0
                            attendancePendingInvites = emptyList()
                            attendanceCachePrefs.edit()
                                .putBoolean("owner_$expectedUserId", false)
                                .putBoolean("staff_$expectedUserId", false)
                                .putInt("present_$expectedUserId", 0)
                                .putInt("absent_$expectedUserId", 0)
                                .putInt("total_$expectedUserId", 0)
                                .apply()
                        }
                    } else {
                        attendanceRepository.claimStaffLinks()
                        val latestInvites = attendanceRepository.fetchMyInvites()
                        val hasLinkedStaff =
                            attendanceRepository.fetchMyStaffForHomeCard().isNotEmpty()

                        val isOwner = attendanceRepository.hasOwnerAttendanceAccess()
                        val reportStaff = if (isOwner) {
                            attendanceRepository.fetchOwnerStaffForReport()
                        } else emptyList()
                        val activeReportStaff = reportStaff.filter {
                            it.isActive &&
                                it.linkStatus == "linked" &&
                                it.approvalStatus == "accepted"
                        }
                        val hasOwnerReportEntries = isOwner && reportStaff.any { staff ->
                            staff.isActive && (
                                (staff.linkStatus == "linked" &&
                                    staff.approvalStatus == "accepted") ||
                                (staff.linkStatus == "pending" &&
                                    staff.approvalStatus == "pending")
                            )
                        }

                        val todayAttendanceDate = java.time.LocalDate.now(
                            java.time.ZoneId.of("Asia/Kolkata")
                        ).toString()
                        val todayAttendanceRecords = if (activeReportStaff.isNotEmpty()) {
                            attendanceRepository.fetchOwnerRecords().filter { record ->
                                record.attendanceDate == todayAttendanceDate &&
                                    activeReportStaff.any { it.id == record.staffId }
                            }
                        } else emptyList()
                        val todayPresentCount = todayAttendanceRecords.count {
                            it.status.equals("present", ignoreCase = true)
                        }
                        val todayAbsentCount = todayAttendanceRecords.count {
                            it.status.equals("absent", ignoreCase = true)
                        }
                        val todayTotalCount = activeReportStaff.size

                        // Re-check identity after all requests; ignore stale results
                        // if the user switched Ledger Online accounts mid-refresh.
                        if (attendanceRepository.currentOnlineAccountUserId() == expectedUserId) {
                            attendancePendingInvites = latestInvites
                            attendanceHasLinkedStaff = hasLinkedStaff
                            attendanceHasOwnerReportEntries = hasOwnerReportEntries
                            attendanceTodayPresent = todayPresentCount
                            attendanceTodayAbsent = todayAbsentCount
                            attendanceTodayTotal = todayTotalCount
                            attendanceCachePrefs.edit()
                                .putString("last_user_id", expectedUserId)
                                .putBoolean("owner_$expectedUserId", hasOwnerReportEntries)
                                .putBoolean("staff_$expectedUserId", hasLinkedStaff)
                                .putInt("present_$expectedUserId", todayPresentCount)
                                .putInt("absent_$expectedUserId", todayAbsentCount)
                                .putInt("total_$expectedUserId", todayTotalCount)
                                .apply()
                        }
                    }
                } catch (error: Exception) {
                    // Keep the cached card visibility on network/auth refresh failure.
                    attendanceAuthError =
                        error.message ?: "Unable to refresh attendance access."
                } finally {
                    attendanceAuthBusy = false
                    if (attendanceRefreshQueued) {
                        attendanceRefreshQueued = false
                        val userId = attendanceCacheUserId
                        if (!userId.isNullOrBlank()) {
                            attendanceAuthScope.launch {
                                refreshAttendanceForOnlineAccount(userId)
                            }
                        }
                    }
                }
            }

            LedgerTheme {
                val premiumBillingState by premiumBillingManager.state.collectAsState()
                LaunchedEffect(premiumBillingState.message) {
                    val message = premiumBillingState.message
                    if (message.isNotBlank()) {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    }
                }

                var initialDriveSetupRequired by remember {
                    mutableStateOf(
                        !isInitialDriveSetupComplete()
                    )
                }

                var initialDriveSetupRunning by remember {
                    mutableStateOf(false)
                }

                var selectedParty by remember {
                    mutableStateOf<PartyEntity?>(null)
                }
                var selectedStaffId by remember { mutableStateOf<Long?>(null) }
                var showAttendanceScreen by remember { mutableStateOf(false) }
                var showStaffAttendanceScreen by remember { mutableStateOf(false) }
                var openStaffEditorOnOpen by remember { mutableStateOf(false) }

                var showProfileScreen by remember {
                        mutableStateOf(false)
                }

                var showAllPartiesReport by remember {
                    mutableStateOf(false)
                }

                val mainPagePrefs = remember {
                    this@MainActivity.getSharedPreferences(
                        "ledger_main_page",
                        android.content.Context.MODE_PRIVATE
                    )
                }

                var openDailyExpenseReportOnLaunch by remember { mutableStateOf(false) }
                var openHomeSidebarOnLaunch by remember { mutableStateOf(false) }
                var showCalculateScreen by remember { mutableStateOf(false) }
                var showWriteScreen by remember { mutableStateOf(false) }
                var showDailyExpenseScreen by remember {
                    mutableStateOf(
                        mainPagePrefs.getBoolean("show_daily_expense", false)
                    )
                }


                var showGroupSharingScreen by remember { mutableStateOf(false) }
                var selectedOnlineGroupId by remember { mutableStateOf<String?>(null) }
                var selectedOnlineApprovalRequestId by remember {
                    mutableStateOf<String?>(null)
                }
                var openOnlineGroupEditorOnOpen by remember { mutableStateOf(false) }

                LaunchedEffect(
                    notificationGroupIdState.value,
                    notificationApprovalRequestIdState.value
                ) {
                    val groupId = notificationGroupIdState.value
                    val requestId = notificationApprovalRequestIdState.value
                    if (!groupId.isNullOrBlank()) {
                        openOnlineGroupEditorOnOpen = false
                        selectedOnlineGroupId = groupId
                        selectedOnlineApprovalRequestId = requestId
                        notificationGroupIdState.value = null
                        notificationApprovalRequestIdState.value = null
                    }
                }
                var selectedSharingGroupId by remember { mutableStateOf<Long?>(null) }
                var openGroupEditorOnOpen by remember { mutableStateOf(false) }
                var selectedSharingMemberId by remember { mutableStateOf<Long?>(null) }
                var showGroupExpenseReport by remember { mutableStateOf(false) }

                var showGroupReportSelection by remember { mutableStateOf(false) }
                var showGroupReportModeSelection by remember { mutableStateOf(false) }
                var showGroupMemberReportSelection by remember { mutableStateOf(false) }
                var groupReportOpenedFromSelection by remember { mutableStateOf(false) }

                var selectedReportGroup by remember {
                    mutableStateOf<com.ledger.app.data.SharingGroupEntity?>(null)
                }
                var reportGroups by remember {
                    mutableStateOf<List<com.ledger.app.data.SharingGroupEntity>>(emptyList())
                }
                var reportGroupsLoading by remember { mutableStateOf(false) }
                var reportGroupMembers by remember {
                    mutableStateOf<List<com.ledger.app.data.SharingMemberEntity>>(emptyList())
                }
                var reportGroupMembersLoading by remember { mutableStateOf(false) }

fun setDailyExpenseScreenVisible(visible: Boolean) {
                    showDailyExpenseScreen = visible
                    mainPagePrefs.edit()
                        .putBoolean("show_daily_expense", visible)
                        .apply()
                }

                BackHandler(enabled = showAttendanceScreen || showStaffAttendanceScreen || showDailyExpenseScreen || showGroupSharingScreen || showCalculateScreen || showWriteScreen) {
                    if (showStaffAttendanceScreen) {
                        showStaffAttendanceScreen = false
                    } else if (showAttendanceScreen) {
                        showAttendanceScreen = false
                    } else if (showWriteScreen) {
                        showWriteScreen = false
                    } else if (showGroupSharingScreen) {
                        showGroupSharingScreen = false
                    } else if (showCalculateScreen) {
                        showCalculateScreen = false
                    } else {
                        setDailyExpenseScreenVisible(false)
                    }
                }

                var showReportTypeSelection by remember { mutableStateOf(false) }
                var showPartyReportSelection by remember { mutableStateOf(false) }
                var showOnePartyReport by remember { mutableStateOf(false) }
                var selectedReportParty by remember { mutableStateOf<PartyEntity?>(null) }
                var reportEntries by remember { mutableStateOf<List<LedgerEntryEntity>>(emptyList()) }
                var reportRecords by remember { mutableStateOf<List<InterestAccountEntryEntity>>(emptyList()) }
                var reportSettlements by remember {
                    mutableStateOf<List<com.ledger.app.data.TransactionSettlementEntity>>(emptyList())
                }
                var reportDataLoading by remember { mutableStateOf(false) }
                var reportParties by remember { mutableStateOf<List<PartyEntity>>(emptyList()) }
                var reportPartiesLoading by remember { mutableStateOf(false) }


                LaunchedEffect(showPartyReportSelection) {
                    if (showPartyReportSelection) {
                        reportPartiesLoading = true
                        try {
                            reportParties = withContext(Dispatchers.IO) {
                                repository.getParties()
                            }
                        } finally {
                            reportPartiesLoading = false
                        }
                    }
                }

                val viewModel = remember {
                    LedgerViewModel(
                        repository,
                        onDataChanged = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        },
                        isPremium = { premiumBillingManager.state.value.isPremium }
                    )
                }



                LaunchedEffect(showGroupReportSelection) {
                    if (showGroupReportSelection) {
                        reportGroupsLoading = true
                        try {
                            reportGroups = withContext(Dispatchers.IO) {
                                LedgerDatabaseProvider.get(this@MainActivity)
                                    .sharingGroupDao().getAll()
                            }
                        } finally {
                            reportGroupsLoading = false
                        }
                    }
                }

                LaunchedEffect(selectedReportGroup?.id) {
                    val group = selectedReportGroup
                    if (group != null) {
                        reportGroupMembersLoading = true
                        try {
                            reportGroupMembers = withContext(Dispatchers.IO) {
                                LedgerDatabaseProvider.get(this@MainActivity)
                                    .sharingMemberDao().getByGroup(group.id)
                            }
                        } finally {
                            reportGroupMembersLoading = false
                        }
                    } else {
                        reportGroupMembers = emptyList()
                    }
                }

LaunchedEffect(showOnePartyReport, selectedReportParty?.id) {
                    val party = selectedReportParty
                    if (showOnePartyReport && party != null) {
                        reportDataLoading = true
                        try {
                            val loadedData = withContext(Dispatchers.IO) {
                                Triple(
                                    repository.getEntries(party.id),
                                    repository.getInterestAccountEntries(party.id),
                                    LedgerDatabaseProvider.get(this@MainActivity)
                                        .transactionSettlementDao()
                                        .getByPartyId(party.id)
                                )
                            }
                            reportEntries = loadedData.first
                            reportRecords = loadedData.second
                            reportSettlements = loadedData.third
                        } finally {
                            reportDataLoading = false
                        }
                    }
                }

                LaunchedEffect(initialDriveSetupRequired) {
                    if (!initialDriveSetupRequired) {
                        googleDriveSyncCoordinator.resume()
                    }
                }
                LaunchedEffect(
                    notificationTypeState.value,
                    notificationApprovalRequestIdState.value,
                    notificationStaffIdState.value,
                    notificationWorkplaceIdState.value
                ) {
                    when (notificationTypeState.value) {
                        "attendance_approval" -> {
                            showActivityScreen = false
                            showHelpScreen = false
                            showStaffAttendanceScreen = false
                            showAttendanceScreen = true
                            showProfileScreen = false
                            setDailyExpenseScreenVisible(false)
                            showGroupSharingScreen = false
                            selectedParty = null
                            selectedStaffId = null

                            showReportTypeSelection = false
                            showPartyReportSelection = false
                            showOnePartyReport = false
                            showAllPartiesReport = false
                            showGroupReportSelection = false
                            showGroupReportModeSelection = false
                            showGroupMemberReportSelection = false
                            showGroupExpenseReport = false
                            selectedReportParty = null
                            selectedReportGroup = null
                        }

                        "staff_invitation" -> {
                            showActivityScreen = false
                            showHelpScreen = false
                            showAttendanceScreen = false
                            showStaffAttendanceScreen = false
                            showProfileScreen = false
                            setDailyExpenseScreenVisible(false)
                            showGroupSharingScreen = false
                            selectedParty = null
                            selectedStaffId = null

                            showReportTypeSelection = false
                            showPartyReportSelection = false
                            showOnePartyReport = false
                            showAllPartiesReport = false
                            showGroupReportSelection = false
                            showGroupReportModeSelection = false
                            showGroupMemberReportSelection = false
                            showGroupExpenseReport = false
                            selectedReportParty = null
                            selectedReportGroup = null

                            attendanceAuthScope.launch {
                                val userId = attendanceCacheUserId
                                    ?: attendanceRepository.currentOnlineAccountUserId()
                                if (!userId.isNullOrBlank()) {
                                    attendanceCacheUserId = userId
                                    refreshAttendanceForOnlineAccount(userId)
                                }
                            }
                        }
                    }

                    if (
                        notificationTypeState.value == "attendance_approval" ||
                        notificationTypeState.value == "staff_invitation"
                    ) {
                        notificationTypeState.value = null
                        notificationApprovalRequestIdState.value = null
                        notificationStaffIdState.value = null
                        notificationWorkplaceIdState.value = null
                    }
                }


                if (initialDriveSetupRequired) {
                    InitialGoogleDriveSetupScreen(
                        isRunning = initialDriveSetupRunning,
                        onConnect = {
                            initialDriveSetupRunning = true

                            initialDriveSetupCompletion = {
                                initialDriveSetupRunning = false
                                initialDriveSetupRequired = false
                            }

                            performInitialGoogleDriveSetup(
                                onComplete = {
                                    initialDriveSetupCompletion?.invoke()
                                    initialDriveSetupCompletion = null
                                }
                            )
                        }
                    )
                } else if (showReportTypeSelection) {
                    ReportTypeSelectionScreen(
                        onBack = {
                            showReportTypeSelection = false
                        },
                        onAllPartiesClick = {
                            showReportTypeSelection = false
                            showAllPartiesReport = true
                        },
                        onOnePartyClick = {
                            showReportTypeSelection = false
                            showPartyReportSelection = true
                        },
                        onGroupReportsClick = {
                            showReportTypeSelection = false
                            showGroupReportSelection = true
                        },
                        onDailyExpensesClick = {
                            showReportTypeSelection = false
                            openDailyExpenseReportOnLaunch = true
                            setDailyExpenseScreenVisible(true)
                        }
                    )
                } else if (showGroupReportSelection) {
                    GroupReportSelectionScreen(
                        groups = reportGroups,
                        isLoading = reportGroupsLoading,
                        onBack = {
                            showGroupReportSelection = false
                            showReportTypeSelection = true
                        },
                        onGroupClick = { group ->
                            selectedReportGroup = group
                            showGroupReportSelection = false
                            showGroupReportModeSelection = true
                        }
                    )
                } else if (showGroupReportModeSelection && selectedReportGroup != null) {
                    GroupReportModeSelectionScreen(
                        groupName = selectedReportGroup!!.name,
                        onBack = {
                            showGroupReportModeSelection = false
                            showGroupReportSelection = true
                        },
                        onAllMembersClick = {
                            selectedSharingGroupId = selectedReportGroup!!.id
                            selectedSharingMemberId = null
                            groupReportOpenedFromSelection = true
                            showGroupReportModeSelection = false
                            showGroupExpenseReport = true
                        },
                        onOneMemberClick = {
                            showGroupReportModeSelection = false
                            showGroupMemberReportSelection = true
                        }
                    )
                } else if (showGroupMemberReportSelection && selectedReportGroup != null) {
                    GroupMemberReportSelectionScreen(
                        members = reportGroupMembers,
                        isLoading = reportGroupMembersLoading,
                        onBack = {
                            showGroupMemberReportSelection = false
                            showGroupReportModeSelection = true
                        },
                        onMemberClick = { member ->
                            selectedSharingGroupId = selectedReportGroup!!.id
                            selectedSharingMemberId = member.id
                            groupReportOpenedFromSelection = true
                            showGroupMemberReportSelection = false
                            showGroupExpenseReport = true
                        }
                    )
                } else if (showPartyReportSelection) {
                    PartyReportSelectionScreen(
                        parties = reportParties,
                        isLoading = reportPartiesLoading,
                        onBack = {
                            showPartyReportSelection = false
                            showReportTypeSelection = true
                        },
                        onPartyClick = { party ->
                            reportDataLoading = true
                            reportEntries = emptyList()
                            reportRecords = emptyList()
                            selectedReportParty = party
                            showPartyReportSelection = false
                            showOnePartyReport = true
                        }
                    )
                } else if (showOnePartyReport && selectedReportParty != null) {
                    val reportParty = selectedReportParty!!
                    if (reportDataLoading) {
                        androidx.compose.material3.CircularProgressIndicator()
                    } else {
                        PartyReportScreen(
                            party = reportParty,
                            entries = reportEntries,
                            records = reportRecords,
                            settlements = reportSettlements,
                            onBack = {
                                showOnePartyReport = false
                                showPartyReportSelection = true
                            }
                        )
                    }
                } else if (showAllPartiesReport) {
                    AllPartiesReportScreen(
                        repository = repository,
                        onBack = {
                            showAllPartiesReport = false
                        }
                    )
                } else if (showGroupExpenseReport) {
                    GroupExpenseReportScreen(
                        groupId = selectedSharingGroupId ?: 0L,
                        memberId = selectedSharingMemberId,
                        onBack = {
                            showGroupExpenseReport = false
                            selectedSharingMemberId = null
                            if (groupReportOpenedFromSelection) {
                                groupReportOpenedFromSelection = false
                                showGroupReportModeSelection = true
                            }
                        }
                    )
                } else if (showActivityScreen) {
                    ActivityScreen(
                        database = ledgerSqliteDatabase,
                        onBack = { showActivityScreen = false }
                    )
                } else if (showHelpScreen) {
                    HelpScreen(onBack = { showHelpScreen = false })
                } else if (showStaffAttendanceScreen) {
                    StaffAttendanceScreen(
                        onBack = { showStaffAttendanceScreen = false }
                    )
                } else if (showAttendanceScreen) {
                    AttendanceScreen(
                        onBack = { showAttendanceScreen = false }
                    )
                } else if (showProfileScreen) {
                    ProfileScreen(
                        onChangeDriveEmail = { onUpdated -> connectGoogleDrive(onUpdated) },
                        onConnectDrive = {
                            showProfileScreen = false
                            initialDriveSetupRunning = false
                            initialDriveSetupRequired = true
                        },
                        onDownloadBackup = { downloadGoogleDriveBackupToDevice() },
                        onCreateBackupCopy = { createGoogleDriveBackupCopy() },
                        onRestoreBackup = { launchLocalBackupRestorePicker() },
                        onRestoreFromDrive = { showDriveBackupRestorePicker() },
                        onClearData = {
                            googleDriveSyncCoordinator.pause()
                            Toast.makeText(
                                this,
                                "Deleting Ledger backup from Google Drive...",
                                Toast.LENGTH_LONG
                            ).show()

                            googleDriveAuthorization.authorize()
                                .addOnSuccessListener { authResult ->
                                    if (authResult.hasResolution()) {
                                        Toast.makeText(
                                            this,
                                            "Please reconnect Google Drive, then try Clear Data again. Local data has not been deleted.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@addOnSuccessListener
                                    }

                                    val token = authResult.accessToken
                                    if (token.isNullOrBlank()) {
                                        Toast.makeText(
                                            this,
                                            "Google Drive authorization failed. Local data has not been deleted.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@addOnSuccessListener
                                    }

                                    googleDriveBackup.deleteAllBackups(token) { result ->
                                        runOnUiThread {
                                            result.onSuccess {
                                                try {
                                                    // Only clear local data after Drive confirms deletion.
                                                    LedgerDatabaseProvider.close()

                                                    databaseList().toList().forEach { dbName ->
                                                        val dbFile = getDatabasePath(dbName)
                                                        listOf(
                                                            dbFile,
                                                            java.io.File(dbFile.path + "-wal"),
                                                            java.io.File(dbFile.path + "-shm"),
                                                            java.io.File(dbFile.path + "-journal")
                                                        ).forEach { file ->
                                                            if (file.exists() && !file.delete()) {
                                                                throw java.io.IOException(
                                                                    "Unable to delete local database file: ${file.name}"
                                                                )
                                                            }
                                                        }
                                                    }

                                                    val prefsDir = java.io.File(
                                                        applicationInfo.dataDir,
                                                        "shared_prefs"
                                                    )
                                                    prefsDir.listFiles()?.forEach { file ->
                                                        if (!file.deleteRecursively()) {
                                                            throw java.io.IOException(
                                                                "Unable to clear local preferences."
                                                            )
                                                        }
                                                    }

                                                    listOf(
                                                        filesDir,
                                                        cacheDir,
                                                        noBackupFilesDir,
                                                        codeCacheDir
                                                    ).distinct().forEach { dir ->
                                                        dir.listFiles()?.forEach { file ->
                                                            if (!file.deleteRecursively()) {
                                                                throw java.io.IOException(
                                                                    "Unable to clear local app files."
                                                                )
                                                            }
                                                        }
                                                    }

                                                    val launchIntent =
                                                        packageManager.getLaunchIntentForPackage(packageName)

                                                    if (launchIntent != null) {
                                                        launchIntent.addFlags(
                                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        )
                                                        finishAffinity()
                                                        startActivity(launchIntent)
                                                        android.os.Process.killProcess(
                                                            android.os.Process.myPid()
                                                        )
                                                    } else {
                                                        Toast.makeText(
                                                            this,
                                                            "Local data and Google Drive backup deleted. Please reopen Ledger.",
                                                            Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                } catch (error: Exception) {
                                                    Toast.makeText(
                                                        this,
                                                        "Drive backup was deleted, but local cleanup failed: ${error.message ?: "Unknown error"}. Please reopen Ledger and check the device.",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }.onFailure { error ->
                                                Toast.makeText(
                                                    this,
                                                    "Google Drive backup deletion failed. Local data was not deleted: ${error.message ?: "Unknown error"}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(
                                        this,
                                        "Google Drive authorization failed. Local data was not deleted: ${error.message ?: "Unknown error"}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                        },
                        onBack = {
                            showProfileScreen = false
                        },
                        onProfileSaved = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        },
        isPremium = premiumBillingState.isPremium,
        premiumPrice = premiumBillingState.productPrice,
        onUpgradeClick = {
            launchPremiumPurchase()
        }
    )
                } else if (selectedOnlineGroupId != null) {
                    OnlineGroupDetailScreen(
                        groupId = selectedOnlineGroupId!!,
                        isPremium = premiumBillingState.isPremium,
                        onBack = {
                            selectedOnlineGroupId = null
                            selectedOnlineApprovalRequestId = null
                            openOnlineGroupEditorOnOpen = false
                        },
                        openEditGroupNameOnOpen = openOnlineGroupEditorOnOpen,
                        initialApprovalRequestId = selectedOnlineApprovalRequestId
                    )
                } else if (showWriteScreen) {
                    WriteScreen(
                        isPremium = premiumBillingState.isPremium,
                        onHomeClick = { showWriteScreen = false },
                        onExpensesClick = { showWriteScreen = false; setDailyExpenseScreenVisible(true) },
                        onCalculateClick = { showWriteScreen = false; showCalculateScreen = true },
                        onDataChanged = { googleDriveSyncCoordinator.requestAutoSync() }
                    )
                } else if (showGroupSharingScreen) {
                    GroupSharingScreen(
                        initialGroupId = selectedSharingGroupId,
                        openGroupEditorOnOpen = openGroupEditorOnOpen,
                        onBack = {
                            showGroupSharingScreen = false
                            selectedSharingGroupId = null
                            openGroupEditorOnOpen = false
                        },
                        onPartiesClick = {
                            showGroupSharingScreen = false
                            setDailyExpenseScreenVisible(false)
                        },
                        onExpensesClick = {
                            showGroupSharingScreen = false
                            setDailyExpenseScreenVisible(true)
                        },
                        onDataChanged = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        },
                        isPremium = premiumBillingState.isPremium,
                        onUpgradeClick = { launchPremiumPurchase() },
                        onGroupReportClick = { groupId ->
                            groupReportOpenedFromSelection = false
                            selectedSharingGroupId = groupId
                            selectedSharingMemberId = null
                            showGroupExpenseReport = true
                        },
                        onMemberReportClick = { groupId, memberId ->
                            groupReportOpenedFromSelection = false
                            selectedSharingGroupId = groupId
                            selectedSharingMemberId = memberId
                            showGroupExpenseReport = true
                        },
                        onGroupDeleted = {
                            selectedSharingGroupId = null
                            showGroupSharingScreen = false
                            setDailyExpenseScreenVisible(false)
                        },
                        onWriteClick = { showGroupSharingScreen = false; showWriteScreen = true }
                    )
                } else if (showCalculateScreen) {
                    CalculateScreen(
                        isPremium = premiumBillingState.isPremium,
                        onHomeClick = { showCalculateScreen = false },
                        onExpensesClick = { showCalculateScreen = false; setDailyExpenseScreenVisible(true) },
                        onWriteClick = { showCalculateScreen = false; showWriteScreen = true }
                    )
                } else if (showDailyExpenseScreen) {
                    DailyExpenseScreen(
                        onOpenHomeSidebar = {
                            openHomeSidebarOnLaunch = true
                            setDailyExpenseScreenVisible(false)
                        },
                        openReportOnLaunch = openDailyExpenseReportOnLaunch,
                        onReportLaunchHandled = {
                            openDailyExpenseReportOnLaunch = false
                        },
                        onPartiesClick = { setDailyExpenseScreenVisible(false) },
                        onGroupSharingClick = {
                            setDailyExpenseScreenVisible(false)
                            showGroupSharingScreen = true
                        },
                        onCalculateClick = { setDailyExpenseScreenVisible(false); showCalculateScreen = true },
                        onWriteClick = { setDailyExpenseScreenVisible(false); showWriteScreen = true },
                        onProfileClick = { showProfileScreen = true },
                        onExpenseDataChanged = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        },
                        onDriveConnectionRequired = { connectGoogleDrive() },
                        isPremium = premiumBillingState.isPremium,
                        onUpgradeClick = { launchPremiumPurchase() }
                    )
                } else if (selectedStaffId != null) {
                    StaffDetailScreen(
                        staffId = selectedStaffId!!,
                        openStaffEditorOnOpen = openStaffEditorOnOpen,
                        onBack = {
                            selectedStaffId = null
                            openStaffEditorOnOpen = false
                        },
                        onDataChanged = { googleDriveSyncCoordinator.requestAutoSync() },
                        isPremium = premiumBillingState.isPremium,
                        onUpgradeClick = { launchPremiumPurchase() }
                    )
                } else if (selectedParty == null) {
                    HomeScreen(
                        onActivityClick = { showActivityScreen = true },
                        onHelpClick = { showHelpScreen = true },
                        viewModel = viewModel,
                        onPartyClick = { party ->
                            selectedParty = party
                        },
                        onProfileClick = {
                            showProfileScreen = true
                        },
                        onReportsClick = {
                            showReportTypeSelection = true
                        },
                        onDailyExpenseClick = { setDailyExpenseScreenVisible(true) },
                        onCalculateClick = { showCalculateScreen = true },
                        onWriteClick = { showWriteScreen = true },
                            onDailyExpenseReportClick = {
                                openDailyExpenseReportOnLaunch = true
                                setDailyExpenseScreenVisible(true)
                            },
                            openSidebarOnLaunch = openHomeSidebarOnLaunch,
                            onSidebarLaunchHandled = {
                                openHomeSidebarOnLaunch = false
                            },
                        onGroupClick = { groupId ->
                            selectedSharingGroupId = groupId
                            openGroupEditorOnOpen = false
                            showGroupSharingScreen = true
                        },
                        onOnlineGroupClick = { groupId ->
                            openOnlineGroupEditorOnOpen = false
                            selectedOnlineGroupId = groupId
                        },
                        onOnlineGroupLongClick = { groupId ->
                            openOnlineGroupEditorOnOpen = true
                            selectedOnlineGroupId = groupId
                        },
                        onGroupLongClick = { groupId ->
                            selectedSharingGroupId = groupId
                            openGroupEditorOnOpen = true
                            showGroupSharingScreen = true
                        },
                        onGroupSharingClick = {
                            setDailyExpenseScreenVisible(false)
                            showGroupSharingScreen = true
                        },
                        isPremium = premiumBillingState.isPremium,
                        premiumPrice = premiumBillingState.productPrice,
                        autoSyncOff = autoSyncOffState.value,
                        onAutoSyncOff = {
                            googleDriveSyncCoordinator.pause()
                            autoSyncOffState.value = true
                        },
                        onAutoSyncOn = {
                            googleDriveSyncCoordinator.resume()
                            autoSyncOffState.value = false
                        },
                        onUpgradeClick = { launchPremiumPurchase() },
                            onStaffClick = { staffId ->
                                openStaffEditorOnOpen = false
                                selectedStaffId = staffId
                            },
                            onStaffLongClick = { staffId ->
                                openStaffEditorOnOpen = true
                                selectedStaffId = staffId
                            },
                            onDriveConnectionRequired = { connectGoogleDrive() },
                            onAttendanceClick = { showAttendanceScreen = true },
                            onOnlineAccountSessionReady = { userId ->
                                attendanceCacheUserId = userId
                                if (userId.isNullOrBlank()) {
                                    attendanceHasOwnerReportEntries = false
                                    attendanceHasLinkedStaff = false
                                    attendancePendingInvites = emptyList()
                                    attendanceCachePrefs.edit().remove("last_user_id").apply()
                                } else {
                                    // Swap to this account's cached state immediately;
                                    // refreshAttendanceForOnlineAccount runs asynchronously.
                                    attendanceHasOwnerReportEntries =
                                        attendanceCachePrefs.getBoolean("owner_$userId", false)
                                    attendanceHasLinkedStaff =
                                        attendanceCachePrefs.getBoolean("staff_$userId", false)
                                    attendanceTodayPresent =
                                        attendanceCachePrefs.getInt("present_$userId", 0)
                                    attendanceTodayAbsent =
                                        attendanceCachePrefs.getInt("absent_$userId", 0)
                                    attendanceTodayTotal =
                                        attendanceCachePrefs.getInt("total_$userId", 0)
                                    attendanceCachePrefs.edit()
                                        .putString("last_user_id", userId)
                                        .apply()
                                    attendanceAuthScope.launch {
                                        refreshAttendanceForOnlineAccount(userId)
                                    }
                                }
                            },
                            onAttendanceDataChanged = {
                                val userId = attendanceCacheUserId
                                if (!userId.isNullOrBlank()) {
                                    attendanceAuthScope.launch {
                                        refreshAttendanceForOnlineAccount(userId)
                                    }
                                }
                            },
                            showOwnerAttendanceReport = attendanceHasOwnerReportEntries,
                            attendanceTodayPresent = attendanceTodayPresent,
                            attendanceTodayAbsent = attendanceTodayAbsent,
                            attendanceTodayTotal = attendanceTodayTotal,
                            hasAttendanceAccess = attendanceHasLinkedStaff,
                            onStaffAttendanceClick = { showStaffAttendanceScreen = true },
                            pendingAttendanceInvites = attendancePendingInvites,
                            attendanceInviteBusy = attendanceAuthBusy,
                            onAttendanceInviteAccept = { staffId ->
                                attendanceAuthScope.launch {
                                    attendanceAuthBusy = true
                                    attendanceAuthError = null
                                    try {
                                        attendanceRepository.acceptMyInvite(staffId)
                                        attendancePendingInvites = attendanceRepository.fetchMyInvites()
                                        val userId = attendanceCacheUserId
                                        if (!userId.isNullOrBlank()) {
                                            refreshAttendanceForOnlineAccount(userId)
                                        }
                                    } catch (error: Exception) {
                                        val message =
                                            error.message ?: "Unable to accept attendance invitation."
                                        attendanceAuthError = message
                                        Toast.makeText(
                                            this@MainActivity,
                                            message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        attendanceAuthBusy = false
                                    }
                                }
                            },
                            onAttendanceInviteReject = { staffId ->
                                attendanceAuthScope.launch {
                                    attendanceAuthBusy = true
                                    attendanceAuthError = null
                                    try {
                                        attendanceRepository.rejectMyInvite(staffId)
                                        attendancePendingInvites =
                                            attendanceRepository.fetchMyInvites()

                                        Toast.makeText(
                                            this@MainActivity,
                                            "Attendance invitation rejected.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (error: Exception) {
                                        val message = error.message
                                            ?: "Unable to reject attendance invitation."
                                        attendanceAuthError = message

                                        Toast.makeText(
                                            this@MainActivity,
                                            message,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } finally {
                                        attendanceAuthBusy = false
                                    }
                                }
                            },
                            onDataChanged = {
                            android.util.Log.d("LedgerSyncDebug", "MainActivity PartyLedgerScreen callback reached")
                            googleDriveSyncCoordinator.requestAutoSync()
                        }
                    )
                } else {
                    PartyLedgerScreen(
                        party = selectedParty!!,
                        viewModel = viewModel,
                        onBack = {
                            selectedParty = null
                        },
                        isPremium = premiumBillingState.isPremium,
                        onUpgradeClick = { launchPremiumPurchase() },
                        onDataChanged = {
                            googleDriveSyncCoordinator.requestAutoSync()
                        }
                    )
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::premiumBillingManager.isInitialized) {
            premiumBillingManager.refreshOwnership()
        }

        if (::appUpdateManager.isInitialized) {
            checkForAppUpdate()
        }
    }

    override fun onDestroy() {
        if (::appUpdateManager.isInitialized) {
            appUpdateManager.unregisterListener(appUpdateInstallListener)
        }
        if (::premiumBillingManager.isInitialized) premiumBillingManager.close()
        super.onDestroy()
    }

    private fun checkForAppUpdate() {
        if (isCheckingForAppUpdate ||
            isUpdateFlowInProgress ||
            isFlexibleUpdateAccepted
        ) return

        isCheckingForAppUpdate = true

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                isCheckingForAppUpdate = false

                if (isFinishing || isDestroyed) return@addOnSuccessListener

                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    showUpdateDownloadedDialog()
                    return@addOnSuccessListener
                }

                if (
                    info.updateAvailability() == com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                ) {
                    val availableVersion = info.availableVersionCode()

                    if (lastPromptedUpdateVersionCode == availableVersion) {
                        return@addOnSuccessListener
                    }

                    lastPromptedUpdateVersionCode = availableVersion

                    android.app.AlertDialog.Builder(this)
                        .setTitle("Update Available")
                        .setMessage(
                            "A new version of Ledger is available. " +
                                "Update now to get the latest features and improvements."
                        )
                        .setPositiveButton("Update Now") { _, _ ->
                            try {
                                isUpdateFlowInProgress = true

                                appUpdateManager.startUpdateFlowForResult(
                                    info,
                                    updateFlowLauncher,
                                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                                )
                            } catch (error: Exception) {
                                isUpdateFlowInProgress = false
                                Toast.makeText(
                                    this,
                                    "Unable to start update. Please try again later.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        .setNegativeButton("Later", null)
                        .show()
                }
            }
            .addOnFailureListener {
                isCheckingForAppUpdate = false
            }
    }

    private fun showUpdateDownloadedDialog() {
        if (isFinishing || isDestroyed || isUpdateCompletionDialogShowing) return

        isUpdateCompletionDialogShowing = true

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("Update Ready")
            .setMessage(
                "The Ledger update has been downloaded. " +
                    "Restart the app now to install the update."
            )
            .setPositiveButton("Restart Now") { _, _ ->
                appUpdateManager.completeUpdate()
            }
            .setNegativeButton("Later", null)
            .create()

        dialog.setOnDismissListener {
            isUpdateCompletionDialogShowing = false
        }

        dialog.show()
    }

    private fun launchPremiumPurchase() {
        val accountId = DriveAccountBinding.currentAccountId(applicationContext)
        if (accountId.isNullOrBlank()) {
            Toast.makeText(
                this,
                "Connect Google Drive and verify the account before purchasing Premium.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        premiumBillingManager.launchPurchase(this, accountId)
    }

    private fun captureDriveAccountIdentity(
        accessToken: String?,
        onVerified: ((String) -> Unit)? = null
    ) {
        DriveAccountBinding.verifyAndCapture(applicationContext, accessToken) { result ->
            runOnUiThread {
                result.onSuccess { accountId ->
                    if (::googleDriveSyncCoordinator.isInitialized) {
                        googleDriveSyncCoordinator.bindVerifiedAccount(accountId)
                    }
                    if (::premiumBillingManager.isInitialized) premiumBillingManager.refreshOwnership()
                    val email = DriveAccountBinding.currentEmail(applicationContext)
                    val callback = pendingDriveEmailChange
                    pendingDriveEmailChange = null
                    if (email != null) callback?.invoke(email)
                    onVerified?.invoke(accountId)
                    Toast.makeText(this, "Google Drive account verified.", Toast.LENGTH_LONG).show()
                }.onFailure {
                    pendingDriveEmailChange = null
                    Toast.makeText(this, "Unable to verify the Google Drive account. Please reconnect Drive.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun connectGoogleDrive(onEmailChanged: ((String) -> Unit)? = null) {
        pendingDriveRestoreOnlyConnection = onEmailChanged == null
        if (onEmailChanged != null) {
            pendingDriveEmailChange = onEmailChanged
        } else {
            googleDriveSyncCoordinator.pause()
        }

        googleDriveAuthorization
            .authorize(forceSelectAccount = onEmailChanged != null)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        val pendingIntent = result.pendingIntent
                        if (pendingIntent == null) {
                            pendingDriveRestoreOnlyConnection = false
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(
                                this,
                                "Google Drive authorization is required.",
                                Toast.LENGTH_LONG
                            ).show()
                            return@addOnSuccessListener
                        }

                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            GOOGLE_DRIVE_AUTH_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (error: Exception) {
                        pendingDriveRestoreOnlyConnection = false
                        googleDriveSyncCoordinator.onAuthorizationCancelled()
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else if (pendingDriveRestoreOnlyConnection) {
                    restoreFromNewDriveAccount(result.accessToken)
                } else {
                    captureDriveAccountIdentity(result.accessToken) {
                        googleDriveSyncCoordinator.resume()
                    }
                }
            }
            .addOnFailureListener {
                pendingDriveEmailChange = null
                pendingDriveRestoreOnlyConnection = false
                googleDriveSyncCoordinator.onAuthorizationCancelled()
                Toast.makeText(
                    this,
                    "Google Drive authorization failed.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    /*
     * A first-time Drive connection must check and restore the remote backup
     * before local data can be synced to that account.
     */
    private fun restoreFromNewDriveAccount(accessToken: String?) {
        if (accessToken.isNullOrBlank()) {
            pendingDriveRestoreOnlyConnection = false
            googleDriveSyncCoordinator.onAuthorizationCancelled()
            Toast.makeText(this, "Google Drive access token is unavailable.", Toast.LENGTH_LONG).show()
            return
        }
        Toast.makeText(this, "Verifying Google Drive account...", Toast.LENGTH_SHORT).show()
        DriveAccountBinding.verifyAndCapture(applicationContext, accessToken) { identityResult ->
            runOnUiThread {
                identityResult.onSuccess { accountId ->
                    pendingDriveRestoreOnlyConnection = false
                    googleDriveSyncCoordinator.bindVerifiedAccount(accountId)
                    if (::premiumBillingManager.isInitialized) premiumBillingManager.refreshOwnership()
                    Toast.makeText(this, "Checking this account's Google Drive backup...", Toast.LENGTH_SHORT).show()
                    googleDriveRestore.findBackupMetadata(accessToken) { backupResult ->
                        backupResult.onSuccess { metadata ->
                            if (metadata == null || metadata.fileId.isBlank()) {
                                googleDriveSyncCoordinator.markNoRemoteBackupFound()
                                Toast.makeText(this, "No backup found in this Google account. Your local data is unchanged.", Toast.LENGTH_LONG).show()
                                return@onSuccess
                            }
                            Toast.makeText(this, "Backup found. Restoring...", Toast.LENGTH_LONG).show()
                            googleDriveRestore.restore(accessToken, metadata.fileId, accountId) { restoreResult ->
                                restoreResult.onSuccess {
                                    if (DriveAccountBinding.currentAccountId(applicationContext) != accountId) {
                                        googleDriveSyncCoordinator.onAuthorizationCancelled()
                                        Toast.makeText(this, "Drive account changed. Local database was not replaced.", Toast.LENGTH_LONG).show()
                                        return@onSuccess
                                    }
                                    replaceDatabaseAndRestart(onComplete = {}, remoteModifiedTime = metadata.modifiedTime)
                                }.onFailure { error ->
                                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                                    Toast.makeText(this, "Backup restore failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.onFailure { error ->
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(this, "Unable to check Google Drive: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }.onFailure {
                    pendingDriveRestoreOnlyConnection = false
                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                    Toast.makeText(this, "Google Drive account verification failed. Local data was not changed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /*
     * Manual and automatic backup requests are serialized by
     * GoogleDriveSyncCoordinator. No direct Drive upload is started here.
     */

    private fun showRemoteBackupChoice(
        fileId: String,
        modifiedTime: String,
        accessToken: String,
        driveAccountId: String
    ) {
        if (DriveAccountBinding.currentAccountId(applicationContext) != driveAccountId) {
            googleDriveSyncCoordinator.pause()
            return
        }
        if (isFinishing || isDestroyed) {
            googleDriveSyncCoordinator.pause()
            return
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("Backup found in this Google Drive account")
            .setMessage(
                "This Google Drive account already contains a Ledger backup. " +
                    "Choose how you want to continue.\n\n" +
                    "Restore Backup: replaces the Ledger data currently stored on this device " +
                    "with the selected Google Drive backup.\n\n" +
                    "Sync Local Data: keeps the data on this device and updates the backup " +
                    "in this Google Drive account."
            )
            .setPositiveButton("Restore Backup") { _, _ ->
                if (DriveAccountBinding.currentAccountId(applicationContext) == driveAccountId) {
                    performAutomaticRemoteRestore(
                        fileId = fileId,
                        modifiedTime = modifiedTime,
                        accessToken = accessToken,
                        driveAccountId = driveAccountId
                    )
                } else {
                    googleDriveSyncCoordinator.pause()
                    Toast.makeText(
                        this,
                        "Google Drive account changed. No data was modified.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Sync Local Data") { _, _ ->
                if (DriveAccountBinding.currentAccountId(applicationContext) == driveAccountId) {
                    googleDriveSyncCoordinator.syncLocalDataAfterRemoteConflict()
                    Toast.makeText(
                        this,
                        "Keeping local data and syncing it to this Google Drive account...",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    googleDriveSyncCoordinator.pause()
                    Toast.makeText(
                        this,
                        "Google Drive account changed. No data was synced.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNeutralButton("Later") { _, _ ->
                // Keep sync paused. The next sync attempt rechecks the remote backup.
                googleDriveSyncCoordinator.pause()
            }
            .setOnCancelListener {
                // Back/outside dismissal must never silently choose either data source.
                googleDriveSyncCoordinator.pause()
            }
            .show()
    }

    private fun performAutomaticRemoteRestore(
        fileId: String,
        modifiedTime: String,
        accessToken: String,
        driveAccountId: String
    ) {
        if (DriveAccountBinding.currentAccountId(applicationContext) != driveAccountId) {
            googleDriveSyncCoordinator.onAuthorizationCancelled()
            return
        }
        Toast.makeText(
            this,
            "Newer Google Drive backup found. Restoring...",
            Toast.LENGTH_LONG
        ).show()

        googleDriveRestore.restore(
            accessToken = accessToken,
            fileIdOverride = fileId,
            driveAccountId = driveAccountId
        ) { restoreResult ->
            restoreResult
                .onSuccess {
                    try {
                        if (DriveAccountBinding.currentAccountId(applicationContext) != driveAccountId) {
                            throw IllegalStateException("Google Drive account changed during restore.")
                        }
                        val validatedFile = googleDriveRestore.getValidatedBackupFile()
                        installValidatedDatabase(validatedFile)

                        googleDriveSyncCoordinator
                            .markRemoteBackupSynced(
                                modifiedTime
                            )

                        Toast.makeText(
                            this,
                            "Newer backup restored successfully.",
                            Toast.LENGTH_LONG
                        ).show()

                        window.decorView.postDelayed(
                            {
                                val launchIntent =
                                    packageManager
                                        .getLaunchIntentForPackage(
                                            packageName
                                        )

                                finishAffinity()

                                if (launchIntent != null) {
                                    launchIntent.addFlags(
                                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    )

                                    startActivity(launchIntent)
                                }
                            },
                            500
                        )
                    } catch (error: Exception) {
                        googleDriveSyncCoordinator
                            .resumeAfterRemoteRestoreFailure()

                        Toast.makeText(
                            this,
                            "Automatic restore failed: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .onFailure { error ->
                    googleDriveSyncCoordinator
                        .resumeAfterRemoteRestoreFailure()

                    Toast.makeText(
                        this,
                        "Automatic restore failed: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun createGoogleDriveBackupCopy() {
        Toast.makeText(this, "Creating a copy on Google Drive...", Toast.LENGTH_SHORT).show()
        googleDriveAuthorization.authorize()
            .addOnSuccessListener { auth ->
                if (auth.hasResolution()) {
                    Toast.makeText(this, "Reconnect Google Drive before creating a copy.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val token = auth.accessToken
                if (token.isNullOrBlank()) {
                    Toast.makeText(this, "Google Drive access token is unavailable.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                DriveAccountBinding.verifyAndCapture(applicationContext, token) { identity ->
                    identity.onSuccess {
                        GoogleDriveBackupCopies(applicationContext).createCopy(token) { result ->
                            result.onSuccess { copy ->
                                Toast.makeText(
                                    this,
                                    "Backup copy created on Google Drive: ${copy.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure { error ->
                                Toast.makeText(
                                    this,
                                    "Could not create Drive copy: ${error.message ?: "Unknown error"}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }.onFailure { error ->
                        Toast.makeText(this, "Google Drive account verification failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Google Drive authorization failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDriveBackupRestorePicker() {
        Toast.makeText(this, "Loading Google Drive backup copies...", Toast.LENGTH_SHORT).show()
        googleDriveAuthorization.authorize()
            .addOnSuccessListener { auth ->
                if (auth.hasResolution()) {
                    Toast.makeText(this, "Reconnect Google Drive before restoring.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val token = auth.accessToken
                if (token.isNullOrBlank()) {
                    Toast.makeText(this, "Google Drive access token is unavailable.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                DriveAccountBinding.verifyAndCapture(applicationContext, token) { identity ->
                    identity.onSuccess { accountId ->
                        GoogleDriveBackupCopies(applicationContext).listBackups(token) { listResult ->
                            listResult.onSuccess { backups ->
                                if (backups.isEmpty()) {
                                    Toast.makeText(this, "No Ledger backup copies found on Google Drive.", Toast.LENGTH_LONG).show()
                                    return@onSuccess
                                }
                                showDriveBackupCopiesDialog(token, accountId, backups)
                            }.onFailure { error ->
                                Toast.makeText(this, "Could not load Drive backups: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }.onFailure { error ->
                        Toast.makeText(this, "Google Drive account verification failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Google Drive authorization failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDriveBackupCopiesDialog(
        accessToken: String,
        accountId: String,
        backups: List<GoogleDriveBackupCopies.BackupCopy>
    ) {
        val density = resources.displayMetrics.density
        fun Int.dp(): Int = (this * density).toInt()

        val listContainer = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(16.dp(), 8.dp(), 16.dp(), 8.dp())
        }

        backups.forEach { item ->
            val readable = runCatching {
                val instant = java.time.Instant.parse(item.modifiedTime)
                java.time.format.DateTimeFormatter
                    .ofPattern("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
                    .withZone(java.time.ZoneId.systemDefault())
                    .format(instant)
            }.getOrDefault(item.modifiedTime)

            val row = android.widget.LinearLayout(this).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(0, 8.dp(), 0, 8.dp())
            }
            val details = android.widget.TextView(this).apply {
                text = "${item.name}\n$readable"
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)
                setPadding(0, 4.dp(), 8.dp(), 4.dp())
                isClickable = true
                setOnClickListener {
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Restore selected backup?")
                        .setMessage(
                            "Selected: ${item.name}\n\n" +
                                "Current local data will first be saved as a safety backup on this device."
                        )
                        .setPositiveButton("Restore") { _, _ ->
                            restoreSelectedGoogleDriveBackup(
                                accessToken,
                                accountId,
                                item.fileId,
                                item.modifiedTime
                            )
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
            row.addView(
                details,
                android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            )

            // Only explicitly named copies get a delete action. The original Ledger Backup.db
            // is deliberately rendered without a Delete button.
            if (item.name.startsWith("Ledger Backup Copy ") && item.name != "Ledger Backup.db") {
                val deleteButton = android.widget.Button(this).apply {
                    text = "Delete"
                    isAllCaps = false
                    setOnClickListener {
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Delete this backup copy?")
                            .setMessage("${item.name}\n\nOnly this copy will be deleted. The original Ledger backup will remain untouched.")
                            .setPositiveButton("Delete") { _, _ ->
                                GoogleDriveBackupCopies(applicationContext).deleteCopy(accessToken, item) { deleteResult ->
                                    deleteResult.onSuccess {
                                        Toast.makeText(this@MainActivity, "Backup copy deleted.", Toast.LENGTH_SHORT).show()
                                        GoogleDriveBackupCopies(applicationContext).listBackups(accessToken) { refreshed ->
                                            refreshed.onSuccess { latest ->
                                                showDriveBackupCopiesDialog(accessToken, accountId, latest)
                                            }.onFailure { error ->
                                                Toast.makeText(this@MainActivity, "Could not refresh backups: ${error.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }.onFailure { error ->
                                        Toast.makeText(this@MainActivity, "Could not delete copy: ${error.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
                row.addView(deleteButton)
            }
            listContainer.addView(row)
            listContainer.addView(android.view.View(this).apply {
                setBackgroundColor(0xFFEAEAEA.toInt())
            }, android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                1.dp()
            ))
        }

        val scroll = android.widget.ScrollView(this).apply { addView(listContainer) }
        android.app.AlertDialog.Builder(this)
            .setTitle("Restore from Google Drive")
            .setView(scroll)
            .setNegativeButton("Close", null)
            .show()
    }

    private fun restoreSelectedGoogleDriveBackup(
        accessToken: String,
        accountId: String,
        fileId: String,
        modifiedTime: String
    ) {
        googleDriveSyncCoordinator.pause()
        googleDriveSyncCoordinator.bindVerifiedAccount(accountId)
        Toast.makeText(this, "Downloading selected backup...", Toast.LENGTH_LONG).show()
        googleDriveRestore.restore(accessToken, fileId, accountId) { restoreResult ->
            restoreResult.onSuccess {
                try {
                    if (DriveAccountBinding.currentAccountId(applicationContext) != accountId) {
                        throw IllegalStateException("Google Drive account changed during restore.")
                    }
                    installValidatedDatabase(googleDriveRestore.getValidatedBackupFile())
                    googleDriveSyncCoordinator.markRemoteRestoreCompleted(modifiedTime)
                    Toast.makeText(this, "Backup restored successfully. A local safety copy was saved.", Toast.LENGTH_LONG).show()
                    window.decorView.postDelayed({
                        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                        finishAffinity()
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            startActivity(launchIntent)
                        }
                    }, 500)
                } catch (error: Exception) {
                    googleDriveSyncCoordinator.resumeAfterRemoteRestoreFailure()
                    Toast.makeText(this, "Restore failed: ${error.message ?: "Unable to replace database"}", Toast.LENGTH_LONG).show()
                }
            }.onFailure { error ->
                googleDriveSyncCoordinator.resumeAfterRemoteRestoreFailure()
                Toast.makeText(this, "Restore failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadGoogleDriveBackupToDevice() {
        Toast.makeText(
            this,
            "Downloading Ledger backup from Google Drive...",
            Toast.LENGTH_LONG
        ).show()

        googleDriveAuthorization.authorize()
            .addOnSuccessListener { authResult ->
                if (authResult.hasResolution()) {
                    Toast.makeText(
                        this,
                        "Please reconnect Google Drive before downloading a backup.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                val token = authResult.accessToken
                if (token.isNullOrBlank()) {
                    Toast.makeText(
                        this,
                        "Google Drive access token is unavailable.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@addOnSuccessListener
                }

                googleDriveRestore.downloadBackupFile(token) { result ->
                    result.onSuccess { backupFile ->
                        pendingBackupDownloadFile = backupFile
                        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "application/octet-stream"
                            putExtra(Intent.EXTRA_TITLE, "Ledger_Backup.db")
                        }

                        try {
                            startActivityForResult(
                                intent,
                                GOOGLE_DRIVE_BACKUP_SAVE_REQUEST_CODE
                            )
                        } catch (error: Exception) {
                            pendingBackupDownloadFile = null
                            backupFile.delete()
                            Toast.makeText(
                                this,
                                "Unable to open the save-file picker.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }.onFailure { error ->
                        Toast.makeText(
                            this,
                            "Backup download failed: ${error.message ?: "Unknown error"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${error.message ?: "Unknown error"}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun launchLocalBackupRestorePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, GOOGLE_DRIVE_LOCAL_RESTORE_REQUEST_CODE)
    }

    private fun restoreLocalBackupFromUri(uri: android.net.Uri) {
        googleDriveSyncCoordinator.pause()

        Thread {
            val selectedFile = java.io.File(cacheDir, "ledger_selected_local_backup.db")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    selectedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Unable to read the selected backup file.")

                // Validate the selected SQLite database before replacing the live database.
                googleDriveRestore.prepareLocalBackupFile(selectedFile)
                selectedFile.delete()

                runOnUiThread {
                    replaceDatabaseAndRestart(onComplete = {})
                }
            } catch (error: Exception) {
                selectedFile.delete()
                runOnUiThread {
                    googleDriveSyncCoordinator.resume()
                    Toast.makeText(
                        this,
                        "Restore failed. The current Ledger data was not replaced: " +
                            (error.message ?: "Invalid backup file"),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun performGoogleDriveRestore() {
        googleDriveSyncCoordinator.pause()
        Toast.makeText(this, "Verifying Google Drive account...", Toast.LENGTH_SHORT).show()
        googleDriveAuthorization.authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    Toast.makeText(this, "Google Drive authorization is required. Connect Google Drive first.", Toast.LENGTH_LONG).show()
                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                    return@addOnSuccessListener
                }
                val accessToken = result.accessToken
                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(this, "Google Drive access token is unavailable.", Toast.LENGTH_LONG).show()
                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                    return@addOnSuccessListener
                }
                DriveAccountBinding.verifyAndCapture(applicationContext, accessToken) { identityResult ->
                    runOnUiThread {
                        identityResult.onSuccess { accountId ->
                            googleDriveSyncCoordinator.bindVerifiedAccount(accountId)
                            googleDriveRestore.findBackupMetadata(accessToken) { metadataResult ->
                                metadataResult.onSuccess { metadata ->
                                    if (metadata == null) {
                                        googleDriveSyncCoordinator.markNoRemoteBackupFound()
                                        Toast.makeText(this, "No backup found in this Google account.", Toast.LENGTH_LONG).show()
                                        return@onSuccess
                                    }
                                    googleDriveRestore.restore(accessToken, metadata.fileId, accountId) { restoreResult ->
                                        restoreResult.onSuccess {
                                            try {
                                                if (DriveAccountBinding.currentAccountId(applicationContext) != accountId) {
                                                    throw IllegalStateException("Google Drive account changed during restore.")
                                                }
                                                installValidatedDatabase(googleDriveRestore.getValidatedBackupFile())
                                                googleDriveSyncCoordinator.markRemoteRestoreCompleted(metadata.modifiedTime)
                                                Toast.makeText(this, "Backup restored successfully.", Toast.LENGTH_LONG).show()
                                                window.decorView.postDelayed({
                                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                                    finishAffinity()
                                                    if (launchIntent != null) {
                                                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                                        startActivity(launchIntent)
                                                    }
                                                }, 500)
                                            } catch (error: Exception) {
                                                googleDriveSyncCoordinator.resumeAfterRemoteRestoreFailure()
                                                Toast.makeText(this, "Restore failed: ${error.message ?: "Unable to replace database"}", Toast.LENGTH_LONG).show()
                                            }
                                        }.onFailure { error ->
                                            googleDriveSyncCoordinator.resumeAfterRemoteRestoreFailure()
                                            Toast.makeText(this, "Restore failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }.onFailure { error ->
                                    googleDriveSyncCoordinator.resumeAfterRemoteRestoreFailure()
                                    Toast.makeText(this, "Unable to check Google Drive: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.onFailure { error ->
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(this, "Google Drive account verification failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                googleDriveSyncCoordinator.onAuthorizationCancelled()
                Toast.makeText(this, "Google Drive authorization failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
    }

    private fun isInitialDriveSetupComplete(): Boolean {
        return getSharedPreferences(
            "ledger_setup",
            MODE_PRIVATE
        ).getBoolean(
            "initial_drive_setup_complete",
            false
        )
    }

    private fun markInitialDriveSetupComplete() {
        getSharedPreferences(
            "ledger_setup",
            MODE_PRIVATE
        )
            .edit()
            .putBoolean(
                "initial_drive_setup_complete",
                true
            )
            .apply()
    }

    private fun performInitialGoogleDriveSetup(
        onComplete: () -> Unit
    ) {
        Toast.makeText(
            this,
            "Connect Google Drive to continue.",
            Toast.LENGTH_SHORT
        ).show()

        googleDriveAuthorization
            .authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    try {
                        val pendingIntent =
                            result.pendingIntent

                        if (pendingIntent == null) {
                            Toast.makeText(
                                this,
                                "Google Drive authorization is required.",
                                Toast.LENGTH_LONG
                            ).show()

                            return@addOnSuccessListener
                        }

                        startIntentSenderForResult(
                            pendingIntent.intentSender,
                            GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE,
                            null,
                            0,
                            0,
                            0
                        )
                    } catch (error: Exception) {
                        Toast.makeText(
                            this,
                            "Unable to open Google Drive authorization.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    return@addOnSuccessListener
                }

                continueInitialGoogleDriveSetup(
                    onComplete
                )
            }
            .addOnFailureListener { error ->
                Toast.makeText(
                    this,
                    "Google Drive authorization failed: ${
                        error.message ?: "Unknown error"
                    }",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun continueInitialGoogleDriveSetup(onComplete: () -> Unit) {
        googleDriveAuthorization.authorize()
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val sender = result.pendingIntent?.intentSender
                    if (sender == null) {
                        Toast.makeText(this, "Google Drive authorization is required.", Toast.LENGTH_LONG).show()
                    } else {
                        try {
                            startIntentSenderForResult(sender, GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE, null, 0, 0, 0)
                        } catch (error: Exception) {
                            Toast.makeText(this, "Unable to open Google Drive authorization.", Toast.LENGTH_LONG).show()
                        }
                    }
                    return@addOnSuccessListener
                }
                val accessToken = result.accessToken
                if (accessToken.isNullOrBlank()) {
                    Toast.makeText(this, "Google Drive access token is unavailable.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                Toast.makeText(this, "Verifying Google Drive account and checking backup...", Toast.LENGTH_SHORT).show()
                DriveAccountBinding.verifyAndCapture(applicationContext, accessToken) { identityResult ->
                    runOnUiThread {
                        identityResult.onSuccess { accountId ->
                            googleDriveSyncCoordinator.bindVerifiedAccount(accountId)
                            if (::premiumBillingManager.isInitialized) premiumBillingManager.refreshOwnership()
                            googleDriveRestore.findBackupMetadata(accessToken) { backupResult ->
                                backupResult.onSuccess { metadata ->
                                    if (metadata == null || metadata.fileId.isBlank()) {
                                        markInitialDriveSetupComplete()
                                        googleDriveSyncCoordinator.markNoRemoteBackupFound()
                                        Toast.makeText(this, "No backup found. Starting fresh.", Toast.LENGTH_SHORT).show()
                                        onComplete()
                                        return@onSuccess
                                    }
                                    Toast.makeText(this, "Backup found. Restoring...", Toast.LENGTH_LONG).show()
                                    googleDriveRestore.restore(accessToken, metadata.fileId, accountId) { restoreResult ->
                                        restoreResult.onSuccess {
                                            if (DriveAccountBinding.currentAccountId(applicationContext) != accountId) {
                                                googleDriveSyncCoordinator.onAuthorizationCancelled()
                                                Toast.makeText(this, "Drive account changed. Local data was not replaced.", Toast.LENGTH_LONG).show()
                                                return@onSuccess
                                            }
                                            replaceDatabaseAndRestart(onComplete, remoteModifiedTime = metadata.modifiedTime)
                                        }.onFailure { error ->
                                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                                            Toast.makeText(this, "Backup restore failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }.onFailure { error ->
                                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                                    Toast.makeText(this, "Unable to check Google Drive: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }.onFailure { error ->
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(this, "Google Drive account verification failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Google Drive authorization failed: ${error.message ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Creates a durable, app-private snapshot of the current database before any restore.
     * The copy is retained in filesDir/restore_safety and is never overwritten by a later restore.
     */
    private fun createLocalSafetyBackup() {
        val database = LedgerDatabaseProvider.get(applicationContext).openHelper.writableDatabase
        val folder = File(filesDir, "restore_safety")
        if (!folder.exists() && !folder.mkdirs()) {
            throw IllegalStateException("Local safety-backup folder could not be created.")
        }
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss_SSS", java.util.Locale.US)
            .format(java.util.Date())
        val destination = File(folder, "Ledger_Before_Restore_$stamp.db")
        val escapedPath = destination.absolutePath.replace("'", "''")
        database.execSQL("VACUUM INTO '$escapedPath'")
        if (!destination.exists() || destination.length() <= 0L) {
            destination.delete()
            throw IllegalStateException("Automatic local safety backup could not be created. Restore was cancelled.")
        }
    }

    /** Installs an already validated DB while retaining a rollback copy until the rename succeeds. */
    private fun installValidatedDatabase(validatedFile: File) {
        require(validatedFile.exists() && validatedFile.length() > 0L) { "Validated backup file is missing or empty." }
        // Safety copy must complete before the live database is closed or replaced.
        createLocalSafetyBackup()
        val databaseFile = getDatabasePath(LedgerDatabaseProvider.DATABASE_NAME)
        val parent = databaseFile.parentFile ?: throw IllegalStateException("Database directory is unavailable.")
        if (!parent.exists() && !parent.mkdirs()) throw IllegalStateException("Database directory could not be created.")
        val tempFile = File(parent, databaseFile.name + ".restore_tmp")
        val rollbackFile = File(parent, databaseFile.name + ".restore_rollback")
        val suffixes = listOf("-wal", "-shm", "-journal")
        val sidecarBackups = suffixes.associateWith { suffix -> File(parent, databaseFile.name + suffix + ".restore_rollback") }
        tempFile.delete()
        rollbackFile.delete()
        sidecarBackups.values.forEach { it.delete() }
        validatedFile.copyTo(tempFile, overwrite = true)
        if (tempFile.length() != validatedFile.length()) throw IllegalStateException("Temporary database copy is incomplete.")
        LedgerDatabaseProvider.close()
        var originalMoved = false
        try {
            if (databaseFile.exists()) {
                if (!databaseFile.renameTo(rollbackFile)) throw IllegalStateException("Existing database could not be secured for rollback.")
                originalMoved = true
            }
            suffixes.forEach { suffix ->
                val live = File(parent, databaseFile.name + suffix)
                val backupSidecar = sidecarBackups.getValue(suffix)
                if (live.exists() && !live.renameTo(backupSidecar)) throw IllegalStateException("Database sidecar could not be secured: $suffix")
            }
            if (!tempFile.renameTo(databaseFile)) throw IllegalStateException("Validated database could not be installed.")
            if (!databaseFile.exists() || databaseFile.length() != validatedFile.length()) throw IllegalStateException("Installed database verification failed.")
            rollbackFile.delete()
            sidecarBackups.values.forEach { it.delete() }
            validatedFile.delete()
        } catch (error: Exception) {
            databaseFile.delete()
            if (originalMoved && rollbackFile.exists() && !rollbackFile.renameTo(databaseFile)) {
                error.addSuppressed(IllegalStateException("Rollback database could not be restored; preserved at ${rollbackFile.absolutePath}"))
            }
            sidecarBackups.forEach { (suffix, backupSidecar) ->
                if (backupSidecar.exists()) backupSidecar.renameTo(File(parent, databaseFile.name + suffix))
            }
            tempFile.delete()
            throw error
        }
    }

    private fun replaceDatabaseAndRestart(
        onComplete: () -> Unit,
        remoteModifiedTime: String? = null
    ) {
        try {
            val validatedFile =
                googleDriveRestore.getValidatedBackupFile()

            installValidatedDatabase(validatedFile)

            markInitialDriveSetupComplete()

            if (remoteModifiedTime != null) {
                // Initial Google Drive restore: do not upload the restored backup again.
                googleDriveSyncCoordinator.markRemoteRestoreCompleted(
                    remoteModifiedTime
                )
                Toast.makeText(
                    this,
                    "Backup restored successfully.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                // Preserve existing local backup restore behaviour.
                googleDriveSyncCoordinator.resume()
                googleDriveSyncCoordinator.requestAutoSync()
                Toast.makeText(
                    this,
                    "Backup restored successfully. Sync requested.",
                    Toast.LENGTH_LONG
                ).show()
            }

            window.decorView.postDelayed(
                {
                    val launchIntent =
                        packageManager.getLaunchIntentForPackage(
                            packageName
                        )

                    finishAffinity()

                    if (launchIntent != null) {
                        launchIntent.addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                        )

                        startActivity(
                            launchIntent
                        )
                    }
                },
                500
            )
        } catch (error: Exception) {
            if (remoteModifiedTime != null) {
                googleDriveSyncCoordinator.onAuthorizationCancelled()
            } else {
                googleDriveSyncCoordinator.resume()
            }

            Toast.makeText(
                this,
                "Restore failed: ${
                    error.message ?: "Unable to replace database"
                }",
                Toast.LENGTH_LONG
            ).show()

            onComplete()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == GOOGLE_DRIVE_BACKUP_SAVE_REQUEST_CODE) {
            val source = pendingBackupDownloadFile
            pendingBackupDownloadFile = null

            if (resultCode == RESULT_OK && data?.data != null && source != null) {
                try {
                    val destination = data.data!!
                    val output = contentResolver.openOutputStream(destination)
                        ?: throw IllegalStateException("Unable to open the selected destination.")
                    output.use { stream ->
                        source.inputStream().use { input -> input.copyTo(stream) }
                    }
                    Toast.makeText(
                        this,
                        "Ledger backup saved successfully.",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (error: Exception) {
                    Toast.makeText(
                        this,
                        "Could not save backup: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                } finally {
                    source.delete()
                }
            } else {
                source?.delete()
                Toast.makeText(
                    this,
                    "Backup download cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (requestCode == GOOGLE_DRIVE_LOCAL_RESTORE_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data?.data != null) {
                restoreLocalBackupFromUri(data.data!!)
            } else {
                Toast.makeText(
                    this,
                    "Backup selection cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (requestCode == GOOGLE_DRIVE_AUTH_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    val authorizationResult =
                        googleDriveAuthorization.authorizationResultFromIntent(data)

                    if (authorizationResult.hasResolution()) {
                        val pendingIntent = authorizationResult.pendingIntent
                        if (pendingIntent != null) {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                GOOGLE_DRIVE_AUTH_REQUEST_CODE,
                                null,
                                0,
                                0,
                                0
                            )
                        } else {
                            pendingDriveEmailChange = null
                            Toast.makeText(
                                this,
                                "Google Drive authorization could not continue.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else if (pendingDriveRestoreOnlyConnection) {
                        restoreFromNewDriveAccount(authorizationResult.accessToken)
                    } else {
                        captureDriveAccountIdentity(authorizationResult.accessToken) {
                            googleDriveSyncCoordinator.resume()
                        }
                    }
                } catch (error: Exception) {
                    pendingDriveEmailChange = null
                    pendingDriveRestoreOnlyConnection = false
                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                    Toast.makeText(
                        this,
                        "Google Drive account verification failed.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                pendingDriveEmailChange = null
                pendingDriveRestoreOnlyConnection = false
                googleDriveSyncCoordinator.onAuthorizationCancelled()
                Toast.makeText(
                    this,
                    "Google Drive connection cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        if (requestCode == GOOGLE_DRIVE_SYNC_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                try {
                    val authorizationResult =
                        googleDriveAuthorization.authorizationResultFromIntent(data)

                    if (authorizationResult.hasResolution()) {
                        val pendingIntent = authorizationResult.pendingIntent
                        if (pendingIntent != null) {
                            startIntentSenderForResult(
                                pendingIntent.intentSender,
                                GOOGLE_DRIVE_SYNC_REQUEST_CODE,
                                null,
                                0,
                                0,
                                0
                            )
                        } else {
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(
                                this,
                                "Google Drive authorization could not continue.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        val accessToken = authorizationResult.accessToken
                        if (accessToken.isNullOrBlank()) {
                            googleDriveSyncCoordinator.onAuthorizationCancelled()
                            Toast.makeText(
                                this,
                                "Google Drive access token is unavailable.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            // Refresh the Drive identity and repeat the startup
                            // remote-backup check before allowing any upload.
                            captureDriveAccountIdentity(accessToken) {
                                googleDriveSyncCoordinator.resume()
                            }
                        }
                    }
                } catch (error: Exception) {
                    googleDriveSyncCoordinator.onAuthorizationCancelled()
                    Toast.makeText(
                        this,
                        "Google Drive authorization failed: ${error.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                googleDriveSyncCoordinator.onAuthorizationCancelled()
                Toast.makeText(
                    this,
                    "Google Drive authorization cancelled. Automatic backup will retry after the next data change.",
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        if (
            requestCode ==
            GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE
        ) {
            if (resultCode == RESULT_OK) {
                continueInitialGoogleDriveSetup(
                    onComplete = {
                        initialDriveSetupCompletion?.invoke()
                        initialDriveSetupCompletion = null
                    }
                )
            } else {
                initialDriveSetupCompletion?.invoke()
                initialDriveSetupCompletion = null
                Toast.makeText(
                    this,
                    "Google Drive connection is required.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        private const val GOOGLE_DRIVE_AUTH_REQUEST_CODE = 9001
        private const val GOOGLE_DRIVE_SYNC_REQUEST_CODE = 9002
        private const val GOOGLE_DRIVE_INITIAL_SETUP_REQUEST_CODE = 9003
        private const val GOOGLE_DRIVE_BACKUP_SAVE_REQUEST_CODE = 9004
        private const val GOOGLE_DRIVE_LOCAL_RESTORE_REQUEST_CODE = 9005
    }
}

@androidx.compose.runtime.Composable
private fun InitialGoogleDriveSetupScreen(
    isRunning: Boolean,
    onConnect: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .padding(paddingValues)
                    .padding(24.dp)
        ) {
            Spacer(
                modifier =
                    Modifier.height(80.dp)
            )

            Text(
                text = "Connect Google Drive",
                style =
                    MaterialTheme.typography.headlineMedium
            )

            Spacer(
                modifier =
                    Modifier.height(16.dp)
            )

            Text(
                text =
                    "Ledger uses Google Drive to protect your data. " +
                        "Connect Google Drive to continue."
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(
                text =
                    "If a Ledger backup is found, it will be restored automatically. " +
                        ""
            )

            Spacer(
                modifier =
                    Modifier.height(32.dp)
            )

            if (isRunning) {
                CircularProgressIndicator()

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                Text(
                    text =
                        "Connecting and checking your backup..."
                )
            } else {
                Button(
                    onClick = onConnect
                ) {
                    Text(
                        text = "Connect to Google Drive"
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun ProfileScreen(
    onBack: () -> Unit,
    onProfileSaved: () -> Unit,
    onChangeDriveEmail: ((String) -> Unit) -> Unit,
    onConnectDrive: () -> Unit,
    onDownloadBackup: () -> Unit,
    onCreateBackupCopy: () -> Unit,
    onRestoreBackup: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onClearData: () -> Unit,
    isPremium: Boolean,
    premiumPrice: String,
    onUpgradeClick: () -> Unit
) {
    val driveEmailContext = androidx.compose.ui.platform.LocalContext.current
    var driveEmail by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            driveEmailContext.getSharedPreferences(
                "ledger_drive_account_binding",
                android.content.Context.MODE_PRIVATE
            ).getString("drive_account_email", null)
        )
    }

var showLocalRestoreConfirmation by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    var showClearDataConfirmation by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var showTypeToClearConfirmation by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var clearDataConfirmationText by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("")
    }

    var showDriveAccountChangeConfirmation by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    var legalDocument by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    if (legalDocument != null) {
        LegalDocumentDialog(
            document = legalDocument!!,
            onDismiss = { legalDocument = null }
        )
    }

    if (showDriveAccountChangeConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showDriveAccountChangeConfirmation = false
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF171717),
            textContentColor = androidx.compose.ui.graphics.Color(0xFF424242),
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Outlined.Person,
                    contentDescription = null
                )
            },
            title = {
                androidx.compose.material3.Text("Change Google Drive account?")
            },
            text = {
                androidx.compose.material3.Text(
                    "If the selected Google account already contains a Ledger backup, " +
                        "you will be asked whether to restore that backup or keep and sync " +
                        "the data currently on this device. No backup will be restored or " +
                        "overwritten until you choose an option."
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDriveAccountChangeConfirmation = false
                        onChangeDriveEmail { updatedEmail ->
                            driveEmail = updatedEmail
                        }
                    }
                ) {
                    androidx.compose.material3.Text("Continue")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showDriveAccountChangeConfirmation = false
                    }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    if (showClearDataConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showClearDataConfirmation = false
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF171717),
            textContentColor = androidx.compose.ui.graphics.Color(0xFF424242),
            title = {
                androidx.compose.material3.Text("Clear All Data?")
            },
            text = {
                androidx.compose.material3.Text(
                    "WARNING: This action will permanently delete all data stored " +
                        "locally by Ledger on this device. This includes your parties, " +
                        "transactions, ledger entries, expenses, staff records, salary " +
                        "details, groups, settlements, activity history, local settings, " +
                        "and other locally stored app data.\n\n" +
                        "Any data that has not been backed up may be lost permanently. " +
                        "Please download or verify a backup before proceeding if you " +
                        "may need these records later.\n\n" +
                        "Your Ledger backup stored in Google Drive will also be permanently deleted. The app will delete its Ledger Backup.db files from its private Google Drive app storage before clearing local data. If Google Drive deletion fails, local data will be kept. " +
                        "You can use Restore Backup later to recover data from that backup. " +
                        "Clearing local data may also reset this device's app preferences " +
                        "and Google Drive connection settings.\n\n" +
                        "This action cannot be undone. Are you sure you want to continue?"
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showClearDataConfirmation = false
                        clearDataConfirmationText = ""
                        showTypeToClearConfirmation = true
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFB42318),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    androidx.compose.material3.Text("Clear Data")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showClearDataConfirmation = false
                    }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    if (showTypeToClearConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showTypeToClearConfirmation = false
                clearDataConfirmationText = ""
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF171717),
            textContentColor = androidx.compose.ui.graphics.Color(0xFF424242),
            title = {
                androidx.compose.material3.Text("Confirm Permanent Deletion")
            },
            text = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text(
                        "To permanently delete local Ledger data and its Google Drive backup, type CLEAR below. This cannot be undone."
                    )
                    androidx.compose.material3.OutlinedTextField(
                        value = clearDataConfirmationText,
                        onValueChange = { clearDataConfirmationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { androidx.compose.material3.Text("Type CLEAR to confirm") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showTypeToClearConfirmation = false
                        clearDataConfirmationText = ""
                        onClearData()
                    },
                    enabled = clearDataConfirmationText == "CLEAR",
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFB42318),
                        contentColor = androidx.compose.ui.graphics.Color.White
                    )
                ) {
                    androidx.compose.material3.Text("Clear Data")
                }
            },
            dismissButton = {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        showTypeToClearConfirmation = false
                        clearDataConfirmationText = ""
                    }
                ) {
                    androidx.compose.material3.Text("Cancel")
                }
            }
        )
    }

    if (showLocalRestoreConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {
                showLocalRestoreConfirmation = false
            },
            containerColor = androidx.compose.ui.graphics.Color.White,
            titleContentColor = androidx.compose.ui.graphics.Color(0xFF171717),
            textContentColor = androidx.compose.ui.graphics.Color(0xFF424242),
            title = {
                androidx.compose.material3.Text("Restore Ledger Backup?")
            },
            text = {
                androidx.compose.material3.Text(
                    "Choose where to restore from. Before replacing current data, Ledger will automatically save a safety copy on this device."
                )
            },
            confirmButton = {
                androidx.compose.foundation.layout.Column(
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    androidx.compose.material3.Button(
                        onClick = {
                            showLocalRestoreConfirmation = false
                            onRestoreBackup()
                        },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Restore from Local Device")
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showLocalRestoreConfirmation = false
                            onRestoreFromDrive()
                        },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Restore from Google Drive")
                    }
                    androidx.compose.material3.TextButton(
                        onClick = { showLocalRestoreConfirmation = false },
                        modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Cancel")
                    }
                }
            },
            dismissButton = {}
        )
    }

    BackHandler {
        onBack()
    }

    val context =
        androidx.compose.ui.platform.LocalContext.current

    val profileViewModel: com.ledger.app.ui.ProfileViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(
            factory =
                com.ledger.app.ui.ProfileViewModelFactory(
                    context.applicationContext
                        as android.app.Application
                )
        )

    val profileState by
        profileViewModel.uiState.collectAsState()

    val scope =
        androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    var isEditing by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf(false)
        }

    var name by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var mobile by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var editName by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    var editMobile by
        androidx.compose.runtime.remember {
            androidx.compose.runtime.mutableStateOf("")
        }

    androidx.compose.runtime.LaunchedEffect(
        profileState.name,
        profileState.mobile
    ) {
        name = profileState.name
        mobile = profileState.mobile

        if (!isEditing) {
            editName = profileState.name
            editMobile = profileState.mobile
        }
    }

    val initials =
        name.trim()
            .split(Regex("\\s+"))
            .filter {
                it.isNotBlank()
            }
            .take(2)
            .joinToString("") {
                it.first().uppercase()
            }
            .ifBlank {
                "U"
            }

    androidx.compose.material3.Scaffold(
        topBar = {
            androidx.compose.material3.Surface(
                tonalElevation = 3.dp
            ) {
                androidx.compose.foundation.layout.Row(
                    modifier =
                        androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(
                                horizontal = 16.dp,
                                vertical = 10.dp
                            ),
                    verticalAlignment =
                        androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.IconButton(
                        onClick = onBack
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector =
                                androidx.compose.material.icons.Icons.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }

                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Profile",
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        androidx.compose.material3.Text(
                            text = "Your personal details",
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .bodySmall
                        )
                    }
                }
            }
        }
    ) { paddingValues ->

        androidx.compose.foundation.lazy.LazyColumn(
            modifier =
                androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            verticalArrangement =
                androidx.compose.foundation.layout.Arrangement
                    .spacedBy(14.dp),
            contentPadding =
                androidx.compose.foundation.layout.PaddingValues(
                    top = 16.dp,
                    bottom = 24.dp
                )
        ) {

            item {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isPremium)
                            androidx.compose.ui.graphics.Color(0xFFFFC247)
                        else
                            androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                    ),
                    modifier =
                        androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape =
                        androidx.compose.material3.MaterialTheme
                            .shapes
                            .extraLarge,
                    elevation =
                        androidx.compose.material3.CardDefaults
                            .cardElevation(
                                defaultElevation = 2.dp
                            )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        horizontalAlignment =
                            androidx.compose.ui.Alignment.CenterHorizontally
                    ) {

                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                androidx.compose.foundation.layout.Arrangement.End
                        ) {
                            val profileIsPremium = isPremium

                            androidx.compose.material3.Surface(
                                color = if (profileIsPremium)
                                    androidx.compose.ui.graphics.Color(0xFFE8F5E9)
                                else
                                    androidx.compose.ui.graphics.Color(0xFFF1F3F5),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50)
                            ) {
                                androidx.compose.material3.Text(
                                    text = if (profileIsPremium) "Premium" else "Free User",
                                    modifier = androidx.compose.ui.Modifier.padding(
                                        horizontal = 12.dp,
                                        vertical = 6.dp
                                    ),
                                    color = if (profileIsPremium)
                                        androidx.compose.ui.graphics.Color(0xFF1B5E20)
                                    else
                                        androidx.compose.ui.graphics.Color(0xFF495057),
                                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            }
                        }


                        androidx.compose.foundation.layout.Box(
                            modifier =
                                androidx.compose.ui.Modifier
                                    .size(88.dp)
                                    .clip(
                                        androidx.compose.foundation
                                            .shape
                                            .CircleShape
                                    )
                                    .background(
                                        androidx.compose.material3.MaterialTheme
                                            .colorScheme
                                            .primaryContainer
                                    ),
                            contentAlignment =
                                androidx.compose.ui.Alignment.Center
                        ) {
                            androidx.compose.material3.Text(
                                text = initials,
                                style =
                                    androidx.compose.material3.MaterialTheme
                                        .typography
                                        .headlineMedium,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                color =
                                    androidx.compose.material3.MaterialTheme
                                        .colorScheme
                                        .onPrimaryContainer
                            )
                        }

                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(14.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                name.ifBlank {
                                    "Your Name"
                                },
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .titleLarge,
                            fontWeight =
                                androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        androidx.compose.foundation.layout.Spacer(
                            modifier =
                                androidx.compose.ui.Modifier.height(4.dp)
                        )

                        androidx.compose.material3.Text(
                            text =
                                mobile.ifBlank {
                                    "Add your mobile number"
                                },
                            style =
                                androidx.compose.material3.MaterialTheme
                                    .typography
                                    .bodyMedium
                        )
                    }
                }
            }

            item {
                androidx.compose.material3.Card(
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                    ),
                    modifier =
                        androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape =
                        androidx.compose.material3.MaterialTheme
                            .shapes
                            .large
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier =
                            androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                        verticalArrangement =
                            androidx.compose.foundation.layout.Arrangement
                                .spacedBy(14.dp)
                    ) {

                        androidx.compose.foundation.layout.Row(
                            modifier =
                                androidx.compose.ui.Modifier.fillMaxWidth(),
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier =
                                    androidx.compose.ui.Modifier.weight(1f)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Personal Details",
                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .titleMedium,
                                    fontWeight =
                                        androidx.compose.ui.text.font.FontWeight
                                            .Bold
                                )

                                androidx.compose.material3.Text(
                                    text =
                                        "Manage your name and mobile number",
                                    style =
                                        androidx.compose.material3.MaterialTheme
                                            .typography
                                            .bodySmall
                                )
                            }

                            if (!isEditing) {
                                androidx.compose.material3.TextButton(
                                    onClick = {
                                        editName = name
                                        editMobile = mobile
                                        isEditing = true
                                    }
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Edit",
                                        fontWeight =
                                            androidx.compose.ui.text.font.FontWeight
                                                .Bold
                                    )
                                }
                            }
                        }

                        if (isEditing) {

                            androidx.compose.material3.OutlinedTextField(
                                value = editName,
                                onValueChange = {
                                    editName = it
                                },
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                label = {
                                    androidx.compose.material3.Text(
                                        "Name"
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            androidx.compose.material.icons.Icons
                                                .Outlined
                                                .Person,
                                        contentDescription = null
                                    )
                                }
                            )

                            androidx.compose.material3.OutlinedTextField(
                                value = editMobile,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone),

                                onValueChange = {
                                    editMobile = it
                                },
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                label = {
                                    androidx.compose.material3.Text(
                                        "Mobile Number"
                                    )
                                },
                                singleLine = true,
                                leadingIcon = {
                                    androidx.compose.material3.Icon(
                                        imageVector =
                                            androidx.compose.material.icons.Icons
                                                .Outlined
                                                .Phone,
                                        contentDescription = null
                                    )
                                }
                            )

                            androidx.compose.foundation.layout.Row(
                                modifier =
                                    androidx.compose.ui.Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    androidx.compose.foundation.layout.Arrangement
                                        .spacedBy(10.dp)
                            ) {

                                androidx.compose.material3.OutlinedButton(
                                    onClick = {
                                        editName = name
                                        editMobile = mobile
                                        isEditing = false
                                    },
                                    modifier =
                                        androidx.compose.ui.Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        "Cancel"
                                    )
                                }

                                androidx.compose.material3.Button(
                                    onClick = {
                                        val cleanName =
                                            editName.trim()

                                        val cleanMobile =
                                            editMobile.trim()

                                        if (cleanName.isNotBlank()) {
                                            scope.launch {
                                                val saved =
                                                    profileViewModel.saveProfile(
                                                        name = cleanName,
                                                        mobile = cleanMobile
                                                    )

                                                if (saved) {
                                                    name = cleanName
                                                    mobile = cleanMobile
                                                    isEditing = false
                                            onProfileSaved()
                                                }
                                            }
                                        }
                                    },
                                    modifier =
                                        androidx.compose.ui.Modifier.weight(1f)
                                ) {
                                    androidx.compose.material3.Text(
                                        text = "Save Changes",
                                        fontWeight =
                                            androidx.compose.ui.text.font.FontWeight
                                                .Bold
                                    )
                                }
                            }

                        } else {

                                                        ProfileDetailRow(
                                icon =
                                    androidx.compose.material.icons.Icons
                                        .Outlined
                                        .Phone,
                                label = "Mobile Number",
                                value =
                                    mobile.ifBlank {
                                        "Not added"
                                    }
                            )
                        }
                    }
                }
            }


            item {
                com.ledger.app.cloudgroup.CloudAccountCard()
            }

            item {
                androidx.compose.material3.Card(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                    )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement =
                            androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
                    ) {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment =
                                androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement =
                                androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
                        ) {
                            androidx.compose.material3.Surface(
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                color = androidx.compose.ui.graphics.Color(0xFFE8EAF6)
                            ) {
                                androidx.compose.material3.Icon(
                                    imageVector =
                                        androidx.compose.material.icons.Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color(0xFF3949AB),
                                    modifier = androidx.compose.ui.Modifier.padding(12.dp)
                                )
                            }

                            androidx.compose.foundation.layout.Column(
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                verticalArrangement =
                                    androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                            ) {
                                androidx.compose.material3.Text(
                                    text = "Google Drive Backup",
                                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                                androidx.compose.material3.Text(
                                    text = "Account used for backup and sync",
                                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        androidx.compose.material3.HorizontalDivider(
                            color = androidx.compose.ui.graphics.Color(0xFFEAEAEA)
                        )

                        androidx.compose.foundation.layout.Column(
                            verticalArrangement =
                                androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "Connected Google Account",
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.material3.Text(
                                text = driveEmail?.takeIf { it.isNotBlank() }
                                    ?: "Not connected",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }

                        androidx.compose.material3.Button(
                            onClick = {
                                if (driveEmail.isNullOrBlank()) {
                                    onConnectDrive()
                                } else {
                                    showDriveAccountChangeConfirmation = true
                                }
                            },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        ) {
                            androidx.compose.material.icons.Icons.Outlined.Person.let { icon ->
                                androidx.compose.material3.Icon(
                                    imageVector = icon,
                                    contentDescription = null
                                )
                            }
                            androidx.compose.foundation.layout.Spacer(
                                modifier = androidx.compose.ui.Modifier.width(8.dp)
                            )
                            androidx.compose.material3.Text(
                                text = if (driveEmail.isNullOrBlank()) {
                                    "Connect Google Account"
                                } else {
                                    "Change Google Account"
                                },
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                            )
                        }

                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onDownloadBackup,
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                            ) {
                                androidx.compose.material3.Text("Download Backup")
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showLocalRestoreConfirmation = true },
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                            ) {
                                androidx.compose.material3.Text("Restore Backup")
                            }
                        }

                        androidx.compose.foundation.layout.Row(
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = onCreateBackupCopy,
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                            ) {
                                androidx.compose.material3.Text("Create Copy")
                            }
                            androidx.compose.material3.OutlinedButton(
                                onClick = { showClearDataConfirmation = true },
                                modifier = androidx.compose.ui.Modifier.weight(1f),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                    contentColor = androidx.compose.ui.graphics.Color(0xFFB42318)
                                )
                            ) {
                                androidx.compose.material3.Text("Clear Data")
                            }
                        }

                    }
                }
            }

                    item {
                androidx.compose.material3.Card(
                    modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                    shape = androidx.compose.material3.MaterialTheme.shapes.large,
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, androidx.compose.ui.graphics.Color(0xFFE0E0E0)
                    )
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = "Legal & Support",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        androidx.compose.material3.OutlinedButton(
                            onClick = { legalDocument = "privacy" },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Text("Privacy Policy")
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { legalDocument = "terms" },
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.material3.Text("Terms & Conditions")
                        }
                    }
                }
            }

    if (!isPremium) {
                item {
                    androidx.compose.material3.Card(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            androidx.compose.ui.graphics.Color(0xFFE7D7AE)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = androidx.compose.ui.graphics.Color(0xFFFFFBF1)
                        )
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = androidx.compose.ui.Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = "Ledger Premium Yearly",
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            androidx.compose.material3.Text(
                                text = "Get unlimited parties, expenses, groups and members with a yearly Premium subscription.",
                                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                            )
                            androidx.compose.material3.Button(
                                onClick = onUpgradeClick,
                                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFFFFC247),
                                    contentColor = androidx.compose.ui.graphics.Color(0xFF302000)
                                )
                            ) {
                                androidx.compose.material3.Text(
                                    text = if (premiumPrice.isBlank())
                                        "Subscribe to Premium"
                                    else
                                        "Subscribe yearly · $premiumPrice/year"
                                )
                            }
                        }
                    }
                }
            }
}
    }
}


@androidx.compose.runtime.Composable
private fun ProfileDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    androidx.compose.foundation.layout.Row(
        modifier =
            androidx.compose.ui.Modifier.fillMaxWidth(),
        verticalAlignment =
            androidx.compose.ui.Alignment.CenterVertically
    ) {
        androidx.compose.material3.Surface(
            shape =
                androidx.compose.foundation.shape.CircleShape,
            color =
                androidx.compose.material3.MaterialTheme
                    .colorScheme
                    .secondaryContainer,
            modifier =
                androidx.compose.ui.Modifier.size(42.dp)
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment =
                    androidx.compose.ui.Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(
            modifier =
                androidx.compose.ui.Modifier.width(14.dp)
        )

        androidx.compose.foundation.layout.Column(
            modifier =
                androidx.compose.ui.Modifier.weight(1f)
        ) {
            androidx.compose.material3.Text(
                text = label,
                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .labelMedium
            )

            androidx.compose.material3.Text(
                text = value,
                style =
                    androidx.compose.material3.MaterialTheme
                        .typography
                        .bodyLarge,
                fontWeight =
                    androidx.compose.ui.text.font.FontWeight.Medium
            )
        }
    }
}



@androidx.compose.runtime.Composable
private fun LegalDocumentDialog(
    document: String,
    onDismiss: () -> Unit
) {
    val isPrivacy = document == "privacy"
    val title = if (isPrivacy) "Privacy Policy" else "Terms & Conditions"

    val sections = if (isPrivacy) listOf(
        "1. Overview" to "Align Ledger is a personal bookkeeping application for managing party ledgers, transactions, daily expenses and groups. This policy explains how app data is handled.",
        "2. Data stored on your device" to "Ledger records you create, such as party details, transaction entries, expenses, notes and profile information, may be stored locally on your device. Anyone with access to your unlocked device or its app data may be able to access this information.",
        "3. Google Drive backup and sync" to "If you choose to connect Google Drive and use backup or sync features, the app may access Google Drive for the purpose of backing up or restoring your Ledger data. Google account authentication and Google's own privacy terms apply. You can choose not to use these online features; local app functionality may still be available.",
        "4. Purchases" to "If you purchase Premium, Google Play processes the payment. The app may check purchase status to enable Premium features. Payment card details are handled by Google Play, not by the app itself.",
        "5. Sharing and disclosure" to "The app is designed to use your records for its bookkeeping and backup features. Do not enter information you do not have permission to store. Data saved to a Google account or shared device may be accessible according to that account's settings and permissions.",
        "6. Retention and deletion" to "Local records generally remain on your device until you delete them, clear app data or uninstall the app. Backup copies may remain in Google Drive until you delete them there. Keep your own backup of important records.",
        "7. Security and responsibility" to "Use a secure device and protect your Google account. No storage or transmission method can be guaranteed completely secure. The app cannot protect data exposed through a compromised device or account.",
        "8. Children's privacy" to "Ledger is a bookkeeping tool and is not specifically designed for children. Do not provide personal information unless you are authorised to do so.",
        "9. Policy changes and contact" to "This policy may be updated in future app versions. The version included in the installed app can be read offline. For questions or privacy requests, use the developer contact details shown on the app's Google Play listing."
    ) else listOf(
        "1. Acceptance" to "By using Align Ledger, you agree to these Terms & Conditions. If you do not agree, discontinue use of the app.",
        "2. Intended use" to "Ledger is a bookkeeping and record-keeping tool. You are responsible for the accuracy, completeness and lawful use of all information entered into the app.",
        "3. Your records" to "Review entries carefully and retain independent copies of important records. The app is not a substitute for professional accounting, tax, legal or financial advice, and its calculations or reports should be independently checked when relied upon for important decisions.",
        "4. Data, backups and device changes" to "You are responsible for safeguarding your device and arranging backups. Local data may be lost if the app is uninstalled, app data is cleared, the device is damaged or access is lost. Google Drive backup or restore requires a compatible account, connectivity and the necessary permissions; availability is not guaranteed at all times.",
        "5. Premium purchases" to "Premium features are subject to the product and purchase terms displayed by Google Play at the time of purchase. Payment, refunds and purchase disputes are handled under applicable Google Play policies and law. Premium access may require verification of the purchase.",
        "6. Acceptable use" to "Do not misuse the app, attempt unauthorised access, interfere with its operation or use it in violation of applicable law or the rights of others.",
        "7. Availability and limitations" to "The app is provided as a record-keeping aid. To the extent permitted by applicable law, the developer does not guarantee uninterrupted operation, error-free calculations, permanent data availability or successful recovery of every backup. Nothing in these terms excludes rights that cannot legally be excluded.",
        "8. Changes and termination" to "Features and these terms may change in future versions. You may stop using the app at any time. Keep copies of records you need before uninstalling or clearing app data.",
        "9. Governing law" to "These terms are subject to applicable laws of India, without limiting any mandatory rights available to consumers under applicable law.",
        "10. Contact" to "For support or questions, use the developer contact details provided on the app's Google Play listing."
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            androidx.compose.material3.Text(
                text = title,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        },
        text = {
            androidx.compose.foundation.layout.Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(androidx.compose.foundation.rememberScrollState()),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                androidx.compose.material3.Text(
                    text = "Align Ledger • Offline document",
                    style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.primary
                )
                sections.forEach { (heading, body) ->
                    androidx.compose.foundation.layout.Column(
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                    ) {
                        androidx.compose.material3.Text(
                            text = heading,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            style = androidx.compose.material3.MaterialTheme.typography.titleSmall
                        )
                        androidx.compose.material3.Text(
                            text = body,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                androidx.compose.material3.Text(
                    text = "Please review these documents carefully. They are general app terms and are not a substitute for legal advice.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                androidx.compose.material3.Text("Close")
            }
        }
    )
}
