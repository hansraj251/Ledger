package com.ledger.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "parties")
data class PartyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val mobile: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
