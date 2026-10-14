package com.ledger.app.ui

import com.ledger.app.data.GroupExpensePaymentEntity

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import com.ledger.app.data.CurrencySettings
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.window.Dialog

import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.ledger.app.data.GroupExpenseDao
import com.ledger.app.data.GroupExpenseEntity
import com.ledger.app.data.GroupExpenseSplitDao
import com.ledger.app.data.GroupExpenseSplitEntity
import com.ledger.app.data.GroupSettlementEntity
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.SharingGroupDao
import com.ledger.app.data.SharingGroupEntity
import com.ledger.app.data.SharingMemberDao
import com.ledger.app.data.SharingMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.Check
import androidx.compose.foundation.layout.navigationBarsPadding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Calendar
import java.util.TimeZone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.heightIn

private data class GroupSharingData(
    val groups: List<SharingGroupEntity>,
    val members: List<SharingMemberEntity>,
    val expenses: List<GroupExpenseEntity>,
    val splits: Map<Long, List<GroupExpenseSplitEntity>>,
    val payments: Map<Long, List<GroupExpensePaymentEntity>>
)

private data class PaymentDraft(
    val memberId: Long,
    val memberName: String,
    val amount: Double
)

private data class SplitDraft(
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val shareUnits: Double
)

private fun memberExpenseTotal(
    memberId: Long,
    splits: Map<Long, List<GroupExpenseSplitEntity>>
): Double = splits.values.asSequence()
    .flatten()
    .filter { it.memberId == memberId }
    .sumOf { it.amount }

private fun memberReceivedTotal(
    memberId: Long,
    settlements: List<GroupSettlementEntity>
): Double = settlements.asSequence()
    .filter { it.memberId == memberId }
    .sumOf { it.amount }

private fun memberRemainingBalance(
    memberId: Long,
    splits: Map<Long, List<GroupExpenseSplitEntity>>,
    settlements: List<GroupSettlementEntity>,
    payments: Map<Long, List<GroupExpensePaymentEntity>> = emptyMap()
): Double {
    val paid = payments.values.asSequence().flatten()
        .filter { it.memberId == memberId }.sumOf { it.amount }
    return memberExpenseTotal(memberId, splits) -
        memberReceivedTotal(memberId, settlements) - paid
}

private const val FREE_GROUP_LIMIT = 4
private const val FREE_GROUP_EXTRA_MEMBER_LIMIT = 4

