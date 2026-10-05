const db = require("../../db");

async function createPlansTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS plans (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            slug TEXT UNIQUE NOT NULL,

            display_name TEXT NOT NULL,

            description TEXT,

            plan_type TEXT NOT NULL DEFAULT 'food',

            sort_order INTEGER NOT NULL,

            status TEXT DEFAULT 'active',

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Plans table ready"
        );

    } catch (err) {

        console.error(
            "❌ Plans table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createPlansTable;