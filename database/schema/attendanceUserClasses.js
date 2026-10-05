const db =
    require("../../db");

async function createAttendanceUserClassesTable() {

    const sql = `

        CREATE TABLE IF NOT EXISTS attendance_user_classes (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            user_id INTEGER NOT NULL,

            class_id INTEGER NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (user_id)
                REFERENCES users(id),

            FOREIGN KEY (class_id)
                REFERENCES classes(id),

            UNIQUE (
                user_id,
                class_id
            )

        )

    `;

    try {

        await db.runAsync(
            sql
        );

        console.log(
            "✅ Attendance user classes table ready"
        );

    }
    catch (err) {

        console.error(
            "❌ Attendance user classes table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createAttendanceUserClassesTable;