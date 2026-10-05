Auth.requireSchoolOwner();
let currentSubscription = null;

async function loadSubscription() {
    try {
        const data = await API.get("/api/subscription/school");

        if (!data.success) {
            Notify.error(data.message || "Unable to load subscription");
            return;
        }

        currentSubscription = data.subscription || null;
    } catch (err) {
        console.error("Subscription load error:", err);
    }
}

function isSubscriptionBlocked() {
    const status = (
        currentSubscription?.subscription_status || ""
    ).toLowerCase();

    return status === "expired" || status === "suspended";
}

function getSubscriptionBlockMessage() {
    const status = (
        currentSubscription?.subscription_status || ""
    ).toLowerCase();

    if (status === "suspended") {
        return "Your subscription is suspended. Please contact Align Support.";
    }

    if (status === "expired") {
        return "Your subscription has expired. Please renew your subscription.";
    }

    return "Your subscription does not allow this action.";
}
function getCurrentAcademicYear() {

    const today =
        new Date();

    const year =
        today.getFullYear();

    const month =
        today.getMonth() + 1;

    const startYear =
        month >= 4
            ? year
            : year - 1;

    return `${startYear}-${String(
        startYear + 1
    ).slice(-2)}`;

}
const feesStudentTableBody =
    document.getElementById(
        "feesStudentTableBody"
    );

const feesManagementPanel =
    document.getElementById(
        "feesManagementPanel"
    );

const feesStudentName =
    document.getElementById(
        "feesStudentName"
    );

const feesStudentDetails =
    document.getElementById(
        "feesStudentDetails"
    );

const managementTotalFee =
    document.getElementById(
        "managementTotalFee"
    );

const managementTotalPaid =
    document.getElementById(
        "managementTotalPaid"
    );

const managementBalance =
    document.getElementById(
        "managementBalance"
    );

const closeFeesPanelBtn =
    document.getElementById(
        "closeFeesPanelBtn"
    );
const addFeeStructureBtn =
    document.getElementById(
        "addFeeStructureBtn"
    );

const recordPaymentBtn =
    document.getElementById(
        "recordPaymentBtn"
    );
const paymentForm =
    document.getElementById(
        "paymentForm"
    );  

const paymentDateInput =
    document.getElementById(
        "paymentDate"
    );

const paymentModeInput =
    document.getElementById(
        "paymentMode"
    );

const paymentReceiptNoInput =
    document.getElementById(
        "paymentReceiptNo"
    );

const paymentRemarksInput =
    document.getElementById(
        "paymentRemarks"
    );

const savePaymentBtn =
    document.getElementById(
        "savePaymentBtn"
    );

const cancelPaymentBtn =
    document.getElementById(
        "cancelPaymentBtn"
    );

const paymentFormResult =
    document.getElementById(
        "paymentFormResult"
    );

const feeStructuresBody =
    document.getElementById(
        "feeStructuresBody"
    );

const feePaymentsBody =
    document.getElementById(
        "feePaymentsBody"
    );
const feeStructureForm =
    document.getElementById(
        "feeStructureForm"
    );

const feeHeadInput =
    document.getElementById(
        "feeHead"
    );

const feeAmountInput =
    document.getElementById(
        "feeAmount"
    );

const feeEffectiveFromInput =
    document.getElementById(
        "feeEffectiveFrom"
    );

const saveFeeStructureBtn =
    document.getElementById(
        "saveFeeStructureBtn"
    );

const cancelFeeStructureBtn =
    document.getElementById(
        "cancelFeeStructureBtn"
    );

const feeStructureFormResult =
    document.getElementById(
        "feeStructureFormResult"
    );

closeFeesPanelBtn.addEventListener(
    "click",
    closeFeesPanel
);
addFeeStructureBtn.addEventListener(
    "click",
    openFeeStructureForm
);

cancelFeeStructureBtn.addEventListener(
    "click",
    closeFeeStructureForm
);
saveFeeStructureBtn.addEventListener(
    "click",
    saveFeeStructure
);
recordPaymentBtn.addEventListener(
    "click",
    openPaymentForm
);

cancelPaymentBtn.addEventListener(
    "click",
    closePaymentForm
);

savePaymentBtn.addEventListener(
    "click",
    savePayment
);

function openFeeStructureForm() {
    if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    feeStructureFormResult.textContent =
        "";

    feeStructureFormResult.className =
        "text-sm";

    feeHeadInput.value =
        "";

    feeAmountInput.value =
        "";

    feeEffectiveFromInput.value =
    getLocalDate()

    feeStructureForm.classList.remove(
        "hidden"
    );

    feeHeadInput.focus();

}

