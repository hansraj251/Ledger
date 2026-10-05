const SchoolSubscriptionPayment = {

async open(subscription) {

    const response =
    await API.get(
        "/api/subscription/school-plans"
    );

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    const currentPlan =
        response.plans.find(

            plan =>

                plan.id ===
                subscription.plan_id

        );

    if (!currentPlan) {

        Notify.error(
            "Current plan not found."
        );

        return;

    }

const renewPlans = [

    currentPlan

];

const upgradePlans =

    response.plans.filter(

        plan =>

            plan.sort_order >

            currentPlan.sort_order

    );

let modalTitle =
    "Manage Subscription";

if (

    upgradePlans.length === 0

) {

    modalTitle =
        "Renew Subscription";

}

this.showPlanModal(

    modalTitle,

    renewPlans,

    upgradePlans

);

},

 showPlanModal(
    modalTitle,
    renewPlans,
    upgradePlans = []
) {

    let html = "";

    const renderPlans = (
        title,
        plans
    ) => {

        if (
            plans.length === 0
        ) {

            return;

        }

        for (const plan of plans) {
            html += `

<h2 class="mb-4 text-lg font-semibold">

${
    title === "Renew"

        ? `Renew ${plan.display_name}`

        : `Upgrade to ${plan.display_name}`
}

</h2>

`;

            html += `

<div class="mb-5 rounded-xl border p-4">

<h3 class="text-lg font-semibold">

${plan.display_name}

</h3>

<p class="mb-3 text-sm text-slate-500">

${plan.description || ""}

</p>

`;

            for (const pricing of plan.pricing) {

                html += `

<label class="mb-2 flex cursor-pointer items-center rounded-lg border p-3 hover:bg-slate-200">

<input
type="radio"
name="pricing"
value="${pricing.id}"
class="mr-3">

<div class="flex-1">

<div class="font-medium">

${pricing.duration_days} Days

</div>

<div class="text-sm text-slate-500">
    ₹${Number(pricing.price).toFixed(2)}
</div>

</div>

</label>

`;

            }

            html += `

</div>

`;

        }

    };

    renderPlans(

    "Renew",

    renewPlans

);

    if (

        renewPlans.length &&
        upgradePlans.length

    ) {

        html += `

<hr class="my-6">

`;

    }

    renderPlans(

    "Upgrade",

    upgradePlans

);

    Modal.open(

    modalTitle,

    html,

        async () => {

            const selectedPricing =
                document.querySelector(
                    'input[name="pricing"]:checked'
                );

            if (!selectedPricing) {

                Notify.error(
                    "Please select a plan."
                );

                return;

            }

            await this.createOrder(

                Number(
                    selectedPricing.value
                )

            );

        },

        {

            buttonText:
                "Continue",

            loadingText:
                "Creating Order..."

        }

    );

},

async createOrder(pricingId) {

    const response =
    await API.post(

        "/api/subscription/school/create-order",

        {
            pricingId
        }

    );

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    Modal.close();

    this.openCheckout(
    response.key,
    response.order
);

},

   openCheckout(
    key,
    order
) {

    const razorpay =
        new Razorpay({

            key,

            amount:
                order.amount,

            currency:
                order.currency,

            name:
                "Align",

            description:
                "Subscription Payment",

            order_id:
                order.id,

            handler:
    async response => {

        try {

            await this.verifyPayment(
                response
            );

        } catch (err) {

            console.error(err);

            Notify.error(
                "Payment verification failed."
            );

        }

    },

            theme: {

                color:
                    "#2563eb"

            }

        })
        ;


razorpay.on(

    "payment.failed",

    response => {

        Notify.error(
            response.error.description
                ||
            "Payment failed."
        );

    }

);

 razorpay.open();

},

    async verifyPayment(payment) {

    const response =
    await API.post(

        "/api/subscription/school/verify-payment",

        {
            razorpay_order_id:
                payment.razorpay_order_id,

            razorpay_payment_id:
                payment.razorpay_payment_id,

            razorpay_signature:
                payment.razorpay_signature
        }

    );

    if (!response.success) {

        Notify.error(
            response.message
        );

        return;

    }

    Notify.success(
        "Subscription upgraded successfully."
    );

    await loadSubscription();

}

};
