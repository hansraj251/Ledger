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
const salaryMonth =
    document.getElementById(
        "salaryMonth"
    );

const totalSalary =
    document.getElementById(
        "totalSalary"
    );

const totalPaid =
    document.getElementById(
        "totalPaid"
    );

const pendingAmount =
    document.getElementById(
        "pendingAmount"
    );

const salaryTableBody =
    document.getElementById(
        "salaryTableBody"
    );

const result =
    document.getElementById(
        "result"
    );

const salaryPaymentModal =
    document.getElementById(
        "salaryPaymentModal"
    );

const salaryPaymentForm =
    document.getElementById(
        "salaryPaymentForm"
    );

const paymentTeacherId =
    document.getElementById(
        "paymentTeacherId"
    );

const paymentStaffName =
    document.getElementById(
        "paymentStaffName"
    );

const paymentSalaryAmount =
    document.getElementById(
        "paymentSalaryAmount"
    );

const paymentAlreadyPaid =
    document.getElementById(
        "paymentAlreadyPaid"
    );

const paymentPendingAmount =
    document.getElementById(
        "paymentPendingAmount"
    );

const salaryAmountInput =
    document.getElementById(
        "salaryAmountInput"
    );

const salaryPaymentDate =
    document.getElementById(
        "salaryPaymentDate"
    );

const salaryPaymentMode =
    document.getElementById(
        "salaryPaymentMode"
    );

const salaryReferenceNo =
    document.getElementById(
        "salaryReferenceNo"
    );

const salaryRemarks =
    document.getElementById(
        "salaryRemarks"
    );

const paymentResult =
    document.getElementById(
        "paymentResult"
    );

const submitSalaryPaymentBtn =
    document.getElementById(
        "submitSalaryPaymentBtn"
    );

let selectedStaff =
    null;

let pendingSalaryStaff =
    [];

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
        Number(
            amount || 0
        )
    );

}

function escapeHtml(
    value
) {

    const div =
        document.createElement(
            "div"
        );

    div.textContent =
        value ?? "";

    return div.innerHTML;

}

async function loadPendingSalary() {

    try {

        result.textContent =
            "";

        const response =
            await API.get(
                "/api/salary-payments/pending"
            );

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to load pending salary"
            );

        }

        salaryMonth.textContent =
            `Salary Month: ${response.salaryMonth}`;

        totalSalary.textContent =
            formatCurrency(
                response.totalSalary
            );

        totalPaid.textContent =
            formatCurrency(
                response.totalPaid
            );

        pendingAmount.textContent =
            formatCurrency(
                response.pendingAmount
            );

        renderStaff(
            response.staff || []
        );

    }
    catch (err) {

        console.error(err);

        salaryTableBody.innerHTML = `

            <tr>

                <td
                    colspan="7"
                    class="px-4 py-8 text-center text-sm text-red-600">

                    ${escapeHtml(
                        err.message ||
                        "Unable to load salary"
                    )}

                </td>

            </tr>

        `;

    }

}

function renderStaff(
    staff
) {

    pendingSalaryStaff =
        staff;

    if (
        staff.length === 0
    ) {

        salaryTableBody.innerHTML = `

            <tr>

                <td
                    colspan="7"
                    class="px-4 py-8 text-center text-sm text-slate-500">

                    No salary records found.

                </td>

            </tr>

        `;

        return;

    }

    salaryTableBody.innerHTML =
        staff.map(
            member => {

                const fullyPaid =
                    Number(
                        member.pendingAmount
                    ) <= 0;

                return `

                    <tr
                        class="border-slate-200 transition-colors hover:bg-slate-200">

                        <td
                            class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                            ${escapeHtml(
                                member.employeeId ||
                                "-"
                            )}

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4">

                            <div
                                class="font-medium text-slate-800">

                                ${escapeHtml(
                                    member.teacherName
                                )}

                            </div>

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                            ${escapeHtml(
                                member.designationName ||
                                "-"
                            )}

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4 text-right text-sm font-medium text-slate-700">

                            ${formatCurrency(
                                member.salaryAmount
                            )}

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4 text-right text-sm text-emerald-600">

                            ${formatCurrency(
                                member.totalPaid
                            )}

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4 text-right">

                            <span
                                class="font-semibold ${
                                    fullyPaid
                                        ? "text-emerald-600"
                                        : "text-amber-600"
                                }">

                                ${formatCurrency(
                                    member.pendingAmount
                                )}

                            </span>

                        </td>

                        <td
                            class="whitespace-nowrap px-4 py-4 text-right">

                            ${
                                fullyPaid

                                ?

                                `

                                <span
                                    class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">

                                    Fully Paid

                                </span>

                                `

                                :

                                `

                                <button
                                    type="button"
                                    onclick="openSalaryPayment(${Number(
                                        member.teacherId
                                    )})"
                                    class="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-blue-700">

                                    Pay Salary

                                </button>

                                `

                            }

                        </td>

                    </tr>

                `;

            }
        ).join("");

}