function closeFeeStructureForm() {

    feeStructureForm.classList.add(
        "hidden"
    );

}
async function openPaymentForm() {
     if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    paymentFormResult.textContent =
        "";

    paymentFormResult.className =
        "text-sm";

const feeHeadOptions =

    feesManagementPanel.dataset.feeStructures
        ? JSON.parse(
            feesManagementPanel.dataset.feeStructures
        )
        : [];

const studentId =
    feesManagementPanel.dataset.studentId;

const academicYear =
    getCurrentAcademicYear();

const paymentFeeItems =
    document.getElementById(
        "paymentFeeItems"
    );

paymentFeeItems.innerHTML = `

    <div
        class="py-6 text-center text-sm text-slate-500">

        Loading fee heads...

    </div>

`;

const paidData =
    await API.get(

        `/api/fee-payments/student/${studentId}/paid-by-head?academicYear=${encodeURIComponent(academicYear)}`

    );

if (
    !paidData.success
) {

    throw new Error(
        paidData.message ||
        "Unable to load paid fee details"
    );

}

const paidMap =
    new Map();

(
    paidData.paidByFeeStructure ||
    []
).forEach(

    item => {

        paidMap.set(

            Number(
                item.fee_structure_id
            ),

            Number(
                item.paid_amount || 0
            )

        );

    }

);

paymentFeeItems.innerHTML = "";

feeHeadOptions
    .filter(

        fee =>

            fee.status === "active"

    )
    .forEach(

        fee => {

            const totalFee =
                Number(
                    fee.amount || 0
                );

            const paidAmount =
                Number(
                    paidMap.get(
                        Number(
                            fee.id
                        )
                    ) || 0
                );

            const outstanding =
                Math.max(
                    totalFee -
                    paidAmount,
                    0
                );
                if (
    outstanding <= 0
) {

    return;

}

            const row =
                document.createElement(
                    "div"
                );

            row.className =
                "rounded-xl border border-slate-200 bg-white p-4";

            row.innerHTML = `

                <div
                    class="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">

                    <div>

                        <p
                            class="font-semibold text-slate-800">

                            ${escapeHtml(
                                fee.fee_head
                            )}

                        </p>

                        <div
                            class="mt-1 flex flex-wrap gap-x-4 gap-y-1 text-xs text-slate-500">

                            <span>
                                Total:
                                ${formatCurrency(
                                    totalFee
                                )}
                            </span>

                            <span>
                                Paid:
                                ${formatCurrency(
                                    paidAmount
                                )}
                            </span>

                            <span
                                class="${
                                    outstanding > 0
                                        ? "font-semibold text-red-600"
                                        : "font-semibold text-emerald-600"
                                }">

                                Outstanding:
                                ${formatCurrency(
                                    outstanding
                                )}

                            </span>

                        </div>

                    </div>

                    <div
                        class="w-full md:w-48">

                        <input
                            type="number"
                            min="0"
                            max="${outstanding}"
                            step="0.01"
                            value=""
                            data-fee-structure-id="${fee.id}"
                            data-fee-head="${escapeHtml(
                                fee.fee_head
                            )}"
                            data-max-amount="${outstanding}"
                            class="payment-fee-amount w-full rounded-xl border border-slate-300 bg-white p-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                            placeholder="Pay amount"
                            ${
                                outstanding <= 0
                                    ? "disabled"
                                    : ""
                            }>

                    </div>

                </div>

            `;

            paymentFeeItems.appendChild(
                row
            );

        }

    );

    paymentFeeItems
        .querySelectorAll(
            ".payment-fee-amount"
        )
        .forEach(

            input => {

                input.addEventListener(
                    "input",
                    updatePaymentTotal
                );

            }

        );

    updatePaymentTotal();

    paymentDateInput.value =
        getLocalDate()

    paymentModeInput.value =
        "";

    paymentRemarksInput.value =
        "";

    paymentForm.classList.remove(
        "hidden"
    );

    const firstAmountInput =
        paymentFeeItems.querySelector(
            ".payment-fee-amount"
        );

    if (
        firstAmountInput
    ) {

        firstAmountInput.focus();

    }

}
function updatePaymentTotal() {

    const inputs =
        document.querySelectorAll(
            ".payment-fee-amount"
        );

    let total = 0;

    inputs.forEach(

        input => {

            const amount =
                Number(
                    input.value || 0
                );

            if (
                Number.isFinite(
                    amount
                )
            ) {

                total += amount;

            }

        }

    );

    const totalElement =
        document.getElementById(
            "paymentTotalAmount"
        );

    if (
        totalElement
    ) {

        totalElement.textContent =
            formatCurrency(
                total
            );

    }

}
function closePaymentForm() {

    paymentForm.classList.add(
        "hidden"
    );

    paymentFormResult.textContent =
        "";

    paymentFormResult.className =
        "text-sm";

    const paymentFeeItems =
        document.getElementById(
            "paymentFeeItems"
        );

    if (
        paymentFeeItems
    ) {

        paymentFeeItems.innerHTML =
            "";

    }

}
async function savePayment() {
    if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    try {

        paymentFormResult.textContent =
            "";

        paymentFormResult.className =
            "text-sm";

        const studentId =
            feesManagementPanel.dataset.studentId;

        const academicYear =
            getCurrentAcademicYear();

        const paymentDate =
            paymentDateInput.value;

        const paymentMode =
            paymentModeInput.value;

        const remarks =
            paymentRemarksInput.value.trim();

        if (
            !studentId
        ) {

            throw new Error(
                "Student not selected"
            );

        }

        const feeInputs =
            document.querySelectorAll(
                ".payment-fee-amount"
            );

        const items = [];

        feeInputs.forEach(

            input => {

                const amount =
                    Number(
                        input.value || 0
                    );

                if (
                    Number.isFinite(
                        amount
                    ) &&
                    amount > 0
                ) {

                    const maxAmount =
                        Number(
                            input.dataset.maxAmount ||
                            0
                        );

                    if (
                        amount > maxAmount
                    ) {

                        throw new Error(
                            `${input.dataset.feeHead} payment cannot exceed ${formatCurrency(maxAmount)}`
                        );

                    }

                    items.push({

                        feeStructureId:
                            Number(
                                input.dataset.feeStructureId
                            ),

                        feeHead:
                            input.dataset.feeHead,

                        amount

                    });

                }

            }

        );

        if (
            items.length === 0
        ) {

            throw new Error(
                "Enter payment amount for at least one fee head"
            );

        }

        const totalAmount =
            items.reduce(

                (
                    total,
                    item
                ) =>

                    total +
                    Number(
                        item.amount || 0
                    ),

                0

            );

        const balance =
            Number(
                feesManagementPanel.dataset.balance ||
                0
            );

        if (
            totalAmount > balance
        ) {

            throw new Error(
                `Payment amount cannot exceed outstanding balance of ${formatCurrency(balance)}`
            );

        }

        if (
            !paymentDate
        ) {

            throw new Error(
                "Payment date is required"
            );

        }

        if (
            !paymentMode
        ) {

            throw new Error(
                "Payment mode is required"
            );

        }

        savePaymentBtn.disabled =
            true;

        savePaymentBtn.textContent =
            "Saving...";

        const response =
            await API.post(
                "/api/fee-payments",
                {

                    studentId:
                        Number(
                            studentId
                        ),

                    academicYear,

                    amount:
                        totalAmount,

                    items,

                    paymentDate,

                    paymentMode,

                    remarks:
                        remarks ||
                        null

                }
            );

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to record payment"
            );

        }

        paymentFormResult.textContent =
            "Payment recorded successfully.";

        paymentFormResult.className =
            "text-sm text-emerald-600";

        closePaymentForm();

        await openFees(
            Number(
                studentId
            )
        );

    }
    catch (err) {

        console.error(err);

        paymentFormResult.textContent =
            err.message ||
            "Unable to record payment.";

        paymentFormResult.className =
            "text-sm text-red-600";

    }
    finally {

        savePaymentBtn.disabled =
            false;

        savePaymentBtn.textContent =
            "Save Payment";

    }

}
async function saveFeeStructure() {
     if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    try {

        feeStructureFormResult.textContent =
            "";

        feeStructureFormResult.className =
            "text-sm";

        const feeHead =
            feeHeadInput.value.trim();

        const amount =
            Number(
                feeAmountInput.value
            );

        const effectiveFrom =
            feeEffectiveFromInput.value;

        const editingId =
            feeStructureForm.dataset.editingId;

        if (
            !feeHead
        ) {

            throw new Error(
                "Fee head is required"
            );

        }

        if (
            !Number.isFinite(amount) ||
            amount <= 0
        ) {

            throw new Error(
                "Enter a valid fee amount"
            );

        }

        if (
            !effectiveFrom
        ) {

            throw new Error(
                "Effective date is required"
            );

        }

        const studentId =
            feesManagementPanel.dataset.studentId;

        const academicYear =
            getCurrentAcademicYear();

        if (
            !studentId
        ) {

            throw new Error(
                "Student not selected"
            );

        }

        saveFeeStructureBtn.disabled =
            true;

        saveFeeStructureBtn.textContent =
            editingId
                ? "Updating..."
                : "Saving...";

        let response;

        if (
            editingId
        ) {

            response =
                await API.put(
                    `/api/fee-structures/${editingId}`,
                    {
                        academicYear,
                        feeHead,
                        amount,
                        effectiveFrom
                    }
                );

        }
        else {

            response =
                await API.post(
                    "/api/fee-structures",
                    {
                        studentId:
                            Number(
                                studentId
                            ),

                        academicYear,

                        feeHead,

                        amount,

                        effectiveFrom

                    }
                );

        }

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                (
                    editingId
                        ? "Unable to update fee structure"
                        : "Unable to save fee structure"
                )
            );

        }

        feeStructureFormResult.textContent =
            editingId
                ? "Fee structure updated successfully."
                : "Fee structure saved successfully.";

        feeStructureFormResult.className =
            "text-sm text-emerald-600";

        feeStructureForm.dataset.editingId =
            "";

        closeFeeStructureForm();

        await openFees(
            Number(
                studentId
            )
        );

    }
    catch (err) {

        console.error(err);

        feeStructureFormResult.textContent =
            err.message ||
            "Unable to save fee structure.";

        feeStructureFormResult.className =
            "text-sm text-red-600";

    }
    finally {

        saveFeeStructureBtn.disabled =
            false;

        saveFeeStructureBtn.textContent =
            "Save Fee";

    }

}