@Composable
fun GroupSharingScreen(
    onBack: () -> Unit,
    initialGroupId: Long? = null,
    onPartiesClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onDataChanged: () -> Unit,
    isPremium: Boolean = false,
    onUpgradeClick: () -> Unit = {},
    onGroupDeleted: () -> Unit = {},
    onGroupReportClick: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember(context) { LedgerDatabaseProvider.get(context) }
    val groupDao = remember(database) { database.sharingGroupDao() }
    val memberDao = remember(database) { database.sharingMemberDao() }
    val expenseDao = remember(database) { database.groupExpenseDao() }
    val splitDao = remember(database) { database.groupExpenseSplitDao() }
    val settlementDao = remember(database) { database.groupSettlementDao() }
    val paymentDao = remember(database) { database.groupExpensePaymentDao() }
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf(emptyList<SharingGroupEntity>()) }
    var members by remember { mutableStateOf(emptyList<SharingMemberEntity>()) }
    var expenses by remember { mutableStateOf(emptyList<GroupExpenseEntity>()) }
    var splits by remember { mutableStateOf(emptyMap<Long, List<GroupExpenseSplitEntity>>()) }
    var paymentsByExpense by remember { mutableStateOf(emptyMap<Long, List<GroupExpensePaymentEntity>>()) }
    var settlements by remember { mutableStateOf(emptyList<GroupSettlementEntity>()) }
    var settlingMember by remember { mutableStateOf<SharingMemberEntity?>(null) }




    var showSettlementMemberPicker by remember { mutableStateOf(false) }
    var activeGroupId by remember(initialGroupId) {
        mutableStateOf(initialGroupId)
    }
    var selectedGroupSection by remember(activeGroupId) {
        mutableStateOf("Expense")
    }
    var refresh by remember { mutableStateOf(0) }

    LaunchedEffect(refresh, activeGroupId) {
        val groupId = activeGroupId
        settlements = withContext(Dispatchers.IO) {
            if (groupId == null) emptyList()
            else settlementDao.getByGroup(groupId)
        }
    }

    var groupSearch by remember { mutableStateOf("") }
    var showHeaderMenu by remember { mutableStateOf(false) }

    var showGroupEditor by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<SharingGroupEntity?>(null) }
    var groupName by remember { mutableStateOf("") }
    var deletingGroup by remember { mutableStateOf<SharingGroupEntity?>(null) }

    var showMemberEditor by remember { mutableStateOf(false) }
    var defaultShareMember by remember { mutableStateOf<SharingMemberEntity?>(null) }
    var defaultShareText by remember { mutableStateOf("1") }
    var editingMember by remember { mutableStateOf<SharingMemberEntity?>(null) }
    var memberName by remember { mutableStateOf("") }
    var deletingMember by remember { mutableStateOf<SharingMemberEntity?>(null) }
    var groupDeleteBlocked by remember { mutableStateOf(false) }
    var showFreeLimitDialog by remember { mutableStateOf(false) }
    var freeLimitMessage by remember { mutableStateOf("") }

    var showExpenseEditor by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<GroupExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<GroupExpenseEntity?>(null) }

    LaunchedEffect(refresh, activeGroupId) {
        val loaded = withContext(Dispatchers.IO) {
            val allGroups = groupDao.getAll()

            // Every group must always contain exactly one reserved "You" member.
            allGroups.forEach { group ->
                val groupMembers = memberDao.getByGroup(group.id)
                val youMembers = groupMembers.filter {
                    it.name.equals("You", ignoreCase = true)
                }

                if (youMembers.isEmpty()) {
                    memberDao.insert(
                        SharingMemberEntity(groupId = group.id, name = "You")
                    )
                } else {
                    // Normalize the reserved member's display name.
                    val primaryYou = youMembers.first()
                    if (primaryYou.name != "You") {
                        memberDao.update(primaryYou.copy(name = "You"))
                    }

                    // Remove duplicate reserved members if any already exist.
                    youMembers.drop(1).forEach { memberDao.delete(it) }
                }
            }

            val currentMembers = activeGroupId?.let { memberDao.getByGroup(it) }.orEmpty()
            val currentExpenses = activeGroupId?.let { expenseDao.getByGroup(it) }.orEmpty()
            val currentSplits = currentExpenses.associate { expense ->
                expense.id to splitDao.getByExpense(expense.id)
            }
            val currentPayments = currentExpenses.associate { expense ->
                expense.id to paymentDao.getByExpense(expense.id)
            }
            GroupSharingData(allGroups, currentMembers, currentExpenses, currentSplits, currentPayments)
        }
        groups = loaded.groups
        members = loaded.members
        expenses = loaded.expenses
        splits = loaded.splits
        paymentsByExpense = loaded.payments
        if (activeGroupId != null && groups.none { it.id == activeGroupId }) {
            activeGroupId = null
        }
    }

    val activeGroup = groups.firstOrNull { it.id == activeGroupId }
    val requestCreateGroup: () -> Unit = {
        if (!isPremium && groups.size >= FREE_GROUP_LIMIT) {
            freeLimitMessage =
                "The Free plan allows up to 4 groups. Upgrade to Premium to create unlimited groups."
            showFreeLimitDialog = true
        } else {
            editingGroup = null
            groupName = ""
            showGroupEditor = true
        }
    }

    val requestAddMember: () -> Unit = {
        val extraMembers = members.count {
            !it.name.equals("You", ignoreCase = true)
        }
        if (!isPremium && extraMembers >= FREE_GROUP_EXTRA_MEMBER_LIMIT) {
            freeLimitMessage =
                "The Free plan allows up to 5 members per group, including You. Upgrade to Premium for unlimited members."
            showFreeLimitDialog = true
        } else {
            editingMember = null
            memberName = ""
            showMemberEditor = true
        }
    }

    if (showFreeLimitDialog) {
        androidx.compose.material3.AlertDialog(
            containerColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            onDismissRequest = { showFreeLimitDialog = false },
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👑", style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Unlock Premium",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF171B3A)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        freeLimitMessage,
                        color = Color(0xFF55566A)
                    )
                    Surface(
                        color = Color(0xFFFFFBF1),
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFFFD782)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "✦  PREMIUM BENEFITS",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B5A08)
                            )
                            Text(
                                "✓  Unlimited members in every group",
                                color = Color(0xFF202238)
                            )
                            Text(
                                "✓  Unlimited groups",
                                color = Color(0xFF202238)
                            )
                            Text(
                                "✓  Google Drive backup & sync",
                                color = Color(0xFF202238)
                            )
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
                    androidx.compose.material3.TextButton(
                        onClick = { showFreeLimitDialog = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Not now", maxLines = 1)
                    }

                    androidx.compose.material3.Button(
                        onClick = {
                            showFreeLimitDialog = false
                            onUpgradeClick()
                        },
                        modifier = Modifier.weight(2f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFC247),
                            contentColor = Color(0xFF302000)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Unlock Premium",
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            dismissButton = {}
        )
    }


    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .then(if (activeGroupId == null) Modifier.statusBarsPadding() else Modifier)
                    .padding(
                        start = if (activeGroupId == null) 16.dp else 0.dp,
                        end = if (activeGroupId == null) 16.dp else 0.dp,
                        top = if (activeGroupId == null) 16.dp else 0.dp,
                        bottom = 16.dp
                    )
            ) {
            if (activeGroupId == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = groupSearch,
                        onValueChange = { groupSearch = it },
                        modifier = Modifier.weight(1f).height(52.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Search groups"
                            )
                        },
                        placeholder = {
                            Text("Search groups", fontSize = 14.sp, lineHeight = 16.sp)
                        }
                    )

                    Spacer(Modifier.width(8.dp))

                    Box {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = Color.White,
                            tonalElevation = 0.dp,
                            shadowElevation = 1.dp
                        ) {
                            IconButton(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(
                                        1.dp,
                                        Color(0xFFE7D7AE),
                                        RoundedCornerShape(14.dp)
                                    ),
                                onClick = { showHeaderMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,

                                    contentDescription = "More options"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showHeaderMenu,
                            onDismissRequest = { showHeaderMenu = false },
                            modifier = Modifier
                                .width(250.dp)
                                .clip(androidx.compose.ui.graphics.RectangleShape)
                                .background(Color.White)
                                    .border(
                                        1.dp,
                                        androidx.compose.ui.graphics.Color(0xFFE7D7AE),
                                        androidx.compose.ui.graphics.RectangleShape
                                    )
                        ) {
                            DropdownMenuItem(
                                text = { Text("Refresh") },
                                onClick = {
                                    showHeaderMenu = false
                                    refresh++
                                }
                            )
                        }
                    }
                }
            }

