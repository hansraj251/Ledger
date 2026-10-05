Auth.requireSchoolOwner();
let currentSubscription = null;

async function loadSubscription() {
    const data = await API.get("/api/subscription/school");

    if (!data.success) {
        Notify.error(data.message || "Unable to load subscription");
        return;
    }

    currentSubscription = data.subscription || null;
}

function getSubscriptionStatus() {
    return String(
        currentSubscription?.subscription_status ||
        currentSubscription?.status ||
        ""
    ).trim().toLowerCase();
}

function isSubscriptionBlocked() {
    const status = getSubscriptionStatus();

    return (
        status === "expired" ||
        status === "suspended"
    );
}

function getSubscriptionBlockMessage() {
    const status = getSubscriptionStatus();

    if (status === "expired") {
        return "Your subscription has expired. Please renew your subscription.";
    }

    if (status === "suspended") {
        return "Your subscription is suspended. Please contact Align Support.";
    }

    return "Your subscription does not allow this action.";
}
const salaryStaffTableBody =
    document.getElementById(
        "salaryStaffTableBody"
    );
const salaryForm =
    document.getElementById(
        "salaryForm"
    );

const salaryFormElement =
    document.getElementById(
        "salaryFormElement"
    );

const closeSalaryFormBtn =
    document.getElementById(
        "closeSalaryFormBtn"
    );

const cancelSalaryBtn =
    document.getElementById(
        "cancelSalaryBtn"
    );

const salaryTeacherId =
    document.getElementById(
        "salaryTeacherId"
    );

const salaryStaffName =
    document.getElementById(
        "salaryStaffName"
    );

const salaryStaffDesignation =
    document.getElementById(
        "salaryStaffDesignation"
    );

const salaryFormResult =
    document.getElementById(
        "salaryFormResult"
    );

const basicSalaryInput =
    document.getElementById(
        "basicSalary"
    );

const hraInput =
    document.getElementById(
        "hra"
    );

const otherAllowancesInput =
    document.getElementById(
        "otherAllowances"
    );

const pfInput =
    document.getElementById(
        "pf"
    );

const esiInput =
    document.getElementById(
        "esi"
    );

const professionalTaxInput =
    document.getElementById(
        "professionalTax"
    );

const otherDeductionsInput =
    document.getElementById(
        "otherDeductions"
    );

const effectiveFromInput =
    document.getElementById(
        "effectiveFrom"
    );
const salaryHistoryBody =
    document.getElementById(
        "salaryHistoryBody"
    );

(async function initSalaryPage() {
    await loadSubscription();
    await loadStaff();
})();
closeSalaryFormBtn.addEventListener(
    "click",
    closeSalaryForm
);

