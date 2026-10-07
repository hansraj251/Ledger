package com.ledger.app.data

import android.content.Context
import androidx.room.Room

object LedgerDatabaseProvider {

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
            ).build().also {
                instance = it
            }
        }
    }
}