Spacer(Modifier.height(12.dp))

            if (activeGroup == null) {
                if (groups.isEmpty()) {
                    Text(
                        "No groups yet. Create a group to start splitting expenses.",
                        modifier = Modifier.padding(12.dp)
                    )
                }
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(groups.filter { it.name.contains(groupSearch, ignoreCase = true) }, key = { it.id }) { group ->
                        GroupCard(
                            group = group,
                            onClick = { activeGroupId = group.id }
                        )
                    }
                }



            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Box(modifier = Modifier.statusBarsPadding()) {
Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (initialGroupId != null) onBack()
                                else activeGroupId = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBack,
                                contentDescription = "Back"
                            )
                        }

                        val groupInitials = activeGroup.name
                            .trim()
                            .split(" ")
                            .filter { it.isNotBlank() }
                            .take(2)
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

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    editingGroup = activeGroup
                                    groupName = activeGroup.name
                                    showGroupEditor = true
                                }
                        ) {
                            Text(
                                text = activeGroup.name,
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

                        Box {
                            IconButton(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))

                                    ,
                                onClick = { showHeaderMenu = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,

                                    contentDescription = "Group options"
                                )
                            }
                            DropdownMenu(
                                expanded = showHeaderMenu,
                                onDismissRequest = { showHeaderMenu = false },
                                modifier = Modifier
                                    .width(250.dp)
                                    .clip(androidx.compose.ui.graphics.RectangleShape)
                                    .background(Color.White)
                                    .border(
                                        1.dp,
                                        androidx.compose.ui.graphics.Color(0xFFE7D7AE),
                                        androidx.compose.ui.graphics.RectangleShape
                                    )
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Add Member") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.GroupAdd,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        requestAddMember()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Add Expense") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.ReceiptLong,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        editingExpense = null
                                        showExpenseEditor = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settle") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        showSettlementMemberPicker = true
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Group Expense Report") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Description,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showHeaderMenu = false
                                        activeGroupId?.let(onGroupReportClick)
                                    }
                                )
                            }
                        }
                    }
                    }
                }


                Spacer(Modifier.height(8.dp))

                if (selectedGroupSection == "Members") {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (members.isEmpty()) {
                        Text("No members yet. Use the three-dot menu to add a member.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(members, key = { it.id }) { member ->
                                val memberBalance = memberRemainingBalance(member.id, splits, settlements, paymentsByExpense)

                                androidx.compose.material3.Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (member.name.equals("You", ignoreCase = true)) {
                                                Modifier.combinedClickable(
                                                    onClick = { },
                                                    onLongClick = {
                                                        defaultShareMember = member
                                                        defaultShareText = decimal(member.defaultShareUnits)
                                                    }
                                                )
                                            } else {
                                                Modifier.combinedClickable(
                                                    onClick = {
                                                        editingMember = member
                                                        memberName = member.name
                                                        showMemberEditor = true
                                                    },
                                                    onLongClick = {
                                                        defaultShareMember = member
                                                        defaultShareText = decimal(member.defaultShareUnits)
                                                    }
                                                )
                                            }
                                        ),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = androidx.compose.material3.CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = androidx.compose.material3.CardDefaults.cardElevation(
                                        defaultElevation = 1.dp
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primaryContainer,
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = member.name.trim().take(1).uppercase(),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = member.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            if (!member.name.equals("You", ignoreCase = true)) {
                                                val isExcessPaid = memberBalance < -0.009
                                                Text(
                                                    text = if (isExcessPaid) "Excess Paid" else "Remaining balance",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isExcessPaid) Color(0xFFDC2626)
                                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(Modifier.width(12.dp))

                                        if (!member.name.equals("You", ignoreCase = true)) {
                                            val isExcessPaid = memberBalance < -0.009
                                            Text(
                                                text = money(if (isExcessPaid) kotlin.math.abs(memberBalance) else memberBalance),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isExcessPaid) Color(0xFFDC2626)
                                                    else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 14.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                requestAddMember()
                            },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = if (isPremium) Color(0xFFFFC247)
                                else MaterialTheme.colorScheme.primary,
                            contentColor = if (isPremium) Color(0xFF302000)
                                else MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add Member"
                            )
                        }
                    }

                    }
}


 else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (expenses.isEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.CardDefaults.cardColors(
                                containerColor = androidx.compose.ui.graphics.Color.White
                            ),
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
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

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        "No expenses yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = androidx.compose.ui.graphics.Color(0xFF202124)
                                    )
                                    Text(
                                        "Expenses added to this group will appear here.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(expenses, key = { it.id }) { expense ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingExpense = expense
                                        showExpenseEditor = true
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = androidx.compose.ui.graphics.Color.White
                                ),
                                elevation = androidx.compose.material3.CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                expense.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = androidx.compose.ui.graphics.Color(0xFF202124)
                                            )
                                            Text(
                                                expense.splitMode,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = androidx.compose.ui.graphics.Color(0xFF626B78)
                                            )
                                            Text(
                                                java.text.SimpleDateFormat(
                                                    "dd MMM yyyy",
                                                    java.util.Locale.getDefault()
                                                ).format(java.util.Date(expense.expenseDate)),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = androidx.compose.ui.graphics.Color(0xFF737B87)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            money(expense.amount),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = androidx.compose.ui.graphics.Color(0xFF166534)
                                        )
                                    }

                                    if (expense.note.isNotBlank()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            color = androidx.compose.ui.graphics.Color(0xFFF5F7FA),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(
                                                text = expense.note,
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = androidx.compose.ui.graphics.Color(0xFF424A55)
                                            )
                                        }
                                    }

                                    val expenseSplits = splits[expense.id].orEmpty()
                                    val expensePayments = paymentsByExpense[expense.id].orEmpty()
                                    val displayedMemberIds = (expenseSplits.map { it.memberId } +
                                        expensePayments.map { it.memberId }).distinct()

                                    displayedMemberIds.forEach { memberId ->
                                        val split = expenseSplits.firstOrNull { it.memberId == memberId }
                                        val payment = expensePayments.firstOrNull { it.memberId == memberId }
                                        val memberName = split?.memberNameSnapshot
                                            ?: payment?.memberNameSnapshot.orEmpty()
                                        val shareAmount = split?.amount ?: 0.0
                                        val memberPaid = expensePayments
                                            .filter { it.memberId == memberId }
                                            .sumOf { it.amount }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    memberName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = androidx.compose.ui.graphics.Color(0xFF202124)
                                                )
                                                Text(
                                                    "Paid ${money(memberPaid)}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = androidx.compose.ui.graphics.Color(0xFF737B87)
                                                )
                                                if (split != null && expense.splitMode == "Share") {
                                                    Text(
                                                        "${split.shareUnits} shares",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = androidx.compose.ui.graphics.Color(0xFF737B87)
                                                    )
                                                }
                                            }
                                            Text(
                                                money(shareAmount),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = androidx.compose.ui.graphics.Color(0xFF202124)
                                            )
                                        }
                                    }

                                }
                            }
                        }
                    }
                    // Add Expense icon above the bottom footer
                    Box(
                        modifier = Modifier.align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 14.dp)
                    ) {
                        FloatingActionButton(
                            onClick = {
                                editingExpense = null
                                showExpenseEditor = true
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
                    }


                    }
}

                // Group section footer tabs
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
                        Surface(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(17.dp))
                                .clickable { selectedGroupSection = "Expense" },
                            shape = RoundedCornerShape(17.dp),
                            border = if (selectedGroupSection != "Expense") {
                                if (isPremium) BorderStroke(1.dp, Color(0xFFFFC247))
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            } else null,
                            color = if (selectedGroupSection == "Expense") {
                                if (isPremium) Color(0xFFFFC247)
                                else MaterialTheme.colorScheme.primary
                            } else if (isPremium) Color.White else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                    tint = if (isPremium) Color(0xFF302000)
                                        else if (selectedGroupSection == "Expense")
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Expenses",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPremium) Color(0xFF302000)
                                        else if (selectedGroupSection == "Expense")
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f)
                                .clip(RoundedCornerShape(17.dp))
                                .clickable { selectedGroupSection = "Members" },
                            shape = RoundedCornerShape(17.dp),
                            border = if (selectedGroupSection != "Members") {
                                if (isPremium) BorderStroke(1.dp, Color(0xFFFFC247))
                                else BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            } else null,
                            color = if (selectedGroupSection == "Members") {
                                if (isPremium) Color(0xFFFFC247)
                                else MaterialTheme.colorScheme.primary
                            } else if (isPremium) Color.White else Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.GroupAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(19.dp),
                                    tint = if (isPremium) Color(0xFF302000)
                                        else if (selectedGroupSection == "Members")
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Members (${members.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPremium) Color(0xFF302000)
                                        else if (selectedGroupSection == "Members")
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

            }
            }
        }
        if (activeGroupId == null) {
            LedgerBottomNavigation(
                selectedTab = "groupSharing",
                isPremium = isPremium,
                onPartiesClick = onPartiesClick,
                onExpensesClick = onExpensesClick,
                onGroupSharingClick = onBack
            )
        }
    }

    if (showGroupEditor) {
        if (editingGroup == null) {
            NameEditorDialog(
                title = "Create group",
                initialValue = groupName,
                label = "Group name",
                addPartyStyle = true,
                onDismiss = { showGroupEditor = false },
                onSave = { value ->
                    scope.launch {
                    if (!isPremium) {
                        val existingGroupCount = withContext(Dispatchers.IO) {
                            groupDao.getAll().size
                        }
                        if (existingGroupCount >= FREE_GROUP_LIMIT) {
                            showGroupEditor = false
                            freeLimitMessage =
                                "The Free plan allows up to 4 groups. Upgrade to Premium to create unlimited groups."
                            showFreeLimitDialog = true
                            return@launch
                        }
                    }
                        withContext(Dispatchers.IO) {
                            val groupId = groupDao.insert(SharingGroupEntity(name = value))
                            memberDao.insert(
                                SharingMemberEntity(groupId = groupId, name = "You")
                            )
                        }
                        onDataChanged()
                    showGroupEditor = false
                        refresh++
                    }
                }
            )
        } else {
            GroupEditDialog(
                name = groupName,
                onNameChange = { groupName = it },
                onDismiss = { showGroupEditor = false },
                onSave = { value ->
                    scope.launch {
                        val group = editingGroup
                        if (group != null) {
                            withContext(Dispatchers.IO) {
                                groupDao.update(group.copy(name = value))
                            }
                        }
                        onDataChanged()
                    showGroupEditor = false
                        refresh++
                    }
                },
                onDelete = {
                    val group = editingGroup
                    showGroupEditor = false
                    if (group != null) deletingGroup = group
                },
                canDelete = editingGroup?.let { group ->
                    val groupMembers = members.filter { it.groupId == group.id }
                        .filterNot { it.name.equals("You", ignoreCase = true) }
                    groupMembers.all { member ->
                        memberRemainingBalance(member.id, splits, settlements, paymentsByExpense) <= 0.009
                    }
                } ?: false
            )
        }
    }


    if (defaultShareMember != null) {
        val target = defaultShareMember
        val parsedShare = defaultShareText.toDoubleOrNull()
        val validShare = parsedShare != null && parsedShare.isFinite() && parsedShare > 0.0

        AlertDialog(
            onDismissRequest = { defaultShareMember = null },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Default Share",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF171B3A)
                    )
                    Text(
                        target?.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF3157D5)
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Set this member's usual share. It will be filled in automatically when you create a new expense using Share mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF667085)
                    )

                    OutlinedTextField(
                        value = defaultShareText,
                        onValueChange = { defaultShareText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default shares") },
                        placeholder = { Text("e.g. 1 or 2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF3157D5),
                            unfocusedBorderColor = Color(0xFFDCE2EE),
                            focusedLabelColor = Color(0xFF3157D5),
                            cursorColor = Color(0xFF3157D5),
                            focusedTextColor = Color(0xFF171B3A),
                            unfocusedTextColor = Color(0xFF171B3A)
                        )
                    )

                    if (defaultShareText.isNotBlank() && !validShare) {
                        Text(
                            "Enter a number greater than zero.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = validShare,
                    onClick = {
                        val member = target
                        val value = parsedShare
                        if (member != null && value != null && value.isFinite() && value > 0.0) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    memberDao.update(member.copy(defaultShareUnits = value))
                                }
                                defaultShareMember = null
                                onDataChanged()
                                refresh++
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF3157D5),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFBFC9E8),
                        disabledContentColor = Color.White
                    )
                ) {
                    Text(
                        "Save Default",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { defaultShareMember = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF667085),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    if (showMemberEditor && activeGroupId != null) {
        NameEditorDialog(
            title = if (editingMember == null) "Add member" else "Edit member",
            initialValue = memberName,
            label = "Member name",
            addPartyStyle = true,
            onDelete = {
                val member = editingMember
                showMemberEditor = false
                if (
                    member != null &&
                    !member.name.equals("You", ignoreCase = true) &&
                    memberRemainingBalance(member.id, splits, settlements, paymentsByExpense) <= 0.009
                ) deletingMember = member
            },
            onSettle = {
                val member = editingMember
                showMemberEditor = false
                if (member != null) settlingMember = member
            },
            canDelete = editingMember?.let {
                !it.name.equals("You", ignoreCase = true) &&
                    memberRemainingBalance(it.id, splits, settlements, paymentsByExpense) <= 0.009
            } ?: false,
            isSettled = editingMember?.let {
                memberRemainingBalance(it.id, splits, settlements, paymentsByExpense) <= 0.009
            } ?: false,
            onDismiss = { showMemberEditor = false },
            onSave = { value ->
                val groupId = activeGroupId ?: return@NameEditorDialog
                scope.launch {
                    val oldMemberForLimit = editingMember
                    if (oldMemberForLimit == null && !isPremium) {
                        val extraMemberCount = withContext(Dispatchers.IO) {
                            memberDao.getByGroup(groupId).count {
                                !it.name.equals("You", ignoreCase = true)
                            }
                        }
                        if (extraMemberCount >= FREE_GROUP_EXTRA_MEMBER_LIMIT) {
                            showMemberEditor = false
                            freeLimitMessage =
                                "The Free plan allows up to 5 members per group, including You. Upgrade to Premium for unlimited members."
                            showFreeLimitDialog = true
                            return@launch
                        }
                    }
                    withContext(Dispatchers.IO) {
                        val old = editingMember
                        if (old == null) {
                            // "You" is reserved and is created automatically for every group.
                            if (!value.equals("You", ignoreCase = true)) {
                                val existingYou = memberDao.getByGroup(groupId).any {
                                    it.name.equals("You", ignoreCase = true)
                                }
                                if (!existingYou) {
                                    memberDao.insert(
                                        SharingMemberEntity(groupId = groupId, name = "You")
                                    )
                                }
                                memberDao.insert(
                                    SharingMemberEntity(groupId = groupId, name = value)
                                )
                            }
                        } else if (!old.name.equals("You", ignoreCase = true)) {
                            memberDao.update(old.copy(name = value))
                        }
                    }
                    onDataChanged()
                    showMemberEditor = false
                    refresh++
                }
            }
        )
    }

    if (showSettlementMemberPicker) {
        val settlementMembers = members.filterNot {
            it.name.equals("You", ignoreCase = true)
        }

        Dialog(
            onDismissRequest = { showSettlementMemberPicker = false }
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(0.94f),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Settle",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Choose a member to record payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { showSettlementMemberPicker = false }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    HorizontalDivider()

                    if (settlementMembers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No other members in this group yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            settlementMembers.forEach { member ->
                                val remaining = memberRemainingBalance(
                                    member.id,
                                    splits,
                                    settlements
                                )
                                val isSettled = remaining <= 0.009

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(
                                                alpha = 0.48f
                                            )
                                        )
                                        .clickable {
                                            showSettlementMemberPicker = false
                                            settlingMember = member
                                        }
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme.primary.copy(
                                                    alpha = 0.10f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = member.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = if (isSettled) "Settled" else "Remaining balance",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = money(remaining),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSettled)
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            else
                                                MaterialTheme.colorScheme.primary
                                        )
                                        Icon(
                                            imageVector = Icons.Outlined.ChevronRight,
                                            contentDescription = "Open settlement",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showSettlementMemberPicker = false },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }

    settlingMember?.let { member ->
        val totalExpense = memberExpenseTotal(member.id, splits)
        val totalReceived = memberReceivedTotal(member.id, settlements)
        val totalPaid = paymentsByExpense.values.asSequence().flatten()
            .filter { it.memberId == member.id }.sumOf { it.amount }
        val balance = (totalExpense - totalReceived - totalPaid).coerceAtLeast(0.0)

        SettlementDialog(
            member = member,
            totalExpense = totalExpense,
            totalReceived = totalReceived,
            remainingBalance = balance,
            history = settlements.filter { it.memberId == member.id },
            onDelete = { record ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        settlementDao.deleteById(record.id)
                    }
                    onDataChanged()
                    refresh++
                }
            },
            onDismiss = { settlingMember = null },
            onSave = { amount ->
                settlingMember = null
                scope.launch {
                    withContext(Dispatchers.IO) {
                        settlementDao.insert(
                            GroupSettlementEntity(
                                groupId = member.groupId,
                                memberId = member.id,
                                memberNameSnapshot = member.name,
                                amount = amount
                            )
                        )
                    }
                    onDataChanged()
                    refresh++
                }
            }
        )
    }

    deletingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { deletingGroup = null },
            title = { Text("Delete group?") },
            text = { Text("This will delete the group's members and group expenses. ") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val hasUnsettledMembers = withContext(Dispatchers.IO) {
                            val groupMembers = memberDao.getByGroup(group.id)
                                .filterNot { it.name.equals("You", ignoreCase = true) }
                            val groupExpenses = expenseDao.getByGroup(group.id)
                            val groupSettlements = settlementDao.getByGroup(group.id)

                            groupMembers.any { member ->
                                val totalExpense = groupExpenses.sumOf { expense ->
                                    splitDao.getByExpense(expense.id)
                                        .filter { it.memberId == member.id }
                                        .sumOf { it.amount }
                                }
                                val totalReceived = groupSettlements
                                    .filter { it.memberId == member.id }
                                    .sumOf { it.amount }
                                val totalPaid = groupExpenses.sumOf { expense ->
                                    paymentDao.getByExpense(expense.id)
                                        .filter { it.memberId == member.id }
                                        .sumOf { it.amount }
                                }

                                kotlin.math.abs(totalExpense - totalReceived - totalPaid) > 0.009
                            }
                        }

                        if (hasUnsettledMembers) {
                            deletingGroup = null
                            groupDeleteBlocked = true
                        } else {
                            withContext(Dispatchers.IO) {
                                paymentDao.deleteByGroup(group.id)
                                splitDao.deleteByGroup(group.id)
                                settlementDao.deleteByGroup(group.id)
                                expenseDao.deleteByGroup(group.id)
                                memberDao.deleteByGroup(group.id)
                                groupDao.delete(group)
                            }
                            onDataChanged()
                            deletingGroup = null
                            if (activeGroupId == group.id) activeGroupId = null
                            refresh++
                            onGroupDeleted()
                        }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) { Text("Cancel") }
            }
        )
    }

    if (groupDeleteBlocked) {
        AlertDialog(
            onDismissRequest = { groupDeleteBlocked = false },
            title = { Text("Settlement pending") },
            text = {
                Text(
                    "This group cannot be deleted until every member except You has a zero remaining balance. Please settle all outstanding balances first."
                )
            },
            confirmButton = {
                TextButton(onClick = { groupDeleteBlocked = false }) {
                    Text("OK")
                }
            }
        )
    }

    deletingMember?.let { member ->
        AlertDialog(
            onDismissRequest = { deletingMember = null },
            title = { Text("Remove member?") },
            text = { Text("A member can be removed only after the full balance is settled. Historical expense splits retain their saved details.") },
            confirmButton = {
                TextButton(onClick = {
                    deletingMember = null
                    if (
                        !member.name.equals("You", ignoreCase = true) &&
                        memberRemainingBalance(member.id, splits, settlements, paymentsByExpense) <= 0.009
                    ) {
                        scope.launch {
                            withContext(Dispatchers.IO) { memberDao.delete(member) }
                            onDataChanged()
                    refresh++
                        }
                    }
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deletingMember = null }) { Text("Cancel") }
            }
        )
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete group expense?") },
            text = { Text("Delete ${expense.title} (${money(expense.amount)}) and its split details?") },
            confirmButton = {
                TextButton(onClick = {
                    deletingExpense = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            paymentDao.deleteByExpense(expense.id)
                            splitDao.deleteByExpense(expense.id)
                            expenseDao.delete(expense)
                        }
                        onDataChanged()
                    refresh++
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) { Text("Cancel") }
            }
        )
    }

    if (showExpenseEditor && activeGroupId != null) {
        GroupExpenseEditorDialog(
            members = members,
            initial = editingExpense,
            initialSplits = editingExpense?.let { splits[it.id].orEmpty() }.orEmpty(),
            initialPayments = editingExpense?.let { paymentsByExpense[it.id].orEmpty() }.orEmpty(),
            isPremium = isPremium,
            onDismiss = { showExpenseEditor = false },
            onDelete = {
                val expense = editingExpense
                showExpenseEditor = false
                if (expense != null) deletingExpense = expense
            },
            onSave = { title, amount, expenseDate, note, mode, drafts, paymentDrafts ->
                val groupId = activeGroupId ?: return@GroupExpenseEditorDialog
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val old = editingExpense
                        val expenseId = if (old == null) {
                            expenseDao.insert(
                                GroupExpenseEntity(
                                    groupId = groupId,
                                    title = title,
                                    amount = amount,
                                    category = "General",
                                    note = note,
                                    expenseDate = expenseDate,
                                    splitMode = mode
                                )
                            )
                        } else {
                            expenseDao.update(
                                old.copy(
                                    title = title,
                                    amount = amount,
                                    category = old.category,
                                    note = note,
                                    expenseDate = expenseDate,
                                    splitMode = mode
                                )
                            )
                            splitDao.deleteByExpense(old.id)
                            old.id
                        }

                        paymentDao.deleteByExpense(expenseId)
                        if (paymentDrafts.isNotEmpty()) {
                            paymentDao.insertAll(paymentDrafts.map {
                                GroupExpensePaymentEntity(
                                    expenseId = expenseId,
                                    memberId = it.memberId,
                                    memberNameSnapshot = it.memberName,
                                    amount = it.amount
                                )
                            })
                        }

                        splitDao.insertAll(
                            drafts.map {
                                GroupExpenseSplitEntity(
                                    expenseId = expenseId,
                                    memberId = it.memberId,
                                    memberNameSnapshot = it.memberName,
                                    amount = it.amount,
                                    shareUnits = it.shareUnits
                                )
                            }
                        )
                    }
                    onDataChanged()
                    showExpenseEditor = false
                    refresh++
                }
            }
        )
    }
}

