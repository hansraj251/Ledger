package com.ledger.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_advance_transactions",
    indices = [Index(value = ["staffId"])]
)
data class StaffAdvanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val transactionType: String,
    val amount: Double,
    val transactionDate: Long,
    val note: String = "",
    @ColumnInfo(defaultValue = "''") val salaryMonth: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
