package com.ledger.app.cloudgroup

class CloudGroupRepository(
    private val remote: CloudGroupRemoteDataSource
) {

    suspend fun currentUserId(): String? {
        return remote.currentUserId()
    }

    suspend fun loadGroups(): List<CloudGroup> {
        return remote.fetchGroups()
    }

    suspend fun createGroup(name: String): CloudGroup {
        val cleanName = name.trim()

        require(cleanName.isNotEmpty()) {
            "Group name cannot be empty."
        }

        require(cleanName.length <= 120) {
            "Group name cannot exceed 120 characters."
        }

        val ownerId = remote.currentUserId()
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                "Please connect your Google account before creating a group."
            )

        return remote.insertGroup(cleanName, ownerId)
    }

    suspend fun claimPendingMemberships(): Int {
        return remote.claimPendingMemberships()
    }

    suspend fun loadMembers(groupId: String): List<CloudGroupMember> {
        requireValidId(groupId, "Group")
        return remote.fetchMembers(groupId)
    }

    suspend fun addMember(
        groupId: String,
        memberName: String,
        memberEmail: String
    ): CloudGroupMemberAddResult {
        requireValidId(groupId, "Group")

        val cleanName = memberName.trim()
        require(cleanName.isNotEmpty()) {
            "Member name cannot be empty."
        }
        require(cleanName.length <= 120) {
            "Member name cannot exceed 120 characters."
        }

        val cleanEmail = memberEmail.trim().lowercase()
        require(
            cleanEmail.matches(
                Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
            )
        ) {
            "Please enter a valid email address."
        }

        return remote.addMember(groupId, cleanName, cleanEmail)
    }

    suspend fun updateMemberName(
        groupId: String,
        memberId: String,
        memberName: String
    ): String {
        requireValidId(groupId, "Group")
        requireValidId(memberId, "Member")

        val cleanName = memberName.trim()
        require(cleanName.isNotEmpty()) {
            "Member name cannot be empty."
        }
        require(cleanName.length <= 120) {
            "Member name cannot exceed 120 characters."
        }

        return remote.updateMemberName(groupId, memberId, cleanName)
    }

    suspend fun removeMember(
        groupId: String,
        memberId: String
    ): String {
        requireValidId(groupId, "Group")
        requireValidId(memberId, "Member")
        return remote.removeMember(groupId, memberId)
    }

    suspend fun loadGroupExpenses(
        groupId: String
    ): CloudGroupExpenseData {
        requireValidId(groupId, "Group")
        return remote.fetchGroupExpenses(groupId)
    }

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
    ): String {
        requireValidId(groupId, "Group")
        if (expenseId != null) requireValidId(expenseId, "Expense")

        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Expense title cannot be empty." }
        require(cleanTitle.length <= 200) { "Expense title is too long." }
        require(amount.isFinite() && amount > 0.0) {
            "Enter a valid expense amount."
        }
        require(category in setOf("Cash", "Online")) {
            "Invalid payment type."
        }
        require(splitMode in setOf("Equal", "Custom", "Share")) {
            "Invalid split mode."
        }
        require(expenseDateMs > 0L) { "Expense date is invalid." }
        require(splits.isNotEmpty()) { "At least one split is required." }

        fun toPaise(value: Double): Long {
            require(value.isFinite() && value >= 0.0) { "Invalid amount." }
            return java.math.BigDecimal.valueOf(value)
                .movePointRight(2)
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .longValueExact()
        }

        val totalPaise = toPaise(amount)

        require(splits.map { it.memberId }.distinct().size == splits.size) {
            "Duplicate members in expense splits."
        }
        require(splits.all {
            it.memberId.isNotBlank() &&
                it.memberName.isNotBlank() &&
                it.shareUnits.isFinite() &&
                it.shareUnits >= 0.0
        }) {
            "Invalid split details."
        }
        require(splits.sumOf { toPaise(it.amount) } == totalPaise) {
            "Split amounts must equal the expense amount."
        }

        require(payments.map { it.memberId }.distinct().size == payments.size) {
            "Duplicate members in expense payments."
        }
        require(payments.all {
            it.memberId.isNotBlank() &&
                it.memberName.isNotBlank() &&
                it.paymentType in setOf("Cash", "Online") &&
                it.amount.isFinite() &&
                it.amount > 0.0
        }) {
            "Invalid payment details."
        }
        require(
            payments.isEmpty() ||
                payments.sumOf { toPaise(it.amount) } == totalPaise
        ) {
            "Payment amounts must equal the expense amount."
        }

        return remote.saveGroupExpense(
            groupId = groupId,
            expenseId = expenseId,
            title = cleanTitle,
            amount = totalPaise / 100.0,
            category = category,
            note = note.trim(),
            expenseDateMs = expenseDateMs,
            splitMode = splitMode,
            splits = splits,
            payments = payments
        )
    }

    suspend fun deleteGroupExpense(
        groupId: String,
        expenseId: String
    ): String {
        requireValidId(groupId, "Group")
        requireValidId(expenseId, "Expense")
        return remote.deleteGroupExpense(groupId, expenseId)
    }

    private fun requireValidId(value: String, label: String) {
        require(value.isNotBlank()) {
            "$label ID cannot be empty."
        }
    }
}
