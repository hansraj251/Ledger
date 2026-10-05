const db =
    require("../db");


exports.create =
async (
    listingId,
    buyerName,
    buyerMobile,
    message,
    buyerAccessTokenHash,
    buyerAccessTokenExpiresAt
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO property_contact_requests
            (
                listing_id,
                buyer_name,
                buyer_mobile,
                message,
                buyer_access_token_hash,
                buyer_access_token_expires_at
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            `,
            [
                listingId,
                buyerName,
                buyerMobile,
                message,
                buyerAccessTokenHash,
                buyerAccessTokenExpiresAt
            ]
        );

    return await exports.getById(
        result.lastID
    );
};


exports.getById =
async (
    requestId
) => {

    return await db.getAsync(
        `
        SELECT
            r.*,
            l.title AS listing_title,
            l.seller_id
        FROM property_contact_requests r
        INNER JOIN property_listings l
            ON l.id = r.listing_id
        WHERE r.id = ?
        `,
        [
            requestId
        ]
    );

};


exports.getSellerRequests =
async (
    sellerId
) => {

    return await db.allAsync(
        `
        SELECT
            r.id,
            r.listing_id,
            r.buyer_name,
            r.buyer_mobile,
            r.message,
            r.status,
            r.contacted_at,
            r.contact_shared,
            r.contact_shared_at,
            r.created_at,
            l.title AS listing_title
        FROM property_contact_requests r
        INNER JOIN property_listings l
            ON l.id = r.listing_id
        WHERE l.seller_id = ?
        ORDER BY
            r.created_at DESC
        `,
        [
            sellerId
        ]
    );

};


exports.updateStatus =
async (
    requestId,
    sellerId,
    status
) => {

    const result =
        await db.runAsync(
            `
            UPDATE property_contact_requests
            SET
                status = ?,
                contacted_at =
                    CASE
                        WHEN ? = 'contacted'
                        THEN CURRENT_TIMESTAMP
                        ELSE contacted_at
                    END,
                updated_at =
                    CURRENT_TIMESTAMP
            WHERE id = ?
            AND listing_id IN (
                SELECT id
                FROM property_listings
                WHERE seller_id = ?
            )
            `,
            [
                status,
                status,
                requestId,
                sellerId
            ]
        );

    if (
        result.changes === 0
    ) {

        return null;

    }

    return await exports.getById(
        requestId
    );

};
exports.deleteRequest =
async (
    requestId,
    sellerId
) => {

    const result =
        await db.runAsync(
            `
            DELETE FROM property_contact_requests
            WHERE id = ?
            AND status = 'closed'
            AND listing_id IN (
                SELECT id
                FROM property_listings
                WHERE seller_id = ?
            )
            `,
            [
                requestId,
                sellerId
            ]
        );

    return result.changes > 0;
};
exports.updateContactShared =
async (
    requestId,
    sellerId
) => {

    const result =
        await db.runAsync(
            `
            UPDATE property_contact_requests
            SET
                contact_shared = 1,
                contact_shared_at =
                    CURRENT_TIMESTAMP,
                updated_at =
                    CURRENT_TIMESTAMP
            WHERE id = ?
            AND listing_id IN (
                SELECT id
                FROM property_listings
                WHERE seller_id = ?
            )
            `,
            [
                requestId,
                sellerId
            ]
        );

    if (
        result.changes === 0
    ) {
        return null;
    }

    return await exports.getById(
        requestId
    );
};

exports.getSharedContact =
async (
    requestId
) => {

    return await db.getAsync(
        `
        SELECT
            r.id AS request_id,
            r.listing_id,
            r.contact_shared,
            r.contact_shared_at,
            r.buyer_access_token_hash,
            r.buyer_access_token_expires_at,
            u.name AS seller_name,
            u.email AS seller_email,
            u.mobile AS seller_mobile

        FROM property_contact_requests r

        INNER JOIN property_listings l
            ON l.id = r.listing_id

        INNER JOIN property_users u
            ON u.id = l.seller_id

        WHERE r.id = ?
        AND r.contact_shared = 1
        AND l.status = 'published'
        AND l.moderation_status = 'approved'
        `,
        [
            requestId
        ]
    );
};