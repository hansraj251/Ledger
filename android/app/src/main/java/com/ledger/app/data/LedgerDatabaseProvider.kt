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




private val MIGRATION_29_30 = object : Migration(29, 30) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val columns = mutableSetOf<String>()
        database.query("PRAGMA table_info(activity_logs)").use { cursor ->
            while (cursor.moveToNext()) columns.add(cursor.getString(1))
        }
        if (!columns.contains("details")) {
            database.execSQL(
                "ALTER TABLE activity_logs ADD COLUMN details TEXT NOT NULL DEFAULT ''"
            )
        }

        // Backfill details for records that still exist.
        database.execSQL("""
            UPDATE activity_logs
            SET details = CASE tableName
                WHEN 'parties' THEN COALESCE((
                    SELECT 'Party: ' || name ||
                        CASE WHEN COALESCE(mobile, '') <> ''
                        THEN ' · Mobile: ' || mobile ELSE '' END
                    FROM parties WHERE id = activity_logs.recordId
                ), '')
                WHEN 'ledger_entries' THEN COALESCE((
                    SELECT 'Party: ' || COALESCE(p.name, 'Unknown party') ||
                        ' · Amount: ₹' || printf('%.2f', e.amount) ||
                        ' · Type: ' || e.type ||
                        CASE WHEN COALESCE(e.note, '') <> ''
                        THEN ' · Note: ' || e.note ELSE '' END
                    FROM ledger_entries e
                    LEFT JOIN parties p ON p.id = e.partyId
                    WHERE e.id = activity_logs.recordId
                ), '')
                WHEN 'daily_expenses' THEN COALESCE((
                    SELECT 'Category: ' || category ||
                        ' · Amount: ₹' || printf('%.2f', amount) ||
                        CASE WHEN COALESCE(note, '') <> ''
                        THEN ' · Note: ' || note ELSE '' END
                    FROM daily_expenses WHERE id = activity_logs.recordId
                ), '')
                ELSE ''
            END
            WHERE details = ''
        """.trimIndent())

        val tables = listOf(
            "parties", "ledger_entries", "user_profile",
            "interest_account_entries", "daily_expenses",
            "custom_expense_categories", "sharing_groups",
            "sharing_members", "group_expenses", "group_expense_splits",
            "group_expense_payments", "group_settlements", "app_settings",
            "staff_members", "salary_revisions", "salary_payments",
            "staff_advance_transactions", "staff_bonuses",
            "staff_salary_deductions", "party_sell_items",
            "emi_schedules", "transaction_settlements",
            "emi_charge_settlements"
        )

        fun detailExpression(table: String, row: String): String =
            when (table) {
                "parties" ->
                    "'Party: ' || COALESCE($row.name, '') || " +
                    "CASE WHEN COALESCE($row.mobile, '') <> '' " +
                    "THEN ' · Mobile: ' || $row.mobile ELSE '' END"

                "ledger_entries" ->
                    "'Party: ' || COALESCE((SELECT name FROM parties " +
                    "WHERE id = $row.partyId), 'Unknown party') || " +
                    "' · Amount: ₹' || printf('%.2f', $row.amount) || " +
                    "' · Type: ' || $row.type || " +
                    "CASE WHEN COALESCE($row.note, '') <> '' " +
                    "THEN ' · Note: ' || $row.note ELSE '' END"

                "daily_expenses" ->
                    "'Category: ' || COALESCE($row.category, '') || " +
                    "' · Amount: ₹' || printf('%.2f', $row.amount) || " +
                    "CASE WHEN COALESCE($row.note, '') <> '' " +
                    "THEN ' · Note: ' || $row.note ELSE '' END"

                else -> "'Record ID: ' || $row.rowid"
            }

        for (table in tables) {
            val exists = database.query(
                "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                arrayOf(table)
            ).use { it.moveToFirst() }
            if (!exists) continue

            for (operation in listOf("insert", "update", "delete")) {
                database.execSQL("DROP TRIGGER IF EXISTS activity_${table}_${operation}")
            }

            val insertDetail = detailExpression(table, "NEW")
            val updateDetail = detailExpression(table, "NEW")
            val deleteDetail = detailExpression(table, "OLD")

            database.execSQL("""
                CREATE TRIGGER activity_${table}_insert
                AFTER INSERT ON $table
                BEGIN
                    INSERT INTO activity_logs
                        (tableName, operation, recordId, occurredAt, details)
                    VALUES (
                        '$table', 'ADD', NEW.rowid,
                        CAST(strftime('%s','now') AS INTEGER) * 1000,
                        $insertDetail
                    );
                END
            """.trimIndent())

            database.execSQL("""
                CREATE TRIGGER activity_${table}_update
                AFTER UPDATE ON $table
                BEGIN
                    INSERT INTO activity_logs
                        (tableName, operation, recordId, occurredAt, details)
                    VALUES (
                        '$table', 'EDIT', NEW.rowid,
                        CAST(strftime('%s','now') AS INTEGER) * 1000,
                        $updateDetail
                    );
                END
            """.trimIndent())

            database.execSQL("""
                CREATE TRIGGER activity_${table}_delete
                AFTER DELETE ON $table
                BEGIN
                    INSERT INTO activity_logs
                        (tableName, operation, recordId, occurredAt, details)
                    VALUES (
                        '$table', 'DELETE', OLD.rowid,
                        CAST(strftime('%s','now') AS INTEGER) * 1000,
                        $deleteDetail
                    );
                END
            """.trimIndent())
        }
    }
}








