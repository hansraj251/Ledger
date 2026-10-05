const db = require("../db");

exports.getByPlanId = async (planId) => {

    return await db.allAsync(
        `
        SELECT
            id,
            plan_id,
            duration_days,
            price,
            currency,
            status,
            created_at,
            updated_at
        FROM plan_pricing
        WHERE plan_id = ?
        ORDER BY duration_days
        `,
        [planId]
    );

};

exports.getById = async (pricingId) => {

    return await db.getAsync(
        `
        SELECT
            id,
            plan_id,
            duration_days,
            price,
            currency,
            status,
            created_at,
            updated_at
        FROM plan_pricing
        WHERE id = ?
        `,
        [pricingId]
    );

};

exports.create = async (
    planId,
    durationDays,
    price,
    currency,
    status
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO plan_pricing (
                plan_id,
                duration_days,
                price,
                currency,
                status
            )
            VALUES (?, ?, ?, ?, ?)
            `,
            [
                planId,
                durationDays,
                price,
                currency,
                status
            ]
        );

    return result.lastID;

};

exports.update = async (
    pricingId,
    durationDays,
    price,
    currency,
    status
) => {

    await db.runAsync(
        `
        UPDATE plan_pricing
        SET
            duration_days = ?,
            price = ?,
            currency = ?,
            status = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        `,
        [
            durationDays,
            price,
            currency,
            status,
            pricingId
        ]
    );

};

exports.remove = async (pricingId) => {

    await db.runAsync(
        `
        DELETE FROM plan_pricing
        WHERE id = ?
        `,
        [pricingId]
    );

};

exports.getByPlanAndDuration = async (
    planId,
    durationDays
) => {

    return await db.getAsync(
        `
        SELECT
            id,
            plan_id,
            duration_days,
            price,
            currency,
            status
        FROM plan_pricing
        WHERE
            plan_id = ?
            AND duration_days = ?
        `,
        [
            planId,
            durationDays
        ]
    );

};
exports.getAllActive = async () => {

    return await db.allAsync(
        `
        SELECT
            id,
            plan_id,
            duration_days,
            price,
            currency,
            status
        FROM plan_pricing
        WHERE status = 'active'
        ORDER BY
            plan_id,
            duration_days
        `
    );

};

exports.getAll = async () => {

    return await db.allAsync(
        `
        SELECT

            pp.id,

            pp.plan_id,

            p.display_name AS plan_name,

            pp.duration_days,

            pp.price,

            pp.currency,

            pp.status,

            pp.created_at,

            pp.updated_at

        FROM plan_pricing pp

        INNER JOIN plans p
            ON p.id = pp.plan_id

        ORDER BY

            p.display_name,

            pp.duration_days
        `
    );

};