Auth.requireSchoolOwner();

const salaryMonthInput =
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

const transactionCount =
    document.getElementById(
        "transactionCount"
    );

const salaryHistoryBody =
    document.getElementById(
        "salaryHistoryBody"
    );

const result =
    document.getElementById(
        "result"
    );

function getCurrentSalaryMonth() {

    const today =
        new Date();

    return `${today.getFullYear()}-${String(
        today.getMonth() + 1
    ).padStart(
        2,
        "0"
    )}`;

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

async function loadSalaryHistory() {

    try {

        result.textContent =
            "";

        const month =
            salaryMonthInput.value;

        const response =
            await API.get(
                `/api/salary-payments/history?salaryMonth=${encodeURIComponent(
                    month
                )}`
            );

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to load salary history"
            );

        }

        const history =
            response.history || [];

        renderSummary(
            history
        );

        renderHistory(
            history
        );

    }
    catch (err) {

        console.error(err);

        result.textContent =
            err.message ||
            "Unable to load salary history.";

        result.className =
            "mt-4 text-sm font-medium text-red-600";

        salaryHistoryBody.innerHTML = `

            <tr>

                <td
                    colspan="9"
                    class="px-4 py-8 text-center text-sm text-red-600">

                    ${escapeHtml(
                        err.message ||
                        "Unable to load salary history"
                    )}

                </td>

            </tr>

        `;

    }

}

function renderSummary(
    history
) {

    const totalPaidAmount =
        history.reduce(
            (
                total,
                payment
            ) =>
                total +
                Number(
                    payment.amount_paid ||
                    0
                ),
            0
        );

    const salaryAmounts =
        new Map();

    history.forEach(
        payment => {

            const key =
                Number(
                    payment.teacher_id
                );

            if (
                !salaryAmounts.has(
                    key
                )
            ) {

                salaryAmounts.set(
                    key,
                    Number(
                        payment.salary_amount ||
                        0
                    )
                );

            }

        }
    );

    const totalSalaryAmount =
        Array.from(
            salaryAmounts.values()
        ).reduce(
            (
                total,
                amount
            ) =>
                total +
                amount,
            0
        );

    totalSalary.textContent =
        formatCurrency(
            totalSalaryAmount
        );

    totalPaid.textContent =
        formatCurrency(
            totalPaidAmount
        );

    transactionCount.textContent =
        history.length;

}

function renderHistory(
    history
) {

    if (
        history.length === 0
    ) {

        salaryHistoryBody.innerHTML = `

            <tr>

                <td
                    colspan="9"
                    class="px-4 py-8 text-center text-sm text-slate-500">

                    No salary payment records found for this month.

                </td>

            </tr>

        `;

        return;

    }

    salaryHistoryBody.innerHTML =
        history.map(
            payment => `

                <tr
                    class="border-slate-200 transition-colors hover:bg-slate-200">

                    <td
                        class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.employee_id ||
                            "-"
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4">

                        <div
                            class="font-medium text-slate-800">

                            ${escapeHtml(
                                payment.teacher_name ||
                                "-"
                            )}

                        </div>

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.designation_name ||
                            "-"
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-right text-sm font-medium text-slate-700">

                        ${formatCurrency(
                            payment.salary_amount
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-right text-sm font-semibold text-emerald-600">

                        ${formatCurrency(
                            payment.amount_paid
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.payment_date ||
                            "-"
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.payment_mode ||
                            "-"
                        )}

                    </td>

                    <td
                        class="whitespace-nowrap px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.reference_no ||
                            "-"
                        )}

                    </td>

                    <td
                        class="max-w-xs px-4 py-4 text-sm text-slate-600">

                        ${escapeHtml(
                            payment.remarks ||
                            "-"
                        )}

                    </td>

                </tr>

            `
        ).join("");

}

salaryMonthInput.value =
    getCurrentSalaryMonth();

salaryMonthInput.addEventListener(
    "change",
    loadSalaryHistory
);

loadSalaryHistory();