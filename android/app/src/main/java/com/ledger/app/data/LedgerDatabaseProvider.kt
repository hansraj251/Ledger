package com.ledger.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


private val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS interest_account_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partyId INTEGER NOT NULL,
                amount REAL NOT NULL,
                type TEXT NOT NULL,
                note TEXT NOT NULL,
                transactionDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(partyId) REFERENCES parties(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_interest_account_entries_partyId " +
                "ON interest_account_entries (partyId)"
        )
    }
}


private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS daily_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL,
                expenseDate INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}


private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS app_settings (
                id INTEGER NOT NULL,
                currencyCode TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
        database.execSQL(
            "INSERT OR IGNORE INTO app_settings (id, currencyCode) VALUES (1, 'INR')"
        )
    }
}


private val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sharing_groups (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS sharing_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                groupId INTEGER NOT NULL,
                name TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS group_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                groupId INTEGER NOT NULL,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                note TEXT NOT NULL,
                expenseDate INTEGER NOT NULL,
                splitMode TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS group_expense_splits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                expenseId INTEGER NOT NULL,
                memberId INTEGER NOT NULL,
                memberNameSnapshot TEXT NOT NULL,
                amount REAL NOT NULL,
                shareUnits REAL NOT NULL
            )
        """.trimIndent())
    }
}

private val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS group_settlements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                groupId INTEGER NOT NULL,
                memberId INTEGER NOT NULL,
                memberNameSnapshot TEXT NOT NULL,
                amount REAL NOT NULL,
                receivedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}


private val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE sharing_members ADD COLUMN defaultShareUnits REAL NOT NULL DEFAULT 1.0"
        )
    }
}

object LedgerDatabaseProvider {


private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE ledger_entries ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0"
        )
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE ledger_entries ADD COLUMN transactionDate INTEGER NOT NULL DEFAULT 0"
        )
    }
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                id INTEGER NOT NULL,
                name TEXT NOT NULL,
                mobile TEXT NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )
    }
}


    const val DATABASE_NAME = "ledger.db"

    @Volatile
    private var instance: LedgerDatabase? = null

    fun close() {
        synchronized(this) {
            instance?.close()
            instance = null
        }
    }

    fun get(context: Context): LedgerDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                LedgerDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10)
                .build().also {
                instance = it
            }
        }
    }
}
