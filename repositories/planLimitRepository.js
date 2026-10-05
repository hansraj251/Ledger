const db = require("../db");

exports.getLimit = async (planId, limitKey) => {

    return await db.getAsync(
        `
        SELECT
            limit_value
        FROM plan_limits
        WHERE
            plan_id = ?
            AND limit_key = ?
        LIMIT 1
        `,
        [planId, limitKey]
    );

};

exports.getWaiterDeviceLimit = async (planId) => {

    const limit = await exports.getLimit(
        planId,
        "waiter_devices"
    );

    return limit ? limit.limit_value : null;

};
exports.updateWaiterDeviceLimit =
async (
    planId,
    value
) => {

    const existing =
        await exports.getLimit(
            planId,
            "waiter_devices"
        );

    if (existing) {

        await db.runAsync(
            `
            UPDATE plan_limits
            SET
                limit_value = ?
            WHERE
                plan_id = ?
                AND limit_key = 'waiter_devices'
            `,
            [
                value,
                planId
            ]
        );

        return;

    }

    await db.runAsync(
        `
        INSERT INTO plan_limits
        (
            plan_id,
            limit_key,
            limit_value
        )
        VALUES
        (
            ?,
            'waiter_devices',
            ?
        )
        `,
        [
            planId,
            value
        ]
    );

};

exports.getActiveStudentLimit =
async (
    planId
) => {

    const limit =
        await exports.getLimit(
            planId,
            "active_students"
        );

    return limit
        ? limit.limit_value
        : null;

};

exports.updateActiveStudentLimit =
async (
    planId,
    value
) => {

    const existing =
        await exports.getLimit(
            planId,
            "active_students"
        );

    if (existing) {

        await db.runAsync(
            `
            UPDATE plan_limits
            SET
                limit_value = ?
            WHERE
                plan_id = ?
                AND limit_key = 'active_students'
            `,
            [
                value,
                planId
            ]
        );

        return;

    }

    await db.runAsync(
        `
        INSERT INTO plan_limits
        (
            plan_id,
            limit_key,
            limit_value
        )
        VALUES
        (
            ?,
            'active_students',
            ?
        )
        `,
        [
            planId,
            value
        ]
    );

};