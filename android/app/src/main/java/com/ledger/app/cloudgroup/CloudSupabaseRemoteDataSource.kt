package com.ledger.app.cloudgroup

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.put

class CloudSupabaseRemoteDataSource(
    private val supabase: SupabaseClient = CloudSupabaseClient.client
) : CloudGroupRemoteDataSource {

    override suspend fun currentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun fetchGroups(): List<CloudGroup> {
        return supabase
            .from("cloud_groups")
            .select()
            .decodeList<CloudGroup>()
    }

    override suspend fun insertGroup(
        name: String,
        ownerId: String
    ): CloudGroup {
        val payload = CloudGroupInsert(name = name, ownerId = ownerId)

        return supabase
            .from("cloud_groups")
            .insert(payload) {
                select()
            }
            .decodeSingle<CloudGroup>()
    }

    override suspend fun updateGroupName(
        groupId: String,
        name: String
    ): String {
        return supabase.postgrest
            .rpc(
                "update_cloud_group_name",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_name", name)
                }
            )
            .decodeAs<String>()
    }

    override suspend fun deleteGroup(groupId: String): String {
        return supabase.postgrest
            .rpc(
                "delete_cloud_group",
                buildJsonObject {
                    put("p_group_id", groupId)
                }
            )
            .decodeAs<String>()
    }

    override suspend fun claimPendingMemberships(): Int {
        return supabase.postgrest
            .rpc("claim_pending_cloud_group_memberships", buildJsonObject {})
            .decodeAs<Int>()
    }

    suspend fun syncMyCloudProfileName(name: String): Int {
        return supabase.postgrest
            .rpc(
                "sync_my_cloud_profile_name",
                buildJsonObject {
                    put("p_name", name)
                }
            )
            .decodeAs<Int>()
    }

    override suspend fun fetchMembers(
        groupId: String
    ): List<CloudGroupMember> {
        return supabase
            .from("cloud_group_members")
            .select {
                filter {
                    eq("group_id", groupId)
                }
            }
            .decodeList<CloudGroupMember>()
    }

    override suspend fun addMember(
        groupId: String,
        memberName: String,
        memberEmail: String
    ): CloudGroupMemberAddResult {
        return supabase.postgrest
            .rpc(
                "add_cloud_group_member",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_member_name", memberName)
                    put("p_member_email", memberEmail)
                }
            )
            .decodeList<CloudGroupMemberAddResult>()
            .single()
    }

    override suspend fun updateMemberName(
        groupId: String,
        memberId: String,
        memberName: String
    ): String {
        return supabase.postgrest
            .rpc(
                "update_cloud_group_member",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_member_id", memberId)
                    put("p_member_name", memberName)
                }
            )
            .decodeAs<String>()
    }

    override suspend fun updateOwnSettlementPermission(
        groupId: String,
        memberId: String,
        allow: Boolean
    ): Boolean {
        return supabase.postgrest
            .rpc(
                "update_own_settlement_permission",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_member_id", memberId)
                    put("p_allow", allow)
                }
            )
            .decodeAs<Boolean>()
    }

    override suspend fun removeMember(
        groupId: String,
        memberId: String
    ): String {
        return supabase.postgrest
            .rpc(
                "remove_cloud_group_member",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_member_id", memberId)
                }
            )
            .decodeAs<String>()
    }

    override suspend fun fetchGroupExpenses(
        groupId: String
    ): CloudGroupExpenseData {
        val expenses = supabase
            .from("cloud_group_expenses")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupExpense>()

        val splits = supabase
            .from("cloud_group_expense_splits")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupExpenseSplit>()

        val payments = supabase
            .from("cloud_group_expense_payments")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupExpensePayment>()

        val settlements = supabase
            .from("cloud_group_settlements")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupSettlement>()

        val pendingRequests = supabase
            .from("cloud_group_expense_approval_requests")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupExpenseApprovalRequest>()
            .filter { it.status == "pending" || it.status == "changes_required" }

        val approvals = supabase
            .from("cloud_group_expense_approvals")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupExpenseApproval>()

        return CloudGroupExpenseData(
            expenses = expenses,
            splits = splits,
            payments = payments,
            settlements = settlements,
            pendingRequests = pendingRequests,
            approvals = approvals
        )
    }

    override suspend fun fetchGroupSettlementTransactions(
        groupId: String
    ): List<CloudGroupSettlementTransaction> {
        return supabase
            .from("cloud_group_settlement_transactions")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<CloudGroupSettlementTransaction>()
    }

    override suspend fun insertGroupSettlementTransaction(
        groupId: String,
        fromMemberId: String,
        toMemberId: String,
        amount: Double,
        paymentType: String,
        settlementDateMs: Long
    ): CloudGroupSettlementTransaction {
        val payload = CloudGroupSettlementTransactionInsert(
            groupId = groupId,
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            amount = amount,
            paymentType = paymentType,
            settlementDateMs = settlementDateMs
        )

        return supabase
            .from("cloud_group_settlement_transactions")
            .insert(payload) {
                select()
            }
            .decodeSingle<CloudGroupSettlementTransaction>()
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
        val splitJson = buildJsonArray {
            splits.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("share_units", item.shareUnits)
                })
            }
        }

        val paymentJson = buildJsonArray {
            payments.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("payment_type", item.paymentType)
                })
            }
        }

        val result = supabase.postgrest.rpc(
            "save_cloud_group_expense",
            buildJsonObject {
                put("p_group_id", groupId)
                if (expenseId == null) {
                    put("p_expense_id", JsonNull)
                } else {
                    put("p_expense_id", expenseId)
                }
                put("p_title", title)
                put("p_amount", amount)
                put("p_category", category)
                put("p_note", note)
                put("p_expense_date_ms", expenseDateMs)
                put("p_split_mode", splitMode)
                put("p_splits", splitJson)
                put("p_payments", paymentJson)
            }
        )

        return result.decodeAs<String>()
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
        val splitJson = buildJsonArray {
            splits.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("share_units", item.shareUnits)
                })
            }
        }
        val paymentJson = buildJsonArray {
            payments.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("payment_type", item.paymentType)
                })
            }
        }

        return supabase.postgrest.rpc(
            "submit_cloud_group_expense_for_approval",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_request_id", requestId?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_title", title)
                put("p_amount", amount)
                put("p_category", category)
                put("p_note", note)
                put("p_expense_date_ms", expenseDateMs)
                put("p_split_mode", splitMode)
                put("p_splits", splitJson)
                put("p_payments", paymentJson)
            }
        ).decodeAs<String>()
    }

    override suspend fun respondToGroupExpenseApproval(
        groupId: String,
        requestId: String,
        approve: Boolean,
        rejectionNote: String?
    ): String {
        return supabase.postgrest.rpc(
            "respond_cloud_group_expense_approval",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_request_id", requestId)
                put("p_approve", approve)
                put("p_rejection_note", rejectionNote?.let { JsonPrimitive(it) } ?: JsonNull)
            }
        ).decodeAs<String>()
    }

    override suspend fun submitGroupExpenseEditForApproval(
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
        val splitJson = buildJsonArray {
            splits.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("share_units", item.shareUnits)
                })
            }
        }
        val paymentJson = buildJsonArray {
            payments.forEach { item ->
                add(buildJsonObject {
                    put("member_id", item.memberId)
                    put("amount", item.amount)
                    put("payment_type", item.paymentType)
                })
            }
        }
        return supabase.postgrest.rpc(
            "submit_cloud_group_expense_edit_for_approval",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_expense_id", expenseId)
                put("p_request_id", requestId?.let { JsonPrimitive(it) } ?: JsonNull)
                put("p_title", title)
                put("p_amount", amount)
                put("p_category", category)
                put("p_note", note)
                put("p_expense_date_ms", expenseDateMs)
                put("p_split_mode", splitMode)
                put("p_splits", splitJson)
                put("p_payments", paymentJson)
            }
        ).decodeAs<String>()
    }

    override suspend fun submitGroupExpenseDeleteForApproval(
        groupId: String,
        expenseId: String,
        requestId: String?
    ): String {
        return supabase.postgrest.rpc(
            "submit_cloud_group_expense_delete_for_approval",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_expense_id", expenseId)
                put("p_request_id", requestId?.let { JsonPrimitive(it) } ?: JsonNull)
            }
        ).decodeAs<String>()
    }

    override suspend fun cancelGroupExpenseApprovalRequest(
        groupId: String,
        requestId: String
    ): String {
        return supabase.postgrest.rpc(
            "cancel_cloud_group_expense_approval_request",
            buildJsonObject {
                put("p_group_id", groupId)
                put("p_request_id", requestId)
            }
        ).decodeAs<String>()
    }

    override suspend fun deleteGroupExpense(
        groupId: String,
        expenseId: String
    ): String {
        return supabase.postgrest
            .rpc(
                "delete_cloud_group_expense",
                buildJsonObject {
                    put("p_group_id", groupId)
                    put("p_expense_id", expenseId)
                }
            )
            .decodeAs<String>()
    }

    @Serializable
    private data class CloudGroupSettlementTransactionInsert(
        @SerialName("group_id") val groupId: String,
        @SerialName("from_member_id") val fromMemberId: String,
        @SerialName("to_member_id") val toMemberId: String,
        val amount: Double,
        @SerialName("payment_type") val paymentType: String,
        @SerialName("settlement_date_ms") val settlementDateMs: Long
    )

    @Serializable
    private data class CloudGroupInsert(
        val name: String,
        @SerialName("owner_id") val ownerId: String
    )
}
