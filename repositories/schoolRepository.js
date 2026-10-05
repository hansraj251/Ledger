const db =
    require("../db");

exports.getById =
async (
    schoolId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM schools
        WHERE id = ?
        `,
        [
            schoolId
        ]
    );

};
exports.updateProfile =
async (
    schoolId,
    data
) => {

    await db.runAsync(
        `
        UPDATE schools
        SET
            name = ?,
            owner_name = ?,
            mobile = ?,
            address = ?,
            city = ?,
            state = ?,
            pincode = ?,
            receipt_footer_message = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        `,
        [
            data.name,
            data.ownerName,
            data.mobile,
            data.address,
            data.city,
            data.state,
            data.pincode,
            data.receiptFooterMessage,
            schoolId
        ]
    );

    return await exports.getById(
        schoolId
    );
};
exports.updateLogo =
async (
    schoolId,
    logo
) => {

    const result =
        await db.runAsync(
            `
            UPDATE schools
            SET
                logo = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            `,
            [
                logo,
                schoolId
            ]
        );

    return result.changes;

};