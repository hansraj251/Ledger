const db = require("../../db");

async function createPropertyListingImagesTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS property_listing_images (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            listing_id INTEGER NOT NULL,

            image_url TEXT NOT NULL,

            sort_order INTEGER
                NOT NULL DEFAULT 0,

            is_cover INTEGER
                NOT NULL DEFAULT 0,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            FOREIGN KEY (listing_id)
                REFERENCES property_listings(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Property listing images table ready"
        );

    } catch (err) {

        console.error(
            "❌ Property listing images table creation failed:",
            err.message
        );

        throw err;

    }

}

module.exports =
    createPropertyListingImagesTable;
