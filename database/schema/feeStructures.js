const db =
    require("../../db");

async function createFeeStructuresTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS fee_structures (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            student_id INTEGER NOT NULL,

            academic_year TEXT NOT NULL,

            fee_head TEXT NOT NULL,

            amount REAL
                DEFAULT 0,

            effective_from DATE,

            status TEXT
                DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (school_id)
                REFERENCES schools(id),

            FOREIGN KEY (student_id)
                REFERENCES students(id)

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Fee structures table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Fee structures table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createFeeStructuresTable;