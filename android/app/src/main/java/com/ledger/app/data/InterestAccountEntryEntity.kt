package com.ledger.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "interest_account_entries",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["partyId"])]
)
data class InterestAccountEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partyId: Long,
    val amount: Double,
    // GAVE = Int. You Gave; GOT = Int. You Got
    val type: String,
    val note: String = "",
    val transactionDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
