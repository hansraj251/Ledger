const db = require("../../db");

async function createPropertyContactRequestsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS property_contact_requests (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            listing_id INTEGER NOT NULL,

            buyer_name TEXT NOT NULL,

            buyer_mobile TEXT NOT NULL,

            message TEXT,

            status TEXT
                NOT NULL DEFAULT 'new',

            contacted_at DATETIME,

            contact_shared INTEGER
                NOT NULL DEFAULT 0,

            contact_shared_at DATETIME,

            buyer_access_token_hash TEXT,

            buyer_access_token_expires_at DATETIME,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (listing_id)
                REFERENCES property_listings(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Property contact requests table ready"
        );

    } catch (err) {

        console.error(
            "❌ Property contact requests table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createPropertyContactRequestsTable;
