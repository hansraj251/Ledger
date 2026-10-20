package com.ledger.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_expense_categories")
data class CustomExpenseCategoryEntity(
    @PrimaryKey
    val name: String,
    val icon: String,
    val quantityType: String,
    val defaultPrice: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
