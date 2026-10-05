const db = require("../db");

exports.getAll = async () => {

    return await db.allAsync(
        `
        SELECT
            id,
            username,
            name,
            status
        FROM super_admin
        ORDER BY id ASC
        `
    );

};

exports.getById = async (
    id
) => {

    return await db.getAsync(
        `
        SELECT
            id,
            username,
            name,
            status,
            password
        FROM super_admin
        WHERE id = ?
        `,
        [
            id
        ]
    );

};

exports.getByUsername = async (
    username
) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM super_admin
        WHERE username = ?
        `,
        [
            username
        ]
    );

};

exports.create = async (
    name,
    username,
    password,
    status
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO super_admin
            (
                name,
                username,
                password,
                status
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?
            )
            `,
            [
                name,
                username,
                password,
                status
            ]
        );

    return result.lastID;

};

exports.update = async (
    id,
    name,
    username,
    status
) => {
    
const account =

    await db.runAsync(
        `
        UPDATE super_admin
        SET
            name = ?,
            username = ?,
            status = ?
        WHERE id = ?
        `,
        [
            name,
            username,
            status,
            id
        ]
    );

};

exports.updatePassword = async (
    id,
    password
) => {
    if (
    !password
) {

    throw new Error(
        "Password is required"
    );

}

if (
    password.length < 8
) {

    throw new Error(
        "Password must be at least 8 characters"
    );

}

    await db.runAsync(
        `
        UPDATE super_admin
        SET
            password = ?
        WHERE id = ?
        `,
        [
            password,
            id
        ]
    );

};

exports.delete = async (
    id
) => {

    await db.runAsync(
        `
        DELETE
        FROM super_admin
        WHERE id = ?
        `,
        [
            id
        ]
    );

};
exports.countActive = async () => {

    const result =
        await db.getAsync(
            `
            SELECT
                COUNT(*) AS total
            FROM super_admin
            WHERE status = 'active'
            `
        );

    return result.total;

};

exports.countOthersActive = async (
    id
) => {

    const result =
        await db.getAsync(
            `
            SELECT
                COUNT(*) AS total
            FROM super_admin
            WHERE
                status = 'active'
                AND id != ?
            `,
            [
                id
            ]
        );

    return result.total;

};