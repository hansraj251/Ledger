package com.ledger.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_expenses")
data class DailyExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val category: String,
    val note: String = "",
    val expenseDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
