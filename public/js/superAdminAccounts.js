async function loadLatestPosVersion() {

    const response =
        await API.get(
            "/api/super-admin/app-version"
        );

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    document.getElementById(
        "latestPosVersion"
    ).value =
        response.latest_version || "";

}
async function saveLatestPosVersion() {

    const input =
        document.getElementById(
            "latestPosVersion"
        );

    const version =
        input.value.trim();

    if (!version) {

        Notify.error(
            "Version is required."
        );

        return;

    }

    const response =
        await API.put(
            "/api/super-admin/app-version",
            {
                version
            }
        );

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    Notify.success(
        "Latest POS version updated successfully."
    );

}
function initializeSuperAdminAccounts() {

    bindEvents();

    loadAccounts();

}


async function loadAccounts() {

    const token =
        SuperAdminAuth.token();

    if (!token) {

        location.href =
            "/login.html";

        return;

    }

    try {

        const response =
            await fetch(
                "/api/super-admin/accounts",
                {
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

        const data =
            await response.json();

        const tableBody =
            document.getElementById(
                "accountsTableBody"
            );

        tableBody.innerHTML =
            "";

        if (!data.success) {

            Notify.error(
                data.message
            );

            return;

        }

        if (
            data.accounts.length === 0
        ) {

            tableBody.innerHTML =
                `
                <tr>

                    <td
                        colspan="4"
                        class="py-8 text-center text-slate-500">

                        No Super Admin accounts found

                    </td>

                </tr>
                `;

            return;

        }

        let html =
            "";

        data.accounts.forEach(
            account => {

                html +=
                    renderAccountRow(
                        account
                    );

            }
        );

        tableBody.innerHTML =
            html;
        bindTableEvents();    

    } catch (err) {

        console.error(
            err
        );

        Notify.error(
            "Failed to load accounts."
        );

    }

}
function openAccountModal(
    account = null
) {

    document
        .getElementById(
            "accountForm"
        )
        .reset();

    document
        .getElementById(
            "accountId"
        ).value = "";

    document
        .getElementById(
            "passwordField"
        ).classList.remove(
            "hidden"
        );

    document
        .getElementById(
            "accountModalTitle"
        ).textContent =
        "Add Super Admin";

    if (account) {

        document
            .getElementById(
                "accountModalTitle"
            ).textContent =
            "Edit Super Admin";

        document
            .getElementById(
                "accountId"
            ).value =
            account.id;

        document
            .getElementById(
                "accountName"
            ).value =
            account.name;

        document
            .getElementById(
                "accountUsername"
            ).value =
            account.username;

        document
            .getElementById(
                "accountStatus"
            ).value =
            account.status;

        document
            .getElementById(
                "passwordField"
            ).classList.add(
                "hidden"
            );

    }

    document
        .getElementById(
            "accountModal"
        )
        .classList.remove(
            "hidden"
        );

    document
        .getElementById(
            "accountModal"
        )
        .classList.add(
            "flex"
        );

}
function closeAccountModal() {

    document
        .getElementById(
            "accountModal"
        )
        .classList.remove(
            "flex"
        );

    document
        .getElementById(
            "accountModal"
        )
        .classList.add(
            "hidden"
        );

}
async function saveAccount(
    event
) {
    
    let data;
    const token =
    SuperAdminAuth.token();

    event.preventDefault();

    const id =
        document
            .getElementById(
                "accountId"
            )
            .value;

    const payload = {

        name:
            document
                .getElementById(
                    "accountName"
                )
                .value
                .trim(),

        username:
            document
                .getElementById(
                    "accountUsername"
                )
                .value
                .trim(),

        status:
            document
                .getElementById(
                    "accountStatus"
                )
                .value

    };



    if (id) {

        const response =
    await fetch(
        `/api/super-admin/accounts/${id}`,
        {
            method:
                "PUT",
            headers: {
                "Content-Type":
                    "application/json",
                Authorization:
                    `Bearer ${token}`
            },
            body:
                JSON.stringify(
                    payload
                )
        }
    );
    data =
    await response.json();


    }
    else {

        payload.password =
            document
                .getElementById(
                    "accountPassword"
                )
                .value;

        const response =
    await fetch(
        "/api/super-admin/accounts",
        {
            method:
                "POST",
            headers: {
                "Content-Type":
                    "application/json",
                Authorization:
                    `Bearer ${token}`
            },
            body:
                JSON.stringify(
                    payload
                )
        }
    );
    data =
    await response.json();
    

    }

    if (!data.success) {

        Notify.error(
    data.message
);

        return;

    }

    Notify.success(
    data.message
);

    closeAccountModal();

    loadAccounts();

}
function renderAccountRow(
    account
) {

    return `
        <tr class="border-b">

         <td class="whitespace-nowrap py-3 pr-10">

    ${account.name}

</td>

<td class="w-[220px] whitespace-nowrap py-3">

    ${account.username}

</td>

<td class="w-[140px] whitespace-nowrap py-3">

            <td class="min-w-[260px] py-3">

                ${
                    account.status ===
                    "active"

                    ?

                    `<span class="rounded bg-green-100 px-2 py-1 text-xs text-green-700">

                        Active

                    </span>`

                    :

                    `<span class="rounded bg-red-100 px-2 py-1 text-xs text-red-700">

                        Inactive

                    </span>`
                }

            </td>

            <td class="w-[260px] py-3">

    <div class="flex justify-end gap-2 whitespace-nowrap">

        <button
            class="edit-account-btn rounded bg-slate-600 px-3 py-1 text-sm text-white whitespace-nowrap"
            data-id="${account.id}">

            Edit

        </button>

        <button
            class="password-account-btn rounded bg-yellow-500 px-3 py-1 text-sm text-white whitespace-nowrap"
            data-id="${account.id}">

            Password

        </button>

        <button
            class="delete-account-btn rounded bg-red-600 px-3 py-1 text-sm text-white whitespace-nowrap"
            data-id="${account.id}">

            Delete

        </button>

    </div>

</td>

        </tr>
        `;

}

async function editAccount(
    id
) {

    const token =
        SuperAdminAuth.token();

    try {

        const response =
            await fetch(
                `/api/super-admin/accounts`,
                {
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

        const data =
            await response.json();

        if (!data.success) {

            Notify.error(
                data.message
            );

            return;

        }

        const account =
            data.accounts.find(
                account =>
                    account.id ==
                    id
            );

        if (!account) {

            Notify.error(
                "Account not found."
            );

            return;

        }

        openAccountModal(
            account
        );

    } catch (error) {

        console.error(
            error
        );

        Notify.error(
            "Failed to load account."
        );

    }

}
async function deleteAccount(
    id
) {

    if (
        !confirm(
            "Are you sure you want to delete this Super Admin?"
        )
    ) {

        return;

    }

    const token =
        SuperAdminAuth.token();

    try {

        const response =
            await fetch(
                `/api/super-admin/accounts/${id}`,
                {
                    method:
                        "DELETE",
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

        const data =
            await response.json();

        if (!data.success) {

            Notify.error(
                data.message
            );

            return;

        }

        Notify.success(
            data.message
        );

        loadAccounts();

    } catch (error) {

        console.error(
            error
        );

        Notify.error(
            "Failed to delete account."
        );

    }

}

function bindEvents() {

    document
        .getElementById(
            "addAccountBtn"
        )
        .addEventListener(
            "click",
            () => {

                openAccountModal();

            }
        );

    document
        .getElementById(
            "closeAccountModal"
        )
        .addEventListener(
            "click",
            closeAccountModal
        );

    document
        .getElementById(
            "cancelAccountBtn"
        )
        .addEventListener(
            "click",
            closeAccountModal
        );
        document
    .getElementById(
        "accountForm"
    )
    .addEventListener(
        "submit",
        saveAccount
    );
    document
    .getElementById(
        "passwordForm"
    )
    .addEventListener(
        "submit",
        savePassword
    );

document
    .getElementById(
        "closePasswordModal"
    )
    .addEventListener(
        "click",
        closePasswordModal
    );

document
    .getElementById(
        "cancelPasswordBtn"
    )
    .addEventListener(
        "click",
        closePasswordModal
    );

}
function bindTableEvents() {

    document
        .querySelectorAll(
            ".edit-account-btn"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    () => {

                        editAccount(
                            button.dataset.id
                        );

                    }
                );

            }
        );

    document
        .querySelectorAll(
            ".password-account-btn"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    () => {

                        changePassword(
                            button.dataset.id
                        );

                    }
                );

            }
        );

    document
        .querySelectorAll(
            ".delete-account-btn"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    () => {

                        deleteAccount(
                            button.dataset.id
                        );

                    }
                );

            }
        );

}

