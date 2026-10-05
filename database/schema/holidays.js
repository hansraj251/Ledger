const db =
    require("../../db");

async function createHolidaysTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS holidays (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            holiday_date DATE NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (school_id)
                REFERENCES schools(id),

            UNIQUE (
                school_id,
                holiday_date
            )

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Holidays table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Holidays table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createHolidaysTable;