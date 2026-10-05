const db =
    require("../db");

exports.getByPartyId =
async (
    partyId
) => {

    return await db.allAsync(

        `
        SELECT

            id,

            party_id,

            transaction_type,

            amount,

            transaction_date,

            description,

            payment_mode,

            reference_no,

            created_at,

            updated_at

        FROM ledger_transactions

        WHERE party_id = ?

        ORDER BY
            transaction_date DESC,
            id DESC
        `,

        [
            partyId
        ]

    );

};

exports.getById =
async (
    transactionId,
    partyId
) => {

    return await db.getAsync(

        `
        SELECT

            id,

            party_id,

            transaction_type,

            amount,

            transaction_date,

            description,

            payment_mode,

            reference_no,

            created_at,

            updated_at

        FROM ledger_transactions

        WHERE
            id = ?
            AND party_id = ?

        `,

        [
            transactionId,
            partyId
        ]

    );

};

exports.create =
async (
    partyId,
    transactionType,
    amount,
    transactionDate,
    description,
    paymentMode,
    referenceNo
) => {

    const result =
        await db.runAsync(

            `
            INSERT INTO ledger_transactions
            (
                party_id,
                transaction_type,
                amount,
                transaction_date,
                description,
                payment_mode,
                reference_no
            )
            VALUES
            (
                ?,
                ?,
                ?,
                ?,
                ?,
                ?,
                ?
            )
            `,

            [
                partyId,
                transactionType,
                amount,
                transactionDate,
                description,
                paymentMode,
                referenceNo
            ]

        );

    return await exports.getById(
        result.lastID,
        partyId
    );

};

exports.update =
async (
    transactionId,
    partyId,
    transactionType,
    amount,
    transactionDate,
    description,
    paymentMode,
    referenceNo
) => {

    await db.runAsync(

        `
        UPDATE ledger_transactions

        SET

            transaction_type = ?,

            amount = ?,

            transaction_date = ?,

            description = ?,

            payment_mode = ?,

            reference_no = ?,

            updated_at = CURRENT_TIMESTAMP

        WHERE

            id = ?

            AND party_id = ?

        `,

        [
            transactionType,
            amount,
            transactionDate,
            description,
            paymentMode,
            referenceNo,
            transactionId,
            partyId
        ]

    );

    return await exports.getById(
        transactionId,
        partyId
    );

};

exports.delete =
async (
    transactionId,
    partyId
) => {

    await db.runAsync(

        `
        DELETE FROM ledger_transactions

        WHERE
            id = ?
            AND party_id = ?

        `,

        [
            transactionId,
            partyId
        ]

    );

};


exports.getSummaryByPartyId =
async (
    partyId
) => {

    return await db.getAsync(

        `
        SELECT

            COALESCE(
                SUM(
                    CASE
                        WHEN transaction_type = 'credit'
                        THEN amount
                        ELSE 0
                    END
                ),
                0
            ) AS total_credit,

            COALESCE(
                SUM(
                    CASE
                        WHEN transaction_type = 'debit'
                        THEN amount
                        ELSE 0
                    END
                ),
                0
            ) AS total_debit

        FROM ledger_transactions

        WHERE party_id = ?

        `,

        [
            partyId
        ]

    );

};
