package com.ledger.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.app.data.SharingGroupEntity
import com.ledger.app.data.SharingMemberEntity

@Composable
fun GroupReportSelectionScreen(
    groups: List<SharingGroupEntity>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onGroupClick: (SharingGroupEntity) -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            ReportPageHeader(
                title = "Group Reports",
                subtitle = "Choose a group to continue",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            groups.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No groups available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a group to view group reports.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = groups, key = { it.id }) { group ->
                    ReportOptionCard(
                        letter = group.name.firstOrNull()?.toString() ?: "G",
                        title = group.name,
                        subtitle = "View group reports",
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupReportModeSelectionScreen(
    groupName: String,
    onBack: () -> Unit,
    onAllMembersClick: () -> Unit,
    onOneMemberClick: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            ReportPageHeader(
                title = groupName,
                subtitle = "Select a group report",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReportOptionCard(
                letter = "A",
                title = "All Member Report",
                subtitle = "View the complete group's expense report.",
                onClick = onAllMembersClick
            )
            ReportOptionCard(
                letter = "M",
                title = "One Member Report",
                subtitle = "Choose a member to view their expense report.",
                onClick = onOneMemberClick
            )
        }
    }
}

@Composable
fun GroupMemberReportSelectionScreen(
    members: List<SharingMemberEntity>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onMemberClick: (SharingMemberEntity) -> Unit
) {
    BackHandler { onBack() }

    val selectableMembers = members.filterNot {
        it.name.trim().equals("You", ignoreCase = true)
    }

    Scaffold(
        topBar = {
            ReportPageHeader(
                title = "One Member Report",
                subtitle = "Choose a group member",
                onBack = onBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            selectableMembers.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No members available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "This group has no selectable members.",
                        modifier = Modifier.padding(top = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Group Members",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(items = selectableMembers, key = { it.id }) { member ->
                    ReportOptionCard(
                        letter = member.name.firstOrNull()?.toString() ?: "M",
                        title = member.name,
                        subtitle = "View this member's report",
                        onClick = { onMemberClick(member) }
                    )
                }
            }
        }
    }
}
