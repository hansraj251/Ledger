package com.ledger.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_entries",
    foreignKeys = [
        ForeignKey(
            entity = PartyEntity::class,
            parentColumns = ["id"],
            childColumns = ["partyId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["partyId"])
    ]
)
data class LedgerEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partyId: Long,
    val amount: Double,
    val type: EntryType,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
