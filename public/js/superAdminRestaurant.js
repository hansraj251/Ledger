let plans = [];
function getRestaurantId() {

    return new URLSearchParams(
        location.search
    ).get("id");

}

async function loadRestaurant() {

    await loadPlans();

    const id =
        getRestaurantId();

    const response =
        await fetch(

            `/api/super-admin/restaurants/${id}`,

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

        if (data.success) {

    Notify.success(
        data.message
    );

} else {

    Notify.error(
        data.message
    );

}

        return;

    }

    renderRestaurant(
        data.restaurant
    );

}
function renderRestaurant(
    restaurant
) {
const remainingDays =
    Math.ceil(
        (
            new Date(restaurant.plan_end) -
            new Date()
        ) / 86400000
    ); 

    document.getElementById(
        "restaurantContent"
    ).innerHTML = `

<div class="space-y-8">

<div class="rounded-2xl bg-white p-8 shadow">

<h2 class="mb-6 text-2xl font-bold">

User Information

</h2>

<div class="grid gap-6 md:grid-cols-2">

<div>

<p class="text-sm text-slate-500">

Restaurant

</p>

<p class="font-semibold">

${restaurant.name}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Owner

</p>

<p class="font-semibold">

${restaurant.owner_name}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Mobile

</p>

<p>

${restaurant.mobile}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Email

</p>

<p>

${restaurant.email}

</p>

</div>

</div>

</div>

<div class="rounded-2xl bg-white p-8 shadow">

<h2 class="mb-6 text-2xl font-bold">

Subscription

</h2>

<div class="grid gap-6 md:grid-cols-2">

<div>

<p class="text-sm text-slate-500">

Current Plan

</p>

<select
    id="planSelect"
    class="mt-2 w-full rounded-lg border border-slate-300 p-3">

${plans.map(plan => `

<option

value="${plan.id}"

${plan.id == restaurant.plan_id ? "selected" : ""}>

${plan.display_name}

</option>

`).join("")}

</select>

</div>

<div>

<p class="text-sm text-slate-500">

Status

</p>

<select
    id="statusSelect"
    onchange="updateSubscriptionForm()"
    class="mt-2 w-full rounded-lg border border-slate-300 p-3">

<option
value="trial"
${restaurant.subscription_status==="trial"?"selected":""}>

Trial

</option>

<option
value="active"
${restaurant.subscription_status==="active"?"selected":""}>

Active

</option>

<option
value="expired"
${restaurant.subscription_status==="expired"?"selected":""}>

Expired

</option>

<option
value="suspended"
${restaurant.subscription_status==="suspended"?"selected":""}>

Suspended

</option>

</select>

</div>

<div id="planStartSection">

<p class="text-sm text-slate-500">

Plan Start

</p>

<p>

${restaurant.plan_start}

</p>

</div>

<div id="planEndSection">

<p class="text-sm text-slate-500">

Plan End

</p>

<p>

${restaurant.plan_end}

</p>

</div>

<div id="daysSection">

<p class="text-sm text-slate-500">

Remaining Days

</p>

<input

id="daysInput"

type="number"

value="${remainingDays}"

min="1"

class="mt-2 w-full rounded-lg border border-slate-300 p-3">

</div>

</div>

<div class="mt-8">

<button

id="saveSubscriptionBtn"

onclick="saveSubscription()"

class="rounded-xl bg-slate-600 px-6 py-3 font-semibold text-white hover:bg-blue-700">

Save Subscription

</button>

</div>
</div>

</div>

`;
updateSubscriptionForm();

}

function updateSubscriptionForm() {
    

    const status =
        document.getElementById(
            "statusSelect"
        ).value;

    const hideValidity =
        status === "expired" ||
        status === "suspended";

    document
        .getElementById(
            "planStartSection"
        )
        .classList.toggle(
            "hidden",
            hideValidity
        );

    document
        .getElementById(
            "planEndSection"
        )
        .classList.toggle(
            "hidden",
            hideValidity
        );

    document
        .getElementById(
            "daysSection"
        )
        .classList.toggle(
            "hidden",
            hideValidity
        );
     

    const saveButton =
    document.getElementById(
        "saveSubscriptionBtn"
    );

saveButton.className =
    "rounded-xl px-6 py-3 font-semibold text-white";

switch (status) {

    case "expired":

        saveButton.textContent =
            "Expire Subscription";

        saveButton.classList.add(
            "bg-red-600",
            "hover:bg-red-700"
        );

        break;

    case "suspended":

        saveButton.textContent =
            "Suspend Restaurant";

        saveButton.classList.add(
            "bg-orange-500",
            "hover:bg-slate-600"
        );

        break;

    default:

        saveButton.textContent =
            "Save Subscription";

        saveButton.classList.add(
            "bg-slate-600",
            "hover:bg-blue-700"
        );

}    
}
async function loadPlans() {

    const response =
        await fetch(

            "/api/super-admin/plans",

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

    if (data.success) {

        plans =
            data.plans;

    }

}
async function submitSubscription() {

    const id =
        getRestaurantId();

    const response =
        await fetch(

            `/api/super-admin/restaurants/${id}/subscription`,

            {

                method: "PUT",

                headers: {

                    "Content-Type":
                        "application/json",

                    Authorization:
                        "Bearer " +
                        SuperAdminAuth.token()

                },

                body: JSON.stringify({

                    plan_id:
                        document.getElementById(
                            "planSelect"
                        ).value,

                    subscription_status:
                        document.getElementById(
                            "statusSelect"
                        ).value,

                    days:
                        Number(
                            document.getElementById(
                                "daysInput"
                            ).value
                        )

                })

            }

        );

    const data =
        await response.json();

    if (data.success) {

        Notify.success(
            data.message
        );

        Modal.close();

        loadRestaurant();

    } else {

        Notify.error(
            data.message
        );

    }

}

async function saveSubscription() {

    const status =
        document.getElementById(
            "statusSelect"
        ).value;

    if (
        status === "expired"
    ) {

        Modal.confirm(

            "Expire Subscription",

            `
<p class="text-slate-600">
This will immediately expire the restaurant subscription.
The owner will still be able to log in and renew the subscription.
</p>
            `,

            async () => {

                await submitSubscription();

            },

            {

                buttonText:
                    "Expire",

                buttonClass:
                    "bg-red-600",

                loadingText:
                    "Expiring..."

            }

        );

        return;

    }

    if (
        status === "suspended"
    ) {

        Modal.confirm(

            "Suspend Restaurant",

            `
<p class="text-slate-600">
This will suspend the restaurant immediately.
The restaurant will not be able to use Align until it is activated again.
</p>
            `,

            async () => {

                await submitSubscription();

            },

            {

                buttonText:
                    "Suspend",

                buttonClass:
                    "bg-orange-500",

                loadingText:
                    "Suspending..."

            }

        );

        return;

    }

    await submitSubscription();

}