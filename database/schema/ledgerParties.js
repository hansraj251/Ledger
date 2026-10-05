const db = require("../../db");

async function createLedgerPartiesTable() {
    const sql = `
        CREATE TABLE IF NOT EXISTS ledger_parties (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            business_id INTEGER NOT NULL,

            name TEXT NOT NULL,

            mobile TEXT,

            email TEXT,

            address TEXT,

            opening_balance REAL NOT NULL DEFAULT 0,

            opening_balance_type TEXT
                NOT NULL DEFAULT 'credit'
                CHECK (
                    opening_balance_type IN (
                        'credit',
                        'debit'
                    )
                ),

            notes TEXT,

            status TEXT NOT NULL DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (business_id)
                REFERENCES ledger_businesses(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Ledger parties table ready"
        );

    } catch (err) {

        console.error(
            "❌ Ledger parties table creation failed:",
            err.message
        );

        throw err;
    }
}

module.exports =
    createLedgerPartiesTable;

async function createLedgerPartiesIndexes() {
    const sql = `
        CREATE INDEX IF NOT EXISTS idx_ledger_parties_business
        ON ledger_parties(business_id)
    `;

    try {
        await db.runAsync(sql);
        console.log("✅ Ledger parties indexes ready");
    } catch (err) {
        console.error(
            "❌ Ledger parties index creation failed:",
            err.message
        );
        throw err;
    }
}

module.exports.createIndexes =
    createLedgerPartiesIndexes;
