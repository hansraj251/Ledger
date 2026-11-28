package com.ledger.app.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.app.data.*
import com.ledger.app.reminders.WriteReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val writeRepeatOptions = listOf("Never", "Daily", "Weekly", "Monthly", "Yearly")
private val writePalette = listOf(
    0xFFFFFFFF, 0xFFFFF3CD, 0xFFDFF5E1,
    0xFFDDEBFF, 0xFFF3E2FF, 0xFFFFE1E1
)
private val writeBg = Color(0xFFF6F7FB)
private val writeBlue = Color(0xFF246BCE)
private val writeText = Color(0xFF172033)
private val writeMuted = Color(0xFF697386)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WriteScreen(
    isPremium: Boolean,
    onHomeClick: () -> Unit,
    onExpensesClick: () -> Unit,
    onCalculateClick: () -> Unit,
    onDataChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember(context) { LedgerDatabaseProvider.get(context) }
    val scope = rememberCoroutineScope()
    val notes by db.writeNoteDao().observeAll().collectAsState(initial = emptyList())
    val tasks by db.writeTaskDao().observeAll().collectAsState(initial = emptyList())

    var section by remember { mutableStateOf("Notes") }
    var search by remember { mutableStateOf("") }
    var editingNote by remember { mutableStateOf<WriteNoteEntity?>(null) }
    var editingTask by remember { mutableStateOf<WriteTaskEntity?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    var deleteNoteTarget by remember { mutableStateOf<WriteNoteEntity?>(null) }
    var deleteTaskTarget by remember { mutableStateOf<WriteTaskEntity?>(null) }
    var selectedNoteIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedTaskIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

    val selectedCount = selectedNoteIds.size + selectedTaskIds.size
    val selectionMode = selectedCount > 0

    fun toggleNoteSelection(id: Long) {
        selectedNoteIds = if (id in selectedNoteIds) selectedNoteIds - id else selectedNoteIds + id
    }

    fun toggleTaskSelection(id: Long) {
        selectedTaskIds = if (id in selectedTaskIds) selectedTaskIds - id else selectedTaskIds + id
    }

    fun clearSelection() {
        selectedNoteIds = emptySet()
        selectedTaskIds = emptySet()
    }

    BackHandler(enabled = editorOpen) {
        editorOpen = false
        editingNote = null
        editingTask = null
    }
    BackHandler(enabled = !editorOpen) {
        if (selectionMode) clearSelection() else onHomeClick()
    }

    if (editorOpen) {
        if (section == "Notes") {
            NoteEditorPage(
                existing = editingNote,
                onBack = { editorOpen = false; editingNote = null },
                onSave = { note ->
                    scope.launch(Dispatchers.IO) {
                        val id = db.writeNoteDao().upsert(note)
                        val savedId = if (note.id == 0L) id else note.id
                        WriteReminderScheduler.schedule(context, savedId, "note", note.title, note.reminderAt, note.repeatRule)
                        onDataChanged()
                    }
                    editorOpen = false
                }
            )
        } else {
            TaskEditorPage(
                existing = editingTask,
                onBack = { editorOpen = false; editingTask = null },
                onSave = { task ->
                    scope.launch(Dispatchers.IO) {
                        val id = db.writeTaskDao().upsert(task)
                        val savedId = if (task.id == 0L) id else task.id
                        WriteReminderScheduler.schedule(context, savedId, "task", task.title, task.reminderAt, task.repeatRule)
                        onDataChanged()
                    }
                    editorOpen = false
                }
            )
        }
    } else {
        Scaffold(
            containerColor = Color.White,
            bottomBar = {
                LedgerBottomNavigation(
                    selectedTab = "write",
                    isPremium = isPremium,
                    onPartiesClick = onHomeClick,
                    onExpensesClick = onExpensesClick,
                    onCalculateClick = onCalculateClick,
                    onWriteClick = {}
                )
            },
            floatingActionButton = {
                if (!selectionMode) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            editingNote = null
                            editingTask = null
                            editorOpen = true
                        },
                        containerColor = writeBlue,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(17.dp),
                        icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                        text = { Text(if (section == "Notes") "New Note" else "New Task", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        ) { inset ->
            Column(
                Modifier.fillMaxSize().padding(inset).padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text(if (section == "Notes") "Search notes" else "Search tasks") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = writeMuted) },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = writeBlue,
                            unfocusedBorderColor = Color(0xFFE1E5ED)
                        )
                    )
                    if (selectedCount > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            selectedCount.toString(),
                            color = writeBlue,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelMedium
                        )
                        IconButton(onClick = { showBulkDeleteDialog = true }) {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = "Delete selected",
                                tint = Color(0xFFD64545)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(15.dp)).padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Notes", "Tasks").forEach { tab ->
                        val selected = section == tab
                        Box(
                            Modifier.weight(1f)
                                .background(if (selected) Color(0xFFE7F0FF) else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { section = tab; search = "" }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tab, color = if (selected) writeBlue else writeMuted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (section == "Notes") {
                    val filtered = notes.filter {
                        search.isBlank() || it.title.contains(search, true) || it.content.contains(search, true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyWriteState(if (search.isBlank()) "No notes yet" else "No matching notes", if (search.isBlank()) "Create a note to keep important information handy" else "Try a different search")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(filtered, key = { it.id }) { note ->
                                val isSelected = note.id in selectedNoteIds
                                Card(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                toggleNoteSelection(note.id)
                                            } else {
                                                editingNote = note
                                                editorOpen = true
                                            }
                                        },
                                        onLongClick = { toggleNoteSelection(note.id) }
                                    ),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE7F0FF) else Color(note.color)
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) writeBlue else Color(0xFFE5E8EF)
                                    ),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Column(Modifier.padding(start = 15.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (selectionMode) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = { toggleNoteSelection(note.id) }
                                                )
                                            }
                                            Text(
                                                note.title.ifBlank { "Untitled note" },
                                                Modifier.weight(1f),
                                                color = writeText,
                                                fontWeight = FontWeight.SemiBold,
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            if (!selectionMode) {
                                                IconButton(onClick = {
                                                    scope.launch(Dispatchers.IO) {
                                                        db.writeNoteDao().upsert(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
                                                        onDataChanged()
                                                    }
                                                }) {
                                                    Icon(
                                                        if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                                        contentDescription = if (note.isPinned) "Unpin note" else "Pin note",
                                                        tint = if (note.isPinned) writeBlue else writeMuted
                                                    )
                                                }
                                            }
                                        }
                                        if (note.content.isNotBlank()) {
                                            Text(note.content, color = writeText, maxLines = 4, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            note.reminderAt?.let {
                                                Icon(Icons.Outlined.Alarm, null, tint = writeBlue, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    "${formatWriteReminder(it)} · ${note.repeatRule}",
                                                    color = writeMuted,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            } ?: Spacer(Modifier.weight(1f))
                                            if (!selectionMode) {
                                                IconButton(
                                                    onClick = { deleteNoteTarget = note },
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Delete note",
                                                        tint = Color(0xFFD64545)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val filtered = tasks.filter {
                        search.isBlank() || it.title.contains(search, true) || it.details.contains(search, true)
                    }
                    if (filtered.isEmpty()) {
                        EmptyWriteState(if (search.isBlank()) "No tasks yet" else "No matching tasks", if (search.isBlank()) "Add a task and keep track of what needs to be done" else "Try a different search")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(9.dp),
                            contentPadding = PaddingValues(bottom = 90.dp)
                        ) {
                            items(filtered, key = { it.id }) { task ->
                                val isSelected = task.id in selectedTaskIds
                                Card(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                toggleTaskSelection(task.id)
                                            } else {
                                                editingTask = task
                                                editorOpen = true
                                            }
                                        },
                                        onLongClick = { toggleTaskSelection(task.id) }
                                    ),
                                    shape = RoundedCornerShape(17.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFFE7F0FF) else Color.White
                                    ),
                                    border = BorderStroke(
                                        if (isSelected) 2.dp else 1.dp,
                                        if (isSelected) writeBlue else Color(0xFFE8EBF1)
                                    ),
                                    elevation = CardDefaults.cardElevation(1.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (selectionMode) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { toggleTaskSelection(task.id) }
                                            )
                                        } else if (task.repeatRule == "Never") {
                                            Checkbox(
                                                checked = task.isCompleted,
                                                onCheckedChange = { checked ->
                                                    scope.launch(Dispatchers.IO) {
                                                        db.writeTaskDao().setCompleted(task.id, checked)
                                                        onDataChanged()
                                                    }
                                                }
                                            )
                                        } else {
                                            Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Outlined.Alarm, null, tint = writeBlue)
                                            }
                                        }
                                        Column(Modifier.weight(1f).padding(start = 5.dp, end = 6.dp)) {
                                            Text(task.title, color = if (task.isCompleted) writeMuted else writeText, fontWeight = FontWeight.SemiBold)
                                            if (task.details.isNotBlank()) Text(task.details, color = writeMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                                            task.reminderAt?.let { Text("${formatWriteReminder(it)} · ${task.repeatRule}", color = writeMuted, style = MaterialTheme.typography.labelSmall) }
                                            if (task.repeatRule != "Never") Text("Repeats ${task.repeatRule.lowercase()}", color = writeBlue, style = MaterialTheme.typography.labelSmall)
                                        }
                                        if (!selectionMode) {
                                            IconButton(onClick = { deleteTaskTarget = task }) {
                                                Icon(Icons.Outlined.Delete, "Delete task", tint = Color(0xFFD64545))
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

        if (showBulkDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showBulkDeleteDialog = false },
                containerColor = Color.White,
                title = { Text("Delete selected items?", color = writeText, fontWeight = FontWeight.SemiBold) },
                text = {
                    Text(
                        "Permanently delete $selectedCount selected ${if (selectedCount == 1) "item" else "items"}? This cannot be undone.",
                        color = writeMuted
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBulkDeleteDialog = false
                        val noteIds = selectedNoteIds
                        val taskIds = selectedTaskIds
                        clearSelection()
                        scope.launch(Dispatchers.IO) {
                            noteIds.forEach { id ->
                                db.writeNoteDao().delete(id)
                                WriteReminderScheduler.cancel(context, id, "note")
                            }
                            taskIds.forEach { id ->
                                db.writeTaskDao().delete(id)
                                WriteReminderScheduler.cancel(context, id, "task")
                            }
                            onDataChanged()
                        }
                    }) {
                        Text("Delete", color = Color(0xFFD64545), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBulkDeleteDialog = false }) {
                        Text("Cancel", color = writeBlue)
                    }
                },
                shape = RoundedCornerShape(22.dp)
            )
        }

        deleteNoteTarget?.let { note ->
            AlertDialog(
                onDismissRequest = { deleteNoteTarget = null },
                containerColor = Color.White,
                title = { Text("Delete note?", color = writeText, fontWeight = FontWeight.SemiBold) },
                text = { Text("“${note.title.ifBlank { "Untitled note" }}” will be permanently deleted.", color = writeMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        deleteNoteTarget = null
                        scope.launch(Dispatchers.IO) {
                            db.writeNoteDao().delete(note.id)
                            WriteReminderScheduler.cancel(context, note.id, "note")
                            onDataChanged()
                        }
                    }) { Text("Delete", color = Color(0xFFD64545), fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = { TextButton(onClick = { deleteNoteTarget = null }) { Text("Cancel", color = writeBlue) } },
                shape = RoundedCornerShape(22.dp)
            )
        }
        deleteTaskTarget?.let { task ->
            AlertDialog(
                onDismissRequest = { deleteTaskTarget = null },
                containerColor = Color.White,
                title = { Text("Delete task?", color = writeText, fontWeight = FontWeight.SemiBold) },
                text = { Text("“${task.title}” will be permanently deleted.", color = writeMuted) },
                confirmButton = {
                    TextButton(onClick = {
                        deleteTaskTarget = null
                        scope.launch(Dispatchers.IO) {
                            db.writeTaskDao().delete(task.id)
                            WriteReminderScheduler.cancel(context, task.id, "task")
                            onDataChanged()
                        }
                    }) { Text("Delete", color = Color(0xFFD64545), fontWeight = FontWeight.SemiBold) }
                },
                dismissButton = { TextButton(onClick = { deleteTaskTarget = null }) { Text("Cancel", color = writeBlue) } },
                shape = RoundedCornerShape(22.dp)
            )
        }
    }
}

@Composable
private fun EmptyWriteState(title: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().padding(top = 72.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = writeText, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = writeMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun formatWriteReminder(time: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(time))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSettings(
    initialAt: Long?,
    initialRepeat: String,
    onChange: (Long?, String) -> Unit
) {
    val context = LocalContext.current
    var at by remember { mutableStateOf(initialAt) }
    var repeat by remember { mutableStateOf(initialRepeat) }
    var repeatExpanded by remember { mutableStateOf(false) }

    fun updateDate(year: Int, month: Int, day: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = at ?: System.currentTimeMillis()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            if (at == null) {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
            }
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        at = cal.timeInMillis
        onChange(at, repeat)
    }

    Column(
        Modifier.fillMaxWidth().background(Color(0xFFF6F8FC), RoundedCornerShape(15.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Reminder settings", color = writeText, fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = at ?: System.currentTimeMillis() }
                    DatePickerDialog(context, { _, y, m, d -> updateDate(y, m, d) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (at == null) "Choose date" else SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(at!!))) }
            OutlinedButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = at ?: System.currentTimeMillis()
                        if (at == null) { set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0) }
                    }
                    TimePickerDialog(context, { _, h, m ->
                        val next = Calendar.getInstance().apply {
                            timeInMillis = at ?: System.currentTimeMillis()
                            if (at == null) {
                                set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
                                set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
                                set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
                            }
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        at = next.timeInMillis
                        onChange(at, repeat)
                    }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (at == null) "Time · 9:00 AM" else SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(at!!))) }
        }
        ExposedDropdownMenuBox(expanded = repeatExpanded, onExpandedChange = { repeatExpanded = it }) {
            OutlinedTextField(
                value = repeat,
                onValueChange = {},
                readOnly = true,
                label = { Text("Repeat") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(repeatExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                shape = RoundedCornerShape(13.dp)
            )
            ExposedDropdownMenu(
                expanded = repeatExpanded,
                onDismissRequest = { repeatExpanded = false },
                modifier = Modifier.background(Color.White, RoundedCornerShape(14.dp))
            ) {
                writeRepeatOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option, color = if (repeat == option) writeBlue else writeText, fontWeight = if (repeat == option) FontWeight.SemiBold else FontWeight.Normal) }, onClick = {
                        repeat = option
                        repeatExpanded = false
                        onChange(at, repeat)
                    })
                }
            }
        }
        if (at != null) {
            TextButton(onClick = { at = null; repeat = "Never"; onChange(null, repeat) }) {
                Text("Remove reminder", color = writeMuted)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditorPage(
    existing: WriteNoteEntity?,
    onBack: () -> Unit,
    onSave: (WriteNoteEntity) -> Unit
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var body by remember(existing?.id) { mutableStateOf(existing?.content ?: "") }
    var pinned by remember(existing?.id) { mutableStateOf(existing?.isPinned ?: false) }
    var color by remember(existing?.id) { mutableLongStateOf(existing?.color ?: writePalette.first()) }
    var reminder by remember(existing?.id) { mutableStateOf(existing?.reminderAt) }
    var repeat by remember(existing?.id) { mutableStateOf(existing?.repeatRule ?: "Never") }
    var reminderExpanded by remember { mutableStateOf(reminder != null) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Note" else "Edit Note", color = writeText, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = writeText) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        onSave(WriteNoteEntity(
                            id = existing?.id ?: 0,
                            title = title.trim(),
                            content = body,
                            color = color,
                            isPinned = pinned,
                            reminderAt = reminder,
                            repeatRule = repeat,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        ))
                    },
                    enabled = title.isNotBlank() || body.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = writeBlue)
                ) { Text("Save Note", fontWeight = FontWeight.SemiBold) }
            }
        }
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).padding(horizontal = 18.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Note title") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(14.dp), colors = editorFieldColors()
            )
            OutlinedTextField(
                value = body, onValueChange = { body = it },
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 170.dp),
                placeholder = { Text("Start writing...") },
                shape = RoundedCornerShape(14.dp), colors = editorFieldColors(),
                minLines = 7
            )
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Note colour", color = writeText, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    writePalette.forEach { swatch ->
                        val selected = color == swatch
                        Box(
                            Modifier.size(37.dp).background(Color(swatch), CircleShape)
                                .clickable { color = swatch },
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.Transparent,
                                border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) writeBlue else Color(0xFFD4D9E2))
                            ) {}
                            if (selected) Icon(Icons.Outlined.Check, null, tint = writeBlue, modifier = Modifier.size(19.dp))
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Pin note", Modifier.weight(1f), color = writeText, fontWeight = FontWeight.Medium)
                Switch(checked = pinned, onCheckedChange = { pinned = it })
            }
            OutlinedButton(
                onClick = { reminderExpanded = !reminderExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE1E5ED))
            ) {
                Icon(Icons.Outlined.Alarm, null, tint = writeBlue)
                Spacer(Modifier.width(8.dp))
                Text(if (reminder == null) "Add reminder" else "Reminder · ${formatWriteReminder(reminder!!)}", color = writeText)
            }
            if (reminderExpanded) {
                ReminderSettings(reminder, repeat) { at, rule -> reminder = at; repeat = rule }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorPage(
    existing: WriteTaskEntity?,
    onBack: () -> Unit,
    onSave: (WriteTaskEntity) -> Unit
) {
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var details by remember(existing?.id) { mutableStateOf(existing?.details ?: "") }
    var reminder by remember(existing?.id) { mutableStateOf(existing?.reminderAt) }
    var repeat by remember(existing?.id) { mutableStateOf(existing?.repeatRule ?: "Never") }
    var reminderExpanded by remember { mutableStateOf(reminder != null) }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New Task" else "Edit Task", color = writeText, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back", tint = writeText) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        onSave(WriteTaskEntity(
                            id = existing?.id ?: 0,
                            title = title.trim(),
                            details = details,
                            isCompleted = if (repeat == "Never") existing?.isCompleted ?: false else false,
                            reminderAt = reminder,
                            repeatRule = repeat,
                            createdAt = existing?.createdAt ?: System.currentTimeMillis()
                        ))
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp).height(52.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = writeBlue)
                ) { Text("Save Task", fontWeight = FontWeight.SemiBold) }
            }
        }
    ) { inset ->
        Column(
            Modifier.fillMaxSize().padding(inset).padding(horizontal = 18.dp).padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            OutlinedTextField(
                value = title, onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Task title") },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                shape = RoundedCornerShape(14.dp), colors = editorFieldColors()
            )
            OutlinedTextField(
                value = details, onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                placeholder = { Text("Add details (optional)") },
                shape = RoundedCornerShape(14.dp), colors = editorFieldColors(),
                minLines = 5
            )
            OutlinedButton(
                onClick = { reminderExpanded = !reminderExpanded },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0xFFE1E5ED))
            ) {
                Icon(Icons.Outlined.Alarm, null, tint = writeBlue)
                Spacer(Modifier.width(8.dp))
                Text(if (reminder == null) "Add reminder" else "Reminder · ${formatWriteReminder(reminder!!)}", color = writeText)
            }
            if (reminderExpanded) {
                ReminderSettings(reminder, repeat) { at, rule -> reminder = at; repeat = rule }
            }
            if (repeat != "Never") {
                Text("Repeating tasks do not have a completion checkbox.", color = writeBlue, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun editorFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = writeBlue,
    unfocusedBorderColor = Color(0xFFE1E5ED)
)
