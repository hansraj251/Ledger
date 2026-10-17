package com.ledger.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "salary_revisions",
    indices = [Index(value = ["staffId"])]
)
data class SalaryRevisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val amount: Double,
    val effectiveDate: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
