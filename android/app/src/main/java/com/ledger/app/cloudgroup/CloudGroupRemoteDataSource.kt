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

    suspend fun updateOwnSettlementPermission(
        groupId: String,
        memberId: String,
        allow: Boolean
    ): Boolean

    suspend fun fetchGroupExpenses(
        groupId: String
    ): CloudGroupExpenseData

    suspend fun fetchGroupSettlementTransactions(
        groupId: String
    ): List<CloudGroupSettlementTransaction>

    suspend fun insertGroupSettlementTransaction(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        paymentType: String,
        settlementDateMs: Long
    ): CloudGroupSettlementTransaction

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

    suspend fun submitGroupExpenseForApproval(
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
        throw UnsupportedOperationException("Expense approvals are not supported by this data source.")
    }

    suspend fun respondToGroupExpenseApproval(
        groupId: String,
        requestId: String,
        approve: Boolean,
        rejectionNote: String?
    ): String {
        throw UnsupportedOperationException("Expense approvals are not supported by this data source.")
    }

    suspend fun submitGroupExpenseEditForApproval(
        groupId: String,
        expenseId: String,
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
        throw UnsupportedOperationException("Expense edit approvals are not supported by this data source.")
    }

    suspend fun submitGroupExpenseDeleteForApproval(
        groupId: String,
        expenseId: String,
        requestId: String? = null
    ): String {
        throw UnsupportedOperationException("Expense delete approvals are not supported by this data source.")
    }

    suspend fun cancelGroupExpenseApprovalRequest(
        groupId: String,
        requestId: String
    ): String {
        throw UnsupportedOperationException("Cancelling expense approval requests is not supported by this data source.")
    }

    suspend fun deleteGroupExpense(
        groupId: String,
        expenseId: String
    ): String

    suspend fun removeMember(
        groupId: String,
        memberId: String
    ): String
}
