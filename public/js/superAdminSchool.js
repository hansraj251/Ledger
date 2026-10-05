let schoolPlans = [];

function getSchoolId() {

    return new URLSearchParams(
        location.search
    ).get("id");

}

async function loadSchoolPlans() {

    const response =
        await fetch(
            "/api/super-admin/school-plans",
            {
                headers: {
                    Authorization:
                        "Bearer " +
                        SuperAdminAuth.token()
                }
            }
        );

    const data =
        await response.json();

    if (!data.success) {

        throw new Error(
            data.message ||
            "Unable to load school plans"
        );

    }

    schoolPlans =
        (
            data.plans ||
            []
        ).filter(
            plan =>
                plan.plan_type ===
                "school"
        );

}

async function loadSchool() {

    try {

        await loadSchoolPlans();

        const id =
            getSchoolId();

        if (!id) {

            throw new Error(
                "School ID is missing"
            );

        }

        const response =
            await fetch(

                `/api/super-admin/schools/${id}`,

                {
                    headers: {
                        Authorization:
                            "Bearer " +
                            SuperAdminAuth.token()
                    }
                }

            );

        const data =
            await response.json();

        if (!data.success) {

            throw new Error(
                data.message ||
                "Unable to load school"
            );

        }

        renderSchool(
            data.school
        );

    }
    catch (err) {

        console.error(err);

        Notify.error(
            err.message ||
            "Unable to load school"
        );

        document.getElementById(
            "schoolContent"
        ).textContent =
            "Unable to load school.";

    }

}

function renderSchool(
    school
) {

    const remainingDays =
        school.plan_end
            ? Math.ceil(
                (
                    new Date(
                        school.plan_end
                    ) -
                    new Date()
                ) /
                86400000
            )
            : 0;

    document.getElementById(
        "schoolContent"
    ).innerHTML = `

<div class="space-y-8">

    <div
        class="rounded-2xl bg-white p-8 shadow">

        <h2
            class="mb-6 text-2xl font-bold">

            School Information

        </h2>

        <div
            class="grid gap-6 md:grid-cols-2">

            <div>

                <p
                    class="text-sm text-slate-500">

                    School

                </p>

                <p
                    class="font-semibold">

                    ${school.name || "-"}

                </p>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    School Code

                </p>

                <p
                    class="font-semibold">

                    ${school.school_code || "-"}

                </p>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    Owner

                </p>

                <p
                    class="font-semibold">

                    ${school.owner_name || "-"}

                </p>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    Mobile

                </p>

                <p>

                    ${school.mobile || "-"}

                </p>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    Email

                </p>

                <p>

                    ${school.email || "-"}

                </p>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    Status

                </p>

                <p>

                    ${school.status || "-"}

                </p>

            </div>

        </div>

    </div>

    <div
        class="rounded-2xl bg-white p-8 shadow">

        <h2
            class="mb-6 text-2xl font-bold">

            Subscription

        </h2>

        <div
            class="grid gap-6 md:grid-cols-2">

            <div>

                <p
                    class="text-sm text-slate-500">

                    Current Plan

                </p>

                <select
                    id="schoolPlanSelect"
                    class="mt-2 w-full rounded-lg border border-slate-300 p-3">

                    ${schoolPlans.map(
                        plan => `

                        <option
                            value="${plan.id}"
                            ${
                                plan.id ==
                                school.plan_id
                                    ? "selected"
                                    : ""
                            }>

                            ${plan.display_name}

                        </option>

                        `
                    ).join("")}

                </select>

            </div>

            <div>

                <p
                    class="text-sm text-slate-500">

                    Status

                </p>

                <select
                    id="schoolStatusSelect"
                    onchange="updateSchoolSubscriptionForm()"
                    class="mt-2 w-full rounded-lg border border-slate-300 p-3">

                    <option
                        value="trial"
                        ${
                            school.subscription_status ===
                            "trial"
                                ? "selected"
                                : ""
                        }>

                        Trial

                    </option>

                    <option
                        value="active"
                        ${
                            school.subscription_status ===
                            "active"
                                ? "selected"
                                : ""
                        }>

                        Active

                    </option>

                    <option
                        value="expired"
                        ${
                            school.subscription_status ===
                            "expired"
                                ? "selected"
                                : ""
                        }>

                        Expired

                    </option>

                    <option
                        value="suspended"
                        ${
                            school.subscription_status ===
                            "suspended"
                                ? "selected"
                                : ""
                        }>

                        Suspended

                    </option>

                </select>

            </div>

            <div
                id="schoolPlanStartSection">

                <p
                    class="text-sm text-slate-500">

                    Plan Start

                </p>

                <p>

                    ${school.plan_start || "-"}

                </p>

            </div>

            <div
                id="schoolPlanEndSection">

                <p
                    class="text-sm text-slate-500">

                    Plan End

                </p>

                <p>

                    ${school.plan_end || "-"}

                </p>

            </div>

            <div
                id="schoolDaysSection">

                <p
                    class="text-sm text-slate-500">

                    Remaining Days

                </p>

                <input
                    id="schoolDaysInput"
                    type="number"
                    value="${Math.max(
                        remainingDays,
                        1
                    )}"
                    min="1"
                    class="mt-2 w-full rounded-lg border border-slate-300 p-3">

            </div>

        </div>

        <div
            class="mt-8">

            <button
                type="button"
                id="saveSchoolSubscriptionBtn"
                onclick="saveSchoolSubscription()"
                class="rounded-xl bg-slate-600 px-6 py-3 font-semibold text-white hover:bg-blue-700">

                Save Subscription

            </button>

        </div>

    </div>

</div>

`;

    updateSchoolSubscriptionForm();

}

function updateSchoolSubscriptionForm() {

    const status =
        document.getElementById(
            "schoolStatusSelect"
        )?.value;

    const hideValidity =
        status === "expired" ||
        status === "suspended";

    document.getElementById(
        "schoolPlanStartSection"
    )?.classList.toggle(
        "hidden",
        hideValidity
    );

    document.getElementById(
        "schoolPlanEndSection"
    )?.classList.toggle(
        "hidden",
        hideValidity
    );

    document.getElementById(
        "schoolDaysSection"
    )?.classList.toggle(
        "hidden",
        hideValidity
    );

}

async function saveSchoolSubscription() {

    const schoolId =
        getSchoolId();

    const planId =
        document.getElementById(
            "schoolPlanSelect"
        ).value;

    const status =
        document.getElementById(
            "schoolStatusSelect"
        ).value;

    const days =
        Number(
            document.getElementById(
                "schoolDaysInput"
            ).value
        );

    if (
        !planId
    ) {

        Notify.error(
            "Please select a school plan."
        );

        return;

    }

    if (
        status !== "expired" &&
        status !== "suspended" &&
        days < 1
    ) {

        Notify.error(
            "Validity days must be greater than zero."
        );

        return;

    }

    try {

        const response =
            await fetch(

                `/api/super-admin/schools/${schoolId}/subscription`,

                {

                    method:
                        "PUT",

                    headers: {

                        Authorization:
                            "Bearer " +
                            SuperAdminAuth.token(),

                        "Content-Type":
                            "application/json"

                    },

                    body:
                        JSON.stringify({

                            planId,

                            status,

                            days

                        })

                }

            );

        const data =
            await response.json();

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to update subscription"
            );

        }

        Notify.success(
            data.message ||
            "School subscription updated successfully"
        );

        await loadSchool();

    }
    catch (err) {

        console.error(err);

        Notify.error(
            err.message ||
            "Unable to update subscription"
        );

    }

}
