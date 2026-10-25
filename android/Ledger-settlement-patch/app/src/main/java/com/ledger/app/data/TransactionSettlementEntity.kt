package com.ledger.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transaction_settlements",
    foreignKeys = [ForeignKey(
        entity = LedgerEntryEntity::class,
        parentColumns = ["id"],
        childColumns = ["transactionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["transactionId"])]
)
data class TransactionSettlementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val amount: Double,
    val settlementDate: Long,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
