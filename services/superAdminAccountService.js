const bcrypt =
    require("bcrypt");

const superAdminAccountRepository =
    require("../repositories/superAdminAccountRepository");

exports.getAll =
async () => {

    return await superAdminAccountRepository
        .getAll();

};

exports.create =
async (
    data
) => {

    const existing =
        await superAdminAccountRepository
            .getByUsername(
                data.username
            );

    if (existing) {

        throw new Error(
            "Username already exists"
        );

    }

    const password =
        await bcrypt.hash(
            data.password,
            10
        );

    return await superAdminAccountRepository
        .create(
            data.name,
            data.username,
            password,
            data.status
        );

};

exports.update =
async (
    id,
    data,
    currentUserId
) => {

    const existing =
        await superAdminAccountRepository
            .getByUsername(
                data.username
            );

    if (
        existing &&
        existing.id != id
    ) {

        throw new Error(
            "Username already exists"
        );

    }
    if (
    id == currentUserId &&
    data.status === "inactive"
) {

    throw new Error(
        "You cannot deactivate your own account"
    );

}

    await superAdminAccountRepository
        .update(
            id,
            data.name,
            data.username,
            data.status
        );

};

exports.changePassword =
async (
    id,
    password
) => {

    const hash =
        await bcrypt.hash(
            password,
            10
        );

    await superAdminAccountRepository
        .updatePassword(
            id,
            hash
        );

};

exports.delete =
async (
    id,
    currentUserId
) => {
    if (
    id == currentUserId
) {

    throw new Error(
        "You cannot delete your own account"
    );

}

    const accounts =
        await superAdminAccountRepository
            .getAll();

    const activeAccounts =
        accounts.filter(
            account =>
                account.status ===
                "active"
        );

    const account =
        accounts.find(
            account =>
                account.id == id
        );

    if (!account) {

        throw new Error(
            "Account not found"
        );

    }

    if (
        account.status ===
            "active" &&
        activeAccounts.length ===
            1
    ) {

        throw new Error(
            "Cannot delete the last active Super Admin"
        );

    }
    

    await superAdminAccountRepository
        .delete(
            id
        );

};