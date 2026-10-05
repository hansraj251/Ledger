const db = require("../db");


exports.create = async (
    listingId,
    imageUrl,
    sortOrder,
    isCover
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO property_listing_images
            (
                listing_id,
                image_url,
                sort_order,
                is_cover
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?
            )
            `,
            [
                listingId,
                imageUrl,
                sortOrder,
                isCover
            ]
        );

    return await db.getAsync(
        `
        SELECT
            *
        FROM property_listing_images
        WHERE id = ?
        `,
        [
            result.lastID
        ]
    );

};


exports.getByListingId =
async (
    listingId
) => {

    return await db.allAsync(
        `
        SELECT
            *
        FROM property_listing_images
        WHERE listing_id = ?
        ORDER BY
            is_cover DESC,
            sort_order ASC,
            id ASC
        `,
        [
            listingId
        ]
    );

};


exports.getById =
async (
    imageId
) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM property_listing_images
        WHERE id = ?
        `,
        [
            imageId
        ]
    );

};


exports.clearCover =
async (
    listingId
) => {

    await db.runAsync(
        `
        UPDATE property_listing_images
        SET
            is_cover = 0
        WHERE listing_id = ?
        `,
        [
            listingId
        ]
    );

};


exports.setCover =
async (
    imageId,
    listingId
) => {

    await db.runAsync(
        `
        UPDATE property_listing_images
        SET
            is_cover = 1
        WHERE id = ?
        AND listing_id = ?
        `,
        [
            imageId,
            listingId
        ]
    );

};


exports.delete =
async (
    imageId,
    listingId
) => {

    const result =
        await db.runAsync(
            `
            DELETE FROM property_listing_images
            WHERE id = ?
            AND listing_id = ?
            `,
            [
                imageId,
                listingId
            ]
        );

    return result.changes;

};


exports.deleteByListingId =
async (
    listingId
) => {

    await db.runAsync(
        `
        DELETE FROM property_listing_images
        WHERE listing_id = ?
        `,
        [
            listingId
        ]
    );

};