async function closeFeesPanel() {

    feesManagementPanel.classList.add(
        "hidden"
    );

    await loadStudents();

}
async function openFees(
    studentId
) {

    try {
        feesManagementPanel.dataset.studentId =
    studentId;
    closePaymentForm();


        feesManagementPanel.classList.remove(
            "hidden"
        );

        feesStudentName.textContent =
            "Loading...";

        feesStudentDetails.textContent =
            "";

        const academicYear =
            getCurrentAcademicYear();

        const [
            studentData,
            feeData,
            paymentData
        ] =
            await Promise.all([

                API.get(
                    `/api/students/${studentId}`
                ),

                API.get(
                    `/api/fee-structures/student/${studentId}?academicYear=${encodeURIComponent(academicYear)}`
                ),

                API.get(
                    `/api/fee-payments/student/${studentId}/total?academicYear=${encodeURIComponent(academicYear)}`
                )

            ]);

        if (
            !studentData.success
        ) {

            throw new Error(
                studentData.message ||
                "Unable to load student"
            );

        }

        if (
            !feeData.success
        ) {

            throw new Error(
                feeData.message ||
                "Unable to load fee structure"
            );

        }

        if (
            !paymentData.success
        ) {

            throw new Error(
                paymentData.message ||
                "Unable to load payments"
            );

        }

        const student =
            studentData.student;

        const totalFee =
            (
                feeData.feeStructures ||
                []
            )
                .filter(
                    fee =>
                        fee.status === "active"
                )
                .reduce(
                    (
                        total,
                        fee
                    ) =>
                        total +
                        Number(
                            fee.amount ||
                            0
                        ),
                    0
                );

        const totalPaid =
            Number(
                paymentData.totalPaid ||
                0
            );

        const balance =
            totalFee -
            totalPaid;

        feesStudentName.textContent =
            student.name;

        feesStudentDetails.textContent =
            `${student.admission_no} • ${student.class_name || "-"} / ${student.section || "-"}`;

        managementTotalFee.textContent =
            formatCurrency(
                totalFee
            );

        managementTotalPaid.textContent =
            formatCurrency(
                totalPaid
            );

        managementBalance.textContent =
            formatCurrency(
                balance
            );
        feesManagementPanel.dataset.balance =
                balance;

        managementBalance.className =
            `mt-2 text-2xl font-bold ${
                balance > 0
                    ? "text-red-700"
                    : "text-emerald-700"
            }`;
        feesManagementPanel.dataset.feeStructures =
    JSON.stringify(
        feeData.feeStructures || []
    );    

        renderFeeStructures(
    feeData.feeStructures
);

await loadPaymentHistory(
    studentId,
    academicYear
);

        feesManagementPanel.scrollIntoView({
            behavior: "smooth",
            block: "start"
        });

    }
    catch (err) {

        console.error(err);

        feesStudentName.textContent =
            "Unable to load fees";

        feesStudentDetails.textContent =
            err.message ||
            "Unable to load student fees.";

    }

}

