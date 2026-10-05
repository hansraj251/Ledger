const db =
    require("../../db");

async function createAttendanceTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS attendance (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            student_id INTEGER NOT NULL,

            attendance_date DATE NOT NULL,

            status TEXT NOT NULL,

            remarks TEXT,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (school_id)
                REFERENCES schools(id),

            FOREIGN KEY (student_id)
                REFERENCES students(id),

            UNIQUE (
                school_id,
                student_id,
                attendance_date
            )

        )
    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Attendance table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Attendance table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createAttendanceTable;
