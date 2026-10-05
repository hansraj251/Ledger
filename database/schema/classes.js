const db =
    require("../../db");

async function createClassesTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS classes (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            name TEXT NOT NULL,

            section TEXT NOT NULL,

            status TEXT
                DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (school_id)
                REFERENCES schools(id),

            UNIQUE (
                school_id,
                name,
                section
            )

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Classes table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Classes table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createClassesTable;