function changePassword(
    id
) {

    document
        .getElementById(
            "passwordForm"
        )
        .reset();

    document
        .getElementById(
            "passwordAccountId"
        ).value =
        id;

    document
        .getElementById(
            "passwordModal"
        )
        .classList.remove(
            "hidden"
        );

    document
        .getElementById(
            "passwordModal"
        )
        .classList.add(
            "flex"
        );

}

function closePasswordModal() {

    document
        .getElementById(
            "passwordModal"
        )
        .classList.remove(
            "flex"
        );

    document
        .getElementById(
            "passwordModal"
        )
        .classList.add(
            "hidden"
        );

}
async function savePassword(
    event
) {

    event.preventDefault();

    const token =
        SuperAdminAuth.token();

    const id =
        document
            .getElementById(
                "passwordAccountId"
            )
            .value;

    const password =
        document
            .getElementById(
                "newPassword"
            )
            .value
            .trim();

    if (!password) {

        Notify.error(
            "Password is required."
        );

        return;

    }

    try {

        const response =
            await fetch(
                `/api/super-admin/accounts/${id}/password`,
                {
                    method:
                        "PATCH",
                    headers: {
                        "Content-Type":
                            "application/json",
                        Authorization:
                            `Bearer ${token}`
                    },
                    body:
                        JSON.stringify({
                            password
                        })
                }
            );

        const data =
            await response.json();

        if (!data.success) {

            Notify.error(
                data.message
            );

            return;

        }

        Notify.success(
            data.message
        );

        document
            .getElementById(
                "passwordForm"
            )
            .reset();

        closePasswordModal();

    }
    catch (error) {

        console.error(
            error
        );

        Notify.error(
            "Failed to update password."
        );

    }

}
loadLatestPosVersion();