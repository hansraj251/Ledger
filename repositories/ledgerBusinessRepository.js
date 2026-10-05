const db =
    require("../db");

exports.getByAccountId =
async (
    accountId
) => {

    return await db.getAsync(
        `
        SELECT
            id,
            account_id,
            business_name,
            mobile,
            address,
            currency,
            status,
            created_at,
            updated_at
        FROM ledger_businesses
        WHERE account_id = ?
        ORDER BY id
        LIMIT 1
        `,
        [
            accountId
        ]
    );
};

exports.getById =
async (
    businessId,
    accountId
) => {

    return await db.getAsync(
        `
        SELECT
            id,
            account_id,
            business_name,
            mobile,
            address,
            currency,
            status,
            created_at,
            updated_at
        FROM ledger_businesses
        WHERE
            id = ?
            AND account_id = ?
        `,
        [
            businessId,
            accountId
        ]
    );
};

exports.create =
async (
    accountId,
    businessName,
    mobile,
    address,
    currency
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO ledger_businesses
            (
                account_id,
                business_name,
                mobile,
                address,
                currency
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                ?
            )
            `,
            [
                accountId,
                businessName,
                mobile,
                address,
                currency
            ]
        );

    return await db.getAsync(
        `
        SELECT
            id,
            account_id,
            business_name,
            mobile,
            address,
            currency,
            status,
            created_at,
            updated_at
        FROM ledger_businesses
        WHERE id = ?
        `,
        [
            result.lastID
        ]
    );
};

exports.update =
async (
    businessId,
    accountId,
    businessName,
    mobile,
    address,
    currency
) => {

    await db.runAsync(
        `
        UPDATE ledger_businesses
        SET
            business_name = ?,
            mobile = ?,
            address = ?,
            currency = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE
            id = ?
            AND account_id = ?
        `,
        [
            businessName,
            mobile,
            address,
            currency,
            businessId,
            accountId
        ]
    );

    return await exports.getById(
        businessId,
        accountId
    );
};
