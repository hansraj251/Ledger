const db = require("../db");


/*
|--------------------------------------------------------------------------
| Create Listing
|--------------------------------------------------------------------------
*/

exports.create = async (

    sellerId,

    title,

    subtitle,

    description,

    price,

    priceType,

    rentAmount,

    tokenRequired,

    tokenAmount,

    status,

    moderationStatus,

    contactPreference,
    location,
address,
city,
state,
pincode

) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO property_listings
            (
                seller_id,
                title,
                subtitle,
                description,
                price,
                price_type,
                rent_amount,
                token_required,
                token_amount,
                status,
                moderation_status,
                contact_preference,
                location,
address,
city,
state,
pincode
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            `,
            [

                sellerId,

                title,

                subtitle,

                description,

                price,

                priceType,

                rentAmount,

                tokenRequired,

                tokenAmount,

                status,

                moderationStatus,

                contactPreference,
                location,
address,
city,
state,
pincode

            ]
        );


    return await exports.getById(
        result.lastID
    );

};


/*
|--------------------------------------------------------------------------
| Get Listing By ID
|--------------------------------------------------------------------------
*/

exports.getById = async (

    listingId

) => {

    return await db.getAsync(
        `
        SELECT
            l.*,
            u.name AS seller_name
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


/*
|--------------------------------------------------------------------------
| Get Listing For Seller
|--------------------------------------------------------------------------
*/

exports.getByIdForSeller = async (

    listingId,

    sellerId

) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM property_listings
        WHERE id = ?
        AND seller_id = ?
        `,
        [
            listingId,
            sellerId
        ]
    );

};


/*
|--------------------------------------------------------------------------
| Get Seller Listings
|--------------------------------------------------------------------------
*/

exports.getSellerListings = async (

    sellerId

) => {

    const listings =
        await db.allAsync(
            `
            SELECT
                l.*
            FROM property_listings l
            WHERE l.seller_id = ?
            ORDER BY
                l.created_at DESC
            `,
            [
                sellerId
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


/*
|--------------------------------------------------------------------------
| Get Published Listings
|--------------------------------------------------------------------------
*/

exports.getPublishedListings = async () => {

    const listings =
        await db.allAsync(
            `
            SELECT
                l.id,
                l.title,
                l.subtitle,
                l.description,
                l.price,
                l.price_type,
                l.rent_amount,
                l.token_required,
                l.token_amount,
                l.contact_preference,
l.location,
l.address,
l.city,
l.state,
l.pincode,
l.latitude,
l.longitude,
l.created_at
            FROM property_listings l
            WHERE l.status = 'published'
            AND l.moderation_status = 'approved'
            ORDER BY
                l.created_at DESC
            `
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


/*
|--------------------------------------------------------------------------
| Update Listing
|--------------------------------------------------------------------------
*/

exports.update = async (

    listingId,

    sellerId,

    title,

    subtitle,

    description,

    price,

    priceType,

    rentAmount,

    tokenRequired,

    tokenAmount,

    contactPreference,

    location,

    address,

    city,

    state,

    pincode

) => {

    await db.runAsync(

        `

        UPDATE property_listings

        SET

            title = ?,

            subtitle = ?,

            description = ?,

            price = ?,

            price_type = ?,

            rent_amount = ?,

            token_required = ?,

            token_amount = ?,

            contact_preference = ?,

            location = ?,

            address = ?,

            city = ?,

            state = ?,

            pincode = ?,

            updated_at =

                CURRENT_TIMESTAMP

        WHERE id = ?

        AND seller_id = ?

        `,

        [

            title,

            subtitle,

            description,

            price,

            priceType,

            rentAmount,

            tokenRequired,

            tokenAmount,

            contactPreference,

            location,

            address,

            city,

            state,

            pincode,

            listingId,

            sellerId

        ]

    );

    return await exports.getByIdForSeller(

        listingId,

        sellerId

    );

};

/*
|--------------------------------------------------------------------------
| Update Listing Status
|--------------------------------------------------------------------------
*/

exports.updateStatus = async (

    listingId,

    sellerId,

    status

) => {

    await db.runAsync(
        `
        UPDATE property_listings
        SET

            status = ?,

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?

        AND seller_id = ?
        `,
        [

            status,

            listingId,

            sellerId

        ]
    );


    return await exports.getById(
        listingId
    );

};


/*
|--------------------------------------------------------------------------
| Delete Listing
|--------------------------------------------------------------------------
*/

exports.delete = async (

    listingId,

    sellerId

) => {

    const result =
        await db.runAsync(
            `
            DELETE FROM property_listings

            WHERE id = ?

            AND seller_id = ?
            `,
            [
                listingId,
                sellerId
            ]
        );


    return result.changes;

};


/*
|--------------------------------------------------------------------------
| Get Published Listing By ID
|--------------------------------------------------------------------------
*/

exports.getPublishedById = async (

    listingId

) => {

    const listing =
        await db.getAsync(
            `
            SELECT
                l.id,
                l.seller_id,
                l.title,
                l.subtitle,
                l.description,
                l.price,
                l.price_type,
                l.rent_amount,
                l.token_required,
                l.token_amount,
                l.contact_preference,
                l.created_at,

                u.name AS seller_name,
                u.mobile AS seller_mobile

            FROM property_listings l

            INNER JOIN property_users u
                ON u.id = l.seller_id

            WHERE l.id = ?

            AND l.status = 'published'

            AND l.moderation_status = 'approved'
            `,
            [
                listingId
            ]
        );


    if (
        !listing
    ) {

        return null;

    }


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
                listingId
            ]
        );


    return listing;

};
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
        INSERT OR IGNORE INTO
            property_listing_saves
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
            DELETE FROM
                property_listing_saves

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