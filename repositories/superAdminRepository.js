const db = require("../db");

exports.getDashboardStats = async () => {

    const totalRestaurants =
        await db.getAsync(
            `
            SELECT COUNT(*) AS total
            FROM restaurants
            `
        );

    const totalSchools =
        await db.getAsync(
            `
            SELECT COUNT(*) AS total
            FROM schools
            `
        );

    return {

        totalRestaurants:
            totalRestaurants.total,

        totalSchools:
            totalSchools.total

    };

};

exports.getRestaurants = async () => {

    return await db.allAsync(
        `
        SELECT

            r.id,

            r.restaurant_code,

            r.name,

            r.owner_name,

            r.mobile,

            r.email,

            p.display_name AS plan,

            r.subscription_status,

            r.plan_start,

            r.plan_end,

            r.created_at

        FROM restaurants r

        LEFT JOIN plans p
            ON p.id = r.plan_id

        ORDER BY r.id DESC
        `
    );

};

exports.getRestaurantById = async (
    restaurantId
) => {

    return await db.getAsync(
        `
        SELECT

            r.id,

            r.restaurant_code,

            r.name,

            r.owner_name,

            r.mobile,

            r.email,

            r.address,

            r.city,

            r.state,

            r.pincode,

            r.plan_id,

            p.display_name AS plan,

            r.subscription_status,

            r.plan_start,

            r.plan_end,

            (
    SELECT
        pl.limit_value

    FROM plan_limits pl

    WHERE

        pl.plan_id = r.plan_id

        AND pl.limit_key = 'waiter_devices'

    LIMIT 1

) AS allowed_devices

        FROM restaurants r

        LEFT JOIN plans p
            ON p.id = r.plan_id

        WHERE r.id = ?
        `,
        [
            restaurantId
        ]
    );

};
exports.updateRestaurantSubscription =
async (

    restaurantId,

    planId,

    status,

    days

) => {

    if (
    status === "suspended"
) {

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            plan_id = ?,

            subscription_status = ?,

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [

            planId,

            status,

            restaurantId

        ]
    );

    return;

}

if (
    status === "expired"
) {

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            plan_id = ?,

            subscription_status = ?,

            plan_end = CURRENT_TIMESTAMP,

            updated_at = CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [

            planId,

            status,

            restaurantId

        ]
    );

    return;

}
    

    await db.runAsync(
        `
        UPDATE restaurants

        SET

            plan_id = ?,

            subscription_status = ?,

            plan_start = DATE('now'),

            plan_end = DATE(
                'now',
                '+' || ? || ' days'
            ),

            updated_at =
                CURRENT_TIMESTAMP

        WHERE id = ?
        `,
        [

            planId,

            status,

            days,

            restaurantId

        ]
    );

};

exports.getSchools = async () => {

    return await db.allAsync(

        `
        SELECT
            s.id,
            s.school_code,
            s.name,
            s.owner_name,
            s.mobile,
            s.email,
            p.display_name AS plan,
            s.subscription_status,
            s.plan_start,
            s.plan_end,
            s.created_at
        FROM schools s
        LEFT JOIN plans p
            ON p.id = s.plan_id
            AND p.plan_type = 'school'
        ORDER BY s.id DESC
        `

    );

};
exports.getSchoolById =

async (

    schoolId

) => {

    return await db.getAsync(

        `
        SELECT
            s.id,
            s.school_code,
            s.name,
            s.owner_name,
            s.mobile,
            s.email,
            s.address,
            s.city,
            s.state,
            s.pincode,
            s.plan_id,
            p.display_name AS plan,
            s.subscription_status,
            s.plan_start,
            s.plan_end,
            s.status
        FROM schools s
        LEFT JOIN plans p
            ON p.id = s.plan_id
            AND p.plan_type = 'school'
        WHERE s.id = ?
        `,

        [
            schoolId
        ]

    );

};


exports.updateSchoolSubscription =

async (

    schoolId,

    planId,

    status,

    days

) => {

    if (
        status === "suspended"
    ) {

        await db.runAsync(

            `
            UPDATE schools
            SET
                plan_id = ?,
                subscription_status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            `,

            [
                planId,
                status,
                schoolId
            ]

        );

        return;

    }

    if (
        status === "expired"
    ) {

        await db.runAsync(

            `
            UPDATE schools
            SET
                plan_id = ?,
                subscription_status = ?,
                plan_end = CURRENT_TIMESTAMP,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            `,

            [
                planId,
                status,
                schoolId
            ]

        );

        return;

    }

    await db.runAsync(

        `
        UPDATE schools
        SET
            plan_id = ?,
            subscription_status = ?,
            plan_start = DATE('now'),
            plan_end = DATE(
                'now',
                '+' || ? || ' days'
            ),
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        `,

        [
            planId,
            status,
            days,
            schoolId
        ]

    );

};