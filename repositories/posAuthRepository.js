const db = require("../db");

exports.getUserWithRestaurantByEmail = async (email) => {

    const sql = `
        SELECT

            u.id,
            u.restaurant_id,
            u.name,
            u.email,
            u.mobile,
            u.password,
            u.role,
            u.status,

            r.id AS restaurant_id,
            r.name AS restaurant_name,
            r.owner_name,
            r.mobile AS restaurant_mobile,
            r.email AS restaurant_email,
            r.gst_number,
            r.fssai_number,
            r.address,
            r.city,
            r.state,
            r.pincode,
            r.logo,
            r.restaurant_code,
            r.active_device_id,

            r.plan_id,
            r.subscription_status,
            r.plan_start,
            r.plan_end,

            p.slug AS plan_slug,
            p.display_name,
            p.description AS plan_description,
            p.sort_order,
            p.status AS plan_status,

            pl.limit_value AS allowed_devices,

            r.trial_used,
            r.status AS restaurant_status

        FROM users u

        INNER JOIN restaurants r
            ON r.id = u.restaurant_id

        LEFT JOIN plans p
            ON p.id = r.plan_id

        LEFT JOIN plan_limits pl
            ON pl.plan_id = r.plan_id
           AND pl.limit_key = 'waiter_devices'

        WHERE u.email = ?
    `;

    return await db.getAsync(
        sql,
        [email]
    );

};
exports.updateActiveDeviceId = async (
    restaurantId,
    deviceId
) => {

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            active_device_id = ?

        WHERE id = ?
        `,
        [
            deviceId,
            restaurantId
        ]
    );

};

exports.replaceActiveDevice = async (
    restaurantId,
    deviceId
) => {

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            active_device_id = ?

        WHERE id = ?
        `,
        [
            deviceId,
            restaurantId
        ]
    );

};