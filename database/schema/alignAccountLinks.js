const db = require("../../db");

async function createAlignAccountLinksTable() {
    const sql = `
        CREATE TABLE IF NOT EXISTS align_account_links (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            account_id INTEGER NOT NULL,

            module TEXT NOT NULL,

            module_user_id INTEGER NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            UNIQUE (
                account_id,
                module
            ),

            UNIQUE (
                module,
                module_user_id
            ),

            FOREIGN KEY (account_id)
                REFERENCES align_accounts(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Align account links table ready"
        );

    } catch (err) {

        console.error(
            "❌ Align account links table creation failed:",
            err.message
        );

        throw err;
    }
}

module.exports =
    createAlignAccountLinksTable;
