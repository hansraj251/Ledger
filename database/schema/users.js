const db = require("../../db");

async function createUsersTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS users (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            restaurant_id INTEGER,

            school_id INTEGER,

            name TEXT NOT NULL,

            email TEXT UNIQUE,

            mobile TEXT UNIQUE,

            password TEXT NOT NULL,

            role TEXT NOT NULL DEFAULT 'owner',

            status TEXT NOT NULL DEFAULT 'active',

            last_login DATETIME,

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (restaurant_id)
                REFERENCES restaurants(id),
            FOREIGN KEY (school_id)
                REFERENCES schools(id)

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Users table ready"
        );

    } catch (err) {

        console.error(
            "❌ Users table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createUsersTable;