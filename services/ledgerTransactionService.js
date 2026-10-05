const ledgerTransactionRepository =
    require("../repositories/ledgerTransactionRepository");

const ledgerPartyRepository =
    require("../repositories/ledgerPartyRepository");

const ledgerBusinessService =
    require("./ledgerBusinessService");

function cleanText(value) {

    if (
        value === undefined ||
        value === null
    ) {
        return null;
    }

    const text =
        String(value).trim();

    return text || null;
}

function validateTransactionType(
    transactionType
) {

    const type =
        cleanText(transactionType);

    if (
        type !== "credit" &&
        type !== "debit"
    ) {
        throw new Error(
            "Transaction type must be credit or debit"
        );
    }

    return type;
}

function validateAmount(
    amount
) {

    if (
        amount === undefined ||
        amount === null ||
        amount === ""
    ) {
        throw new Error(
            "Transaction amount is required"
        );
    }

    const value =
        Number(amount);

    if (
        !Number.isFinite(value) ||
        value <= 0
    ) {
        throw new Error(
            "Transaction amount must be a valid positive amount"
        );
    }

    return value;
}

function validateTransactionDate(
    transactionDate
) {

    const date =
        cleanText(transactionDate);

    if (!date) {
        throw new Error(
            "Transaction date is required"
        );
    }

    if (
        !/^\d{4}-\d{2}-\d{2}$/.test(date)
    ) {
        throw new Error(
            "Transaction date must be in YYYY-MM-DD format"
        );
    }

    const parsed =
        new Date(
            `${date}T00:00:00Z`
        );

    if (
        Number.isNaN(
            parsed.getTime()
        ) ||
        parsed.toISOString().slice(0, 10) !== date
    ) {
        throw new Error(
            "Transaction date is invalid"
        );
    }

    return date;
}

async function getPartyForAccount(
    accountId,
    partyId
) {

    const business =
        await ledgerBusinessService
            .getBusiness(accountId);

    const party =
        await ledgerPartyRepository
            .getById(
                partyId,
                business.id
            );

    if (!party) {
        throw new Error(
            "Party not found"
        );
    }

    return party;
}

async function getTransactions(
    accountId,
    partyId
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    return ledgerTransactionRepository
        .getByPartyId(
            party.id
        );
}

async function getTransaction(
    accountId,
    partyId,
    transactionId
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    const transaction =
        await ledgerTransactionRepository
            .getById(
                transactionId,
                party.id
            );

    if (!transaction) {
        throw new Error(
            "Transaction not found"
        );
    }

    return transaction;
}

async function createTransaction(
    accountId,
    partyId,
    {
        transactionType,
        amount,
        transactionDate,
        description,
        paymentMode,
        referenceNo
    }
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    const validatedType =
        validateTransactionType(
            transactionType
        );

    const validatedAmount =
        validateAmount(
            amount
        );

    const validatedDate =
        validateTransactionDate(
            transactionDate
        );

    return ledgerTransactionRepository
        .create(
            party.id,
            validatedType,
            validatedAmount,
            validatedDate,
            cleanText(description),
            cleanText(paymentMode),
            cleanText(referenceNo)
        );
}

async function updateTransaction(
    accountId,
    partyId,
    transactionId,
    {
        transactionType,
        amount,
        transactionDate,
        description,
        paymentMode,
        referenceNo
    }
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    const existing =
        await ledgerTransactionRepository
            .getById(
                transactionId,
                party.id
            );

    if (!existing) {
        throw new Error(
            "Transaction not found"
        );
    }

    const validatedType =
        validateTransactionType(
            transactionType
        );

    const validatedAmount =
        validateAmount(
            amount
        );

    const validatedDate =
        validateTransactionDate(
            transactionDate
        );

    return ledgerTransactionRepository
        .update(
            transactionId,
            party.id,
            validatedType,
            validatedAmount,
            validatedDate,
            cleanText(description),
            cleanText(paymentMode),
            cleanText(referenceNo)
        );
}

async function deleteTransaction(
    accountId,
    partyId,
    transactionId
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    const existing =
        await ledgerTransactionRepository
            .getById(
                transactionId,
                party.id
            );

    if (!existing) {
        throw new Error(
            "Transaction not found"
        );
    }

    await ledgerTransactionRepository
        .delete(
            transactionId,
            party.id
        );

    return {
        success: true
    };
}


async function getPartySummary(
    accountId,
    partyId
) {

    const party =
        await getPartyForAccount(
            accountId,
            partyId
        );

    const summary =
        await ledgerTransactionRepository
            .getSummaryByPartyId(
                party.id
            );

    const openingBalance =
        Number(
            party.opening_balance || 0
        );

    const openingCredit =
        party.opening_balance_type === "credit"
            ? openingBalance
            : 0;

    const openingDebit =
        party.opening_balance_type === "debit"
            ? openingBalance
            : 0;

    const totalCredit =
        openingCredit +
        Number(summary.total_credit || 0);

    const totalDebit =
        openingDebit +
        Number(summary.total_debit || 0);

    const netBalance =
        totalCredit -
        totalDebit;

    return {

        partyId: party.id,

        partyName: party.name,

        openingBalance,

        openingBalanceType:
            party.opening_balance_type,

        totalCredit,

        totalDebit,

        netBalance,

        balanceType:
            netBalance > 0
                ? "receivable"
                : netBalance < 0
                    ? "payable"
                    : "settled"

    };
}

module.exports = {

    getTransactions,

    getTransaction,

    createTransaction,

    updateTransaction,

    deleteTransaction,

    getPartySummary

};