cancelSalaryBtn.addEventListener(
    "click",
    closeSalaryForm
);
async function loadStaff() {

    try {

        const [
            teacherData,
            salaryData
        ] =
            await Promise.all([

                API.get(
                    "/api/teachers"
                ),

                API.get(
                    "/api/salary-structures"
                )

            ]);

        if (
            !teacherData.success
        ) {

            throw new Error(
                teacherData.message ||
                "Unable to load staff"
            );

        }

        if (
            !salaryData.success
        ) {

            throw new Error(
                salaryData.message ||
                "Unable to load salary structures"
            );

        }

        const activeStaff =
            (
                teacherData.teachers ||
                []
            ).filter(
                staff =>
                    staff.status === "active"
            );

        const salaryStructures =
            salaryData.salaryStructures ||
            [];

        renderStaff(
            activeStaff,
            salaryStructures
        );

    }
    catch (err) {

        console.error(err);

        salaryStaffTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-red-600">

                    Unable to load staff.

                </td>

            </tr>
        `;

    }

}

function renderStaff(
    staff,
    salaryStructures
) {

    if (
        !staff ||
        staff.length === 0
    ) {

        salaryStaffTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-slate-500">

                    No active staff found.

                </td>

            </tr>
        `;

        return;

    }

    const salaryMap =
        new Map();

    (
        salaryStructures ||
        []
    ).forEach(
        salary => {

            salaryMap.set(
                Number(
                    salary.teacher_id
                ),
                salary
            );

        }
    );

    salaryStaffTableBody.innerHTML =
        staff
            .map(
                (
                    member
                ) => {

                    const salary =
                        salaryMap.get(
                            Number(
                                member.id
                            )
                        );

                    const basicSalary =
                        Number(
                            salary?.basic_salary ||
                            0
                        );

                    const hra =
                        Number(
                            salary?.hra ||
                            0
                        );

                    const otherAllowances =
                        Number(
                            salary?.other_allowances ||
                            0
                        );

                    const totalEarnings =
                        basicSalary +
                        hra +
                        otherAllowances;

                    const pf =
                        Number(
                            salary?.pf ||
                            0
                        );

                    const esi =
                        Number(
                            salary?.esi ||
                            0
                        );

                    const professionalTax =
                        Number(
                            salary?.professional_tax ||
                            0
                        );

                    const otherDeductions =
                        Number(
                            salary?.other_deductions ||
                            0
                        );

                    const totalDeductions =
                        pf +
                        esi +
                        professionalTax +
                        otherDeductions;

                    const netSalary =
                        totalEarnings -
                        totalDeductions;

                    return `

                        <tr
    class="border-slate-200 align-middle transition-colors hover:bg-slate-200">

                            <td
                                class="whitespace-nowrap px-3 py-3 text-sm text-slate-600">

                                ${escapeHtml(
                                    member.employee_id ||
                                    "-"
                                )}

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3">

                                <div
                                    class="font-medium text-slate-800">

                                    ${escapeHtml(
                                        member.name
                                    )}

                                </div>

                                <div
                                    class="mt-1 text-xs text-slate-500">

                                    ${escapeHtml(
                                        member.mobile ||
                                        "-"
                                    )}

                                </div>

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3 text-sm text-slate-600">

                                ${escapeHtml(
                                    member.designation_name ||
                                    "-"
                                )}

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3 text-sm font-medium text-slate-700">

                                ${formatCurrency(
                                    basicSalary
                                )}

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3">

                                <div
                                    class="text-sm font-medium text-slate-700">

                                    ${formatCurrency(
                                        totalEarnings
                                    )}

                                </div>

                                <div
                                    class="mt-1 text-xs text-slate-500">

                                    Net:
                                    ${formatCurrency(
                                        netSalary
                                    )}

                                </div>

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3">

                                ${
                                    salary
                                    ?
                                    `
                                    <span
                                        class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">

                                        Salary Set

                                    </span>
                                    `
                                    :
                                    `
                                    <span
                                        class="rounded-full bg-amber-100 px-3 py-1 text-xs font-medium text-amber-700">

                                        Not Set

                                    </span>
                                    `
                                }

                            </td>

                            <td
                                class="whitespace-nowrap px-3 py-3">

                                <button
                                    type="button"
                                    onclick="openSalary(${member.id})"
                                    class="shrink-0 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-blue-700">

                                    Manage Salary

                                </button>

                            </td>

                        </tr>

                    `;

                }
            )
            .join("");

}

function formatCurrency(
    amount
) {

    return new Intl.NumberFormat(
        "en-IN",
        {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 0
        }
    ).format(
        amount
    );

}

