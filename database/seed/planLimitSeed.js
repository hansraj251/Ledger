const db = require("../../db");

async function seedPlanLimits() {

    const count =
        await db.getAsync(
            "SELECT COUNT(*) AS total FROM plan_limits"
        );

    if (count.total > 0) {

        console.log(
            "✅ Plan Limits already exist"
        );

        return;

    }

    const plus =
        await db.getAsync(
            `
            SELECT id
            FROM plans
            WHERE slug = 'plus'
            `
        );

    const pro =
        await db.getAsync(
            `
            SELECT id
            FROM plans
            WHERE slug = 'pro'
            `
        );

    if (!plus || !pro) {

        throw new Error(
            "Plans not found"
        );

    }

    await db.runAsync(
        `
        INSERT INTO plan_limits
        (
            plan_id,
            limit_key,
            limit_value
        )
        VALUES (?, ?, ?)
        `,
        [
            plus.id,
            "waiter_devices",
            1
        ]
    );

    await db.runAsync(
        `
        INSERT INTO plan_limits
        (
            plan_id,
            limit_key,
            limit_value
        )
        VALUES (?, ?, ?)
        `,
        [
            pro.id,
            "waiter_devices",
            999
        ]
    );

    console.log(
        "✅ Default Plan Limits created"
    );

}

module.exports = seedPlanLimits;