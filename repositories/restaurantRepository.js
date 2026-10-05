const db = require("../db");


exports.getById = async (restaurantId) => {

    return await db.getAsync(
        `
        SELECT *
        FROM restaurants
        WHERE id = ?
        `,
        [restaurantId]
    );

};


exports.getPlanDetails = async (restaurantId) => {

    return await db.getAsync(
        `
        SELECT

            r.plan_id,

            p.display_name,

            r.subscription_status,

            r.plan_start,

            r.plan_end

        FROM restaurants r

        LEFT JOIN plans p
            ON p.id = r.plan_id

        WHERE r.id = ?
        `,
        [restaurantId]
    );

};
exports.expireSubscription =
async (restaurantId) => {

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            subscription_status =
                'expired',

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [
            restaurantId
        ]
    );

};