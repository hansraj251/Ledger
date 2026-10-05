const db =
    require("../db");

exports.getById =
async (
    accountId
) => {

    return await db.getAsync(
        `
        SELECT
            id,
            name,
            email,
            mobile,
            status,
            created_at,
            updated_at
        FROM align_accounts
        WHERE id = ?
        `,
        [
            accountId
        ]
    );
};

exports.getByEmail =
async (
    email
) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM align_accounts
        WHERE email = ?
        `,
        [
            email
        ]
    );
};

exports.getByMobile =
async (
    mobile
) => {

    return await db.getAsync(
        `
        SELECT
            *
        FROM align_accounts
        WHERE mobile = ?
        `,
        [
            mobile
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
        SELECT
            *
        FROM align_accounts
        WHERE
            (? IS NOT NULL AND email = ?)
            OR
            (? IS NOT NULL AND mobile = ?)
        LIMIT 1
        `,
        [
            email,
            email,
            mobile,
            mobile
        ]
    );
};

exports.getModuleLink =

async (
    module,
    moduleUserId
) => {
    return await db.getAsync(
        `
        SELECT
            id,
            account_id,
            module,
            module_user_id,
            created_at
        FROM align_account_links
        WHERE
            module = ?
            AND module_user_id = ?
        `,
        [
            module,
            moduleUserId
        ]
    );
};

exports.create =
async (
    name,
    email,
    mobile,
    password
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO align_accounts
            (
                name,
                email,
                mobile,
                password
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
                email,
                mobile,
                password
            ]
        );

    return await db.getAsync(
        `
        SELECT
            id,
            name,
            email,
            mobile,
            status,
            created_at,
            updated_at
        FROM align_accounts
        WHERE id = ?
        `,
        [
            result.lastID
        ]
    );
};

exports.linkModuleUser =
async (
    accountId,
    module,
    moduleUserId
) => {

    await db.runAsync(
        `
        INSERT INTO align_account_links
        (
            account_id,
            module,
            module_user_id
        )
        VALUES
        (
            ?,
            ?,
            ?
        )
        `,
        [
            accountId,
            module,
            moduleUserId
        ]
    );

    return await db.getAsync(
        `
        SELECT
            id,
            account_id,
            module,
            module_user_id,
            created_at
        FROM align_account_links
        WHERE id = last_insert_rowid()
        `
    );
};

exports.getLinkedAccount =
async (
    module,
    moduleUserId
) => {

    return await db.getAsync(
        `
        SELECT
            aa.id,
            aa.name,
            aa.email,
            aa.mobile,
            aa.status,
            aal.module,
            aal.module_user_id
        FROM align_account_links aal
        INNER JOIN align_accounts aa
            ON aa.id = aal.account_id
        WHERE
            aal.module = ?
            AND aal.module_user_id = ?
        `,
        [
            module,
            moduleUserId
        ]
    );
};
