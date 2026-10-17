package com.ledger.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staff_bonus_payments",
    indices = [Index(value = ["staffId"])]
)
data class StaffBonusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffId: Long,
    val amount: Double,
    val bonusDate: Long,
    val note: String = "",
    val bonusType: String = "BONUS",
    val createdAt: Long = System.currentTimeMillis()
)
