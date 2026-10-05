const db = require("../../db");

async function createLedgerBusinessesTable() {
    const sql = `
        CREATE TABLE IF NOT EXISTS ledger_businesses (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            account_id INTEGER NOT NULL,

            business_name TEXT NOT NULL,

            mobile TEXT,

            address TEXT,

            currency TEXT NOT NULL DEFAULT 'INR',

            status TEXT NOT NULL DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (account_id)
                REFERENCES align_accounts(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Ledger businesses table ready"
        );

    } catch (err) {

        console.error(
            "❌ Ledger businesses table creation failed:",
            err.message
        );

        throw err;
    }
}

module.exports =
    createLedgerBusinessesTable;

async function createLedgerBusinessesIndexes() {
    const sql = `
        CREATE INDEX IF NOT EXISTS idx_ledger_businesses_account
        ON ledger_businesses(account_id)
    `;

    try {
        await db.runAsync(sql);
        console.log("✅ Ledger businesses indexes ready");
    } catch (err) {
        console.error(
            "❌ Ledger businesses index creation failed:",
            err.message
        );
        throw err;
    }
}

module.exports.createIndexes =
    createLedgerBusinessesIndexes;