private val MIGRATION_36_37 = object : Migration(36, 37) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Replace generic EMI settlement triggers with detailed activity logs.
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_charge_settlements_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_charge_settlements_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_charge_settlements_delete")

        database.execSQL("""
            CREATE TRIGGER activity_emi_charge_settlements_insert
            AFTER INSERT ON emi_charge_settlements
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'emi_charge_settlements',
                    'ADD',
                    NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', NEW.amount) ||
                    ' · EMI Amount: ₹' || printf('%.2f', NEW.emiAmount) ||
                    ' · Interest Amount: ₹' || printf('%.2f', NEW.interestAmount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        NEW.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_emi_charge_settlements_delete
            AFTER DELETE ON emi_charge_settlements
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'emi_charge_settlements',
                    'DELETE',
                    OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', OLD.amount) ||
                    ' · EMI Amount: ₹' || printf('%.2f', OLD.emiAmount) ||
                    ' · Interest Amount: ₹' || printf('%.2f', OLD.interestAmount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        OLD.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(OLD.note, '') <> ''
                        THEN ' · Note: ' || OLD.note ELSE '' END
                );
            END
        """.trimIndent())

        // Backfill existing logs only while their source settlement still exists.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = s.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = s.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', s.amount) ||
                    ' · EMI Amount: ₹' || printf('%.2f', s.emiAmount) ||
                    ' · Interest Amount: ₹' || printf('%.2f', s.interestAmount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        s.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(s.note, '') <> ''
                        THEN ' · Note: ' || s.note ELSE '' END
                FROM emi_charge_settlements s
                WHERE s.id = activity_logs.recordId
            )
            WHERE LOWER(tableName) = 'emi_charge_settlements'
              AND EXISTS (
                  SELECT 1
                  FROM emi_charge_settlements s
                  WHERE s.id = activity_logs.recordId
              )
        """.trimIndent())
    }
}

private val MIGRATION_35_36 = object : Migration(35, 36) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Replace generic EMI installment activity triggers.
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_schedules_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_schedules_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_emi_schedules_delete")

        database.execSQL("""
            CREATE TRIGGER activity_emi_schedules_insert
            AFTER INSERT ON emi_schedules
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'emi_schedules',
                    'ADD',
                    NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown') ||
                    ' · Installment: ' || NEW.installmentNumber ||
                    '/' || NEW.installmentCount ||
                    ' · Frequency: ' || NEW.frequency ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Principal: ₹' || printf('%.2f', NEW.baseAmount) ||
                    ' · Paid Amount: ₹' || printf('%.2f', NEW.paidAmount) ||
                    ' · Due Date: ' || strftime('%d-%m-%Y',
                        NEW.dueDate / 1000, 'unixepoch') ||
                    ' · Status: ' ||
                    CASE WHEN NEW.isPaid = 1 THEN 'Paid' ELSE 'Unpaid' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_emi_schedules_update
            AFTER UPDATE ON emi_schedules
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'emi_schedules',
                    'EDIT',
                    NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown') ||
                    ' · Installment: ' || NEW.installmentNumber ||
                    '/' || NEW.installmentCount ||
                    ' · Frequency: ' || NEW.frequency ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Principal: ₹' || printf('%.2f', NEW.baseAmount) ||
                    ' · Paid Amount: ₹' || printf('%.2f', NEW.paidAmount) ||
                    ' · Due Date: ' || strftime('%d-%m-%Y',
                        NEW.dueDate / 1000, 'unixepoch') ||
                    ' · Status: ' ||
                    CASE WHEN NEW.isPaid = 1 THEN 'Paid' ELSE 'Unpaid' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_emi_schedules_delete
            AFTER DELETE ON emi_schedules
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'emi_schedules',
                    'DELETE',
                    OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown') ||
                    ' · Installment: ' || OLD.installmentNumber ||
                    '/' || OLD.installmentCount ||
                    ' · Frequency: ' || OLD.frequency ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Principal: ₹' || printf('%.2f', OLD.baseAmount) ||
                    ' · Paid Amount: ₹' || printf('%.2f', OLD.paidAmount) ||
                    ' · Due Date: ' || strftime('%d-%m-%Y',
                        OLD.dueDate / 1000, 'unixepoch') ||
                    ' · Status: ' ||
                    CASE WHEN OLD.isPaid = 1 THEN 'Paid' ELSE 'Unpaid' END
                );
            END
        """.trimIndent())

        // Enrich previous logs for installments that still exist.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = s.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = s.transactionId
                    ), 'Unknown') ||
                    ' · Installment: ' || s.installmentNumber ||
                    '/' || s.installmentCount ||
                    ' · Frequency: ' || s.frequency ||
                    ' · Amount: ₹' || printf('%.2f', s.amount) ||
                    ' · Principal: ₹' || printf('%.2f', s.baseAmount) ||
                    ' · Paid Amount: ₹' || printf('%.2f', s.paidAmount) ||
                    ' · Due Date: ' || strftime('%d-%m-%Y',
                        s.dueDate / 1000, 'unixepoch') ||
                    ' · Status: ' ||
                    CASE WHEN s.isPaid = 1 THEN 'Paid' ELSE 'Unpaid' END
                FROM emi_schedules s
                WHERE s.id = activity_logs.recordId
            )
            WHERE LOWER(tableName) = 'emi_schedules'
              AND EXISTS (
                  SELECT 1 FROM emi_schedules s
                  WHERE s.id = activity_logs.recordId
              )
        """.trimIndent())
    }
}

