package com.ledger.app.ui

import androidx.compose.ui.text.font.FontWeight
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
import com.ledger.app.cloudgroup.CloudGroupMember
import com.ledger.app.cloudgroup.CloudGroupExpenseData
import com.ledger.app.cloudgroup.CloudGroupRepository
import com.ledger.app.cloudgroup.CloudSupabaseRemoteDataSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnlineGroupDetailScreen(
    groupId: String,
    isPremium: Boolean,
    onBack: () -> Unit
) {
    val repository = remember {
        CloudGroupRepository(CloudSupabaseRemoteDataSource())
    }
    val scope = rememberCoroutineScope()

    var group by remember(groupId) { mutableStateOf<CloudGroup?>(null) }
    var members by remember(groupId) { mutableStateOf<List<CloudGroupMember>>(emptyList()) }
    var currentUserId by remember(groupId) { mutableStateOf<String?>(null) }
    var isLoading by remember(groupId) { mutableStateOf(true) }
    var isBusy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<CloudGroupMember?>(null) }
    var removingMember by remember { mutableStateOf<CloudGroupMember?>(null) }
    var memberNameInput by remember { mutableStateOf("") }
    var memberEmailInput by remember { mutableStateOf("") }

    var selectedGroupSection by remember(groupId) {
        mutableStateOf("Members")
    }
    var addExpenseRequest by remember { mutableStateOf(0) }
    var selectedMemberDetail by remember { mutableStateOf<CloudGroupMember?>(null) }
    var memberExpenseData by remember {
        mutableStateOf(CloudGroupExpenseData(emptyList(), emptyList(), emptyList()))
    }
    var memberExpenseLoading by remember { mutableStateOf(false) }
    var memberExpenseError by remember { mutableStateOf<String?>(null) }


    val isOwner = group != null &&
        currentUserId != null &&
        group!!.ownerId == currentUserId

    suspend fun refresh() {
        isLoading = true
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
            members = result.second
            currentUserId = result.third

            if (group == null) {
                errorMessage = "This group could not be found or you do not have access."
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            errorMessage = error.message ?: "Could not load Online Group."
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

    LaunchedEffect(groupId) {
        refresh()
    }

    LaunchedEffect(groupId, selectedGroupSection) {
        if (selectedGroupSection == "Members") {
            memberExpenseLoading = true
            memberExpenseError = null
            try {
                memberExpenseData = withContext(Dispatchers.IO) {
                    repository.loadGroupExpenses(groupId)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                memberExpenseError = error.message ?: "Could not load member expense details."
            } finally {
                memberExpenseLoading = false
            }
        }
    }


    BackHandler(onBack = onBack)

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
                                        if (isOwner && !isBusy) {
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
                                        "Members (${members.size})"
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
                                Text(
                                    text = group!!.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${members.size} members",
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
                        text = errorMessage ?: "Group unavailable.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { scope.launch { refresh() } }) {
                        Text("Retry")
                    }
                }

                else -> {
                    if (selectedGroupSection == "Expenses") {
                        OnlineGroupExpensesSection(
                            groupId = groupId,
                            members = members,
                            currentUserId = currentUserId,
                            repository = repository,
                            enabled = !isBusy,
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
                                            memberExpenseData.expenses.sumOf { it.amount }
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (memberExpenseLoading) {
                                        Spacer(Modifier.height(6.dp))
                                        CircularProgressIndicator()
                                    }
                                    memberExpenseError?.let {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            it,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Text(
                                    "${memberExpenseData.expenses.size} entries",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (members.isEmpty()) {
                            Text(
                                text = "No members found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(members, key = { it.id }) { member ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
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
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { selectedMemberDetail = member }
                                                ) {
                                                    Text(
                                                        text = member.memberName,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = member.memberEmail,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    val memberShare = memberExpenseData.splits
                                                        .filter { it.memberId == member.id }
                                                        .sumOf { it.amount }
                                                    val memberPaid = memberExpenseData.payments
                                                        .filter { it.memberId == member.id }
                                                        .sumOf { it.amount }
                                                    val memberBalance = memberShare - memberPaid
                                                    Text(
                                                        text = when {
                                                            member.status != "joined" -> "Invitation pending"
                                                            kotlin.math.abs(memberBalance) < 0.01 -> "Settled"
                                                            memberBalance > 0 -> "Remaining ₹" + String.format(
                                                                java.util.Locale.getDefault(), "%,.2f", memberBalance
                                                            )
                                                            else -> "Excess paid ₹" + String.format(
                                                                java.util.Locale.getDefault(), "%,.2f", -memberBalance
                                                            )
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = when {
                                                            member.status != "joined" -> MaterialTheme.colorScheme.onSurfaceVariant
                                                            memberBalance > 0.009 -> MaterialTheme.colorScheme.error
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                                }
                                                Text(
                                                    text = member.status.replaceFirstChar { it.uppercase() },
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (member.status == "joined")
                                                        MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.error
                                                )
                                            }

                                            if (member.role == "owner") {
                                                Text(
                                                    text = "Group Owner",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
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
                val memberShare = memberExpenseData.splits
                    .filter { it.memberId == detailMember.id }
                    .sumOf { it.amount }
                val memberPaid = memberExpenseData.payments
                    .filter { it.memberId == detailMember.id }
                    .sumOf { it.amount }
                val memberBalance = memberShare - memberPaid
                val memberExpenseIds = (
                    memberExpenseData.splits.filter { it.memberId == detailMember.id }.map { it.expenseId } +
                    memberExpenseData.payments.filter { it.memberId == detailMember.id }.map { it.expenseId }
                ).toSet()
                val detailExpenses = memberExpenseData.expenses
                    .filter { it.id in memberExpenseIds }
                    .sortedByDescending { it.expenseDateMs }

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
                        Column(Modifier.weight(1f)) {
                            Text(
                                detailMember.memberName,
                                modifier = Modifier.clickable(
                                    enabled = isOwner && detailMember.role == "member" && !isBusy
                                ) {
                                    memberNameInput = detailMember.memberName
                                    editingMember = detailMember
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isOwner && detailMember.role == "member")
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                detailMember.memberEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (detailMember.role == "owner") {
                            Text(
                                "Owner",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
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

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Remaining Balance",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    when {
                                        kotlin.math.abs(memberBalance) < 0.01 -> "Settled"
                                        memberBalance > 0 -> "Owes"
                                        else -> "Excess paid"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                "₹" + String.format(
                                    java.util.Locale.getDefault(), "%,.2f",
                                    kotlin.math.abs(memberBalance)
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    kotlin.math.abs(memberBalance) < 0.01 -> MaterialTheme.colorScheme.primary
                                    memberBalance > 0 -> MaterialTheme.colorScheme.error
                                    else -> Color(0xFF16804A)
                                }
                            )
                        }
                    }

                    Text(
                        "Expense History",
                        modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    if (detailExpenses.isEmpty()) {
                        Box(
                            Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No expenses recorded for this member.",
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
                            items(detailExpenses, key = { it.id }) { expense ->
                                val split = memberExpenseData.splits
                                    .firstOrNull {
                                        it.expenseId == expense.id &&
                                            it.memberId == detailMember.id
                                    }
                                val paid = memberExpenseData.payments
                                    .filter {
                                        it.expenseId == expense.id &&
                                            it.memberId == detailMember.id
                                    }
                                    .sumOf { it.amount }

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

    editingMember?.let { member ->
        AlertDialog(
            onDismissRequest = {
                if (!isBusy) editingMember = null
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Edit Member", color = Color(0xFF202124)) },
            text = {
                OutlinedTextField(
                    value = memberNameInput,
                    onValueChange = { memberNameInput = it },
                    label = { Text("Member Name") },
                    singleLine = true,
                    enabled = !isBusy,
                    shape = RoundedCornerShape(14.dp)
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
                            editingMember = null
                            removingMember = member
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }

                    androidx.compose.material3.OutlinedButton(
                        enabled = !isBusy && memberNameInput.isNotBlank(),
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
            onDismissRequest = {
                if (!isBusy) removingMember = null
            },
            title = { Text("Remove Member?") },
            text = {
                Text(
                    "Remove ${member.memberName} from this group? " +
                        "Historical records may prevent removal."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        removingMember = null
                        runMemberAction {
                            repository.removeMember(groupId, member.id)
                        }
                    }
                ) {
                    Text(
                        "Remove",
                        color = MaterialTheme.colorScheme.error
                    )
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
