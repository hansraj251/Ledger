package com.ledger.app.cloudgroup

interface CloudGroupRemoteDataSource {

    suspend fun currentUserId(): String?

    suspend fun fetchGroups(): List<CloudGroup>

    suspend fun insertGroup(name: String, ownerId: String): CloudGroup

    suspend fun updateGroupName(
        groupId: String,
        name: String
    ): String

    suspend fun deleteGroup(groupId: String): String

    suspend fun claimPendingMemberships(): Int

    suspend fun fetchMembers(groupId: String): List<CloudGroupMember>

    suspend fun addMember(
        groupId: String,
        memberName: String,
        memberEmail: String
    ): CloudGroupMemberAddResult

    suspend fun updateMemberName(
        groupId: String,
        memberId: String,
        memberName: String
    ): String

    suspend fun fetchGroupExpenses(
        groupId: String
    ): CloudGroupExpenseData

    suspend fun saveGroupExpense(
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
    ): String

    suspend fun deleteGroupExpense(
        groupId: String,
        expenseId: String
    ): String

    suspend fun removeMember(
        groupId: String,
        memberId: String
    ): String
}
