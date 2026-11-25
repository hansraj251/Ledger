package com.ledger.app.ui

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledger.app.cloudgroup.CloudGroup
import com.ledger.app.cloudgroup.CloudGroupCache
import com.ledger.app.cloudgroup.CloudGroupBalanceCalculator
import com.ledger.app.cloudgroup.CloudGroupMemberBalance
import com.ledger.app.cloudgroup.CloudGroupMember
import com.ledger.app.cloudgroup.CloudGroupPairwiseBalanceCalculator
import com.ledger.app.cloudgroup.CloudGroupPairwiseDebt
import com.ledger.app.cloudgroup.CloudGroupExpenseData
import com.ledger.app.cloudgroup.CloudGroupRepository
import com.ledger.app.cloudgroup.CloudSupabaseRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@Composable
fun OnlineGroupDetailScreen(
    groupId: String,
    isPremium: Boolean,
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val cloudGroupCache = remember(context) {
        CloudGroupCache(context.applicationContext)
    }
    val cachedGroupDetail = remember(groupId, cloudGroupCache) {
        cloudGroupCache.loadDetail(groupId)
    }

    val repository = remember {
        CloudGroupRepository(CloudSupabaseRemoteDataSource())
    }
    val scope = rememberCoroutineScope()

    var group by remember(groupId) {
        mutableStateOf(cachedGroupDetail?.group)
    }
    var members by remember(groupId) {
        mutableStateOf(cachedGroupDetail?.members.orEmpty())
    }
    // Keep archived members available for historical calculations; hide them only in the active Members UI.
    val activeMembers = members.filter { it.status != "removed" }
    var currentUserId by remember(groupId) {
        mutableStateOf(cachedGroupDetail?.currentUserId)
    }
    var isLoading by remember(groupId) {
        mutableStateOf(cachedGroupDetail == null)
    }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isOffline by remember(groupId) { mutableStateOf(false) }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditGroupDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var groupNameInput by remember { mutableStateOf("") }
    var deleteBalanceLoading by remember { mutableStateOf(false) }
    var deleteBalanceError by remember { mutableStateOf<String?>(null) }
    var deleteBalances by remember(groupId) {
        mutableStateOf<Map<String, CloudGroupMemberBalance>>(emptyMap())
    }
    var editingMember by remember { mutableStateOf<CloudGroupMember?>(null) }
    var removingMember by remember { mutableStateOf<CloudGroupMember?>(null) }
    var memberNameInput by remember { mutableStateOf("") }
    var memberEmailInput by remember { mutableStateOf("") }

    var memberRemovalBalanceLoading by remember { mutableStateOf(false) }
    var memberRemovalBalanceError by remember { mutableStateOf<String?>(null) }
    var memberRemovalBalances by remember(groupId) {
        mutableStateOf<Map<String, CloudGroupMemberBalance>>(emptyMap())
    }

    var selectedGroupSection by remember(groupId) {
        mutableStateOf("Members")
    }
    var addExpenseRequest by remember { mutableStateOf(0) }
    var selectedMemberDetail by remember { mutableStateOf<CloudGroupMember?>(null) }
    var showSettlementPermissionDialog by remember { mutableStateOf(false) }
    var pendingSettlementPermission by remember { mutableStateOf<Boolean?>(null) }
    // all = normal history, member = selected member paid,
    // others = another member paid in a settlement involving this member.
    var settlementHistoryFilter by remember(selectedMemberDetail?.id) {
        mutableStateOf("all")
    }
    var memberExpenseData by remember(groupId) {
        mutableStateOf(
            cachedGroupDetail?.expenseData
                ?: CloudGroupExpenseData(emptyList(), emptyList(), emptyList())
        )
    }
    var memberExpenseDataLoaded by remember(groupId) {
        mutableStateOf(cachedGroupDetail?.expenseData != null)
    }
    var memberExpenseLoading by remember { mutableStateOf(false) }
    var memberExpenseError by remember { mutableStateOf<String?>(null) }
    var showBalanceBreakdown by remember { mutableStateOf(false) }
    var balanceBreakdownLoading by remember { mutableStateOf(false) }
    var balanceBreakdownError by remember { mutableStateOf<String?>(null) }
    var balanceBreakdownDebts by remember { mutableStateOf<List<CloudGroupPairwiseDebt>>(emptyList()) }
    var balanceBreakdownUnallocated by remember { mutableStateOf<Set<String>>(emptySet()) }


    val isOwner = group != null &&
        currentUserId != null &&
        group!!.ownerId == currentUserId

    suspend fun refresh() {
        if (group == null) {
            isLoading = true
        }
        errorMessage = null
        try {
            val result: Triple<CloudGroup?, List<CloudGroupMember>, String?> =
                withContext(Dispatchers.IO) {
                    val loadedGroup = repository.loadGroups()
                        .firstOrNull { it.id == groupId }
                    val loadedMembers = repository.loadMembers(groupId)
                    val loadedUserId = repository.currentUserId()

                    Triple(loadedGroup, loadedMembers, loadedUserId)
                }
            group = result.first
            isOffline = false
            members = result.second

            // Keep the open Member Detail screen in sync with refreshed data.
            selectedMemberDetail = selectedMemberDetail?.let { selected ->
                result.second.firstOrNull { it.id == selected.id }
            }

            currentUserId = result.third

            cloudGroupCache.saveDetail(
                groupId = groupId,
                group = result.first,
                members = result.second,
                currentUserId = result.third,
                expenseData = if (memberExpenseDataLoaded) memberExpenseData else null
            )

            if (group == null) {
                errorMessage = "This group could not be found or you do not have access."
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            isOffline = error.isConnectivityError()
            errorMessage = if (isOffline) null
                else "Unable to refresh this group. Please try again."
        } finally {
            isLoading = false
        }
    }

    fun runMemberAction(action: suspend () -> Unit) {
        scope.launch {
            isBusy = true
            errorMessage = null
            try {
                withContext(Dispatchers.IO) {
                    action()
                }
                refresh()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                errorMessage = error.message ?: "Operation failed."
            } finally {
                isBusy = false
            }
        }
    }

    fun loadDeleteBalances() {
        scope.launch {
            deleteBalanceLoading = true
            deleteBalanceError = null
            deleteBalances = emptyMap()
            try {
                val result = withContext(Dispatchers.IO) {
                    val data = repository.loadGroupExpenses(groupId)
                    val balances = CloudGroupBalanceCalculator.calculate(
                        members = members,
                        splits = data.splits,
                        payments = data.payments,
                        settlements = data.settlements
                    )
                    data to balances
                }
                memberExpenseData = result.first
                memberExpenseDataLoaded = true
                cloudGroupCache.saveDetail(
                    groupId = groupId,
                    group = group,
                    members = members,
                    currentUserId = currentUserId,
                    expenseData = result.first
                )
                deleteBalances = result.second
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                deleteBalanceError =
                    error.message ?: "Could not verify group balances."
            } finally {
                deleteBalanceLoading = false
            }
        }
    }

    fun deleteGroupNow() {
        scope.launch {
            isBusy = true
            deleteBalanceError = null
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteGroup(groupId)
                }
                showDeleteGroupDialog = false
                onBack()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                deleteBalanceError =
                    error.message ?: "Could not delete this group."
            } finally {
                isBusy = false
            }
        }
    }

    LaunchedEffect(groupId) {
        refresh()
    }

    LaunchedEffect(groupId, selectedGroupSection) {
        if (selectedGroupSection == "Members") {
            memberExpenseLoading = !memberExpenseDataLoaded
            memberExpenseError = null
            try {
                val loadedData = withContext(Dispatchers.IO) {
                    repository.loadGroupExpenses(groupId)
                }
                memberExpenseData = loadedData
                memberExpenseDataLoaded = true

                cloudGroupCache.saveDetail(
                    groupId = groupId,
                    group = group,
                    members = members,
                    currentUserId = currentUserId,
                    expenseData = loadedData
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                isOffline = error.isConnectivityError()
                memberExpenseError = if (isOffline) null
                    else "Unable to load member expense details."
            } finally {
                memberExpenseLoading = false
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep Total Expenses current while Members remains open.
    LaunchedEffect(groupId, selectedGroupSection) {
        if (selectedGroupSection != "Members") return@LaunchedEffect

        while (true) {
            delay(15_000)
            try {
                val loadedData = withContext(Dispatchers.IO) {
                    repository.loadGroupExpenses(groupId)
                }
                memberExpenseData = loadedData
                memberExpenseDataLoaded = true
                memberExpenseError = null
                cloudGroupCache.saveDetail(
                    groupId = groupId,
                    group = group,
                    members = members,
                    currentUserId = currentUserId,
                    expenseData = loadedData
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                isOffline = error.isConnectivityError()
                memberExpenseError = if (isOffline) null
                    else "Unable to refresh member expense details."
            }
        }
    }

    // Refresh immediately when the app returns to foreground.
    DisposableEffect(lifecycleOwner, groupId, selectedGroupSection) {
        if (selectedGroupSection != "Members") {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    scope.launch {
                        try {
                            val loadedData = withContext(Dispatchers.IO) {
                                repository.loadGroupExpenses(groupId)
                            }
                            memberExpenseData = loadedData
                            memberExpenseDataLoaded = true
                            memberExpenseError = null
                            cloudGroupCache.saveDetail(
                                groupId = groupId,
                                group = group,
                                members = members,
                                currentUserId = currentUserId,
                                expenseData = loadedData
                            )
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            isOffline = error.isConnectivityError()
                            memberExpenseError = if (isOffline) null
                                else "Unable to refresh member expense details."
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }


    LaunchedEffect(groupId, editingMember?.id, members) {
        val targetMember = editingMember

        if (targetMember == null) {
            memberRemovalBalances = emptyMap()
            memberRemovalBalanceError = null
            memberRemovalBalanceLoading = false
        } else {
            memberRemovalBalanceLoading = true
            memberRemovalBalanceError = null
            memberRemovalBalances = emptyMap()

            try {
                memberRemovalBalances = withContext(Dispatchers.IO) {
                    val data = repository.loadGroupExpenses(groupId)
                    CloudGroupBalanceCalculator.calculate(
                        members = members,
                        splits = data.splits,
                        payments = data.payments,
                        settlements = data.settlements
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                memberRemovalBalanceError =
                    error.message ?: "Could not verify member balance."
            } finally {
                memberRemovalBalanceLoading = false
            }
        }
    }

    BackHandler(enabled = showBalanceBreakdown) { showBalanceBreakdown = false }
    BackHandler(enabled = !showBalanceBreakdown, onBack = onBack)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isBusy && members.any { it.status == "joined" }) {
                        selectedGroupSection = "Expenses"
                        addExpenseRequest++
                    }
                },
                shape = RoundedCornerShape(18.dp),
                containerColor = if (isPremium) Color(0xFFFFC247)
                    else MaterialTheme.colorScheme.primary,
                contentColor = if (isPremium) Color(0xFF302000)
                    else MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.ReceiptLong,
                    contentDescription = "Add Expense"
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Expenses", "Members").forEach { section ->
                        val selected = selectedGroupSection == section

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(17.dp))
                                .clickable {
                                    if (section == "Members" && selected) {
                                        if (currentUserId != null && !isBusy) {
                                            memberNameInput = ""
                                            memberEmailInput = ""
                                            showAddDialog = true
                                        }
                                    } else {
                                        selectedGroupSection = section
                                    }
                                },
                            shape = RoundedCornerShape(17.dp),
                            border = if (!selected) {
                                if (isPremium) BorderStroke(1.dp, Color(0xFFFFC247))
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            } else null,
                            color = if (selected) {
                                if (isPremium) Color(0xFFFFC247)
                                else MaterialTheme.colorScheme.primary
                            } else if (isPremium) Color.White else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = 8.dp,
                                    vertical = 12.dp
                                ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (section == "Expenses")
                                        Icons.Outlined.ReceiptLong
                                    else Icons.Outlined.GroupAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                    tint = if (isPremium) Color(0xFF302000)
                                    else if (selected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (section == "Members" && selected)
                                        "Add Member"
                                    else if (section == "Members")
                                        "Members (${activeMembers.size})"
                                    else "Expenses",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPremium) Color(0xFF302000)
                                    else if (selected)
                                        MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            if (group != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Box(modifier = Modifier.statusBarsPadding()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                            val groupInitials = group!!.name.trim()
                                .split(" ").filter { it.isNotBlank() }.take(2)
                                .joinToString("") { it.first().uppercase() }
                            Surface(
                                modifier = Modifier.size(44.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = groupInitials.ifBlank { "G" },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                Text(
                                    text = group!!.name,
                                    modifier = Modifier.clickable(enabled = !isBusy) {
                                        if (isOwner) {
                                            groupNameInput = group!!.name
                                            showEditGroupDialog = true
                                        } else {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Only owner can edit or delete",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${activeMembers.size} members",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
            when {
                isLoading -> {
                    CircularProgressIndicator()
                    Text("Loading group...")
                }

                group == null -> {
                    Text(
                        text = if (isOffline) "No Internet Connection"
                            else errorMessage ?: "Group unavailable.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { refresh() } }) {
                        Text("Retry")
                    }
                }

                else -> {
                    if (isOffline) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = Color(0xFFFFF3E0)
                            )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        tint = Color(0xFFB45309)
                                    )
                                    Text(
                                        "No Internet Connection",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Text(
                                    "Please check your internet connection. " +
                                        "Your saved group data is still available on this device.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF92400E)
                                )
                                TextButton(
                                    onClick = {
                                        scope.launch {
                                            refresh()
                                            try {
                                                val loadedData = withContext(Dispatchers.IO) {
                                                    repository.loadGroupExpenses(groupId)
                                                }
                                                memberExpenseData = loadedData
                                                memberExpenseDataLoaded = true
                                                memberExpenseError = null
                                                cloudGroupCache.saveDetail(
                                                    groupId = groupId,
                                                    group = group,
                                                    members = members,
                                                    currentUserId = currentUserId,
                                                    expenseData = loadedData
                                                )
                                                isOffline = false
                                            } catch (error: CancellationException) {
                                                throw error
                                            } catch (error: Exception) {
                                                isOffline = error.isConnectivityError()
                                                if (!isOffline) {
                                                    memberExpenseError =
                                                        "Unable to refresh group expenses. Please try again."
                                                }
                                            }
                                        }
                                    }
                                ) {
                                    Text("Retry")
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    if (selectedGroupSection == "Expenses") {
                        OnlineGroupExpensesSection(
                            groupId = groupId,
                            members = members,
                            currentUserId = currentUserId,
                            repository = repository,
                            enabled = !isBusy,
                            expenseData = memberExpenseData,
                            expenseDataLoaded = memberExpenseDataLoaded,
                            onExpenseDataChange = { loadedData ->
                                memberExpenseData = loadedData
                                memberExpenseDataLoaded = true
                                memberExpenseError = null
                                isOffline = false
                                cloudGroupCache.saveDetail(
                                    groupId = groupId,
                                    group = group,
                                    members = members,
                                    currentUserId = currentUserId,
                                    expenseData = loadedData
                                )
                            },
                            onConnectivityError = {
                                isOffline = true
                            },
                            onAddExpenseRequestConsumed = {
                                addExpenseRequest = 0
                            },
                            addExpenseRequest = addExpenseRequest
                        )
                    }

                    if (selectedGroupSection == "Members") {
                        if (errorMessage != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (isBusy) {
                            Spacer(Modifier.height(8.dp))
                            CircularProgressIndicator()
                        }

                        Spacer(Modifier.height(8.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = Color.White
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.padding(12.dp).size(26.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Total Expenses",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "₹" + String.format(
                                            java.util.Locale.getDefault(),
                                            "%,.2f",
                                            memberExpenseData.expenses.filterNot { it.title.equals("Settled", ignoreCase = true) }.sumOf { it.amount }
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (memberExpenseLoading) {
                                        Spacer(Modifier.height(6.dp))
                                        CircularProgressIndicator()
                                    }
                                    memberExpenseError?.takeUnless { isOffline }?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            it,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Text(
                                    "${memberExpenseData.expenses.count { !it.title.equals("Settled", ignoreCase = true) }} entries",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (activeMembers.isEmpty()) {
                            Text(
                                text = "No members found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeMembers, key = { it.id }) { member ->
                                    val isCurrentUserMember =
                                        currentUserId != null && member.userId == currentUserId

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        border = if (isCurrentUserMember) {
                                            BorderStroke(1.dp, Color(0xFFE8B64A))
                                        } else null,
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
    modifier = Modifier
        .size(44.dp)
        .background(
            MaterialTheme.colorScheme.primaryContainer,
            androidx.compose.foundation.shape.CircleShape
        ),
    contentAlignment = Alignment.Center
) {
    Text(
        text = member.memberName.trim().take(1).uppercase().ifBlank { "M" },
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.SemiBold
    )
}
Spacer(Modifier.width(12.dp))
Column(
    modifier = Modifier
        .weight(1f)
        .clickable { selectedMemberDetail = member }
) {
    val memberShare = memberExpenseData.splits
        .filter { it.memberId == member.id }
        .sumOf { it.amount }
    val memberPaid = memberExpenseData.payments
        .filter { it.memberId == member.id }
        .sumOf { it.amount }
    val memberBalance = memberShare - memberPaid

    val balanceLabel = when {
        member.status != "joined" -> ""
        kotlin.math.abs(memberBalance) < 0.01 -> "Settled"
        memberBalance > 0 -> "Will Give"
        else -> "Will Get"
    }

    val balanceColor = when {
        member.status != "joined" -> MaterialTheme.colorScheme.onSurfaceVariant
        kotlin.math.abs(memberBalance) < 0.01 -> MaterialTheme.colorScheme.primary
        memberBalance > 0 -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
        else -> androidx.compose.ui.graphics.Color(0xFFC62828)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = member.memberName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        if (balanceLabel.isNotBlank()) {
            Text(
                text = balanceLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = balanceColor
            )
        }
    }

    Spacer(Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = member.memberEmail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = if (member.status != "joined") {
                "Invitation pending"
            } else {
                "₹" + String.format(
                    java.util.Locale.getDefault(),
                    "%,.2f",
                    kotlin.math.abs(memberBalance)
                )
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = balanceColor
        )
    }
}
                                                if (member.status == "pending") {
                                                Text(
                                                    text = "Pending",
                                                    style = MaterialTheme.typography.labelMedium,
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
            }
            }
        }
    }

    selectedMemberDetail?.let { detailMember ->
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { selectedMemberDetail = null },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                val nonSettlementExpenseIds = memberExpenseData.expenses
                    .filterNot { it.title.trim().equals("Settled", ignoreCase = true) }
                    .map { it.id }
                    .toSet()

                val memberShare = memberExpenseData.splits
                    .filter {
                        it.memberId == detailMember.id &&
                            it.expenseId in nonSettlementExpenseIds
                    }
                    .sumOf { it.amount }
                val memberPaid = memberExpenseData.payments
                    .filter {
                        it.memberId == detailMember.id &&
                            it.expenseId in nonSettlementExpenseIds
                    }
                    .sumOf { it.amount }
                val memberSettlementExpenseIds = memberExpenseData.expenses
                    .filter { expense ->
                        expense.title.trim().equals("Settled", ignoreCase = true) &&
                            (
                                memberExpenseData.splits.any {
                                    it.expenseId == expense.id &&
                                        it.memberId == detailMember.id
                                } ||
                                memberExpenseData.payments.any {
                                    it.expenseId == expense.id &&
                                        it.memberId == detailMember.id
                                }
                            )
                    }
                    .map { it.id }
                    .toSet()

                val memberSettled = memberExpenseData.payments
                    .filter {
                        it.memberId == detailMember.id &&
                            it.expenseId in memberSettlementExpenseIds
                    }
                    .sumOf { it.amount }

                val memberSettledByOthers = memberExpenseData.payments
                    .filter {
                        it.memberId != detailMember.id &&
                            it.expenseId in memberSettlementExpenseIds
                    }
                    .sumOf { it.amount }
                val memberPairwiseResult = remember(
                    members,
                    memberExpenseData.expenses,
                    memberExpenseData.splits,
                    memberExpenseData.payments,
                    detailMember.id
                ) {
                    runCatching {
                        CloudGroupPairwiseBalanceCalculator.calculate(
                            members = members,
                            expenses = memberExpenseData.expenses,
                            splits = memberExpenseData.splits,
                            payments = memberExpenseData.payments
                        )
                    }.getOrNull()
                }

                val memberGetTotalPaise = memberPairwiseResult?.debts
                    ?.filter { it.toMemberId == detailMember.id }
                    ?.sumOf { it.amountPaise } ?: 0L

                val memberGiveTotalPaise = memberPairwiseResult?.debts
                    ?.filter { it.fromMemberId == detailMember.id }
                    ?.sumOf { it.amountPaise } ?: 0L

                val memberBalance = if (memberPairwiseResult != null) {
                    (memberGiveTotalPaise - memberGetTotalPaise) / 100.0
                } else {
                    memberShare - memberPaid
                }
                val memberExpenseIds = (
                    memberExpenseData.splits
                        .filter { it.memberId == detailMember.id }
                        .map { it.expenseId } +
                    memberExpenseData.payments
                        .filter { it.memberId == detailMember.id }
                        .map { it.expenseId }
                ).toSet()

                // A settlement can involve this member even when another member
                // is the only payer recorded on that settlement transaction.


                val detailExpenses = memberExpenseData.expenses
                    .filter {
                        it.id in memberExpenseIds ||
                            it.id in memberSettlementExpenseIds
                    }
                    .sortedByDescending { it.expenseDateMs }

                val visibleDetailExpenses = when (settlementHistoryFilter) {
                    "member" -> detailExpenses.filter { expense ->
                        expense.id in memberSettlementExpenseIds &&
                            memberExpenseData.payments.any {
                                it.expenseId == expense.id &&
                                    it.memberId == detailMember.id
                            }
                    }

                    "others" -> detailExpenses.filter { expense ->
                        expense.id in memberSettlementExpenseIds &&
                            memberExpenseData.payments.any {
                                it.expenseId == expense.id &&
                                    it.memberId != detailMember.id
                            }
                    }

                    else -> detailExpenses
                }

                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedMemberDetail = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    detailMember.memberName.trim().take(1).uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        val context = androidx.compose.ui.platform.LocalContext.current
                        val canEditDetailMemberName =
                            (isOwner && detailMember.role == "member") ||
                            (
                                currentUserId != null &&
                                detailMember.userId == currentUserId &&
                                detailMember.role in listOf("owner", "member")
                            )

                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clickable(enabled = !isBusy) {
                                    if (canEditDetailMemberName) {
                                        memberNameInput = detailMember.memberName
                                        editingMember = detailMember
                                    } else if (!isOwner) {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Member can edit only own name",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                        ) {
                            Text(
                                detailMember.memberName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (canEditDetailMemberName)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                detailMember.memberEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        val canChangeSettlementPermission =
                            currentUserId != null &&
                                detailMember.userId == currentUserId &&
                                detailMember.status == "joined"

                        if (canChangeSettlementPermission) {
                            Switch(
                                checked = detailMember.allowSettlementByOthers,
                                onCheckedChange = { allow ->
                                    pendingSettlementPermission = allow
                                    showSettlementPermissionDialog = true
                                },
                                enabled = !isBusy
                            )
                        }
                    }
                    HorizontalDivider()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple("Share", memberShare, Color(0xFFE53935)),
                            Triple("Paid", memberPaid, Color(0xFF16804A))
                        ).forEach { (label, amount, tint) ->
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFFE8B64A)),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = Color.White
                                )
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF77500A)
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "₹" + String.format(
                                            java.util.Locale.getDefault(), "%,.2f", amount
                                        ),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = tint,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                showBalanceBreakdown = true
                                balanceBreakdownLoading = true
                                balanceBreakdownError = null
                                balanceBreakdownDebts = emptyList()
                                balanceBreakdownUnallocated = emptySet()
                                scope.launch {
                                    try {
                                        val result = withContext(Dispatchers.IO) {
                                            CloudGroupPairwiseBalanceCalculator.calculate(
                                                members = members,
                                                expenses = memberExpenseData.expenses,
                                                splits = memberExpenseData.splits,
                                                payments = memberExpenseData.payments
                                            )
                                        }
                                        balanceBreakdownDebts = result.debts
                                        balanceBreakdownUnallocated = result.unallocatedExpenseIds
                                    } catch (error: CancellationException) {
                                        throw error
                                    } catch (error: Exception) {
                                        balanceBreakdownError = error.message
                                            ?: "Could not load balance breakdown."
                                    } finally {
                                        balanceBreakdownLoading = false
                                    }
                                }
                            },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            when {
                                memberBalance > 0.01 -> Color(0xFFB7DDBD)
                                memberBalance < -0.01 -> Color(0xFFE7B8B8)
                                else -> Color(0xFFD8DCE3)
                            }
                        ),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Remaining Balance",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    when {
                                        kotlin.math.abs(memberBalance) < 0.01 ->
                                            "${detailMember.memberName} Settled"
                                        memberBalance > 0 ->
                                            "${detailMember.memberName} Will Give"
                                        else ->
                                            "${detailMember.memberName} Will Get"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when {
                                        kotlin.math.abs(memberBalance) < 0.01 ->
                                            MaterialTheme.colorScheme.primary
                                        memberBalance > 0 -> Color(0xFF2E7D32)
                                        else -> Color(0xFFC62828)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            Text(
                                "₹" + String.format(
                                    java.util.Locale.getDefault(), "%,.2f",
                                    kotlin.math.abs(memberBalance)
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    kotlin.math.abs(memberBalance) < 0.01 -> MaterialTheme.colorScheme.primary
                                    memberBalance > 0 -> Color(0xFF2E7D32)
                                    else -> Color(0xFFC62828)
                                },
                                maxLines = 1
                            )
                            }

                        }
                    }
                    }


                    if (visibleDetailExpenses.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                when (settlementHistoryFilter) {
                                    "member" -> "No settlements paid by ${detailMember.memberName} found."
                                    "others" -> "No settlements paid by other members found."
                                    else -> "No expenses recorded for this member."
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(visibleDetailExpenses, key = { it.id }) { expense ->
                                val isSettlement = expense.title.trim()
                                    .equals("Settled", ignoreCase = true)

                                val split = if (isSettlement) {
                                    null
                                } else {
                                    memberExpenseData.splits.firstOrNull {
                                        it.expenseId == expense.id &&
                                            it.memberId == detailMember.id
                                    }
                                }

                                val paid = if (isSettlement) {
                                    0.0
                                } else {
                                    memberExpenseData.payments
                                        .filter {
                                            it.expenseId == expense.id &&
                                                it.memberId == detailMember.id
                                        }
                                        .sumOf { it.amount }
                                }

                                val settlementPayments = if (isSettlement) {
                                    memberExpenseData.payments.filter {
                                        it.expenseId == expense.id
                                    }
                                } else {
                                    emptyList()
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFE8B64A)),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = Color.White
                                    )
                                ) {
                                    Column(Modifier.padding(14.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    expense.title,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    java.text.SimpleDateFormat(
                                                        "dd MMM yyyy",
                                                        java.util.Locale.getDefault()
                                                    ).format(java.util.Date(expense.expenseDateMs)),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                "₹" + String.format(
                                                    java.util.Locale.getDefault(), "%,.2f", expense.amount
                                                ),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (isSettlement) {
                                            settlementPayments
                                                .groupBy { it.memberId }
                                                .forEach { (payerId, records) ->
                                                    val payerName = members
                                                        .firstOrNull { it.id == payerId }
                                                        ?.memberName ?: "Member"
                                                    val payerAmount = records.sumOf { it.amount }

                                                    if (payerAmount > 0.009) {
                                                        Spacer(Modifier.height(6.dp))
                                                        Row {
                                                            Text(
                                                                payerName,
                                                                color = Color(0xFF16804A)
                                                            )
                                                            Spacer(Modifier.weight(1f))
                                                            Text(
                                                                "₹" + String.format(
                                                                    java.util.Locale.getDefault(),
                                                                    "%,.2f",
                                                                    payerAmount
                                                                ),
                                                                color = Color(0xFF16804A),
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                        } else {
                                            split?.let {
                                                Spacer(Modifier.height(8.dp))
                                                Row(horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Member Share", color = Color(0xFFE53935))
                                                    Spacer(Modifier.weight(1f))
                                                    Text(
                                                        "₹" + String.format(
                                                            java.util.Locale.getDefault(), "%,.2f", it.amount
                                                        ),
                                                        color = Color(0xFFE53935),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }

                                            if (paid > 0.009) {
                                                Spacer(Modifier.height(6.dp))
                                                Row {
                                                    Text("Paid", color = Color(0xFF16804A))
                                                    Spacer(Modifier.weight(1f))
                                                    Text(
                                                        "₹" + String.format(
                                                            java.util.Locale.getDefault(), "%,.2f", paid
                                                        ),
                                                        color = Color(0xFF16804A),
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                        if (expense.note.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                expense.note,
                                                style = MaterialTheme.typography.bodySmall
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
    }


    if (showBalanceBreakdown && selectedMemberDetail != null) {
        val detailMember = selectedMemberDetail!!
        val getDebts = balanceBreakdownDebts.filter { it.toMemberId == detailMember.id }
        val giveDebts = balanceBreakdownDebts.filter { it.fromMemberId == detailMember.id }
        val memberNameById = members.associate { it.id to it.memberName }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showBalanceBreakdown = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showBalanceBreakdown = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Remaining Balance",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                detailMember.memberName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()

                    when {
                        balanceBreakdownLoading -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                        balanceBreakdownError != null -> Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                balanceBreakdownError ?: "Could not load balance breakdown.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        else -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (balanceBreakdownUnallocated.isNotEmpty()) {
                                item {
                                    Text(
                                        "Some expenses could not be included because their split/payment details do not reconcile.",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }

                            item {
                                Text(
                                    "You will get",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }

                            if (getDebts.isEmpty()) {
                                item {
                                    Text(
                                        "No members currently owe ${detailMember.memberName}.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(getDebts, key = { "get-${it.fromMemberId}-${it.toMemberId}" }) { debt ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                memberNameById[debt.fromMemberId] ?: "Unknown member",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "₹" + String.format(
                                                    java.util.Locale.getDefault(), "%,.2f",
                                                    debt.amountPaise / 100.0
                                                ),
                                                color = Color(0xFFC62828),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "You will give",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }

                            if (giveDebts.isEmpty()) {
                                item {
                                    Text(
                                        "${detailMember.memberName} does not currently owe any member.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(giveDebts, key = { "give-${it.fromMemberId}-${it.toMemberId}" }) { debt ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = androidx.compose.material3.CardDefaults.cardColors(
                                            containerColor = Color.White
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                memberNameById[debt.toMemberId] ?: "Unknown member",
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                "₹" + String.format(
                                                    java.util.Locale.getDefault(), "%,.2f",
                                                    debt.amountPaise / 100.0
                                                ),
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
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
    }

    if (showEditGroupDialog && isOwner) {
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) showEditGroupDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Edit Group",
                    color = Color(0xFF202124),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                OutlinedTextField(
                    value = groupNameInput,
                    onValueChange = { groupNameInput = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedTextColor = Color(0xFF202124),
                        unfocusedTextColor = Color(0xFF202124)
                    )
                )
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.OutlinedButton(
                        enabled = !isBusy,
                        onClick = {
                            showEditGroupDialog = false
                            deleteBalanceError = null
                            showDeleteGroupDialog = true
                            loadDeleteBalances()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }

                    androidx.compose.material3.OutlinedButton(
                        enabled = !isBusy && groupNameInput.isNotBlank(),
                        onClick = {
                            showEditGroupDialog = false
                            runMemberAction {
                                repository.updateGroupName(groupId, groupNameInput)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { showEditGroupDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteGroupDialog && isOwner) {
        val joinedMembers = members.filter { it.status == "joined" }
        val unsettledMembers = joinedMembers.filter { member ->
            (deleteBalances[member.id]?.remainingBalancePaise ?: 0L) != 0L
        }
        val canDelete =
            !deleteBalanceLoading &&
            deleteBalanceError == null &&
            unsettledMembers.isEmpty()

        AlertDialog(
            onDismissRequest = {
                if (!isBusy) showDeleteGroupDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Delete Group?",
                    color = Color(0xFF202124),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "A group can only be deleted when every joined member's " +
                            "remaining balance is exactly ₹0.00.",
                        color = Color(0xFF424242)
                    )

                    when {
                        deleteBalanceLoading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Text("Checking latest settlements and balances...")
                            }
                        }

                        deleteBalanceError != null -> {
                            Text(
                                deleteBalanceError ?: "Balance verification failed.",
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(
                                enabled = !isBusy,
                                onClick = { loadDeleteBalances() }
                            ) {
                                Text("Retry balance check")
                            }
                        }

                        unsettledMembers.isNotEmpty() -> {
                            Text(
                                "Deletion blocked. These members have outstanding balances:",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                            unsettledMembers.forEach { member ->
                                val paise =
                                    deleteBalances[member.id]?.remainingBalancePaise ?: 0L
                                Text(
                                    "${member.memberName}: ₹" +
                                        String.format(
                                            java.util.Locale.US,
                                            "%.2f",
                                            paise / 100.0
                                        ),
                                    color = Color(0xFF424242)
                                )
                            }
                        }

                        else -> {
                            Text(
                                "All ${joinedMembers.size} joined members have zero balance.",
                                color = Color(0xFF187A45),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Deleting this group will permanently remove its " +
                                    "members, expenses, payments and settlements.",
                                color = Color(0xFF424242)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy && canDelete,
                    onClick = { deleteGroupNow() }
                ) {
                    Text(
                        if (isBusy) "Deleting..." else "Delete permanently",
                        color = if (canDelete) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { showDeleteGroupDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) showAddDialog = false
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = {
                Text(
                    "Add Group Member",
                    color = Color(0xFF202124),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = memberNameInput,
                        onValueChange = { memberNameInput = it },
                        label = { Text("Member Name") },
                        singleLine = true,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color(0xFF202124),
                            unfocusedTextColor = Color(0xFF202124)
                        )
                    )
                    OutlinedTextField(
                        value = memberEmailInput,
                        onValueChange = { memberEmailInput = it },
                        label = { Text("Gmail / Email") },
                        singleLine = true,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            disabledContainerColor = Color.White,
                            focusedTextColor = Color(0xFF202124),
                            unfocusedTextColor = Color(0xFF202124)
                        )
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showAddDialog = false },
                        enabled = !isBusy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFB8B8B8))
                    ) {
                        Text("Cancel", color = Color(0xFF424242))
                    }

                    androidx.compose.material3.OutlinedButton(
                        enabled = !isBusy &&
                            memberNameInput.isNotBlank() &&
                            memberEmailInput.isNotBlank(),
                        onClick = {
                            showAddDialog = false
                            runMemberAction {
                                repository.addMember(
                                    groupId,
                                    memberNameInput,
                                    memberEmailInput
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Add")
                    }
                }
            },
            dismissButton = {}
        )
    }

    if (showSettlementPermissionDialog) {
        val member = selectedMemberDetail
        val requestedPermission = pendingSettlementPermission
        if (member != null && requestedPermission != null) {
            AlertDialog(
                containerColor = Color.White,
                onDismissRequest = {
                    if (!isBusy) {
                        showSettlementPermissionDialog = false
                        pendingSettlementPermission = null
                    }
                },
                title = {
                    Text(
                        if (requestedPermission) "Allow settlements?"
                        else "Disable settlements?"
                    )
                },
                text = {
                    Text(
                        if (requestedPermission) {
                            "Other group members will be able to select you as the settlement receiver."
                        } else {
                            "Other group members will no longer be able to select you as the settlement receiver. You can still pay and settle with other members."
                        }
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = !isBusy,
                        onClick = {
                            val memberId = member.id
                            val allow = requestedPermission
                            showSettlementPermissionDialog = false
                            pendingSettlementPermission = null
                            runMemberAction {
                                val updated = repository.updateOwnSettlementPermission(
                                    groupId = groupId,
                                    memberId = memberId,
                                    allow = allow
                                )
                                if (!updated) {
                                    throw IllegalStateException(
                                        "Could not update settlement permission. Please retry."
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    TextButton(
                        enabled = !isBusy,
                        onClick = {
                            showSettlementPermissionDialog = false
                            pendingSettlementPermission = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }

    editingMember?.let { member ->
        val canEditMemberName =
            (isOwner && member.role == "member") ||
            (
                currentUserId != null &&
                member.userId == currentUserId &&
                member.role in listOf("owner", "member")
            )

        val canShowRemoveButton =
            isOwner &&
            member.role == "member" &&
            member.userId != currentUserId

        AlertDialog(
            onDismissRequest = {
                if (!isBusy) editingMember = null
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Edit Member", color = Color(0xFF202124)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = memberNameInput,
                        onValueChange = { memberNameInput = it },
                        label = { Text("Member Name") },
                        singleLine = true,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp)
                    )

                    val removalBalance = memberRemovalBalances[member.id]

                    if (member.status != "pending") {
                        when {
                            memberRemovalBalanceLoading -> {
                            Text("Checking latest member balance...")
                        }
                        memberRemovalBalanceError != null -> {
                            Text(
                                "Cannot verify balance. Remove is disabled.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        removalBalance == null -> {
                            Text(
                                "Balance unavailable. Member cannot be removed.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        removalBalance.remainingBalancePaise != 0L -> {
                            Text(
                                "Unsettled balance: ₹" + String.format(
                                    java.util.Locale.US,
                                    "%.2f",
                                    removalBalance.remainingBalancePaise / 100.0
                                ) + ". Settle all dues before removing this member.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        else -> {
                            Text(
                                "Balance settled. Member can be removed.",
                                color = Color(0xFF187A45)
                            )
                        }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val removalBalance = memberRemovalBalances[member.id]
                    val canRemoveMember =
                        !isBusy && (
                            member.status == "pending" ||
                            (
                                !memberRemovalBalanceLoading &&
                                memberRemovalBalanceError == null &&
                                removalBalance != null &&
                                removalBalance.remainingBalancePaise == 0L
                            )
                        )

                    if (canShowRemoveButton) {
                        androidx.compose.material3.OutlinedButton(
                            enabled = canRemoveMember,
                            onClick = {
                                editingMember = null
                                removingMember = member
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hide", color = MaterialTheme.colorScheme.error)
                        }
                    }

                    androidx.compose.material3.OutlinedButton(
                        enabled = canEditMemberName && !isBusy && memberNameInput.isNotBlank(),
                        onClick = {
                            editingMember = null
                            runMemberAction {
                                repository.updateMemberName(
                                    groupId,
                                    member.id,
                                    memberNameInput
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { editingMember = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    removingMember?.let { member ->
        AlertDialog(
            containerColor = Color.White,
            onDismissRequest = {
                if (!isBusy) removingMember = null
            },
            title = { Text("Remove Member?") },
            text = {
                Text(
                    "Hide ${member.memberName} from the active members list? " +
                        "Previous expenses and settlements will remain in group history."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        removingMember = null
                        runMemberAction {
                            val latestMembers = repository.loadMembers(groupId)
                            val latestData = repository.loadGroupExpenses(groupId)
                            val latestMember = latestMembers.firstOrNull {
                                it.id == member.id
                            } ?: throw IllegalStateException(
                                "Member not found. Please refresh and retry."
                            )

                            if (latestMember.status != "pending") {
                                val latestBalances = CloudGroupBalanceCalculator.calculate(
                                    members = latestMembers,
                                    splits = latestData.splits,
                                    payments = latestData.payments,
                                    settlements = latestData.settlements
                                )
                                val latestBalance = latestBalances[member.id]
                                    ?: throw IllegalStateException(
                                        "Could not verify this member's balance. Please retry."
                                    )

                                if (latestBalance.remainingBalancePaise != 0L) {
                                    throw IllegalStateException(
                                        "Member cannot be removed. Please settle the outstanding balance first."
                                    )
                                }
                            }

                            repository.removeMember(groupId, member.id)
                        }
                    }
                ) {
                    Text("Hide", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = { removingMember = null }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}


private fun Throwable.isConnectivityError(): Boolean =
    generateSequence(this) { it.cause }.any { error ->
        error is java.net.UnknownHostException ||
            error is java.net.ConnectException ||
            error is java.net.NoRouteToHostException ||
            error is java.net.SocketException ||
            error.message.orEmpty().contains("unable to resolve host", true) ||
            error.message.orEmpty().contains("no address associated with hostname", true) ||
            error.message.orEmpty().contains("network is unreachable", true)
    }
