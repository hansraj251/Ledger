package com.ledger.app.ui

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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.ledger.app.data.LedgerDatabaseProvider
import com.ledger.app.data.SharingGroupDao
import com.ledger.app.data.SharingGroupEntity
import com.ledger.app.data.SharingMemberDao
import com.ledger.app.data.SharingMemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

private data class GroupSharingData(
    val groups: List<SharingGroupEntity>,
    val members: List<SharingMemberEntity>,
    val expenses: List<GroupExpenseEntity>,
    val splits: Map<Long, List<GroupExpenseSplitEntity>>
)

private data class SplitDraft(
    val memberId: Long,
    val memberName: String,
    val amount: Double,
    val shareUnits: Double
)

@Composable
fun GroupSharingScreen(
    onBack: () -> Unit,
    initialGroupId: Long? = null,
    onPartiesClick: () -> Unit,
    onExpensesClick: () -> Unit
) {
    val context = LocalContext.current
    val database = remember(context) { LedgerDatabaseProvider.get(context) }
    val groupDao = remember(database) { database.sharingGroupDao() }
    val memberDao = remember(database) { database.sharingMemberDao() }
    val expenseDao = remember(database) { database.groupExpenseDao() }
    val splitDao = remember(database) { database.groupExpenseSplitDao() }
    val scope = rememberCoroutineScope()

    var groups by remember { mutableStateOf(emptyList<SharingGroupEntity>()) }
    var members by remember { mutableStateOf(emptyList<SharingMemberEntity>()) }
    var expenses by remember { mutableStateOf(emptyList<GroupExpenseEntity>()) }
    var splits by remember { mutableStateOf(emptyMap<Long, List<GroupExpenseSplitEntity>>()) }
    var activeGroupId by remember(initialGroupId) {
        mutableStateOf(initialGroupId)
    }
    var selectedGroupSection by remember(activeGroupId) {
        mutableStateOf("Expense")
    }
    var refresh by remember { mutableStateOf(0) }
    var groupSearch by remember { mutableStateOf("") }
    var showHeaderMenu by remember { mutableStateOf(false) }

    var showGroupEditor by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<SharingGroupEntity?>(null) }
    var groupName by remember { mutableStateOf("") }
    var deletingGroup by remember { mutableStateOf<SharingGroupEntity?>(null) }

    var showMemberEditor by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<SharingMemberEntity?>(null) }
    var memberName by remember { mutableStateOf("") }
    var deletingMember by remember { mutableStateOf<SharingMemberEntity?>(null) }

    var showExpenseEditor by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<GroupExpenseEntity?>(null) }
    var deletingExpense by remember { mutableStateOf<GroupExpenseEntity?>(null) }

    LaunchedEffect(refresh, activeGroupId) {
        val loaded = withContext(Dispatchers.IO) {
            val allGroups = groupDao.getAll()
            val currentMembers = activeGroupId?.let { memberDao.getByGroup(it) }.orEmpty()
            val currentExpenses = activeGroupId?.let { expenseDao.getByGroup(it) }.orEmpty()
            val currentSplits = currentExpenses.associate { expense ->
                expense.id to splitDao.getByExpense(expense.id)
            }
            GroupSharingData(allGroups, currentMembers, currentExpenses, currentSplits)
        }
        groups = loaded.groups
        members = loaded.members
        expenses = loaded.expenses
        splits = loaded.splits
        if (activeGroupId != null && groups.none { it.id == activeGroupId }) {
            activeGroupId = null
        }
    }

    val activeGroup = groups.firstOrNull { it.id == activeGroupId }

    Column(Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = if (activeGroupId == null) 16.dp else 8.dp, bottom = 16.dp)
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
                            IconButton(onClick = { showHeaderMenu = true }) {
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
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
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

                                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    FloatingActionButton(
                        onClick = {
                            editingGroup = null
                            groupName = ""
                            showGroupEditor = true
                        },
                        shape = RoundedCornerShape(18.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = "Add Group"
                        )
                    }
                }

            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (initialGroupId != null) onBack()
                        else activeGroupId = null
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Back to groups"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activeGroup.name,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                editingGroup = activeGroup
                                groupName = activeGroup.name
                                showGroupEditor = true
                            },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Box {
                        IconButton(onClick = { showHeaderMenu = true }) {
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
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
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
                                    editingMember = null
                                    memberName = ""
                                    showMemberEditor = true
                                }
                            )
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = selectedGroupSection == "Expense",
                        onClick = { selectedGroupSection = "Expense" },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                "Expense",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = selectedGroupSection == "Members",
                        onClick = { selectedGroupSection = "Members" },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                "Members (${members.size})",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (selectedGroupSection == "Members") {
                    if (members.isEmpty()) {
                        Text("No members yet. Use the three-dot menu to add a member.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(members, key = { it.id }) { member ->
                                androidx.compose.material3.Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingMember = member
                                            memberName = member.name
                                            showMemberEditor = true
                                        },
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
                                        Text(
                                            text = member.name,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium
                                        )


                                    }
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        FloatingActionButton(
                            onClick = {
                                editingMember = null
                                memberName = ""
                                showMemberEditor = true
                            },
                            shape = RoundedCornerShape(18.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = "Add Member"
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Group expenses",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                editingExpense = null
                                showExpenseEditor = true
                            },
                            enabled = members.isNotEmpty()
                        ) {
                            Text("+ Expense")
                        }
                    }

                    if (expenses.isEmpty()) {
                        Text("No expenses in this group yet.")
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(expenses, key = { it.id }) { expense ->
                            Card(Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                expense.title,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text("${expense.category} • ${expense.splitMode}")
                                        }
                                        Text(
                                            money(expense.amount),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        expense.note,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    splits[expense.id].orEmpty().forEach { split ->
                                        Text(
                                            "${split.memberNameSnapshot}: ${money(split.amount)}" +
                                                if (expense.splitMode == "Share") {
                                                    " (${split.shareUnits} shares)"
                                                } else "",
                                            fontSize = 13.sp
                                        )
                                    }
                                    Row {
                                        TextButton(onClick = {
                                            editingExpense = expense
                                            showExpenseEditor = true
                                        }) { Text("Edit") }
                                        TextButton(onClick = {
                                            deletingExpense = expense
                                        }) { Text("Delete") }
                                    }
                                }
                            }
                        }
                    }
                }

            }
            }
        }
        LedgerBottomNavigation(
            selectedTab = "groupSharing",
            onPartiesClick = onPartiesClick,
            onExpensesClick = onExpensesClick,
            onGroupSharingClick = onBack
        )
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
                        withContext(Dispatchers.IO) {
                            groupDao.insert(SharingGroupEntity(name = value))
                        }
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
                        showGroupEditor = false
                        refresh++
                    }
                },
                onDelete = {
                    val group = editingGroup
                    showGroupEditor = false
                    if (group != null) deletingGroup = group
                }
            )
        }
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
                if (member != null) deletingMember = member
            },
            onDismiss = { showMemberEditor = false },
            onSave = { value ->
                val groupId = activeGroupId ?: return@NameEditorDialog
                scope.launch {
                    withContext(Dispatchers.IO) {
                        val old = editingMember
                        if (old == null) {
                            memberDao.insert(SharingMemberEntity(groupId = groupId, name = value))
                        } else {
                            memberDao.update(old.copy(name = value))
                        }
                    }
                    showMemberEditor = false
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
                    deletingGroup = null
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            splitDao.deleteByGroup(group.id)
                            expenseDao.deleteByGroup(group.id)
                            memberDao.deleteByGroup(group.id)
                            groupDao.delete(group)
                        }
                        if (activeGroupId == group.id) activeGroupId = null
                        refresh++
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingGroup = null }) { Text("Cancel") }
            }
        )
    }

    deletingMember?.let { member ->
        AlertDialog(
            onDismissRequest = { deletingMember = null },
            title = { Text("Remove member?") },
            text = { Text("Old expense splits keep the saved member name and amount.") },
            confirmButton = {
                TextButton(onClick = {
                    deletingMember = null
                    scope.launch {
                        withContext(Dispatchers.IO) { memberDao.delete(member) }
                        refresh++
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
                            splitDao.deleteByExpense(expense.id)
                            expenseDao.delete(expense)
                        }
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
            onDismiss = { showExpenseEditor = false },
            onSave = { title, amount, category, note, mode, drafts ->
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
                                    category = category,
                                    note = note,
                                    splitMode = mode
                                )
                            )
                        } else {
                            expenseDao.update(
                                old.copy(
                                    title = title,
                                    amount = amount,
                                    category = category,
                                    note = note,
                                    splitMode = mode
                                )
                            )
                            splitDao.deleteByExpense(old.id)
                            old.id
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
    onDelete: () -> Unit
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
    onDelete: (() -> Unit)? = null
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

                if (onDelete != null) {
                    OutlinedButton(
                        onClick = onDelete,
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
            Button(
                onClick = {
                    if (value.trim().isNotEmpty()) onSave(value.trim())
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (onDelete != null) "Save Changes" else "Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun GroupExpenseEditorDialog(
    members: List<SharingMemberEntity>,
    initial: GroupExpenseEntity?,
    initialSplits: List<GroupExpenseSplitEntity>,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, String, List<SplitDraft>) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var amountText by remember(initial?.id) {
        mutableStateOf(initial?.amount?.let { decimal(it) } ?: "")
    }
    var category by remember(initial?.id) { mutableStateOf(initial?.category ?: "General") }
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
    val shares = remember(initial?.id) {
        mutableStateMapOf<Long, String>().apply {
            initialSplits.forEach { put(it.memberId, decimal(it.shareUnits)) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add group expense" else "Edit group expense") },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Expense title") }, singleLine = true
                )
                OutlinedTextField(
                    value = amountText, onValueChange = { amountText = it },
                    label = { Text("Total amount") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = category, onValueChange = { category = it },
                    label = { Text("Category") }, singleLine = true
                )
                OutlinedTextField(
                    value = note, onValueChange = { note = it },
                    label = { Text("Note (optional)") }
                )

                Text("Split between", fontWeight = FontWeight.Bold)
                listOf("Equal", "Custom", "Share").forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clickable { mode = option },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = mode == option, onClick = { mode = option })
                        Column {
                            Text(option)
                            Text(
                                when (option) {
                                    "Equal" -> "Divide equally among selected members"
                                    "Custom" -> "Enter each member's amount"
                                    else -> "Enter shares; amounts are calculated proportionally"
                                },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Text("Members", fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    members.forEach { member ->
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
                                    label = { Text("Amount") },
                                    singleLine = true,
                                    modifier = Modifier.width(112.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            } else if (selected.contains(member.id) && mode == "Share") {
                                OutlinedTextField(
                                    value = shares[member.id] ?: "1",
                                    onValueChange = { shares[member.id] = it },
                                    label = { Text("Shares") },
                                    singleLine = true,
                                    modifier = Modifier.width(112.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
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
            Button(onClick = {
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
                onSave(title.trim(), totalCents / 100.0, category.trim().ifEmpty { "General" },
                    note.trim(), mode, drafts)
            }) { Text("Save expense") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun money(amount: Double): String = "₹" + String.format("%.2f", amount)
private fun decimal(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun Modifier.heightInCompat(height: androidx.compose.ui.unit.Dp): Modifier =
    this.height(height)
