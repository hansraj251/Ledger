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


private val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS group_expense_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                expenseId INTEGER NOT NULL,
                memberId INTEGER NOT NULL,
                memberNameSnapshot TEXT NOT NULL,
                amount REAL NOT NULL,
                paidAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_group_expense_payments_expenseId ON group_expense_payments(expenseId)"
        )
    }
}


private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE group_settlements ADD COLUMN paymentType TEXT NOT NULL DEFAULT 'Cash'"
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



private val MIGRATION_14_15 = object : Migration(14, 15) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS staff_advance_transactions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, staffId INTEGER NOT NULL, transactionType TEXT NOT NULL, amount REAL NOT NULL, transactionDate INTEGER NOT NULL, note TEXT NOT NULL, createdAt INTEGER NOT NULL)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_staff_advance_transactions_staffId ON staff_advance_transactions (staffId)")
    }
}

private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS staff_bonus_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                staffId INTEGER NOT NULL,
                amount REAL NOT NULL,
                bonusDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_staff_bonus_payments_staffId " +
                "ON staff_bonus_payments(staffId)"
        )
    }
}

private val MIGRATION_16_17 = object : Migration(16, 17) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE staff_advance_transactions " +
                "ADD COLUMN salaryMonth TEXT NOT NULL DEFAULT ''"
        )
    }
}

private val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE staff_bonus_payments ADD COLUMN bonusType TEXT NOT NULL DEFAULT 'BONUS'"
        )
    }
}

private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE salary_payments ADD COLUMN salaryMonth TEXT NOT NULL DEFAULT ''"
        )
        database.execSQL(
            "UPDATE salary_payments SET salaryMonth = " +
                "strftime('%Y-%m', paymentDate / 1000, 'unixepoch', 'localtime') " +
                "WHERE salaryMonth = ''"
        )
    }
}

private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS staff_members (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                mobile TEXT NOT NULL,
                role TEXT NOT NULL,
                joiningDate INTEGER NOT NULL,
                notes TEXT NOT NULL,
                isActive INTEGER NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS salary_revisions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                staffId INTEGER NOT NULL,
                amount REAL NOT NULL,
                effectiveDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_salary_revisions_staffId " +
                "ON salary_revisions(staffId)"
        )

        database.execSQL("""
            CREATE TABLE IF NOT EXISTS salary_payments (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                staffId INTEGER NOT NULL,
                amount REAL NOT NULL,
                paymentDate INTEGER NOT NULL,
                paymentMode TEXT NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_salary_payments_staffId " +
                "ON salary_payments(staffId)"
        )
    }
}


private val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS custom_expense_categories (
                name TEXT NOT NULL,
                icon TEXT NOT NULL,
                quantityType TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                PRIMARY KEY(name)
            )
            """.trimIndent()
        )
    }
}


private val MIGRATION_20_21 = object : Migration(20, 21) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE custom_expense_categories " +
                "ADD COLUMN defaultPrice TEXT NOT NULL DEFAULT ''"
        )
    }
}




private val MIGRATION_24_25 = object : Migration(24, 25) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS emi_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                frequency TEXT NOT NULL,
                installmentNumber INTEGER NOT NULL,
                installmentCount INTEGER NOT NULL,
                dueDate INTEGER NOT NULL,
                amount REAL NOT NULL,
                isPaid INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(transactionId) REFERENCES ledger_entries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_emi_schedules_transactionId " +
                "ON emi_schedules(transactionId)"
        )
    }
}


private val MIGRATION_26_27 = object : Migration(26, 27) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS emi_charge_settlements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                amount REAL NOT NULL,
                settlementDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(transactionId) REFERENCES ledger_entries(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_emi_charge_settlements_transactionId " +
                "ON emi_charge_settlements(transactionId)"
        )
    }
}


private val MIGRATION_27_28 = object : Migration(27, 28) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE emi_charge_settlements " +
                "ADD COLUMN emiAmount REAL NOT NULL DEFAULT 0.0"
        )
        database.execSQL(
            "ALTER TABLE emi_charge_settlements " +
                "ADD COLUMN interestAmount REAL NOT NULL DEFAULT 0.0"
        )
    }
}

private val MIGRATION_25_26 = object : Migration(25, 26) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE emi_schedules ADD COLUMN baseAmount REAL NOT NULL DEFAULT 0.0"
        )
        database.execSQL(
            "ALTER TABLE emi_schedules ADD COLUMN paidAmount REAL NOT NULL DEFAULT 0.0"
        )
        database.execSQL(
            "UPDATE emi_schedules SET baseAmount = amount WHERE baseAmount = 0.0"
        )
    }
}


private val MIGRATION_23_24 = object : Migration(23, 24) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS transaction_settlements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                amount REAL NOT NULL,
                settlementDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                createdAt INTEGER NOT NULL,
                FOREIGN KEY(transactionId) REFERENCES ledger_entries(id) ON DELETE CASCADE
            )
        """.trimIndent())
        database.execSQL("CREATE INDEX IF NOT EXISTS index_transaction_settlements_transactionId ON transaction_settlements(transactionId)")
    }
}

private val MIGRATION_22_23 = object : Migration(22, 23) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE parties ADD COLUMN hideInterestButton INTEGER NOT NULL DEFAULT 0"
        )
        database.execSQL(
            "ALTER TABLE parties ADD COLUMN hideSellButton INTEGER NOT NULL DEFAULT 0"
        )
    }
}

private val MIGRATION_21_22 = object : Migration(21, 22) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS party_sell_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                partyId INTEGER NOT NULL,
                title TEXT NOT NULL,
                rate REAL NOT NULL,
                quantityType TEXT NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_party_sell_items_partyId " +
                "ON party_sell_items(partyId)"
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



private val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS staff_salary_deductions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                staffId INTEGER NOT NULL,
                amount REAL NOT NULL,
                deductionDate INTEGER NOT NULL,
                note TEXT NOT NULL,
                salaryMonth TEXT NOT NULL,
                createdAt INTEGER NOT NULL
            )
        """.trimIndent())
        database.execSQL(
            "CREATE INDEX IF NOT EXISTS index_staff_salary_deductions_staffId " +
                "ON staff_salary_deductions(staffId)"
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28)
                .build().also {
                instance = it
            }
        }
    }
}
