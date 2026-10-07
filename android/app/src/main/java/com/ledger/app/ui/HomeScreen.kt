package com.ledger.app.ui
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.sp
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.foundation.layout.width

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.app.data.PartyEntity
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: LedgerViewModel,
    onPartyClick: (PartyEntity) -> Unit,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onRestoreClick: () -> Unit
) {
    var partyName by rememberSaveable {
        mutableStateOf("")
    }

    var partyMobile by rememberSaveable {
        mutableStateOf("")
    }

    var isSavingParty by rememberSaveable {
        mutableStateOf(false)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var showAddPartyDialog by remember {
        mutableStateOf(false)
    }

    var showMenu by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.loadParties()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Ledger",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = "Your business, organized.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        Surface(
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp,
                            shadowElevation = 1.dp
                        ) {
                            IconButton(
                                onClick = {
                                    showMenu = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More options"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = {
                                showMenu = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text("Sync to Google Drive")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDone,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onSyncClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Restore backup")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Storage,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onRestoreClick()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text("Profile & settings")
                                },
                                onClick = {
                                    showMenu = false
                                    onProfileClick()
                                }
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddPartyDialog = true
                },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonAdd,
                    contentDescription = "Add party"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = 18.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(17.dp)),
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search"
                        )
                    },
                    placeholder = {
                        Text("Search parties or mobile number")
                    },
                    shape = RoundedCornerShape(17.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your parties",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "${uiState.filteredParties.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (uiState.isLoading) {
                item {
                    LoadingState()
                }
            } else if (uiState.filteredParties.isEmpty()) {
                item {
                    EmptyPartyState(
                        hasSearch = uiState.searchQuery.isNotBlank(),
                        onAddParty = {
                            showAddPartyDialog = true
                        }
                    )
                }
            } else {
                items(
                    items = uiState.filteredParties,
                    key = {
                        it.id
                    }
                ) { party ->
                    PartyCard(
                        party = party,
                        onClick = {
                            onPartyClick(party)
                        }
                    )
                }
            }
        }
    }

    if (showAddPartyDialog) {
        Dialog(
            onDismissRequest = {
                if (!isSavingParty) {
                    showAddPartyDialog = false
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            modifier = Modifier.size(54.dp),
                            shape = RoundedCornerShape(17.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(27.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "PARTY DETAILS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = partyName,
                        onValueChange = {
                            partyName = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSavingParty,
                        label = {
                            Text("Party name")
                        },
                        placeholder = {
                            Text("e.g. Sharma Traders")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = partyMobile,
                        onValueChange = {
                            if (it.length <= 15) {
                                partyMobile = it
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isSavingParty,
                        label = {
                            Text("Mobile number")
                        },
                        placeholder = {
                            Text("Optional")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = null
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showAddPartyDialog = false
                            },
                            modifier = Modifier
                                .weight(0.85f)
                                .height(52.dp),
                            enabled = !isSavingParty,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = {
                                if (partyName.isNotBlank()) {
                                    isSavingParty = true

                                    scope.launch {
                                        viewModel.addParty(
                                            partyName.trim(),
                                            partyMobile.trim()
                                        )

                                        partyName = ""
                                        partyMobile = ""
                                        isSavingParty = false
                                        showAddPartyDialog = false
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1.15f)
                                .height(52.dp),
                            enabled = partyName.isNotBlank() && !isSavingParty,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSavingParty) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Create",
                                    maxLines = 1,
                                    softWrap = false,
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

@Composable
private fun PartyCard(
    party: PartyEntity,
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
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initials = party.name
                .trim()
                .split(" ")
                .filter {
                    it.isNotBlank()
                }
                .take(2)
                .joinToString("") {
                    it.first().uppercase()
                }

            Surface(
                modifier = Modifier.size(50.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials.ifBlank { "P" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(
                modifier = Modifier.size(14.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = party.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = party.mobile.ifBlank { "No mobile number added" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )


            }

        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun EmptyPartyState(
    hasSearch: Boolean,
    onAddParty: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(66.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (hasSearch) {
                            Icons.Outlined.Search
                        } else {
                            Icons.Outlined.Group
                        },
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            Text(
                text = if (hasSearch) {
                    "No matching parties"
                } else {
                    "Your ledger is empty"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = if (hasSearch) {
                    "Try another name or mobile number."
                } else {
                    "Add your first customer or supplier to get started."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!hasSearch) {
                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = onAddParty,
                    shape = RoundedCornerShape(13.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PersonAdd,
                        contentDescription = null
                    )

                    Spacer(
                        modifier = Modifier.size(7.dp)
                    )

                    Text("Add party")
                }
            }
        }
    }
}

@Composable
private fun AddPartyDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    errorMessage: String
) {
    var name by remember {
        mutableStateOf("")
    }

    var mobile by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Column {
                Text(
                    text = "Add new party",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(
                    modifier = Modifier.height(5.dp)
                )

                Text(
                    text = "Create a customer or supplier ledger.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Party name")
                    },
                    shape = RoundedCornerShape(15.dp)
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = {
                        mobile = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text("Mobile number")
                    },
                    shape = RoundedCornerShape(15.dp)
                )

                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAdd(name, mobile)
                },
                enabled = name.trim().isNotEmpty(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create party")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
}
