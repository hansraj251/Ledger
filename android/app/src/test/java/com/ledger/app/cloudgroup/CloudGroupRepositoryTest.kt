package com.ledger.app.cloudgroup

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class CloudGroupRepositoryTest {

    private class FakeRemoteDataSource(
        var userId: String? = "user-123",
        var groups: List<CloudGroup> = emptyList()
    ) : CloudGroupRemoteDataSource {

        var insertedName: String? = null
        var insertedOwnerId: String? = null
        var updatedGroupName: String? = null
        var updatedGroupId: String? = null
        var deletedGroupId: String? = null
        var claimCalls = 0
        var fetchedGroupId: String? = null
        var addedMemberName: String? = null
        var addedMemberEmail: String? = null
        var updatedMemberName: String? = null
        var settlementPermissionMemberId: String? = null
        var settlementPermissionValue: Boolean? = null
        var removedMemberId: String? = null

        override suspend fun currentUserId(): String? = userId

        override suspend fun fetchGroups(): List<CloudGroup> = groups

        override suspend fun insertGroup(
            name: String,
            ownerId: String
        ): CloudGroup {
            insertedName = name
            insertedOwnerId = ownerId
            return CloudGroup(
                id = "group-123",
                name = name,
                ownerId = ownerId,
                createdAt = "2026-11-23T00:00:00Z"
            )
        }

        override suspend fun updateGroupName(
            groupId: String,
            name: String
        ): String {
            updatedGroupId = groupId
            updatedGroupName = name
            return groupId
        }

        override suspend fun deleteGroup(groupId: String): String {
            deletedGroupId = groupId
            return groupId
        }

        override suspend fun claimPendingMemberships(): Int {
            claimCalls++
            return 2
        }

        override suspend fun fetchMembers(
            groupId: String
        ): List<CloudGroupMember> {
            fetchedGroupId = groupId
            return emptyList()
        }

        override suspend fun addMember(
            groupId: String,
            memberName: String,
            memberEmail: String
        ): CloudGroupMemberAddResult {
            addedMemberName = memberName
            addedMemberEmail = memberEmail
            return CloudGroupMemberAddResult(
                memberId = "member-123",
                memberStatus = "pending",
                linkedUserId = null
            )
        }

        override suspend fun updateMemberName(
            groupId: String,
            memberId: String,
            memberName: String
        ): String {
            updatedMemberName = memberName
            return memberId
        }

        override suspend fun updateOwnSettlementPermission(
            groupId: String,
            memberId: String,
            allow: Boolean
        ): Boolean {
            settlementPermissionMemberId = memberId
            settlementPermissionValue = allow
            return true
        }

        var savedExpenseTitle: String? = null
        var submittedApprovalTitle: String? = null
        var submittedApprovalRequestId: String? = null
        var approvalResponse: String? = null
        var deletedExpenseId: String? = null

        override suspend fun fetchGroupExpenses(groupId: String): CloudGroupExpenseData {
            fetchedGroupId = groupId
            return CloudGroupExpenseData(emptyList(), emptyList(), emptyList())
        }

        var fetchedSettlementGroupId: String? = null
        var insertedSettlementAmount: Double? = null

        override suspend fun fetchGroupSettlementTransactions(
            groupId: String
        ): List<CloudGroupSettlementTransaction> {
            fetchedSettlementGroupId = groupId
            return emptyList()
        }

        override suspend fun insertGroupSettlementTransaction(
            groupId: String,
            fromMemberId: String,
            toMemberId: String,
            amount: Double,
            paymentType: String,
            settlementDateMs: Long
        ): CloudGroupSettlementTransaction {
            insertedSettlementAmount = amount
            return CloudGroupSettlementTransaction(
                id = "settlement-123",
                groupId = groupId,
                fromMemberId = fromMemberId,
                toMemberId = toMemberId,
                amount = amount,
                paymentType = paymentType,
                settlementDateMs = settlementDateMs,
                createdBy = "user-123",
                createdAt = "2026-11-24T00:00:00Z"
            )
        }

        override suspend fun saveGroupExpense(
            groupId: String,
            expenseId: String?,
            title: String,
            amount: Double,
            category: String,
            note: String,
            expenseDateMs: Long,
            splitMode: String,
            splits: List<CloudGroupExpenseSplitDraft>,
            payments: List<CloudGroupExpensePaymentDraft>
        ): String {
            savedExpenseTitle = title
            return expenseId ?: "expense-123"
        }

        override suspend fun submitGroupExpenseForApproval(
            groupId: String,
            requestId: String?,
            title: String,
            amount: Double,
            category: String,
            note: String,
            expenseDateMs: Long,
            splitMode: String,
            splits: List<CloudGroupExpenseSplitDraft>,
            payments: List<CloudGroupExpensePaymentDraft>
        ): String {
            submittedApprovalTitle = title
            submittedApprovalRequestId = requestId
            return requestId ?: "approval-request-123"
        }

        override suspend fun respondToGroupExpenseApproval(
            groupId: String,
            requestId: String,
            approve: Boolean,
            rejectionNote: String?
        ): String {
            approvalResponse = if (approve) "approved" else rejectionNote ?: "changes_required"
            return if (approve) "approved" else "changes_required"
        }

        override suspend fun deleteGroupExpense(groupId: String, expenseId: String): String {
            deletedExpenseId = expenseId
            return expenseId
        }

        override suspend fun removeMember(
            groupId: String,
            memberId: String
        ): String {
            removedMemberId = memberId
            return memberId
        }
    }

    @Test
    fun loadGroups_returnsRemoteGroups() = runBlocking {
        val expected = listOf(
            CloudGroup(
                id = "group-1",
                name = "Family",
                ownerId = "user-123",
                createdAt = "2026-11-23T00:00:00Z"
            )
        )
        val repository = CloudGroupRepository(
            FakeRemoteDataSource(groups = expected)
        )
        assertEquals(expected, repository.loadGroups())
    }

    @Test
    fun createGroup_trimsNameAndUsesAuthenticatedUser() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).createGroup(" Family Group ")
        assertEquals("Family Group", remote.insertedName)
        assertEquals("user-123", remote.insertedOwnerId)
        assertEquals("group-123", result.id)
    }

    @Test
    fun createGroup_rejectsBlankName() {
        val remote = FakeRemoteDataSource()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { CloudGroupRepository(remote).createGroup("   ") }
        }
        assertNull(remote.insertedName)
    }

    @Test
    fun createGroup_rejectsNameLongerThan120Characters() {
        val remote = FakeRemoteDataSource()
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(remote).createGroup("A".repeat(121))
            }
        }
        assertNull(remote.insertedName)
    }

    @Test
    fun createGroup_rejectsUnauthenticatedUser() {
        val remote = FakeRemoteDataSource(userId = null)
        assertThrows(IllegalStateException::class.java) {
            runBlocking { CloudGroupRepository(remote).createGroup("Family") }
        }
        assertNull(remote.insertedName)
    }

    @Test
    fun claimPendingMemberships_delegatesToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        assertEquals(2, CloudGroupRepository(remote).claimPendingMemberships())
        assertEquals(1, remote.claimCalls)
    }

    @Test
    fun loadMembers_passesGroupIdToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        CloudGroupRepository(remote).loadMembers("group-123")
        assertEquals("group-123", remote.fetchedGroupId)
    }

    @Test
    fun loadMembers_rejectsBlankGroupId() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(FakeRemoteDataSource()).loadMembers(" ")
            }
        }
    }

    @Test
    fun addMember_trimsNameAndNormalizesEmail() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).addMember(
            "group-123", " Raj ", " TEST@EXAMPLE.COM "
        )
        assertEquals("Raj", remote.addedMemberName)
        assertEquals("test@example.com", remote.addedMemberEmail)
        assertEquals("member-123", result.memberId)
        assertEquals("pending", result.memberStatus)
    }

    @Test
    fun addMember_rejectsInvalidEmail() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(FakeRemoteDataSource()).addMember(
                    "group-123", "Raj", "invalid-email"
                )
            }
        }
    }

    @Test
    fun updateMemberName_trimsName() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).updateMemberName(
            "group-123", "member-123", " New Name "
        )
        assertEquals("New Name", remote.updatedMemberName)
        assertEquals("member-123", result)
    }

    @Test
    fun updateOwnSettlementPermission_delegatesToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).updateOwnSettlementPermission(
            "group-123", "member-123", false
        )
        assertEquals(true, result)
        assertEquals("member-123", remote.settlementPermissionMemberId)
        assertEquals(false, remote.settlementPermissionValue)
    }

    @Test
    fun updateOwnSettlementPermission_rejectsBlankIds() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(FakeRemoteDataSource())
                    .updateOwnSettlementPermission(" ", "member-123", false)
            }
        }
    }

    @Test
    fun removeMember_delegatesToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).removeMember(
            "group-123", "member-123"
        )
        assertEquals("member-123", result)
        assertEquals("member-123", remote.removedMemberId)
    }

    @Test
    fun loadGroupExpenses_passesGroupIdToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        CloudGroupRepository(remote).loadGroupExpenses("group-123")
        assertEquals("group-123", remote.fetchedGroupId)
    }

    @Test
    fun saveGroupExpense_trimsTitleAndDelegates() = runBlocking {
        val remote = FakeRemoteDataSource()
        val splits = listOf(
            CloudGroupExpenseSplitDraft("member-1", "Raj", 100.0, 1.0)
        )
        val result = CloudGroupRepository(remote).saveGroupExpense(
            groupId = "group-123",
            expenseId = null,
            title = "  Dinner  ",
            amount = 100.0,
            category = "Cash",
            note = "",
            expenseDateMs = 1000L,
            splitMode = "Equal",
            splits = splits,
            payments = emptyList()
        )
        assertEquals("Dinner", remote.savedExpenseTitle)
        assertEquals("expense-123", result)
    }

    @Test
    fun saveGroupExpense_rejectsIncorrectSplitTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(FakeRemoteDataSource()).saveGroupExpense(
                    groupId = "group-123",
                    expenseId = null,
                    title = "Dinner",
                    amount = 100.0,
                    category = "Cash",
                    note = "",
                    expenseDateMs = 1000L,
                    splitMode = "Custom",
                    splits = listOf(
                        CloudGroupExpenseSplitDraft("m1", "Raj", 60.0, 1.0),
                        CloudGroupExpenseSplitDraft("m2", "Amit", 30.0, 1.0)
                    ),
                    payments = emptyList()
                )
            }
        }
    }

    @Test
    fun submitGroupExpenseForApproval_trimsTitleAndDelegates() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).submitGroupExpenseForApproval(
            groupId = "group-123",
            requestId = null,
            title = "  Dinner  ",
            amount = 100.0,
            category = "Cash",
            note = " note ",
            expenseDateMs = 1000L,
            splitMode = "Equal",
            splits = listOf(CloudGroupExpenseSplitDraft("member-1", "Raj", 100.0, 1.0)),
            payments = emptyList()
        )
        assertEquals("Dinner", remote.submittedApprovalTitle)
        assertNull(remote.submittedApprovalRequestId)
        assertEquals("approval-request-123", result)
    }

    @Test
    fun submitGroupExpenseForApproval_rejectsIncorrectSplitTotal() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                CloudGroupRepository(FakeRemoteDataSource()).submitGroupExpenseForApproval(
                    groupId = "group-123",
                    requestId = null,
                    title = "Dinner",
                    amount = 100.0,
                    category = "Cash",
                    note = "",
                    expenseDateMs = 1000L,
                    splitMode = "Custom",
                    splits = listOf(CloudGroupExpenseSplitDraft("m1", "Raj", 60.0, 1.0)),
                    payments = emptyList()
                )
            }
        }
    }

    @Test
    fun respondToGroupExpenseApproval_delegates() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).respondToGroupExpenseApproval(
            groupId = "group-123",
            requestId = "request-123",
            approve = false,
            rejectionNote = "  Incorrect amount  "
        )
        assertEquals("changes_required", result)
        assertEquals("Incorrect amount", remote.approvalResponse)
    }

    @Test
    fun deleteGroupExpense_delegatesToRemote() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).deleteGroupExpense(
            "group-123", "expense-123"
        )
        assertEquals("expense-123", result)
        assertEquals("expense-123", remote.deletedExpenseId)
    }


    @Test
    fun loadSettlementTransactions_usesRequestedGroup() = runBlocking {
        val remote = FakeRemoteDataSource()
        CloudGroupRepository(remote).loadSettlementTransactions("group-123")
        assertEquals("group-123", remote.fetchedSettlementGroupId)
    }

    @Test
    fun saveSettlementTransaction_validatesAndRoundsAmount() = runBlocking {
        val remote = FakeRemoteDataSource()
        val result = CloudGroupRepository(remote).saveSettlementTransaction(
            groupId = "group-123",
            fromMemberId = "member-1",
            toMemberId = "member-2",
            amount = 125.456,
            paymentType = "UPI",
            settlementDateMs = 1000L
        )

        assertEquals(125.46, remote.insertedSettlementAmount!!, 0.001)
        assertEquals("settlement-123", result.id)
    }

    @Test
    fun saveSettlementTransaction_rejectsSamePayerAndReceiver() {
        val repository = CloudGroupRepository(FakeRemoteDataSource())

        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.saveSettlementTransaction(
                    groupId = "group-123",
                    fromMemberId = "member-1",
                    toMemberId = "member-1",
                    amount = 100.0,
                    paymentType = "Cash",
                    settlementDateMs = 1000L
                )
            }
        }
    }

}
