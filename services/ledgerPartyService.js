const ledgerPartyRepository =
    require("../repositories/ledgerPartyRepository");

const ledgerTransactionRepository =

    require("../repositories/ledgerTransactionRepository");


const ledgerBusinessService =
    require("./ledgerBusinessService");

function cleanText(value) {
    if (value === undefined || value === null) {
        return null;
    }

    const text =
        String(value).trim();

    return text || null;
}

function validatePartyName(name) {
    const cleanedName =
        cleanText(name);

    if (!cleanedName) {
        throw new Error(
            "Party name is required"
        );
    }

    if (cleanedName.length > 150) {
        throw new Error(
            "Party name must not exceed 150 characters"
        );
    }

    return cleanedName;
}

function validateOpeningBalance(
    openingBalance
) {
    if (
        openingBalance ===
            undefined ||
        openingBalance === null ||
        openingBalance === ""
    ) {
        return 0;
    }

    const amount =
        Number(openingBalance);

    if (
        !Number.isFinite(amount) ||
        amount < 0
    ) {
        throw new Error(
            "Opening balance must be a valid non-negative amount"
        );
    }

    return amount;
}

function validateOpeningBalanceType(
    openingBalanceType
) {
    const type =
        cleanText(
            openingBalanceType
        ) || "credit";

    if (
        type !== "credit" &&
        type !== "debit"
    ) {
        throw new Error(
            "Opening balance type must be credit or debit"
        );
    }

    return type;
}

async function getParties(
    accountId
) {
    const business =
        await ledgerBusinessService
            .getBusiness(accountId);

    const parties =
        await ledgerPartyRepository
            .getByBusinessId(
                business.id
            );

    return Promise.all(

        parties.map(
            async (party) => {

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
                    Number(
                        summary.total_credit || 0
                    );

                const totalDebit =
                    openingDebit +
                    Number(
                        summary.total_debit || 0
                    );

                const netBalance =
                    totalCredit -
                    totalDebit;

                return {
                    ...party,

                    total_credit:
                        totalCredit,

                    total_debit:
                        totalDebit,

                    net_balance:
                        netBalance,

                    balance_type:
                        netBalance > 0
                            ? "receivable"
                            : netBalance < 0
                                ? "payable"
                                : "settled"
                };

            }
        )

    );
}

async function getParty(
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

async function createParty(
    accountId,
    {
        name,
        mobile,
        email,
        address,
        openingBalance,
        openingBalanceType,
        notes
    }
) {
    const business =
        await ledgerBusinessService
            .getBusiness(accountId);

    const validatedName =
        validatePartyName(name);

    const balance =
        validateOpeningBalance(
            openingBalance
        );

    const balanceType =
        validateOpeningBalanceType(
            openingBalanceType
        );

    return ledgerPartyRepository.create(
        business.id,
        validatedName,
        cleanText(mobile),
        cleanText(email),
        cleanText(address),
        balance,
        balanceType,
        cleanText(notes)
    );
}

async function updateParty(
    accountId,
    partyId,
    {
        name,
        mobile,
        email,
        address,
        openingBalance,
        openingBalanceType,
        notes
    }
) {
    const business =
        await ledgerBusinessService
            .getBusiness(accountId);

    const existing =
        await ledgerPartyRepository
            .getById(
                partyId,
                business.id
            );

    if (!existing) {
        throw new Error(
            "Party not found"
        );
    }

    const validatedName =
        validatePartyName(name);

    const balance =
        validateOpeningBalance(
            openingBalance
        );

    const balanceType =
        validateOpeningBalanceType(
            openingBalanceType
        );

    return ledgerPartyRepository.update(
        partyId,
        business.id,
        validatedName,
        cleanText(mobile),
        cleanText(email),
        cleanText(address),
        balance,
        balanceType,
        cleanText(notes)
    );
}

async function deactivateParty(
    accountId,
    partyId
) {
    const business =
        await ledgerBusinessService
            .getBusiness(accountId);

    const existing =
        await ledgerPartyRepository
            .getById(
                partyId,
                business.id
            );

    if (!existing) {
        throw new Error(
            "Party not found"
        );
    }

    return ledgerPartyRepository
        .deactivate(
            partyId,
            business.id
        );
}

module.exports = {
    getParties,
    getParty,
    createParty,
    updateParty,
    deactivateParty
};
