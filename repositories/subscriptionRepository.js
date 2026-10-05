const db = require("../db");

exports.getSubscription = async (
    restaurantId
) => {

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
exports.getSchoolSubscription =
async (
    schoolId
) => {

    return await db.getAsync(
        `
        SELECT

            s.plan_id,

            p.display_name,

            s.subscription_status,

            s.plan_start,

            s.plan_end

        FROM schools s

        LEFT JOIN plans p
            ON p.id = s.plan_id

        WHERE s.id = ?
        `,
        [
            schoolId
        ]
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
exports.updateSubscription =
    async (
        restaurantId,
        planId,
        planStart,
        planEnd
    ) =>
{

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            plan_id = ?,

            subscription_status =
                'active',

            plan_start = ?,

            plan_end = ?,

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [
            planId,
            planStart,
            planEnd,
            restaurantId
        ]
    );

};
exports.updateSchoolSubscription =
async (
    schoolId,
    planId,
    planStart,
    planEnd
) => {

    await db.runAsync(
        `
        UPDATE schools

        SET

            plan_id = ?,

            subscription_status =
                'active',

            plan_start = ?,

            plan_end = ?,

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [
            planId,
            planStart,
            planEnd,
            schoolId
        ]
    );

};
exports.getSubscriptionSyncData = async (
    restaurantId
) => {

    return await db.getAsync(
        `
        SELECT

            r.plan_id,

            r.subscription_status,

            r.plan_start,

            r.plan_end,
            r.active_device_id,

            p.slug,
            p.display_name,
            p.description,
            p.sort_order,
            p.status AS plan_status,

            pl.limit_key,
            pl.limit_value

        FROM restaurants r

        LEFT JOIN plans p
            ON p.id = r.plan_id

        LEFT JOIN plan_limits pl
            ON pl.plan_id = r.plan_id
           AND pl.limit_key = 'waiter_devices'

        WHERE r.id = ?
        `,
        [restaurantId]
    );

};