@Composable
private fun GroupCard(
    group: SharingGroupEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = group.name.trim()
                .split(" ")
                .filter { it.isNotBlank() }
                .take(2)
                .joinToString("") { it.first().uppercase() }

            Surface(
                modifier = Modifier.width(50.dp).height(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials.ifBlank { "G" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun GroupEditDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(26.dp),
            color = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Group",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Update group details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Group name") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp)
                )
                OutlinedButton(
                    onClick = onDelete,
                    enabled = canDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.error
                    ),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.width(18.dp).height(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Delete Group", fontWeight = FontWeight.SemiBold)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(0.85f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                    Button(
                        onClick = { if (name.trim().isNotEmpty()) onSave(name.trim()) },
                        enabled = name.trim().isNotEmpty(),
                        modifier = Modifier.weight(1.15f).height(52.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NameEditorDialog(
    title: String,
    initialValue: String,
    label: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    addPartyStyle: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onSettle: (() -> Unit)? = null,
    canDelete: Boolean = true,
    isSettled: Boolean = false
) {
    var value by remember(title, initialValue) { mutableStateOf(initialValue) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = if (addPartyStyle) RoundedCornerShape(28.dp) else AlertDialogDefaults.shape,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = if (addPartyStyle) 0.dp else AlertDialogDefaults.TonalElevation,
        title = {
            Text(
                text = title,
                style = if (addPartyStyle) MaterialTheme.typography.headlineSmall
                else MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(label) },
                    singleLine = true,
                    shape = if (addPartyStyle) RoundedCornerShape(15.dp)
                    else RoundedCornerShape(12.dp)
                )

                if (onSettle != null && initialValue.isNotBlank()) {
                    Button(
                        onClick = onSettle,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isSettled) "Settled" else "Settle")
                    }
                }

                if (onDelete != null && initialValue.isNotBlank()) {
                    OutlinedButton(
                        onClick = { if (canDelete) onDelete() },
                        enabled = canDelete,
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFDC2626)
                        ),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Remove Member")
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (value.trim().isNotEmpty()) onSave(value.trim())
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {}
    )
}

@Composable
private fun GroupExpenseEditorDialog(
    members: List<SharingMemberEntity>,
    initial: GroupExpenseEntity?,
    initialSplits: List<GroupExpenseSplitEntity>,
    initialPayments: List<GroupExpensePaymentEntity>,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, Double, Long, String, String, List<SplitDraft>, List<PaymentDraft>) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var showMemberSearch by remember { mutableStateOf(false) }
    var showPaidByDialog by remember { mutableStateOf(false) }
    var paidByCustomized by remember(initial?.id) {
        mutableStateOf(initial != null || initialPayments.isNotEmpty())
    }
    var paymentDrafts by remember(initial?.id) {
        mutableStateOf(initialPayments.map {
            PaymentDraft(it.memberId, it.memberNameSnapshot, it.amount)
        })
    }
    var memberSearch by remember { mutableStateOf("") }

    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var expenseDate by remember(initial?.id) {
        mutableStateOf(initial?.expenseDate ?: System.currentTimeMillis())
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var amountText by remember(initial?.id) {
        mutableStateOf(initial?.amount?.let { decimal(it) } ?: "")
    }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var mode by remember(initial?.id) { mutableStateOf(initial?.splitMode ?: "Equal") }
    var error by remember(initial?.id) { mutableStateOf("") }

    val initialIds = initialSplits.map { it.memberId }.toSet()
    val selected = remember(initial?.id, members) {
        mutableStateListOf<Long>().apply {
            addAll(
                if (initial == null) members.map { it.id }
                else members.filter { initialIds.contains(it.id) }.map { it.id }
            )
        }
    }
    val customAmounts = remember(initial?.id) {
        mutableStateMapOf<Long, String>().apply {
            initialSplits.forEach { put(it.memberId, decimal(it.amount)) }
        }
    }
    val shares = remember(initial?.id, members) {
        mutableStateMapOf<Long, String>().apply {
            if (initial != null) {
                initialSplits.forEach { put(it.memberId, decimal(it.shareUnits)) }
            } else {
                members.forEach { put(it.id, decimal(it.defaultShareUnits)) }
            }
        }
    }

    AlertDialog(
        modifier = Modifier.fillMaxSize(),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        shape = androidx.compose.ui.graphics.RectangleShape,
        containerColor = androidx.compose.ui.graphics.Color.White,
        tonalElevation = 0.dp,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Outlined.GroupAdd,
                        contentDescription = null,
                        modifier = Modifier.padding(11.dp).size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = if (initial == null) "Add group expense" else "Edit group expense",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Split and track group spending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(15.dp)
                    )
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                                amountText = it
                                if (!paidByCustomized) {
                                    val value = it.trim().toDoubleOrNull()
                                    val you = members.firstOrNull {
                                        it.name.equals("You", ignoreCase = true)
                                    }
                                    val cents = value
                                        ?.takeIf { it.isFinite() && it > 0.0 }
                                        ?.let { (it * 100.0).roundToLong() }
                                    paymentDrafts = if (you != null && cents != null && cents > 0L) {
                                        listOf(PaymentDraft(you.id, you.name, cents / 100.0))
                                    } else {
                                        emptyList()
                                    }
                                }
                            },
                        label = { Text("Amount") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(15.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .clickable { showDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                                .format(Date(expenseDate)),
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("Date") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(15.dp)
                        )
                    }
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Note") },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.weight(1.1f),
                        shape = RoundedCornerShape(15.dp)
                    )
                }


                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Equal", "Custom", "Share").forEach { option ->
                        val isSelected = mode == option
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { mode = option },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                if (isPremium) Color(0xFFFFC247)
                                else MaterialTheme.colorScheme.primary
                            } else {
                                Color.White
                            },
                            contentColor = if (isSelected) {
                                if (isPremium) Color(0xFF302000)
                                else MaterialTheme.colorScheme.onPrimary
                            } else {
                                if (isPremium) Color(0xFF302000)
                                else MaterialTheme.colorScheme.onSurface
                            },
                            border = if (isSelected) null else
                                androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isPremium) Color(0xFFFFC247)
                                    else MaterialTheme.colorScheme.primary
                                ),
                            tonalElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (isSelected) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    }
                                )
                            }
                        }
                    }
                }

