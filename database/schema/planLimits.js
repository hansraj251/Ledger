const db = require("../../db");

async function createPlanLimitsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS plan_limits (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            plan_id INTEGER NOT NULL,

            limit_key TEXT NOT NULL,

            limit_value INTEGER NOT NULL,

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (plan_id)
                REFERENCES plans(id)
                ON DELETE CASCADE,

            UNIQUE (
                plan_id,
                limit_key
            )

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Plan Limits table ready"
        );

    } catch (err) {

        console.error(
            "❌ Plan Limits table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createPlanLimitsTable;