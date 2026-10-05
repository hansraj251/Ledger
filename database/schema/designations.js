const db =
    require("../../db");

async function createDesignationsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS designations (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            school_id INTEGER NOT NULL,

            name TEXT NOT NULL,

            status TEXT
                DEFAULT 'active',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (
                school_id
            )
            REFERENCES schools(id)

        )
    `;

    await db.runAsync(
        sql
    );

}

module.exports =
    createDesignationsTable;