async function openSalary(
    teacherId
) {
     if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    try {

        salaryFormResult.textContent =
            "";

        salaryFormResult.className =
            "mt-4 text-sm";

        salaryTeacherId.value =
            teacherId;

        const teacherData =
            await API.get(
                "/api/teachers"
            );

        if (
            !teacherData.success
        ) {

            throw new Error(
                teacherData.message ||
                "Unable to load staff"
            );

        }

        const teacher =
            (
                teacherData.teachers ||
                []
            ).find(
                staff =>
                    Number(
                        staff.id
                    ) ===
                    Number(
                        teacherId
                    )
            );

        if (
            !teacher
        ) {

            throw new Error(
                "Staff not found"
            );

        }

        salaryStaffName.textContent =
            teacher.name;

        salaryStaffDesignation.textContent =
            teacher.designation_name ||
            "No designation";

        const salaryData =
            await API.get(
                `/api/salary-structures/staff/${teacherId}`
            );

        if (
            !salaryData.success
        ) {

            throw new Error(
                salaryData.message ||
                "Unable to load salary"
            );

        }

        const salary =
            salaryData.salaryStructure;

        basicSalaryInput.value =
            salary?.basic_salary ??
            "";

        hraInput.value =
            salary?.hra ??
            0;

        otherAllowancesInput.value =
            salary?.other_allowances ??
            0;

        pfInput.value =
            salary?.pf ??
            0;

        esiInput.value =
            salary?.esi ??
            0;

        professionalTaxInput.value =
            salary?.professional_tax ??
            0;

        otherDeductionsInput.value =
            salary?.other_deductions ??
            0;

        effectiveFromInput.value =
    salary?.effective_from ??
    "";

const historyData =
    await API.get(
        `/api/salary-structures/staff/${teacherId}/history`
    );

if (
    !historyData.success
) {

    throw new Error(
        historyData.message ||
        "Unable to load salary history"
    );

}

renderSalaryHistory(
    historyData.salaryStructures
);

salaryForm.classList.remove(
    "hidden"
);

        calculateSalary();

    }
    catch (err) {

        console.error(err);

        salaryFormResult.textContent =
            err.message ||
            "Unable to load salary.";

        salaryFormResult.className =
            "mt-4 text-sm text-red-600";

    }

}
function renderSalaryHistory(
    salaryStructures
) {

    if (
        !salaryStructures ||
        salaryStructures.length === 0
    ) {

        salaryHistoryBody.innerHTML = `
            <tr>

                <td
                    colspan="6"
                    class="px-4 py-6 text-center text-sm text-slate-500">

                    No salary history.

                </td>

            </tr>
        `;

        return;

    }

    salaryHistoryBody.innerHTML =
        salaryStructures
            .map(
                salary => {

                    const basic =
                        Number(
                            salary.basic_salary ||
                            0
                        );

                    const hra =
                        Number(
                            salary.hra ||
                            0
                        );

                    const otherAllowances =
                        Number(
                            salary.other_allowances ||
                            0
                        );

                    const pf =
                        Number(
                            salary.pf ||
                            0
                        );

                    const esi =
                        Number(
                            salary.esi ||
                            0
                        );

                    const professionalTax =
                        Number(
                            salary.professional_tax ||
                            0
                        );

                    const otherDeductions =
                        Number(
                            salary.other_deductions ||
                            0
                        );

                    const gross =
                        basic +
                        hra +
                        otherAllowances;

                    const deductions =
                        pf +
                        esi +
                        professionalTax +
                        otherDeductions;

                    const net =
                        gross -
                        deductions;

                    const isActive =
                        salary.status ===
                        "active";

                    return `

                        <tr
    class="border-slate-200 align-middle transition-colors hover:bg-slate-200">

                            <td
                                class="px-4 py-3 text-sm text-slate-700">

                                ${escapeHtml(
                                    salary.effective_from ||
                                    "-"
                                )}

                            </td>

                            <td
                                class="px-4 py-3 text-sm font-medium text-slate-700">

                                ${formatCurrency(
                                    basic
                                )}

                            </td>

                            <td
                                class="px-4 py-3 text-sm text-slate-700">

                                ${formatCurrency(
                                    gross
                                )}

                            </td>

                            <td
                                class="px-4 py-3 text-sm text-slate-700">

                                ${formatCurrency(
                                    deductions
                                )}

                            </td>

                            <td
                                class="px-4 py-3 text-sm font-medium text-slate-700">

                                ${formatCurrency(
                                    net
                                )}

                            </td>

                            <td
                                class="px-4 py-3">

                                <span
                                    class="${
                                        isActive
                                        ? "rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700"
                                        : "rounded-full bg-slate-100 px-3 py-1 text-xs font-medium text-slate-600"
                                    }">

                                    ${
                                        isActive
                                        ? "Active"
                                        : "Inactive"
                                    }

                                </span>

                            </td>

                        </tr>

                    `;

                }
            )
            .join("");

}
function closeSalaryForm() {

    salaryForm.classList.add(
        "hidden"
    );

    salaryFormElement.reset();

    salaryTeacherId.value =
        "";

    salaryStaffName.textContent =
        "-";

    salaryStaffDesignation.textContent =
        "-";

    salaryFormResult.textContent =
        "";

    calculateSalary();

}
function calculateSalary() {

    const basic =
        Number(
            basicSalaryInput.value ||
            0
        );

    const hra =
        Number(
            hraInput.value ||
            0
        );

    const otherAllowances =
        Number(
            otherAllowancesInput.value ||
            0
        );

    const pf =
        Number(
            pfInput.value ||
            0
        );

    const esi =
        Number(
            esiInput.value ||
            0
        );

    const professionalTax =
        Number(
            professionalTaxInput.value ||
            0
        );

    const otherDeductions =
        Number(
            otherDeductionsInput.value ||
            0
        );

    const gross =
        basic +
        hra +
        otherAllowances;

    const deductions =
        pf +
        esi +
        professionalTax +
        otherDeductions;

    const net =
        gross -
        deductions;

    document.getElementById(
        "grossEarnings"
    ).textContent =
        formatCurrency(
            gross
        );

    document.getElementById(
        "totalDeductions"
    ).textContent =
        formatCurrency(
            deductions
        );

    document.getElementById(
        "netSalary"
    ).textContent =
        formatCurrency(
            net
        );

}
[
    basicSalaryInput,
    hraInput,
    otherAllowancesInput,
    pfInput,
    esiInput,
    professionalTaxInput,
    otherDeductionsInput
].forEach(
    input => {

        input.addEventListener(
            "input",
            calculateSalary
        );

    }
);
salaryFormElement.addEventListener(
    "submit",
    saveSalary
);
async function saveSalary(
    event
) { 
    if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    event.preventDefault();

    const teacherId =
        Number(
            salaryTeacherId.value
        );

    if (
        !teacherId
    ) {

        return;

    }

    const saveSalaryBtn =
        document.getElementById(
            "saveSalaryBtn"
        );

    try {

        saveSalaryBtn.disabled =
            true;

        saveSalaryBtn.textContent =
            "Saving...";

        salaryFormResult.textContent =
            "";

        const salary = {

            teacherId,

            basicSalary:
                Number(
                    basicSalaryInput.value ||
                    0
                ),

            hra:
                Number(
                    hraInput.value ||
                    0
                ),

            otherAllowances:
                Number(
                    otherAllowancesInput.value ||
                    0
                ),

            pf:
                Number(
                    pfInput.value ||
                    0
                ),

            esi:
                Number(
                    esiInput.value ||
                    0
                ),

            professionalTax:
                Number(
                    professionalTaxInput.value ||
                    0
                ),

            otherDeductions:
                Number(
                    otherDeductionsInput.value ||
                    0
                ),

            effectiveFrom:
                effectiveFromInput.value

        };

        const data =
            await API.post(
                "/api/salary-structures",
                salary
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to save salary"
            );

        }

        salaryFormResult.textContent =
            "Salary structure saved successfully.";

        salaryFormResult.className =
            "mt-4 text-sm text-emerald-600";

        await loadStaff();

        setTimeout(
            closeSalaryForm,
            500
        );

    }
    catch (err) {

        console.error(err);

        salaryFormResult.textContent =
            err.message ||
            "Unable to save salary.";

        salaryFormResult.className =
            "mt-4 text-sm text-red-600";

    }
    finally {

        saveSalaryBtn.disabled =
            false;

        saveSalaryBtn.textContent =
            "Save Salary";

    }

}

function escapeHtml(
    value
) {

    return String(
        value ?? ""
    )
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );

}