package com.ledger.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object LedgerDatabaseProvider {

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
                .addMigrations(MIGRATION_1_2)
                .build().also {
                instance = it
            }
        }
    }
}
