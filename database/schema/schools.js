const db = require("../../db");

async function createSchoolsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS schools (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            name TEXT NOT NULL,

            school_code TEXT UNIQUE,

            owner_name TEXT,

            email TEXT,

            mobile TEXT,

            address TEXT,

            city TEXT,

            state TEXT,

            pincode TEXT,

            logo TEXT,

            plan_id INTEGER,

            subscription_status TEXT
                DEFAULT 'trial',

            plan_start DATE,

            plan_end DATE,

            subscription_signature TEXT,

            status TEXT
                DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Schools table ready"
        );

    } catch (err) {

        console.error(
            "❌ Schools table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createSchoolsTable;
