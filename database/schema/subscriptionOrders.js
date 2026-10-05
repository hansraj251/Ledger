const db =
    require("../../db");

async function createSubscriptionOrdersTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS subscription_orders (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            restaurant_id INTEGER,

            school_id INTEGER,

            plan_id INTEGER NOT NULL,

            plan_pricing_id INTEGER NOT NULL,

            razorpay_order_id TEXT NOT NULL UNIQUE,

            razorpay_payment_id TEXT,

            amount REAL NOT NULL,

            currency TEXT NOT NULL,

            duration_days INTEGER NOT NULL,

            payment_method TEXT,

            status TEXT DEFAULT 'pending',

            paid_at DATETIME,

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (restaurant_id)
                REFERENCES restaurants(id),

            FOREIGN KEY (school_id)
                REFERENCES schools(id),

            FOREIGN KEY (plan_id)
                REFERENCES plans(id),

            FOREIGN KEY (plan_pricing_id)
                REFERENCES plan_pricing(id)

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Subscription Orders table ready"
        );

    } catch (err) {

        console.error(
            "❌ Subscription Orders table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createSubscriptionOrdersTable;