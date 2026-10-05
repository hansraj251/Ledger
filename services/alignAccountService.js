const alignAccountRepository =
    require("../repositories/alignAccountRepository");

exports.getAccount =
async (
    accountId
) => {

    const account =
        await alignAccountRepository
            .getById(
                accountId
            );

    if (!account) {
        throw new Error(
            "Align account not found"
        );
    }

    if (account.status !== "active") {
        throw new Error(
            "Align account is not active"
        );
    }

    return account;
};

exports.getByEmail =
async (
    email
) => {

    const cleanEmail =
        String(
            email || ""
        )
        .trim()
        .toLowerCase();

    if (!cleanEmail) {
        throw new Error(
            "Email is required"
        );
    }

    return await alignAccountRepository
        .getByEmail(
            cleanEmail
        );
};

exports.getByMobile =
async (
    mobile
) => {

    const cleanMobile =
        String(
            mobile || ""
        )
        .trim();

    if (!cleanMobile) {
        throw new Error(
            "Mobile is required"
        );
    }

    return await alignAccountRepository
        .getByMobile(
            cleanMobile
        );
};

exports.createAccount =
async (
    name,
    email,
    mobile,
    password
) => {

    const cleanName =
        String(
            name || ""
        ).trim();

    const cleanEmail =
        String(
            email || ""
        )
        .trim()
        .toLowerCase();

    const cleanMobile =
        String(
            mobile || ""
        ).trim();

    const cleanPassword =
        String(
            password || ""
        ).trim();

    if (!cleanName) {
        throw new Error(
            "Name is required"
        );
    }

    if (!cleanEmail && !cleanMobile) {
        throw new Error(
            "Email or mobile is required"
        );
    }

    if (!cleanPassword) {
        throw new Error(
            "Password is required"
        );
    }

    if (cleanEmail) {
        const existingEmail =
            await alignAccountRepository
                .getByEmail(
                    cleanEmail
                );

        if (existingEmail) {
            throw new Error(
                "Email already registered"
            );
        }
    }

    if (cleanMobile) {
        const existingMobile =
            await alignAccountRepository
                .getByMobile(
                    cleanMobile
                );

        if (existingMobile) {
            throw new Error(
                "Mobile already registered"
            );
        }
    }

    return await alignAccountRepository
        .create(
            cleanName,
            cleanEmail || null,
            cleanMobile || null,
            cleanPassword
        );
};

exports.linkModuleUser =
async (
    accountId,
    module,
    moduleUserId
) => {

    if (!accountId) {
        throw new Error(
            "Account ID is required"
        );
    }

    if (!module) {
        throw new Error(
            "Module is required"
        );
    }

    if (!moduleUserId) {
        throw new Error(
            "Module user ID is required"
        );
    }

    const account =
        await alignAccountRepository
            .getById(
                accountId
            );

    if (!account) {
        throw new Error(
            "Align account not found"
        );
    }

    return await alignAccountRepository
        .linkModuleUser(
            accountId,
            module,
            moduleUserId
        );
};

exports.ensureModuleAccount =

async (
    module,
    moduleUserId,
    name,
    email,
    mobile,
    passwordHash
) => {

    if (!module || !moduleUserId) {
        throw new Error(
            "Module and module user ID are required"
        );
    }

    const cleanName =
        String(name || "").trim();

    const cleanEmail =
        String(email || "")
            .trim()
            .toLowerCase();

    const cleanMobile =
        String(mobile || "").trim();

    const cleanPasswordHash =
        String(passwordHash || "").trim();

    if (!cleanName) {
        throw new Error(
            "Account name is required"
        );
    }

    if (!cleanEmail && !cleanMobile) {
        throw new Error(
            "Account email or mobile is required"
        );
    }

    if (!cleanPasswordHash) {
        throw new Error(
            "Account password hash is required"
        );
    }

    return await alignAccountRepository
        .getModuleLink(
            module,
            moduleUserId
        )
        .then(async (existingLink) => {

            if (existingLink) {

                const account =
                    await alignAccountRepository
                        .getById(
                            existingLink.account_id
                        );

                if (!account) {
                    throw new Error(
                        "Linked Align account not found"
                    );
                }

                if (account.status !== "active") {
                    throw new Error(
                        "Linked Align account is not active"
                    );
                }

                return account;
            }

            const emailAccount =
                cleanEmail
                    ? await alignAccountRepository
                        .getByEmail(cleanEmail)
                    : null;

            const mobileAccount =
                cleanMobile
                    ? await alignAccountRepository
                        .getByMobile(cleanMobile)
                    : null;

            if (
                emailAccount &&
                mobileAccount &&
                emailAccount.id !== mobileAccount.id
            ) {
                throw new Error(
                    "Email and mobile belong to different Align accounts"
                );
            }

            const existingAccount =
                emailAccount ||
                mobileAccount;

            let account;

            if (existingAccount) {

                if (
                    existingAccount.status !== "active"
                ) {
                    throw new Error(
                        "Align account is not active"
                    );
                }

                account =
                    existingAccount;

            } else {

                account =
                    await alignAccountRepository
                        .create(
                            cleanName,
                            cleanEmail || null,
                            cleanMobile || null,
                            cleanPasswordHash
                        );
            }

            const conflictingLink =
                await alignAccountRepository
                    .getModuleLink(
                        module,
                        moduleUserId
                    );

            if (conflictingLink) {
                throw new Error(
                    "Align account link already exists"
                );
            }

            await alignAccountRepository
                .linkModuleUser(
                    account.id,
                    module,
                    moduleUserId
                );

            return account;
        });
};

exports.resolveModuleAccount =
async (
    module,
    moduleUserId
) => {

    if (!module || !moduleUserId) {
        throw new Error(
            "Module and module user ID are required"
        );
    }

    const account =
        await alignAccountRepository
            .getLinkedAccount(
                module,
                moduleUserId
            );

    if (!account) {
        throw new Error(
            "Align account link not found"
        );
    }

    if (account.status !== "active") {
        throw new Error(
            "Align account is not active"
        );
    }

    return account;
};
