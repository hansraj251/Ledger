const db = require("../../db");

async function createPlanPricingTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS plan_pricing (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            plan_id INTEGER NOT NULL,

            duration_days INTEGER NOT NULL,

            price REAL NOT NULL,

            currency TEXT NOT NULL DEFAULT 'INR',

            status TEXT NOT NULL DEFAULT 'active',

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (plan_id)
                REFERENCES plans(id)
                ON DELETE CASCADE,

            UNIQUE (
                plan_id,
                duration_days
            )

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Plan Pricing table ready"
        );

    } catch (err) {

        console.error(
            "❌ Plan Pricing table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports = createPlanPricingTable;