(async function initFeesPage() {
    await loadSubscription();
    await loadStudents();
})();

async function loadStudents() {

    try {

        feesStudentTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-sm text-slate-500">

                    Loading students...

                </td>

            </tr>
        `;

        const data =
            await API.get(
                "/api/students"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load students"
            );

        }

        const activeStudents =
            (
                data.students ||
                []
            ).filter(
                student =>
                    student.status === "active"
            );

        renderStudents(
            activeStudents
        );

    }
    catch (err) {

        console.error(err);

        feesStudentTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-sm text-red-600">

                    Unable to load students.

                </td>

            </tr>
        `;

    }

}

async function getStudentFeeSummary(
    studentId,
    academicYear
) {

    const [
        feeData,
        paymentData
    ] =
        await Promise.all([

            API.get(
                `/api/fee-structures/student/${studentId}?academicYear=${encodeURIComponent(academicYear)}`
            ),

            API.get(
                `/api/fee-payments/student/${studentId}/total?academicYear=${encodeURIComponent(academicYear)}`
            )

        ]);

    if (
        !feeData.success
    ) {

        throw new Error(
            feeData.message ||
            "Unable to load fee structure"
        );

    }

    if (
        !paymentData.success
    ) {

        throw new Error(
            paymentData.message ||
            "Unable to load fee payments"
        );

    }

    const totalFee =
        (
            feeData.feeStructures ||
            []
        )
            .filter(
                fee =>
                    fee.status === "active"
            )
            .reduce(
                (
                    total,
                    fee
                ) =>
                    total +
                    Number(
                        fee.amount ||
                        0
                    ),
                0
            );

    const totalPaid =
        Number(
            paymentData.totalPaid ||
            0
        );

    const balance =
        totalFee -
        totalPaid;

    return {

        totalFee,

        totalPaid,

        balance

    };

}

