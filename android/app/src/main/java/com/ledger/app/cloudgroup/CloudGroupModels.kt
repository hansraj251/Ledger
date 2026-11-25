package com.ledger.app.cloudgroup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudGroupMember(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("member_name") val memberName: String,
    @SerialName("member_email") val memberEmail: String,
    val role: String,
    val status: String,
    @SerialName("added_by") val addedBy: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("allow_settlement_by_others") val allowSettlementByOthers: Boolean = true
)

@Serializable
data class CloudGroupExpense(
    val id: String,
    @SerialName("group_id") val groupId: String,
    val title: String,
    val amount: Double,
    val category: String = "",
    val note: String = "",
    @SerialName("expense_date_ms") val expenseDateMs: Long,
    @SerialName("split_mode") val splitMode: String,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at_ms") val createdAtMs: Long,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class CloudGroupExpenseSplit(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("member_name_snapshot") val memberNameSnapshot: String,
    val amount: Double,
    @SerialName("share_units") val shareUnits: Double = 1.0
)

@Serializable
data class CloudGroupExpensePayment(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("expense_id") val expenseId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("member_name_snapshot") val memberNameSnapshot: String,
    val amount: Double,
    @SerialName("paid_at_ms") val paidAtMs: Long,
    @SerialName("payment_type") val paymentType: String = "Cash"
)

@Serializable
data class CloudGroupSettlement(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("member_id") val memberId: String,
    @SerialName("member_name_snapshot") val memberNameSnapshot: String,
    val amount: Double,
    @SerialName("received_at_ms") val receivedAtMs: Long,
    @SerialName("payment_type") val paymentType: String = "Cash"
)

@Serializable
data class CloudGroupSettlementTransaction(
    val id: String,
    @SerialName("group_id") val groupId: String,
    @SerialName("from_member_id") val fromMemberId: String,
    @SerialName("to_member_id") val toMemberId: String,
    val amount: Double,
    @SerialName("payment_type") val paymentType: String,
    @SerialName("settlement_date_ms") val settlementDateMs: Long,
    @SerialName("created_by") val createdBy: String,
    @SerialName("created_at") val createdAt: String
)


@Serializable
data class CloudGroupMemberAddResult(
    @SerialName("member_id") val memberId: String,
    @SerialName("member_status") val memberStatus: String,
    @SerialName("linked_user_id") val linkedUserId: String? = null
)


@Serializable
data class CloudGroupExpenseData(
    val expenses: List<CloudGroupExpense>,
    val splits: List<CloudGroupExpenseSplit>,
    val payments: List<CloudGroupExpensePayment>,
    val settlements: List<CloudGroupSettlement> = emptyList()
)
