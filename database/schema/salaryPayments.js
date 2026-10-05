const db =
    require("../../db");

async function createSalaryPaymentsTable() {

    const sql = `

        CREATE TABLE IF NOT EXISTS salary_payments (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            teacher_id INTEGER NOT NULL,

            salary_month TEXT NOT NULL,

            salary_amount REAL NOT NULL,

            amount_paid REAL NOT NULL,

            payment_date DATE NOT NULL,

            payment_mode TEXT,

            reference_no TEXT,

            remarks TEXT,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (
                school_id
            )
                REFERENCES schools(id),

            FOREIGN KEY (
                teacher_id
            )
                REFERENCES teachers(id)

        )

    `;

    await db.runAsync(
        sql
    );

}

module.exports =
    createSalaryPaymentsTable;