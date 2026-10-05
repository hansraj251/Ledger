const db =
    require("../../db");

async function createTeachersTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS teachers (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            employee_id TEXT,

            name TEXT NOT NULL,

            father_name TEXT,

            mother_name TEXT,

            dob DATE,

            gender TEXT,

            mobile TEXT,

            email TEXT,

            address TEXT,

            qualification TEXT,

            subject TEXT,

            joining_date DATE,

            designation_id INTEGER,

            status TEXT
                DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Teachers table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Teachers table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createTeachersTable;
