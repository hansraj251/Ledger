const db = require("../../db");

async function seedPlans() {

    const count =
        await db.getAsync(
            "SELECT COUNT(*) AS total FROM plans"
        );

    if (count.total > 0) {

        console.log(
            "✅ Plans already exist"
        );

        return;

    }

    await db.runAsync(
        `
        INSERT INTO plans
        (
            slug,
            display_name,
            description,
            sort_order
        )
        VALUES
        (?, ?, ?, ?)
        `,
        [
            "plus",
            "Align Plus",
            "Ideal for single device restaurants",
            1
        ]
    );

    await db.runAsync(
        `
        INSERT INTO plans
        (
            slug,
            display_name,
            description,
            sort_order
        )
        VALUES
        (?, ?, ?, ?)
        `,
        [
            "pro",
            "Align Pro",
            "Ideal for multi-device restaurants",
            2
        ]
    );

    console.log(
        "✅ Default plans created"
    );

}

module.exports = seedPlans;