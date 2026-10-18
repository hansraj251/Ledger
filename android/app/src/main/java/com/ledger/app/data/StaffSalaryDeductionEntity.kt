package com.ledger.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_salary_deductions",
    indices = [Index(value = ["staffId"])]
)
data class StaffSalaryDeductionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val amount: Double,
    val deductionDate: Long,
    val note: String,
    val salaryMonth: String,
    val createdAt: Long = System.currentTimeMillis()
)
