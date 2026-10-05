const db =
    require("../../db");

async function createSalaryStructuresTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS salary_structures (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            teacher_id INTEGER NOT NULL,

            basic_salary REAL
                DEFAULT 0,

            hra REAL
                DEFAULT 0,

            other_allowances REAL
                DEFAULT 0,

            pf REAL
                DEFAULT 0,

            esi REAL
                DEFAULT 0,

            professional_tax REAL
                DEFAULT 0,

            other_deductions REAL
                DEFAULT 0,

            effective_from DATE,

            status TEXT
                DEFAULT 'active',

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
    createSalaryStructuresTable;
