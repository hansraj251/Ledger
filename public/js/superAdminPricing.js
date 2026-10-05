let currentPricing = [];

let currentPlans = [];

async function loadPricing() {

    const pricingResponse =
        await API.get(
            "/api/super-admin/pricing"
        );

    if (!pricingResponse.success) {

        Notify.error(
            pricingResponse.message
        );

        return;

    }

    currentPricing =
        pricingResponse.pricing;

    const plansResponse =
        await API.get(
            "/api/super-admin/all-plans"
        );

    if (!plansResponse.success) {

        Notify.error(
            plansResponse.message
        );

        return;

    }

    currentPlans =
        plansResponse.plans;

    renderPricing();

}
function renderPricing() {

    const tbody =
        document.getElementById(
            "pricingTable"
        );

    let html = "";

    currentPricing.forEach(
        pricing => {

            html += `

<tr>

<td class="p-4">

${pricing.plan_name}

</td>

<td class="p-4">

${pricing.duration_days} Days

</td>

<td class="p-4">

${Align.formatCurrency(pricing.price)}

</td>

<td class="p-4">

${pricing.currency}

</td>

<td class="p-4">

<span class="rounded-full px-3 py-1 text-sm
${
pricing.status === "active"
? "bg-green-100 text-green-700"
: "bg-red-100 text-red-700"
}">

${pricing.status}

</span>

</td>

<td class="p-4">

<button

onclick="editPricing(${pricing.id})"

class="rounded bg-slate-600 px-3 py-2 text-white hover:bg-blue-700">

Edit

</button>

</td>

</tr>

`;

        }

    );

    tbody.innerHTML = html;

}
function editPricing(
    pricingId
) {

    const pricing =
        currentPricing.find(
            p => p.id === pricingId
        );

    if (!pricing) {

        Notify.error(
            "Pricing not found."
        );

        return;

    }

    openPricingModal(
        pricing
    );

}
function openPricingModal(
    pricing = null
) {

    const isEdit =
        pricing !== null;

    let planOptions = "";

    currentPlans.forEach(
        plan => {

            planOptions += `

<option

value="${plan.id}"

${
    pricing &&
    pricing.plan_id === plan.id
        ? "selected"
        : ""
}>

${plan.display_name}

</option>

`;

        }
    );

    Modal.open(

        isEdit
            ? "Edit Pricing"
            : "Add Pricing",

        `

<div class="space-y-4">

<div>

<label class="mb-1 block text-sm font-medium">

Plan

</label>

<select

id="pricingPlan"

class="w-full rounded-lg border p-3"

${isEdit ? "disabled" : ""}>

${planOptions}

</select>

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Duration (Days)

</label>

<input

id="pricingDuration"

type="number"

class="w-full rounded-lg border p-3"

value="${pricing?.duration_days ?? ""}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Price

</label>

<input

id="pricingPrice"

type="number"

step="0.01"

class="w-full rounded-lg border p-3"

value="${pricing?.price ?? ""}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Currency

</label>

<input

id="pricingCurrency"

class="w-full rounded-lg border p-3"

value="${pricing?.currency ?? "INR"}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Status

</label>

<select

id="pricingStatus"

class="w-full rounded-lg border p-3">

<option
value="active"
${pricing?.status === "inactive" ? "" : "selected"}>

Active

</option>

<option
value="inactive"
${pricing?.status === "inactive" ? "selected" : ""}>

Inactive

</option>

</select>

</div>

</div>

`,

       async () => {

    const payload = {

        plan_id:
            Number(
                document.getElementById(
                    "pricingPlan"
                ).value
            ),

        duration_days:
            Number(
                document.getElementById(
                    "pricingDuration"
                ).value
            ),

        price:
            Number(
                document.getElementById(
                    "pricingPrice"
                ).value
            ),

        currency:
            document.getElementById(
                "pricingCurrency"
            ).value.trim(),

        status:
            document.getElementById(
                "pricingStatus"
            ).value

    };

    if (!payload.plan_id) {

        Notify.error(
            "Please select a plan."
        );

        return;

    }

    if (payload.duration_days <= 0) {

        Notify.error(
            "Duration must be greater than zero."
        );

        return;

    }

    if (payload.price <= 0) {

        Notify.error(
            "Price must be greater than zero."
        );

        return;

    }

    let response;

    if (isEdit) {

        response =
            await API.put(

                `/api/super-admin/pricing/${pricing.id}`,

                {

                    duration_days:
                        payload.duration_days,

                    price:
                        payload.price,

                    currency:
                        payload.currency,

                    status:
                        payload.status

                }

            );

    } else {

        response =
            await API.post(

                "/api/super-admin/pricing",

                payload

            );

    }

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    Modal.close();

    Notify.success(

        isEdit
            ? "Pricing updated successfully."
            : "Pricing created successfully."

    );

    await loadPricing();

},

        {

            buttonText:
                isEdit
                    ? "Update Pricing"
                    : "Create Pricing",

            loadingText:
                isEdit
                    ? "Updating..."
                    : "Creating..."

        }

    );

}