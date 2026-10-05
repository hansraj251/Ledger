const db =
    require("../db");

async function getByBusinessId(
    businessId
) {
    return db.allAsync(
        `
        SELECT
            id,
            business_id,
            name,
            mobile,
            email,
            address,
            opening_balance,
            opening_balance_type,
            notes,
            status,
            created_at,
            updated_at
        FROM ledger_parties
        WHERE business_id = ?
        ORDER BY name COLLATE NOCASE ASC, id ASC
        `,
        [businessId]
    );
}

async function getById(
    partyId,
    businessId
) {
    return db.getAsync(
        `
        SELECT
            id,
            business_id,
            name,
            mobile,
            email,
            address,
            opening_balance,
            opening_balance_type,
            notes,
            status,
            created_at,
            updated_at
        FROM ledger_parties
        WHERE id = ?
          AND business_id = ?
        LIMIT 1
        `,
        [
            partyId,
            businessId
        ]
    );
}

async function create(
    businessId,
    name,
    mobile,
    email,
    address,
    openingBalance,
    openingBalanceType,
    notes
) {
    const result =
        await db.runAsync(
            `
            INSERT INTO ledger_parties (
                business_id,
                name,
                mobile,
                email,
                address,
                opening_balance,
                opening_balance_type,
                notes
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            `,
            [
                businessId,
                name,
                mobile,
                email,
                address,
                openingBalance,
                openingBalanceType,
                notes
            ]
        );

    return getById(
        result.lastID,
        businessId
    );
}

async function update(
    partyId,
    businessId,
    name,
    mobile,
    email,
    address,
    openingBalance,
    openingBalanceType,
    notes
) {
    await db.runAsync(
        `
        UPDATE ledger_parties
        SET
            name = ?,
            mobile = ?,
            email = ?,
            address = ?,
            opening_balance = ?,
            opening_balance_type = ?,
            notes = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND business_id = ?
        `,
        [
            name,
            mobile,
            email,
            address,
            openingBalance,
            openingBalanceType,
            notes,
            partyId,
            businessId
        ]
    );

    return getById(
        partyId,
        businessId
    );
}

async function deactivate(
    partyId,
    businessId
) {
    await db.runAsync(
        `
        UPDATE ledger_parties
        SET
            status = 'inactive',
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND business_id = ?
        `,
        [
            partyId,
            businessId
        ]
    );

    return getById(
        partyId,
        businessId
    );
}

module.exports = {
    getByBusinessId,
    getById,
    create,
    update,
    deactivate
};
