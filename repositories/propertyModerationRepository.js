const db =
    require("../db");


exports.getPendingListings =
async () => {

    return await db.allAsync(
        `
        SELECT
            l.id,
            l.seller_id,
            l.title,
            l.subtitle,
            l.description,
            l.price,
            l.price_type,
            l.token_required,
            l.token_amount,
            l.status,
            l.moderation_status,
            l.created_at,
            l.updated_at,
            u.name AS seller_name,
            u.email AS seller_email,
            u.mobile AS seller_mobile
        FROM property_listings l
        INNER JOIN property_users u
            ON u.id = l.seller_id
        WHERE l.moderation_status = 'pending'
        ORDER BY
            l.created_at ASC
        `
    );

};


exports.getById =
async (
    listingId
) => {

    return await db.getAsync(
        `
        SELECT
            l.*,
            u.name AS seller_name,
            u.email AS seller_email,
            u.mobile AS seller_mobile
        FROM property_listings l
        INNER JOIN property_users u
            ON u.id = l.seller_id
        WHERE l.id = ?
        `,
        [
            listingId
        ]
    );

};


exports.updateModeration =
async (
    listingId,
    moderationStatus
) => {

    const result =
        await db.runAsync(
            `
            UPDATE property_listings
            SET
                moderation_status = ?,
                status =
                    CASE
                        WHEN ? = 'approved'
                            THEN 'published'
                        WHEN ? = 'rejected'
                            THEN 'hidden'
                        ELSE status
                    END,
                updated_at =
                    CURRENT_TIMESTAMP
            WHERE id = ?
            `,
            [
                moderationStatus,
                moderationStatus,
                moderationStatus,
                listingId
            ]
        );

    if (
        result.changes === 0
    ) {
        return null;
    }

    return await exports.getById(
        listingId
    );

};
