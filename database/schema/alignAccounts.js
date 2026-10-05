const db = require("../../db");

async function createAlignAccountsTable() {
    const sql = `
        CREATE TABLE IF NOT EXISTS align_accounts (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            name TEXT NOT NULL,

            email TEXT UNIQUE,

            mobile TEXT UNIQUE,

            password TEXT NOT NULL,

            status TEXT NOT NULL DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Align accounts table ready"
        );

    } catch (err) {

        console.error(
            "❌ Align accounts table creation failed:",
            err.message
        );

        throw err;
    }
}

module.exports =
    createAlignAccountsTable;
