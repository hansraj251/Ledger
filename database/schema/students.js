const db =
    require("../../db");

async function createStudentsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS students (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            admission_no TEXT NOT NULL,

            name TEXT NOT NULL,

            father_name TEXT,

            mother_name TEXT,

            dob DATE,

            gender TEXT,

            mobile TEXT,

            address TEXT,

            class_name TEXT,

            section TEXT,

            roll_no TEXT,

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
                admission_no
            )

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Students table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Students table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createStudentsTable;
