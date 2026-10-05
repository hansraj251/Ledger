const db =
    require("../../db");

async function createFeePaymentsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS fee_payments (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            student_id INTEGER NOT NULL,

            academic_year TEXT NOT NULL,

            amount REAL NOT NULL,

            payment_date DATE NOT NULL,

            payment_mode TEXT,

            receipt_no TEXT,

            remarks TEXT,

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
            "✅ Fee payments table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Fee payments table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createFeePaymentsTable;