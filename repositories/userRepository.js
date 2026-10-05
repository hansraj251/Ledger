const db =
    require("../db");

exports.updatePassword =
async (
    email,
    passwordHash
) => {

    await db.runAsync(
        `
        UPDATE users

        SET

            password = ?

        WHERE
            email = ?
        `,
        [
            passwordHash,
            email
        ]
    );

};

exports.getByUsername =
async (
    username
) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM users
        WHERE username = ?
        `,
        [
            username
        ]
    );

};

exports.create =
async (
    data
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO users
            (
                school_id,
                name,
                username,
                password,
                role,
                status
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?
            )
            `,
            [
                data.schoolId,
                data.name,
                data.username,
                data.passwordHash,
                data.role,
                data.status
            ]
        );

    return await db.getAsync(
        `
        SELECT
            id,
            school_id,
            name,
            username,
            role,
            status,
            created_at
        FROM users
        WHERE id = ?
        `,
        [
            result.lastID
        ]
    );

};

exports.getAttendanceUsersBySchool =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT
            id,
            school_id,
            name,
            username,
            role,
            status,
            created_at
        FROM users
        WHERE school_id = ?
        AND role = 'attendance'
        ORDER BY name ASC
        `,
        [
            schoolId
        ]
    );

};