async function renderStudents(
    students
) {

    if (
        !students ||
        students.length === 0
    ) {

        feesStudentTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-sm text-slate-500">

                    No active students found.

                </td>

            </tr>
        `;

        return;

    }

    feesStudentTableBody.innerHTML = `
        <tr>

            <td
                colspan="7"
                class="px-6 py-10 text-center text-sm text-slate-500">

                Loading fee details...

            </td>

        </tr>
    `;

    const academicYear =
    getCurrentAcademicYear();

    try {

        const studentsWithFees =
            await Promise.all(
                students.map(
                    async student => {

                        const feeSummary =
                            await getStudentFeeSummary(
                                student.id,
                                academicYear
                            );

                        return {

                            ...student,

                            ...feeSummary

                        };

                    }
                )
            );

        feesStudentTableBody.innerHTML =
            studentsWithFees
                .map(
                    student => {

                        const balance =
                            Number(
                                student.balance ||
                                0
                            );

                        return `

                            <tr
                                class="border-t border-slate-100">

                                <td
                                    class="px-6 py-4 text-sm text-slate-600">

                                    ${escapeHtml(
                                        student.admission_no
                                    )}

                                </td>

                                <td
                                    class="px-6 py-4">

                                    <div
                                        class="font-medium text-slate-800">

                                        ${escapeHtml(
                                            student.name
                                        )}

                                    </div>

                                    <div
                                        class="mt-1 text-xs text-slate-500">

                                        ${escapeHtml(
                                            student.father_name ||
                                            "-"
                                        )}

                                    </div>

                                </td>

                                <td
                                    class="px-6 py-4 text-sm text-slate-600">

                                    ${escapeHtml(
                                        student.class_name ||
                                        "-"
                                    )}
                                    /
                                    ${escapeHtml(
                                        student.section ||
                                        "-"
                                    )}

                                </td>

                                <td
                                    class="px-6 py-4 text-right text-sm text-slate-600">

                                    ${formatCurrency(
                                        student.totalFee
                                    )}

                                </td>

                                <td
                                    class="px-6 py-4 text-right text-sm text-emerald-600">

                                    ${formatCurrency(
                                        student.totalPaid
                                    )}

                                </td>

                                <td
                                    class="px-6 py-4 text-right text-sm font-medium ${
                                        balance > 0
                                        ? "text-red-600"
                                        : "text-emerald-600"
                                    }">

                                    ${formatCurrency(
                                        balance
                                    )}

                                </td>

                                <td
                                    class="px-6 py-4">

                                    <button
                                        type="button"
                                        onclick="openFees(${student.id})"
                                        class="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700">

                                        Manage Fees

                                    </button>

                                </td>

                            </tr>

                        `;

                    }
                )
                .join("");

    }
    catch (err) {

        console.error(err);

        feesStudentTableBody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="px-6 py-10 text-center text-sm text-red-600">

                    Unable to load fee details.

                </td>

            </tr>
        `;

    }

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
        Number(
            amount ||
            0
        )
    );

}
function renderFeeStructures(
    feeStructures
) {

    const activeFees =
        (
            feeStructures ||
            []
        ).filter(
            fee =>
                fee.status === "active"
        );

    if (
        activeFees.length === 0
    ) {

        feeStructuresBody.innerHTML = `
            <tr>

                <td
                    colspan="4"
                    class="px-4 py-6 text-center text-sm text-slate-500">

                    No fee structure found.

                </td>

            </tr>
        `;

        return;

    }

    feeStructuresBody.innerHTML =
        activeFees
            .map(
                fee => `
                    <tr
                        class="border-t border-slate-100">

                        <td
                            class="px-4 py-3 text-sm font-medium text-slate-700">

                            ${escapeHtml(
                                fee.fee_head
                            )}

                        </td>

                        <td
                            class="px-4 py-3 text-right text-sm font-medium text-slate-700">

                            ${formatCurrency(
                                Number(
                                    fee.amount ||
                                    0
                                )
                            )}

                        </td>

                        <td
                            class="px-4 py-3 text-sm text-slate-600">

                            ${escapeHtml(
                                fee.effective_from ||
                                "-"
                            )}

                        </td>

                        <td
                            class="px-4 py-3 text-right">

                            <button
                                type="button"
                                onclick="editFeeStructure(${fee.id})"
                                class="rounded-lg border border-blue-200 bg-blue-50 px-3 py-1.5 text-xs font-medium text-blue-700 hover:bg-blue-100">

                                Edit

                            </button>

                        </td>

                    </tr>
                `
            )
            .join("");

}
function editFeeStructure(
    feeStructureId
) {

    const feeStructures =
        feesManagementPanel.dataset.feeStructures
            ? JSON.parse(
                feesManagementPanel.dataset.feeStructures
            )
            : [];

    const fee =
        feeStructures.find(
            item =>
                Number(
                    item.id
                ) ===
                Number(
                    feeStructureId
                )
        );

    if (
        !fee
    ) {

        return;

    }

    feeStructureForm.classList.remove(
        "hidden"
    );

    feeHeadInput.value =
        fee.fee_head || "";

    feeAmountInput.value =
        fee.amount ?? "";

    feeEffectiveFromInput.value =
        fee.effective_from || "";

    feeStructureForm.dataset.editingId =
        fee.id;

    saveFeeStructureBtn.textContent =
        "Update Fee";

    feeStructureFormResult.textContent =
        "";

    feeStructureFormResult.className =
        "text-sm";

    feeHeadInput.focus();

}

