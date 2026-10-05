const db = require("../../db");

async function createPropertyUsersTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS property_users (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            name TEXT NOT NULL,

            email TEXT UNIQUE,

            mobile TEXT UNIQUE,

            password TEXT NOT NULL,

            status TEXT NOT NULL DEFAULT 'active',

            last_login DATETIME,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Property users table ready"
        );

    } catch (err) {

        console.error(
            "❌ Property users table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createPropertyUsersTable;
