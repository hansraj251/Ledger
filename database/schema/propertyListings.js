const db = require("../../db");

async function createPropertyListingsTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS property_listings (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            seller_id INTEGER NOT NULL,

            title TEXT NOT NULL,

            subtitle TEXT,

            description TEXT,

            price REAL,

            price_type TEXT
                DEFAULT 'fixed',

            token_required INTEGER
                NOT NULL DEFAULT 0,

            token_amount REAL,

            status TEXT
                NOT NULL DEFAULT 'draft',

            moderation_status TEXT
                NOT NULL DEFAULT 'pending',

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            updated_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,
            
            contact_preference TEXT
                NOT NULL DEFAULT 'show',

            rent_amount REAL,    

                        location TEXT,
            address TEXT,
            city TEXT,
            state TEXT,
            pincode TEXT,
            latitude REAL,
            longitude REAL,

            FOREIGN KEY (seller_id)
                REFERENCES property_users(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Property listings table ready"
        );

    } catch (err) {

        console.error(
            "❌ Property listings table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createPropertyListingsTable;