if (mode == "Share") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F5FF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ⓘ", color = Color(0xFF3157D5), fontSize = 17.sp)
        Text(
            text = "Tip: Long-press a member to set their default share.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF3157D5)
        )
    }
    Spacer(Modifier.height(8.dp))
}

Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
    Text(
        "Members",
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
    IconButton(
        onClick = {
            showMemberSearch = !showMemberSearch
            if (!showMemberSearch) memberSearch = ""
        }
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "Search members"
        )
    }
}
if (showMemberSearch) {
    OutlinedTextField(
        value = memberSearch,
        onValueChange = { memberSearch = it },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Search members") },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null)
        },
        singleLine = true,
        shape = RoundedCornerShape(15.dp)
    )
}
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    members.filter { it.name.contains(memberSearch, ignoreCase = true) }.forEach { member ->
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selected.contains(member.id),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        if (!selected.contains(member.id)) selected.add(member.id)
                                    } else selected.remove(member.id)
                                }
                            )
                            Text(member.name, modifier = Modifier.weight(1f))
                            if (selected.contains(member.id) && mode == "Custom") {
                                OutlinedTextField(
                                    value = customAmounts[member.id] ?: "",
                                    onValueChange = { customAmounts[member.id] = it },
                                    label = { Text("Amount", fontSize = 10.sp, maxLines = 1, lineHeight = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.height(52.dp).width(112.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            } else if (selected.contains(member.id) && mode == "Share") {
                                OutlinedTextField(
                                    value = shares[member.id] ?: "1",
                                    onValueChange = { shares[member.id] = it },
                                    label = { Text("Shares", fontSize = 10.sp, maxLines = 1, lineHeight = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.height(52.dp).width(112.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 14.sp
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initial != null && onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Delete expense")
                    }
                }
                OutlinedButton(
                    onClick = { showPaidByDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(if (paymentDrafts.isEmpty()) "Paid By"
                         else "Paid By · ${money(paymentDrafts.sumOf { it.amount })}")
                }
                Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text("Cancel")
                }
                Button(
                modifier = Modifier.weight(1f),
                onClick = {
                val total = amountText.toDoubleOrNull()
                val chosen = members.filter { selected.contains(it.id) }
                if (title.trim().isEmpty()) {
                    error = "Enter an expense title."
                    return@Button
                }
                if (total == null || !total.isFinite() || total <= 0) {
                    error = "Enter a valid total amount."
                    return@Button
                }
                if (chosen.isEmpty()) {
                    error = "Select at least one member."
                    return@Button
                }

                val totalCents = (total * 100.0).roundToLong()
                if (paymentDrafts.isNotEmpty()) {
                    val paidCents = paymentDrafts.sumOf {
                        (it.amount * 100.0).roundToLong()
                    }
                    if (paidCents != totalCents) {
                        error = "Paid By total must equal expense amount ${money(totalCents / 100.0)}. Update Paid By and try again."
                        return@Button
                    }
                }
                val amounts = mutableListOf<Long>()
                val units = mutableListOf<Double>()

                when (mode) {
                    "Equal" -> {
                        val base = totalCents / chosen.size
                        var remainder = totalCents % chosen.size
                        chosen.forEach {
                            amounts.add(base + if (remainder-- > 0) 1 else 0)
                            units.add(1.0)
                        }
                    }
                    "Custom" -> {
                        chosen.forEach { member ->
                            val entered = customAmounts[member.id]?.toDoubleOrNull()
                            if (entered == null || !entered.isFinite() || entered < 0) {
                                error = "Enter a valid custom amount for ${member.name}."
                                return@Button
                            }
                            amounts.add((entered * 100.0).roundToLong())
                            units.add(1.0)
                        }
                        if (amounts.sum() != totalCents) {
                            error = "Custom amounts must add up to ${money(total)}."
                            return@Button
                        }
                    }
                    else -> {
                        chosen.forEach { member ->
                            val entered = shares[member.id]?.toDoubleOrNull()
                            if (entered == null || !entered.isFinite() || entered <= 0) {
                                error = "Enter positive shares for ${member.name}."
                                return@Button
                            }
                            units.add(entered)
                        }
                        val totalUnits = units.sum()
                        var assigned = 0L
                        units.forEachIndexed { index, unit ->
                            val cents = if (index == units.lastIndex) {
                                totalCents - assigned
                            } else {
                                (totalCents * unit / totalUnits).roundToLong().also {
                                    assigned += it
                                }
                            }
                            amounts.add(cents)
                        }
                    }
                }

                val drafts = chosen.mapIndexed { index, member ->
                    SplitDraft(
                        memberId = member.id,
                        memberName = member.name,
                        amount = amounts[index] / 100.0,
                        shareUnits = units[index]
                    )
                }
                error = ""
                onSave(title.trim(), totalCents / 100.0, expenseDate, note.trim(), mode, drafts, paymentDrafts)
            }, shape = RoundedCornerShape(14.dp)) { Text("Save") }
            }
            }
        }
    )




    if (showPaidByDialog) {
        var paidAmounts by remember(initial?.id, paymentDrafts) {
            mutableStateOf(paymentDrafts.associate {
                it.memberId to String.format(Locale.US, "%.2f", it.amount)
            })
        }
        var paymentError by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaidByDialog = false },
            containerColor = Color.White,
            tonalElevation = 0.dp,
            title = { Text("Paid By") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Enter how much each member paid. Use 0 if they paid nothing.")
                    members.forEach { member ->
                        val isYou = member.name.equals("You", ignoreCase = true)
                        val expenseCents = amountText.trim().toDoubleOrNull()
                            ?.takeIf { it.isFinite() && it > 0.0 }
                            ?.let { (it * 100.0).roundToLong() } ?: 0L
                        val otherMembersCents = members
                            .filterNot { it.name.equals("You", ignoreCase = true) }
                            .sumOf { other ->
                                val raw = if (other.id == member.id && !isYou) {
                                    paidAmounts[member.id].orEmpty()
                                } else {
                                    paidAmounts[other.id].orEmpty()
                                }
                                val value = raw.trim().toDoubleOrNull()
                                    ?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                                (value * 100.0).roundToLong()
                            }
                        val displayedAmount = if (isYou) {
                            String.format(
                                Locale.US,
                                "%.2f",
                                (expenseCents - otherMembersCents).coerceAtLeast(0L) / 100.0
                            )
                        } else {
                            paidAmounts[member.id].orEmpty()
                        }

                        OutlinedTextField(
                            value = displayedAmount,
                            onValueChange = { newValue ->
                                if (!isYou) {
                                    val candidate = newValue.trim().toDoubleOrNull()
                                    if (candidate != null && candidate.isFinite() && candidate >= 0.0) {
                                        val candidateCents = (candidate * 100.0).roundToLong()
                                        val otherCents = members
                                            .filterNot {
                                                it.name.equals("You", ignoreCase = true) ||
                                                    it.id == member.id
                                            }
                                            .sumOf { other ->
                                                val value = paidAmounts[other.id].orEmpty()
                                                    .trim().toDoubleOrNull()
                                                    ?.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                                                (value * 100.0).roundToLong()
                                            }
                                        if (candidateCents + otherCents > expenseCents) {
                                            paymentError =
                                                "Other members cannot pay more than ${money(expenseCents / 100.0)} in total."
                                            return@OutlinedTextField
                                        }
                                    }
                                    paidAmounts = paidAmounts.toMutableMap().apply {
                                        put(member.id, newValue)
                                    }
                                    paymentError = ""
                                }
                            },
                            label = {
                                Text(if (isYou) "You · Remaining (Auto)" else member.name)
                            },
                            singleLine = true,
                            readOnly = isYou,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (paymentError.isNotBlank()) {
                        Text(paymentError, color = MaterialTheme.colorScheme.error)
                    }
                    val totalPaid = paidAmounts.values.sumOf {
                        it.toDoubleOrNull()?.takeIf { value -> value.isFinite() && value >= 0.0 } ?: 0.0
                    }
                    Text("Total paid: ${money(totalPaid)}")
                }
            },
            confirmButton = {
                Button(onClick = {
                    val expenseValue = amountText.trim().toDoubleOrNull()
                    if (expenseValue == null || !expenseValue.isFinite() || expenseValue <= 0.0) {
                        paymentError = "Enter a valid expense amount first."
                        return@Button
                    }
                    val expectedCents = (expenseValue * 100.0).roundToLong()
                    val result = mutableListOf<PaymentDraft>()
                    val you = members.firstOrNull {
                        it.name.equals("You", ignoreCase = true)
                    }
                    if (you == null) {
                        paymentError = "Could not find the You member."
                        return@Button
                    }

                    for (member in members) {
                        if (member.id == you.id) continue
                        val raw = paidAmounts[member.id].orEmpty().trim()
                        if (raw.isBlank()) continue
                        val value = raw.toDoubleOrNull()
                        if (value == null || !value.isFinite() || value < 0.0) {
                            paymentError = "Enter a valid non-negative amount for ${member.name}."
                            return@Button
                        }
                        val cents = (value * 100.0).roundToLong()
                        if (cents > 0L) {
                            result.add(PaymentDraft(member.id, member.name, cents / 100.0))
                        }
                    }

                    val otherMembersCents = result.sumOf {
                        (it.amount * 100.0).roundToLong()
                    }
                    if (otherMembersCents > expectedCents) {
                        paymentError =
                            "Other members cannot pay more than ${money(expectedCents / 100.0)} in total."
                        return@Button
                    }

                    val youCents = expectedCents - otherMembersCents
                    if (youCents > 0L) {
                        result.add(0, PaymentDraft(you.id, you.name, youCents / 100.0))
                    }
                    paymentDrafts = result
                    paidByCustomized = true
                    paymentError = ""
                    showPaidByDialog = false
                }) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { showPaidByDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDatePicker) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val selected = Calendar.getInstance().apply {
            timeInMillis = expenseDate
        }

        android.app.DatePickerDialog(
            context,
            { _, year, month, day ->
                expenseDate = Calendar.getInstance().apply {
                    clear()
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                showDatePicker = false
            },
            selected.get(Calendar.YEAR),
            selected.get(Calendar.MONTH),
            selected.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }
}
@Composable
private fun SettlementDialog(
    member: SharingMemberEntity,
    totalExpense: Double,
    totalReceived: Double,
    remainingBalance: Double,
    history: List<GroupSettlementEntity>,
    onDelete: (GroupSettlementEntity) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var amountText by remember(member.id) { mutableStateOf(String.format(Locale.US, "%.2f", remainingBalance)) }
    val amount = amountText.toDoubleOrNull()
    val validAmount = amount != null &&
        amount > 0.0 &&
        amount <= remainingBalance + 0.009

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = Color.White,
        tonalElevation = 0.dp,
        title = { Text("Settle with ${member.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Total expense: ${money(totalExpense)}")
                Text("Amount received: ${money(totalReceived)}")
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFF5F7FF),
                    border = BorderStroke(1.dp, Color(0xFFE2E7F5))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            "Remaining balance",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF555F75)
                        )
                        Text(
                            java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
                                minimumFractionDigits = 2
                                maximumFractionDigits = 2
                            }.format(remainingBalance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2457C5)
                        )
                    }
                }
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amountText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Received Amount") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("Settlement History", fontWeight = FontWeight.SemiBold)
                androidx.compose.material3.HorizontalDivider(
                    color = Color(0xFFE8E8EC)
                )
                if (history.isEmpty()) {
                    Text("No settlements recorded yet.")
                } else {
                    history.sortedByDescending { it.receivedAt }.forEach { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    money(record.amount),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    SimpleDateFormat(
                                        "dd MMM yyyy, hh:mm a",
                                        Locale.getDefault()
                                    ).format(Date(record.receivedAt)),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = { onDelete(record) }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete settlement",
                                    tint = Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (validAmount) onSave(amount!!) },
                enabled = validAmount,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Settlement")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun money(amount: Double): String = CurrencySettings.format(amount)
private fun decimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun Modifier.heightInCompat(height: androidx.compose.ui.unit.Dp): Modifier =
    this.height(height)
