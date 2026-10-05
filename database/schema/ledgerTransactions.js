const db =
    require("../../db");

async function createLedgerTransactionsTable() {

    const sql = `

        CREATE TABLE IF NOT EXISTS ledger_transactions (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            party_id INTEGER NOT NULL,

            transaction_type TEXT NOT NULL
                CHECK (
                    transaction_type IN (
                        'credit',
                        'debit'
                    )
                ),

            amount REAL NOT NULL,

            transaction_date DATE NOT NULL,

            description TEXT,

            payment_mode TEXT,

            reference_no TEXT,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (
                party_id
            )
                REFERENCES ledger_parties(id)
                ON DELETE CASCADE

        )

    `;

    await db.runAsync(
        sql
    );

    console.log(
        "✅ Ledger transactions table ready"
    );
}

async function createLedgerTransactionsIndexes() {

    const sql = `
        CREATE INDEX IF NOT EXISTS idx_ledger_transactions_party
        ON ledger_transactions(party_id)
    `;

    await db.runAsync(
        sql
    );

    console.log(
        "✅ Ledger transactions indexes ready"
    );
}

module.exports =
    createLedgerTransactionsTable;

module.exports.createIndexes =
    createLedgerTransactionsIndexes;
