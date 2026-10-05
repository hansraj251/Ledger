const db = require("../../db");

async function createPropertyListingSavesTable() {

    const sql = `
        CREATE TABLE IF NOT EXISTS property_listing_saves (

            id INTEGER PRIMARY KEY AUTOINCREMENT,

            user_id INTEGER NOT NULL,

            listing_id INTEGER NOT NULL,

            created_at DATETIME
                DEFAULT CURRENT_TIMESTAMP,

            UNIQUE (
                user_id,
                listing_id
            ),

            FOREIGN KEY (user_id)
                REFERENCES property_users(id)
                ON DELETE CASCADE,

            FOREIGN KEY (listing_id)
                REFERENCES property_listings(id)
                ON DELETE CASCADE

        )
    `;

    try {

        await db.runAsync(sql);

        console.log(
            "✅ Property listing saves table ready"
        );

    } catch (err) {

        console.error(
            "❌ Property listing saves table creation failed:",
            err.message
        );c

        throw err;

    }

}
/*
|--------------------------------------------------------------------------
| Save Listing
|--------------------------------------------------------------------------
*/

exports.saveListing = async (
    userId,
    listingId
) => {

    await db.runAsync(
        `
        INSERT OR IGNORE INTO property_listing_saves
        (
            user_id,
            listing_id
        )
        VALUES
        (
            ?,
            ?
        )
        `,
        [
            userId,
            listingId
        ]
    );

    return await db.getAsync(
        `
        SELECT
            id,
            user_id,
            listing_id,
            created_at
        FROM property_listing_saves
        WHERE user_id = ?
        AND listing_id = ?
        `,
        [
            userId,
            listingId
        ]
    );

};


/*
|--------------------------------------------------------------------------
| Unsave Listing
|--------------------------------------------------------------------------
*/

exports.unsaveListing = async (
    userId,
    listingId
) => {

    const result =
        await db.runAsync(
            `
            DELETE FROM property_listing_saves
            WHERE user_id = ?
            AND listing_id = ?
            `,
            [
                userId,
                listingId
            ]
        );

    return result.changes;

};


/*
|--------------------------------------------------------------------------
| Check Saved Listing
|--------------------------------------------------------------------------
*/

exports.isListingSaved = async (
    userId,
    listingId
) => {

    const saved =
        await db.getAsync(
            `
            SELECT
                id
            FROM property_listing_saves
            WHERE user_id = ?
            AND listing_id = ?
            `,
            [
                userId,
                listingId
            ]
        );

    return !!saved;

};


/*
|--------------------------------------------------------------------------
| Get Saved Listings
|--------------------------------------------------------------------------
*/

exports.getSavedListings = async (
    userId
) => {

    const listings =
        await db.allAsync(
            `
            SELECT
                l.*,
                s.created_at AS saved_at
            FROM property_listing_saves s

            INNER JOIN property_listings l
                ON l.id = s.listing_id

            WHERE s.user_id = ?

            ORDER BY
                s.created_at DESC
            `,
            [
                userId
            ]
        );


    for (
        const listing of listings
    ) {

        listing.images =
            await db.allAsync(
                `
                SELECT
                    id,
                    image_url,
                    sort_order,
                    is_cover
                FROM property_listing_images
                WHERE listing_id = ?
                ORDER BY
                    is_cover DESC,
                    sort_order ASC,
                    id ASC
                `,
                [
                    listing.id
                ]
            );

    }


    return listings;

};

module.exports =
    createPropertyListingSavesTable;