private val MIGRATION_34_35 = object : Migration(34, 35) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Replace generic transaction-settlement triggers with descriptive logs.
        database.execSQL("DROP TRIGGER IF EXISTS activity_transaction_settlements_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_transaction_settlements_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_transaction_settlements_delete")

        database.execSQL("""
            CREATE TRIGGER activity_transaction_settlements_insert
            AFTER INSERT ON transaction_settlements
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'transaction_settlements',
                    'ADD',
                    NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        NEW.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_transaction_settlements_update
            AFTER UPDATE ON transaction_settlements
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'transaction_settlements',
                    'EDIT',
                    NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = NEW.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        NEW.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_transaction_settlements_delete
            AFTER DELETE ON transaction_settlements
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'transaction_settlements',
                    'DELETE',
                    OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = OLD.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        OLD.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(OLD.note, '') <> ''
                        THEN ' · Note: ' || OLD.note ELSE '' END
                );
            END
        """.trimIndent())

        // Enrich existing activity records where the settlement still exists.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Party: ' ||
                    COALESCE((
                        SELECT p.name
                        FROM ledger_entries le
                        JOIN parties p ON p.id = le.partyId
                        WHERE le.id = s.transactionId
                    ), 'Unknown party') ||
                    ' · Transaction: ' ||
                    COALESCE((
                        SELECT CASE le.type
                            WHEN 'CREDIT' THEN 'You Gave'
                            WHEN 'DEBIT' THEN 'You Got'
                            ELSE le.type
                        END
                        FROM ledger_entries le
                        WHERE le.id = s.transactionId
                    ), 'Unknown') ||
                    ' · Settlement: ₹' || printf('%.2f', s.amount) ||
                    ' · Date: ' || strftime('%d-%m-%Y',
                        s.settlementDate / 1000, 'unixepoch') ||
                    CASE WHEN COALESCE(s.note, '') <> ''
                        THEN ' · Note: ' || s.note ELSE '' END
                FROM transaction_settlements s
                WHERE s.id = activity_logs.recordId
            )
            WHERE LOWER(tableName) = 'transaction_settlements'
              AND EXISTS (
                  SELECT 1
                  FROM transaction_settlements s
                  WHERE s.id = activity_logs.recordId
              )
        """.trimIndent())
    }
}

private val MIGRATION_33_34 = object : Migration(33, 34) {
    override fun migrate(database: SupportSQLiteDatabase) {

        // Replace generic party-transaction triggers with descriptive triggers.
        database.execSQL("DROP TRIGGER IF EXISTS activity_ledger_entries_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_ledger_entries_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_ledger_entries_delete")

        database.execSQL("""
            CREATE TRIGGER activity_ledger_entries_insert
            AFTER INSERT ON ledger_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'ledger_entries', 'ADD', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = NEW.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(NEW.type)
                        WHEN 'DEBIT' THEN 'You Got'
                        WHEN 'CREDIT' THEN 'You Gave'
                        ELSE NEW.type
                    END ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_ledger_entries_update
            AFTER UPDATE ON ledger_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'ledger_entries', 'EDIT', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = NEW.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' → ₹' || printf('%.2f', NEW.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(OLD.type)
                        WHEN 'DEBIT' THEN 'You Got'
                        WHEN 'CREDIT' THEN 'You Gave'
                        ELSE OLD.type
                    END ||
                    ' → ' ||
                    CASE UPPER(NEW.type)
                        WHEN 'DEBIT' THEN 'You Got'
                        WHEN 'CREDIT' THEN 'You Gave'
                        ELSE NEW.type
                    END ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_ledger_entries_delete
            AFTER DELETE ON ledger_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'ledger_entries', 'DELETE', OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = OLD.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(OLD.type)
                        WHEN 'DEBIT' THEN 'You Got'
                        WHEN 'CREDIT' THEN 'You Gave'
                        ELSE OLD.type
                    END ||
                    CASE WHEN COALESCE(OLD.note, '') <> ''
                        THEN ' · Note: ' || OLD.note ELSE '' END
                );
            END
        """.trimIndent())

        // Interest Account: record descriptive details for every operation.
        database.execSQL("DROP TRIGGER IF EXISTS activity_interest_account_entries_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_interest_account_entries_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_interest_account_entries_delete")

        database.execSQL("""
            CREATE TRIGGER activity_interest_account_entries_insert
            AFTER INSERT ON interest_account_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'interest_account_entries', 'ADD', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = NEW.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(NEW.type)
                        WHEN 'GOT' THEN 'Interest You Got'
                        WHEN 'GAVE' THEN 'Interest You Gave'
                        ELSE NEW.type
                    END ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_interest_account_entries_update
            AFTER UPDATE ON interest_account_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'interest_account_entries', 'EDIT', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = NEW.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' → ₹' || printf('%.2f', NEW.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(OLD.type)
                        WHEN 'GOT' THEN 'Interest You Got'
                        WHEN 'GAVE' THEN 'Interest You Gave'
                        ELSE OLD.type
                    END ||
                    ' → ' ||
                    CASE UPPER(NEW.type)
                        WHEN 'GOT' THEN 'Interest You Got'
                        WHEN 'GAVE' THEN 'Interest You Gave'
                        ELSE NEW.type
                    END ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                        THEN ' · Note: ' || NEW.note ELSE '' END
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_interest_account_entries_delete
            AFTER DELETE ON interest_account_entries
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'interest_account_entries', 'DELETE', OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Party: ' || COALESCE(
                        (SELECT name FROM parties WHERE id = OLD.partyId),
                        'Unknown party'
                    ) ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(OLD.type)
                        WHEN 'GOT' THEN 'Interest You Got'
                        WHEN 'GAVE' THEN 'Interest You Gave'
                        ELSE OLD.type
                    END ||
                    CASE WHEN COALESCE(OLD.note, '') <> ''
                        THEN ' · Note: ' || OLD.note ELSE '' END
                );
            END
        """.trimIndent())

        // Correct labels in previously saved party transaction activity.
        database.execSQL("""
            UPDATE activity_logs
            SET details = REPLACE(
                REPLACE(details, 'Type: DEBIT', 'Type: You Got'),
                'Type: CREDIT', 'Type: You Gave'
            )
            WHERE tableName = 'ledger_entries'
        """.trimIndent())

        // Backfill old interest activity entries while their records still exist.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Party: ' || COALESCE(p.name, 'Unknown party') ||
                    ' · Amount: ₹' || printf('%.2f', e.amount) ||
                    ' · Type: ' ||
                    CASE UPPER(e.type)
                        WHEN 'GOT' THEN 'Interest You Got'
                        WHEN 'GAVE' THEN 'Interest You Gave'
                        ELSE e.type
                    END ||
                    CASE WHEN COALESCE(e.note, '') <> ''
                        THEN ' · Note: ' || e.note ELSE '' END
                FROM interest_account_entries e
                LEFT JOIN parties p ON p.id = e.partyId
                WHERE e.id = activity_logs.recordId
            )
            WHERE tableName = 'interest_account_entries'
              AND details LIKE 'Record ID:%'
              AND EXISTS (
                  SELECT 1 FROM interest_account_entries e
                  WHERE e.id = activity_logs.recordId
              )
        """.trimIndent())
    }
}

