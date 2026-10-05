const bcrypt = require("bcrypt");
const db = require("../../db");

async function seedSuperAdmin() {

    const admin =
        await db.getAsync(
            "SELECT id FROM super_admin LIMIT 1"
        );

    if (admin) {

        console.log(
            "✅ Super Admin already exists"
        );

        return;

    }

    const password =
        await bcrypt.hash(
            "admin123",
            10
        );

    await db.runAsync(
        `
        INSERT INTO super_admin
        (
            username,
            password,
            name,
            status
        )
        VALUES (?, ?, ?, ?)
        `,
        [
            "admin",
            password,
            "Super Admin",
            "active"
        ]
    );

    console.log(
        "✅ Default Super Admin created"
    );

}

module.exports = seedSuperAdmin;