const db =
    require("../../db");

async function createAppVersionTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS app_version (

            id INTEGER
                PRIMARY KEY,

            latest_version TEXT
                NOT NULL

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ App version table ready"
        );

    } catch (err) {

        console.error(
            "❌ App version table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createAppVersionTable;