async function loadPaymentHistory(
    studentId,
    academicYear
) {

    try {

        const paymentData =
            await API.get(
                `/api/fee-payments/student/${studentId}?academicYear=${encodeURIComponent(academicYear)}`
            );

        if (
            !paymentData.success
        ) {

            throw new Error(
                paymentData.message ||
                "Unable to load payments"
            );

        }

        renderPaymentHistory(
            paymentData.payments
        );

    }
    catch (err) {

        console.error(err);

        feePaymentsBody.innerHTML = `
            <tr>

                <td
                    colspan="4"
                    class="px-4 py-6 text-center text-sm text-red-600">

                    Unable to load payment history.

                </td>

            </tr>
        `;

    }

}


function renderPaymentHistory(
    payments
) {

    if (
        !payments ||
        payments.length === 0
    ) {

        feePaymentsBody.innerHTML = `
            <tr>

                <td
                    colspan="4"
                    class="px-4 py-6 text-center text-sm text-slate-500">

                    No payments found.

                </td>

            </tr>
        `;

        return;

    }

    feePaymentsBody.innerHTML =
        payments
            .map(
                payment => `
                    <tr
                        class="border-t border-slate-100">

                        <td
                            class="px-4 py-3 text-sm text-slate-600">

                            ${escapeHtml(
                                payment.payment_date ||
                                "-"
                            )}

                        </td>

                        <td
                            class="px-4 py-3 text-right text-sm font-medium text-slate-700">

                            ${formatCurrency(
                                Number(
                                    payment.amount ||
                                    0
                                )
                            )}

                        </td>

                        <td
                            class="px-4 py-3 text-sm text-slate-600">

                            ${escapeHtml(
                                payment.payment_mode ||
                                "-"
                            )}

                        </td>

                        <td
    class="px-4 py-3 text-sm">

    <button
        type="button"
        onclick="printFeeReceipt(${payment.id})"
        class="rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-blue-700">

        Print

    </button>

</td>

                    </tr>
                `
            )
            .join("");

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
function numberToWords(
    amount
) {

    const number =
        Number(amount);

    if (
        !Number.isFinite(number)
    ) {

        return "Zero Rupees";

    }

    const rupees =
        Math.floor(number);

    const paise =
        Math.round(
            (
                number -
                rupees
            ) * 100
        );

    const ones = [

        "",

        "One",

        "Two",

        "Three",

        "Four",

        "Five",

        "Six",

        "Seven",

        "Eight",

        "Nine",

        "Ten",

        "Eleven",

        "Twelve",

        "Thirteen",

        "Fourteen",

        "Fifteen",

        "Sixteen",

        "Seventeen",

        "Eighteen",

        "Nineteen"

    ];

    const tens = [

        "",

        "",

        "Twenty",

        "Thirty",

        "Forty",

        "Fifty",

        "Sixty",

        "Seventy",

        "Eighty",

        "Ninety"

    ];

    function convert(
        value
    ) {

        if (
            value < 20
        ) {

            return ones[value];

        }

        if (
            value < 100
        ) {

            return (

                tens[
                    Math.floor(
                        value / 10
                    )
                ] +

                (

                    value % 10
                        ? " " +
                            ones[
                                value % 10
                            ]
                        : ""

                )

            );

        }

        if (
            value < 1000
        ) {

            return (

                ones[
                    Math.floor(
                        value / 100
                    )
                ] +

                " Hundred" +

                (

                    value % 100
                        ? " " +
                            convert(
                                value % 100
                            )
                        : ""

                )

            );

        }

        if (
            value < 100000
        ) {

            return (

                convert(
                    Math.floor(
                        value / 1000
                    )
                ) +

                " Thousand" +

                (

                    value % 1000
                        ? " " +
                            convert(
                                value % 1000
                            )
                        : ""

                )

            );

        }

        if (
            value < 10000000
        ) {

            return (

                convert(
                    Math.floor(
                        value / 100000
                    )
                ) +

                " Lakh" +

                (

                    value % 100000
                        ? " " +
                            convert(
                                value % 100000
                            )
                        : ""

                )

            );

        }

        return (

            convert(
                Math.floor(
                    value / 10000000
                )
            ) +

            " Crore" +

            (

                value % 10000000
                    ? " " +
                        convert(
                            value % 10000000
                        )
                    : ""

            )

        );

    }

    let result =

        rupees === 0
            ? "Zero Rupees"
            : convert(
                rupees
            ) +
              " Rupees";

    if (
        paise > 0
    ) {

        result +=

            " and " +

            convert(
                paise
            ) +

            " Paise";

    }

    return result;

}
async function printFeeReceipt(
    paymentId
) {

    try {

        const data =
            await API.get(
                `/api/fee-payments/${paymentId}/receipt`
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load receipt"
            );

        }

        const payment =
            data.payment;
        const paymentItems =
            payment.items || [];    

        const printWindow =
            window.open(
                "",
                "_blank",
                "width=800,height=900"
            );

        if (
            !printWindow
        ) {

            throw new Error(
                "Please allow pop-ups to print the receipt."
            );

        }

        printWindow.document.write(`

<!DOCTYPE html>

<html>

<head>

<meta charset="UTF-8">

<title>
RECEIPT - ${escapeHtml(
    payment.receipt_no || ""
)}
</title>

<style>

body {
    font-family: Arial, sans-serif;
    margin: 0;
    padding: 30px;
    color: #111827;
}

.receipt {
    max-width: 750px;
    margin: auto;
    border: 1px solid #d1d5db;
    padding: 30px;
}

.header {
    text-align: center;
    border-bottom: 2px solid #111827;
    padding-bottom: 15px;
} 

.school-name {
    font-size: 24px;
    font-weight: bold;
}
.school-logo {
    display: flex;
    justify-content: center;
    margin-bottom: 12px;
}

.school-logo img {
    width: 100px;
    height: 100px;
    object-fit: contain;
}

.school-info {
    margin-top: 6px;
    font-size: 14px;
    line-height: 1.5;
}

.receipt-title {
    margin-top: 18px;
    font-size: 20px;
    font-weight: bold;
}

.receipt-meta {
    display: flex;
    justify-content: space-between;
    margin-top: 20px;
    font-size: 14px;
}

.section-title {
    margin-top: 25px;
    padding-bottom: 6px;
    border-bottom: 1px solid #d1d5db;
    font-weight: bold;
}

.details {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
}

.details td {
    padding: 7px 4px;
    vertical-align: top;
}

.label {
    width: 35%;
    font-weight: bold;
}

.payment-table {
    width: 100%;
    border-collapse: collapse;
    margin-top: 10px;
}

.payment-table th,
.payment-table td {
    border: 1px solid #d1d5db;
    padding: 9px;
    text-align: left;
}

.payment-table th:last-child,
.payment-table td:last-child {
    text-align: right;
}

.amount-words {
    margin-top: 15px;
    font-weight: bold;
}

.signature {
    margin-top: 70px;
    text-align: right;
}

.footer {
    margin-top: 35px;
    padding-top: 12px;
    border-top: 1px solid #d1d5db;
    text-align: center;
    font-size: 12px;
    color: #4b5563;
}

@media print {

    body {
        padding: 0;
    }

    .receipt {
        border: none;
        max-width: none;
    }

}

</style>

</head>

<body>

<div class="receipt">

    <div class="header">

    ${
        payment.school_logo
            ? `
                <div class="school-logo">

                    <img
    src="${escapeHtml(
        payment.school_logo
    )}"
    alt=""
    onerror="this.parentElement.style.display='none';">

                </div>
            `
            : ""
    }

    <div class="school-name">

            ${escapeHtml(
                payment.school_name ||
                "-"
            )}

        </div>

        <div class="school-info">

            ${escapeHtml(
    [
        payment.school_address,
        payment.school_city,
        payment.school_state,
        payment.school_pincode
    ]
        .filter(Boolean)
        .join(", ") ||
    "-"
)}

            <br>

            Contact:
            ${escapeHtml(
                payment.school_mobile ||
                "-"
            )}

        </div>

        

    </div>

    <div class="receipt-meta">

        <div>

            <strong>Receipt No.:</strong>

            ${escapeHtml(
                payment.receipt_no ||
                "-"
            )}

        </div>

        <div>

            <strong>Date:</strong>

            ${escapeHtml(
                payment.payment_date ||
                "-"
            )}

        </div>

    </div>

    <div class="section-title">

        Student Information

    </div>

    <table class="details">

        <tr>

            <td class="label">
                Name
            </td>

            <td>
                ${escapeHtml(
                    payment.student_name ||
                    "-"
                )}
            </td>

        </tr>

        <tr>

            <td class="label">
                Father's Name
            </td>

            <td>
                ${escapeHtml(
                    payment.father_name ||
                    "-"
                )}
            </td>

        </tr>

        <tr>

            <td class="label">
                Contact No.
            </td>

            <td>
                ${escapeHtml(
                    payment.student_mobile ||
                    "-"
                )}
            </td>

        </tr>

        <tr>

            <td class="label">
                Admission No.
            </td>

            <td>
                ${escapeHtml(
                    payment.admission_no ||
                    "-"
                )}
            </td>

        </tr>

        <tr>

            <td class="label">
                Class / Section
            </td>

            <td>

                ${escapeHtml(
                    payment.class_name ||
                    "-"
                )}

                /

                ${escapeHtml(
                    payment.section ||
                    "-"
                )}

            </td>

        </tr>

    </table>

    <div class="section-title">

        Payment Details

    </div>
    <div
    style="margin-top: 10px; font-size: 14px;">

    <strong>Payment Mode:</strong>

    ${escapeHtml(
        payment.payment_mode ||
        "-"
    )}

</div>

    <table class="payment-table">

        <thead>

            <tr>

                <th>
                    Title
                </th>

                <th>
                    Amount
                </th>

            </tr>

        </thead>

        <tbody>

            ${paymentItems
    .map(
        item => `
            <tr>

                <td>
                    ${escapeHtml(
                        item.fee_head ||
                        "-"
                    )}
                </td>

                <td>
                    ${formatCurrency(
                        Number(
                            item.amount ||
                            0
                        )
                    )}
                </td>

            </tr>
        `
    )
    .join("")}

<tr>

    <td
        style="text-align: right; font-weight: bold;">

        Total

    </td>

    <td
        style="font-weight: bold;">

        ${formatCurrency(
            Number(
                payment.amount ||
                0
            )
        )}

    </td>

</tr>

        </tbody>

    </table>

    <div class="amount-words">

        Amount in Words:

        ${numberToWords(
            Number(
                payment.amount ||
                0
            )
        )}

        Only

    </div>

    <div class="signature">

       

        <br>

        <strong>
            Authorized Signatory
        </strong>

    </div>

    <div class="footer">

    ${escapeHtml(
        payment.receipt_footer_message ||
        ""
    )}

</div>

</div>

</body>

</html>

        `);

        printWindow.document.close();

        printWindow.focus();

        setTimeout(
            () => {

                printWindow.print();

            },
            300
        );

    }
    catch (err) {

        console.error(err);

        Notify.error(
            err.message ||
            "Unable to print receipt"
        );

    }

}