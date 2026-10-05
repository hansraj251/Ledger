const ledgerBusinessRepository =
    require("../repositories/ledgerBusinessRepository");

exports.getBusiness =
async (
    accountId
) => {

    const business =
        await ledgerBusinessRepository
            .getByAccountId(
                accountId
            );

    if (!business) {
        throw new Error(
            "Ledger business not found"
        );
    }

    if (business.status !== "active") {
        throw new Error(
            "Ledger business is not active"
        );
    }

    return business;
};

exports.getBusinessById =
async (
    businessId,
    accountId
) => {

    const business =
        await ledgerBusinessRepository
            .getById(
                businessId,
                accountId
            );

    if (!business) {
        throw new Error(
            "Ledger business not found"
        );
    }

    if (business.status !== "active") {
        throw new Error(
            "Ledger business is not active"
        );
    }

    return business;
};

exports.createBusiness =
async (
    accountId,
    businessName,
    mobile,
    address,
    currency
) => {

    const cleanName =
        String(
            businessName || ""
        ).trim();

    const cleanMobile =
        String(
            mobile || ""
        ).trim();

    const cleanAddress =
        String(
            address || ""
        ).trim();

    const cleanCurrency =
        String(
            currency || "INR"
        ).trim().toUpperCase();

    if (!accountId) {
        throw new Error(
            "Account ID is required"
        );
    }

    if (!cleanName) {
        throw new Error(
            "Business name is required"
        );
    }

    if (!cleanCurrency) {
        throw new Error(
            "Currency is required"
        );
    }

    const existing =
        await ledgerBusinessRepository
            .getByAccountId(
                accountId
            );

    if (existing) {
        throw new Error(
            "Ledger business already exists"
        );
    }

    return await ledgerBusinessRepository
        .create(
            accountId,
            cleanName,
            cleanMobile || null,
            cleanAddress || null,
            cleanCurrency
        );
};

exports.updateBusiness =
async (
    businessId,
    accountId,
    businessName,
    mobile,
    address,
    currency
) => {

    const existing =
        await ledgerBusinessRepository
            .getById(
                businessId,
                accountId
            );

    if (!existing) {
        throw new Error(
            "Ledger business not found"
        );
    }

    const cleanName =
        String(
            businessName || ""
        ).trim();

    const cleanMobile =
        String(
            mobile || ""
        ).trim();

    const cleanAddress =
        String(
            address || ""
        ).trim();

    const cleanCurrency =
        String(
            currency || "INR"
        ).trim().toUpperCase();

    if (!cleanName) {
        throw new Error(
            "Business name is required"
        );
    }

    if (!cleanCurrency) {
        throw new Error(
            "Currency is required"
        );
    }

    return await ledgerBusinessRepository
        .update(
            businessId,
            accountId,
            cleanName,
            cleanMobile || null,
            cleanAddress || null,
            cleanCurrency
        );
};
