async function loadPaymentHistory() {

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
                "/api/super-admin/payments",
                {
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

        const data =
            await response.json();

        const table =
            document.getElementById(
                "paymentsTable"
            );

        if (!data.success) {

            table.innerHTML = `

                <tr>

                    <td
                        colspan="8"
                        class="p-6 text-center text-red-600">

                        ${data.message || "Unable to load payment history."}

                    </td>

                </tr>

            `;

            return;

        }

        if (!data.payments.length) {

            table.innerHTML = `

                <tr>

                    <td
                        colspan="8"
                        class="p-6 text-center text-slate-500">

                        No payment history found.

                    </td>

                </tr>

            `;

            return;

        }

        table.innerHTML =
            data.payments
                .map(payment => `

                    <tr class="border-t">

                        <td class="whitespace-nowrap p-4">

                            ${payment.paid_at || payment.created_at || "-"}

                        </td>

                        <td class="p-4">

                            ${payment.restaurant_name || "-"}

                            <div class="text-xs text-slate-500">

                                ${payment.restaurant_code || ""}

                            </div>

                        </td>

                        <td class="p-4">

                            ${payment.plan_name || "-"}

                        </td>

                        <td class="p-4">

                            ${payment.duration_days || "-"} days

                        </td>

                        <td class="p-4 font-medium">

                            ${payment.currency || "INR"}
                            ${payment.amount ?? "-"}

                        </td>

                        <td class="p-4">

                            ${payment.payment_method || "-"}

                        </td>

                        <td class="p-4">

                            ${payment.razorpay_payment_id || "-"}

                        </td>

                        <td class="p-4">

                            ${payment.status || "-"}

                        </td>

                    </tr>

                `)
                .join("");

    } catch (err) {

        console.error(
            "PAYMENT HISTORY ERROR:",
            err
        );

        const table =
            document.getElementById(
                "paymentsTable"
            );

        table.innerHTML = `

            <tr>

                <td
                    colspan="8"
                    class="p-6 text-center text-red-600">

                    Unable to load payment history.

                </td>

            </tr>

        `;

    }

}