function openSalaryPayment(
    teacherId
) {
    if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    const staff =
        pendingSalaryStaff.find(
            member =>
                Number(
                    member.teacherId
                ) ===
                Number(
                    teacherId
                )
        );

    if (
        !staff
    ) {

        return;

    }

    selectedStaff =
        staff;

    paymentTeacherId.value =
        staff.teacherId;

    paymentStaffName.textContent =
        staff.teacherName;

    paymentSalaryAmount.textContent =
        formatCurrency(
            staff.salaryAmount
        );

    paymentAlreadyPaid.textContent =
        formatCurrency(
            staff.totalPaid
        );

    paymentPendingAmount.textContent =
        formatCurrency(
            staff.pendingAmount
        );

    salaryAmountInput.value =
        staff.pendingAmount;

    salaryAmountInput.max =
        staff.pendingAmount;

    salaryPaymentDate.value =
          getLocalDate();

    salaryPaymentMode.value =
        "";

    salaryReferenceNo.value =
        "";

    salaryRemarks.value =
        "";

    paymentResult.textContent =
        "";

    paymentResult.className =
        "mb-4 text-sm font-medium";

    salaryPaymentModal.classList.remove(
        "hidden"
    );

    salaryPaymentModal.classList.add(
        "flex"
    );

}

function closeSalaryPayment() {

    salaryPaymentModal.classList.add(
        "hidden"
    );

    salaryPaymentModal.classList.remove(
        "flex"
    );

    selectedStaff =
        null;

}

salaryPaymentForm.addEventListener(
    "submit",
    async (
        event
    ) => {

        event.preventDefault();

        if (
            !selectedStaff
        ) {

            return;

        }

        paymentResult.textContent =
            "";

        paymentResult.className =
            "mb-4 text-sm font-medium";

        const amount =
            Number(
                salaryAmountInput.value
            );

        if (
            !Number.isFinite(
                amount
            ) ||
            amount <= 0
        ) {

            paymentResult.textContent =
                "Enter a valid payment amount.";

            paymentResult.className =
                "mb-4 text-sm font-medium text-red-600";

            return;

        }

        if (
            amount >
            Number(
                selectedStaff.pendingAmount
            )
        ) {

            paymentResult.textContent =
                "Payment amount cannot exceed outstanding salary.";

            paymentResult.className =
                "mb-4 text-sm font-medium text-red-600";

            return;

        }

        submitSalaryPaymentBtn.disabled =
            true;

        submitSalaryPaymentBtn.textContent =
            "Saving...";

        try {

            const response =
                await API.post(
                    "/api/salary-payments",
                    {

                        teacherId:
                            Number(
                                selectedStaff.teacherId
                            ),

                        salaryMonth:
                            selectedStaff.salaryMonth,

                        amountPaid:
                            amount,

                        paymentDate:
                            salaryPaymentDate.value,

                        paymentMode:
                            salaryPaymentMode.value,

                        referenceNo:
                            salaryReferenceNo.value.trim(),

                        remarks:
                            salaryRemarks.value.trim()

                    }
                );

            if (
                !response.success
            ) {

                throw new Error(
                    response.message ||
                    "Unable to save salary payment"
                );

            }

            paymentResult.textContent =
                "Salary payment saved successfully.";

            paymentResult.className =
                "mb-4 text-sm font-medium text-emerald-600";

            await loadPendingSalary();

            setTimeout(
                () => {

                    closeSalaryPayment();

                },
                700
            );

        }
        catch (err) {

            console.error(err);

            paymentResult.textContent =
                err.message ||
                "Unable to save salary payment.";

            paymentResult.className =
                "mb-4 text-sm font-medium text-red-600";

        }
        finally {

            submitSalaryPaymentBtn.disabled =
                false;

            submitSalaryPaymentBtn.textContent =
                "Pay Salary";

        }

    }
);

salaryPaymentModal.addEventListener(
    "click",
    event => {

        if (
            event.target ===
            salaryPaymentModal
        ) {

            closeSalaryPayment();

        }

    }
);


(async function initSalaryPage() {
    await loadSubscription();
    await loadPendingSalary();
})();