package com.ledger.app.data

enum class EntryType {
    CREDIT,
    DEBIT
}

data class LedgerEntry(
    val id: Long,
    val partyId: Long,
    val amount: Double,
    val type: EntryType,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Party(
    val id: Long,
    val name: String,
    val mobile: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
