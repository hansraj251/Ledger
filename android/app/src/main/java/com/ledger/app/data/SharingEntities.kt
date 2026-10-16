package com.ledger.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sharing_groups")
data class SharingGroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sharing_members")
data class SharingMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val name: String,
    val defaultShareUnits: Double = 1.0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_expenses")
data class GroupExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val note: String = "",
    val expenseDate: Long = System.currentTimeMillis(),
    val splitMode: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_settlements")
data class GroupSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val memberId: Long,
    val memberNameSnapshot: String,
    val amount: Double,
    val receivedAt: Long = System.currentTimeMillis(),
    val paymentType: String = "Cash"
)

@Entity(
    tableName = "group_expense_payments",
    indices = [Index(value = ["expenseId"])]
)
data class GroupExpensePaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val memberNameSnapshot: String,
    val amount: Double,
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_expense_splits")
data class GroupExpenseSplitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val memberId: Long,
    val memberNameSnapshot: String,
    val amount: Double,
    val shareUnits: Double = 1.0
)
