package com.ledger.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "ledger_database_marker"
)
internal data class LedgerDatabaseMarker(
    @PrimaryKey
    val id: Int = 1
)
