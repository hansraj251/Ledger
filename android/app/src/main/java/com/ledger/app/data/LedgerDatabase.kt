package com.ledger.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PartyEntity::class,
        LedgerEntryEntity::class,
        LedgerDatabaseMarker::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun partyDao(): PartyDao

    abstract fun ledgerEntryDao(): LedgerEntryDao
}