private val MIGRATION_32_33 = object : Migration(32, 33) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TRIGGER IF EXISTS activity_sharing_members_insert")
        database.execSQL("DROP TRIGGER IF EXISTS activity_sharing_members_update")
        database.execSQL("DROP TRIGGER IF EXISTS activity_sharing_members_delete")

        database.execSQL("""
            CREATE TRIGGER activity_sharing_members_insert
            AFTER INSERT ON sharing_members
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'sharing_members', 'ADD', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = NEW.groupId),
                        'Unknown group'
                    ) ||
                    ' · Member: ' || COALESCE(NEW.name, '') ||
                    ' · Default Share Units: ' || CAST(NEW.defaultShareUnits AS TEXT)
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_sharing_members_update
            AFTER UPDATE ON sharing_members
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'sharing_members', 'EDIT', NEW.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = OLD.groupId),
                        'Unknown group'
                    ) || ' → ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = NEW.groupId),
                        'Unknown group'
                    ) ||
                    ' · Member: ' || COALESCE(OLD.name, '') ||
                    ' → ' || COALESCE(NEW.name, '') ||
                    ' · Default Share Units: ' || CAST(OLD.defaultShareUnits AS TEXT) ||
                    ' → ' || CAST(NEW.defaultShareUnits AS TEXT)
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_sharing_members_delete
            AFTER DELETE ON sharing_members
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'sharing_members', 'DELETE', OLD.id,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = OLD.groupId),
                        'Unknown group'
                    ) ||
                    ' · Member: ' || COALESCE(OLD.name, '') ||
                    ' · Default Share Units: ' || CAST(OLD.defaultShareUnits AS TEXT)
                );
            END
        """.trimIndent())
    }
}

