const db =
    require("../db");

exports.getByEmail =
async (
    email
) => {

    return await db.getAsync(
        `
        SELECT
            *,
            CASE
                WHEN school_id IS NOT NULL
                    THEN 'school'
                WHEN restaurant_id IS NOT NULL
                    THEN 'food'
                ELSE NULL
            END AS business_type
        FROM users
        WHERE email = ?
        `,
        [
            email
        ]
    );

};

exports.getByEmailOrMobile =
async (
    email,
    mobile
) => {

    return await db.getAsync(
        `
        SELECT id
        FROM users
        WHERE
            email = ?
            OR mobile = ?
        `,
        [
            email,
            mobile
        ]
    );

};

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

exports.existsByEmail =
async (
    email
) => {

    return await db.getAsync(
        `
        SELECT id

        FROM users

        WHERE email = ?
        `,
        [
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
            *,
            CASE
                WHEN school_id IS NOT NULL
                    THEN 'school'
                WHEN restaurant_id IS NOT NULL
                    THEN 'food'
                ELSE NULL
            END AS business_type
        FROM users
        WHERE username = ?
        `,
        [
            username
        ]
    );

};