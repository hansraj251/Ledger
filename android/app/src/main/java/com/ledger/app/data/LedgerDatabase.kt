package com.ledger.app.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        PartyEntity::class,
        LedgerEntryEntity::class,
        LedgerDatabaseMarker::class,
        ProfileEntity::class,
        InterestAccountEntryEntity::class,
        DailyExpenseEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class LedgerDatabase : RoomDatabase() {

    abstract fun partyDao(): PartyDao

    abstract fun ledgerEntryDao(): LedgerEntryDao

    abstract fun profileDao(): ProfileDao

    abstract fun interestAccountEntryDao(): InterestAccountEntryDao

    abstract fun dailyExpenseDao(): DailyExpenseDao
}
