const db =
    require("../db");

exports.getAll =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM designations
        WHERE school_id = ?
        ORDER BY name
        `,
        [
            schoolId
        ]
    );

};

exports.getById =
async (
    schoolId,
    designationId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM designations
        WHERE school_id = ?
        AND id = ?
        `,
        [
            schoolId,
            designationId
        ]
    );

};

exports.getByName =
async (
    schoolId,
    name
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM designations
        WHERE school_id = ?
        AND name = ?
        `,
        [
            schoolId,
            name
        ]
    );

};

exports.create =
async (
    designation
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO designations (
                school_id,
                name
            )
            VALUES (
                ?,
                ?
            )
            `,
            [
                designation.schoolId,
                designation.name
            ]
        );

    return result.lastID;

};

exports.updateStatus =
async (
    schoolId,
    designationId,
    status
) => {

    const result =
        await db.runAsync(
            `
            UPDATE designations
            SET
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE school_id = ?
            AND id = ?
            `,
            [
                status,
                schoolId,
                designationId
            ]
        );

    return result.changes;

};