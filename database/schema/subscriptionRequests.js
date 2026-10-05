const db =
    require("../../db");

function createSubscriptionRequestsTable()
{
    db.run(
        `
        CREATE TABLE IF NOT EXISTS subscription_requests
        (
            id INTEGER PRIMARY KEY AUTOINCREMENT,

            restaurant_id INTEGER NOT NULL,

            current_plan_id INTEGER NOT NULL,

            requested_plan_id INTEGER NOT NULL,

            status TEXT NOT NULL DEFAULT 'pending',

            remarks TEXT,

            processed_by INTEGER,

            created_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (restaurant_id)
                REFERENCES restaurants(id),

            FOREIGN KEY (current_plan_id)
                REFERENCES plans(id),

            FOREIGN KEY (requested_plan_id)
                REFERENCES plans(id),

            FOREIGN KEY (processed_by)
    REFERENCES super_admin(id)
        )
        `
    );
}

module.exports =
    createSubscriptionRequestsTable;