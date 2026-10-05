const db = require("../../db");

async function createSuperAdminTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS super_admin (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            username TEXT UNIQUE NOT NULL,

            password TEXT NOT NULL,

            name TEXT NOT NULL,

            status TEXT DEFAULT 'active',

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            last_login DATETIME

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Super Admin table ready"
        );

    } catch (err) {

        console.error(
            "❌ Super Admin table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createSuperAdminTable;