private val MIGRATION_31_32 = object : Migration(31, 32) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Replace the generic triggers for split records with detail-aware ones.
        listOf("insert", "update", "delete").forEach { operation ->
            database.execSQL(
                "DROP TRIGGER IF EXISTS activity_group_expense_splits_$operation"
            )
        }

        // Backfill details for existing split records that still exist.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Group: ' || COALESCE(g.name, 'Unknown group') ||
                    ' · Expense: ' || COALESCE(e.title, 'Unknown expense') ||
                    ' · Member: ' || COALESCE(s.memberNameSnapshot, 'Unknown member') ||
                    ' · Share: ₹' || printf('%.2f', s.amount) ||
                    ' · Share Units: ' || CAST(s.shareUnits AS TEXT)
                FROM group_expense_splits s
                LEFT JOIN group_expenses e ON e.id = s.expenseId
                LEFT JOIN sharing_groups g ON g.id = e.groupId
                WHERE s.id = activity_logs.recordId
            )
            WHERE tableName = 'group_expense_splits'
              AND details LIKE 'Record ID:%'
              AND EXISTS (
                  SELECT 1 FROM group_expense_splits s
                  WHERE s.id = activity_logs.recordId
              )
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expense_splits_insert
            AFTER INSERT ON group_expense_splits
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expense_splits', 'ADD', NEW.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT g.name FROM group_expenses e
                         LEFT JOIN sharing_groups g ON g.id = e.groupId
                         WHERE e.id = NEW.expenseId), 'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(
                        (SELECT title FROM group_expenses WHERE id = NEW.expenseId),
                        'Unknown expense'
                    ) ||
                    ' · Member: ' || COALESCE(NEW.memberNameSnapshot, 'Unknown member') ||
                    ' · Share: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Share Units: ' || CAST(NEW.shareUnits AS TEXT)
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expense_splits_update
            AFTER UPDATE ON group_expense_splits
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expense_splits', 'EDIT', NEW.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT g.name FROM group_expenses e
                         LEFT JOIN sharing_groups g ON g.id = e.groupId
                         WHERE e.id = NEW.expenseId), 'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(
                        (SELECT title FROM group_expenses WHERE id = NEW.expenseId),
                        'Unknown expense'
                    ) ||
                    ' · Member: ' || COALESCE(NEW.memberNameSnapshot, 'Unknown member') ||
                    ' · Share: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Share Units: ' || CAST(NEW.shareUnits AS TEXT)
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expense_splits_delete
            AFTER DELETE ON group_expense_splits
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expense_splits', 'DELETE', OLD.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT g.name FROM group_expenses e
                         LEFT JOIN sharing_groups g ON g.id = e.groupId
                         WHERE e.id = OLD.expenseId), 'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(
                        (SELECT title FROM group_expenses WHERE id = OLD.expenseId),
                        'Unknown expense'
                    ) ||
                    ' · Member: ' || COALESCE(OLD.memberNameSnapshot, 'Unknown member') ||
                    ' · Share: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Share Units: ' || CAST(OLD.shareUnits AS TEXT)
                );
            END
        """.trimIndent())
    }
}

private val MIGRATION_30_31 = object : Migration(30, 31) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val tables = listOf(
            "activity_group_expenses_insert",
            "activity_group_expenses_update",
            "activity_group_expenses_delete"
        )
        tables.forEach { database.execSQL("DROP TRIGGER IF EXISTS $it") }

        // Add details to older ADD/EDIT logs when the expense still exists.
        database.execSQL("""
            UPDATE activity_logs
            SET details = (
                SELECT
                    'Group: ' || COALESCE(g.name, 'Unknown group') ||
                    ' · Expense: ' || COALESCE(e.title, '') ||
                    ' · Amount: ₹' || printf('%.2f', e.amount) ||
                    ' · Category: ' || COALESCE(e.category, '') ||
                    CASE WHEN COALESCE(e.note, '') <> ''
                         THEN ' · Note: ' || e.note ELSE '' END ||
                    ' · Split: ' || COALESCE(e.splitMode, '')
                FROM group_expenses e
                LEFT JOIN sharing_groups g ON g.id = e.groupId
                WHERE e.id = activity_logs.recordId
            )
            WHERE tableName = 'group_expenses'
              AND operation IN ('ADD', 'EDIT', 'UPDATE')
              AND EXISTS (
                  SELECT 1 FROM group_expenses e
                  WHERE e.id = activity_logs.recordId
              )
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expenses_insert
            AFTER INSERT ON group_expenses
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expenses', 'ADD', NEW.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = NEW.groupId),
                        'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(NEW.title, '') ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Category: ' || COALESCE(NEW.category, '') ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                         THEN ' · Note: ' || NEW.note ELSE '' END ||
                    ' · Split: ' || COALESCE(NEW.splitMode, '')
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expenses_update
            AFTER UPDATE ON group_expenses
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expenses', 'EDIT', NEW.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = NEW.groupId),
                        'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(NEW.title, '') ||
                    ' · Amount: ₹' || printf('%.2f', NEW.amount) ||
                    ' · Category: ' || COALESCE(NEW.category, '') ||
                    CASE WHEN COALESCE(NEW.note, '') <> ''
                         THEN ' · Note: ' || NEW.note ELSE '' END ||
                    ' · Split: ' || COALESCE(NEW.splitMode, '')
                );
            END
        """.trimIndent())

        database.execSQL("""
            CREATE TRIGGER activity_group_expenses_delete
            AFTER DELETE ON group_expenses
            BEGIN
                INSERT INTO activity_logs
                    (tableName, operation, recordId, occurredAt, details)
                VALUES (
                    'group_expenses', 'DELETE', OLD.rowid,
                    CAST(strftime('%s','now') AS INTEGER) * 1000,
                    'Group: ' || COALESCE(
                        (SELECT name FROM sharing_groups WHERE id = OLD.groupId),
                        'Unknown group'
                    ) ||
                    ' · Expense: ' || COALESCE(OLD.title, '') ||
                    ' · Amount: ₹' || printf('%.2f', OLD.amount) ||
                    ' · Category: ' || COALESCE(OLD.category, '') ||
                    CASE WHEN COALESCE(OLD.note, '') <> ''
                         THEN ' · Note: ' || OLD.note ELSE '' END ||
                    ' · Split: ' || COALESCE(OLD.splitMode, '')
                );
            END
        """.trimIndent())
    }
}

private val MIGRATION_28_29 = object : Migration(28, 29) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS activity_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                tableName TEXT NOT NULL,
                operation TEXT NOT NULL,
                recordId INTEGER NOT NULL,
                occurredAt INTEGER NOT NULL
            )
            """.trimIndent()
        )

        // These are application data tables. Skip tables absent in a given
        // installed database version rather than failing the migration.
        val trackedTables = listOf(
            "parties",
            "ledger_entries",
            "user_profile",
            "interest_account_entries",
            "daily_expenses",
            "custom_expense_categories",
            "sharing_groups",
            "sharing_members",
            "group_expenses",
            "group_expense_splits",
            "group_expense_payments",
            "group_settlements",
            "app_settings",
            "staff_members",
            "salary_revisions",
            "salary_payments",
            "staff_advance_transactions",
            "staff_bonuses",
            "staff_salary_deductions",
            "party_sell_items",
            "emi_schedules",
            "transaction_settlements",
            "emi_charge_settlements"
        )

        trackedTables.forEach { table ->
            val cursor = database.query(
                "SELECT name FROM sqlite_master WHERE type='table' AND name='$table'"
            )
            val exists = cursor.use { it.moveToFirst() }

            if (exists) {
                database.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS activity_${table}_insert
                    AFTER INSERT ON $table
                    BEGIN
                        INSERT INTO activity_logs
                            (tableName, operation, recordId, occurredAt)
                        VALUES
                            ('$table', 'ADD', NEW.rowid,
                             CAST(strftime('%s','now') AS INTEGER) * 1000);
                    END
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS activity_${table}_update
                    AFTER UPDATE ON $table
                    BEGIN
                        INSERT INTO activity_logs
                            (tableName, operation, recordId, occurredAt)
                        VALUES
                            ('$table', 'EDIT', NEW.rowid,
                             CAST(strftime('%s','now') AS INTEGER) * 1000);
                    END
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TRIGGER IF NOT EXISTS activity_${table}_delete
                    AFTER DELETE ON $table
                    BEGIN
                        INSERT INTO activity_logs
                            (tableName, operation, recordId, occurredAt)
                        VALUES
                            ('$table', 'DELETE', OLD.rowid,
                             CAST(strftime('%s','now') AS INTEGER) * 1000);
                    END
                    """.trimIndent()
                )
            }
        }
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31, MIGRATION_31_32, MIGRATION_32_33, MIGRATION_33_34, MIGRATION_34_35, MIGRATION_35_36, MIGRATION_36_37)
                .build().also {
                instance = it
            }
        }
    }
}
