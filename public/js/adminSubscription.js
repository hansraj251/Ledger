let currentSubscription =
    null;
async function loadSubscription() {

    const data =
        await API.get(
            "/api/subscription"
        );

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

currentSubscription =
    data.subscription;

renderSubscription(
    data.subscription
);

}

function renderSubscription(subscription) {

    const remainingDays =
    getRemainingDays(
        subscription.plan_end
    );
    const status =
    (
        subscription.subscription_status ||
        ""
    ).toLowerCase();

    let remainingText = "";

switch (
    status
) {

    case "expired":

        remainingText =
            "Expired";

        break;

    case "suspended":

        remainingText =
            "Suspended";

        break;

    default:

        if (remainingDays > 1) {

            remainingText =
                remainingDays +
                " Days";

        } else if (
            remainingDays === 1
        ) {

            remainingText =
                "1 Day";

        } else if (
            remainingDays === 0
        ) {

            remainingText =
                "Today";

        } else {

            remainingText =
                "Expired";

        }

}

    let badgeClass =
    "bg-green-100 text-green-700";

if (
    status ===
    "trial"
) {

    badgeClass =
        "bg-yellow-100 text-yellow-700";

}

if (
    status ===
    "expired"
) {

    badgeClass =
        "bg-red-100 text-red-700";

}
if (
    status ===
    "suspended"
) {

    badgeClass =
        "bg-orange-100 text-orange-700";
        

}

    
    const card =
        document.getElementById(
            "subscriptionCard"
        );

let buttonText =
    "Manage Subscription";

if (

    status === "expired" ||

    subscription.is_highest_plan

) {

    buttonText =
        "Renew Subscription";

}

if (
    status === "suspended"
) {

    buttonText =
        "Contact Administrator";

}

let showUpgradeButton = true;

if (
    status === "suspended"
) {

    showUpgradeButton = false;

}
  

    card.innerHTML = `

<div class="grid gap-6 md:grid-cols-2">

<div>

<p class="text-sm text-slate-500">

Current Plan

</p>

<p class="mt-2 text-2xl font-bold">

${subscription.display_name}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Status

</p>

<p class="mt-2">

<span
class="rounded-full px-3 py-1 ${badgeClass}">

${
    status.charAt(0).toUpperCase() +
    status.slice(1)
}

</span>

</p>

</div>

${
    status === "suspended"
        ? `

<div class="md:col-span-2 rounded-xl bg-orange-50 p-4 text-orange-700">

This Account has been suspended.

Please contact Align Support for assistance.

</div>

`
        : ""
}

${
    status === "trial" ||
    status === "active"
        ? `

<div>

<p class="text-sm text-slate-500">

Plan Start

</p>

<p class="mt-2">

${formatDate(subscription.plan_start)}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Plan End

</p>

<p class="mt-2">

${formatDate(subscription.plan_end)}

</p>

</div>

<div>

<p class="text-sm text-slate-500">

Days Remaining

</p>

<p class="mt-2 text-xl font-bold">

${remainingText}

</p>

</div>

</div>

`
        : ""
}

</div>

${
    showUpgradeButton
        ? `

<div class="mt-10 flex flex-wrap gap-3">

<button

onclick="upgradePlan()"

class="rounded-xl bg-slate-600 px-6 py-3 text-white hover:bg-blue-700">

${buttonText}

</button>

<a

href="/downloads/AlignOS.apk"

download

class="rounded-xl bg-emerald-600 px-6 py-3 font-medium text-white hover:bg-emerald-700">

Download Align Android App

</a>

<a

href="/downloads/AlignPOSSetup.zip"

download

class="rounded-xl bg-emerald-600 px-6 py-3 font-medium text-white hover:bg-emerald-700">

Download Align POS for Windows

</a>

</div>


`
        : ""
}

`;

}

function upgradePlan() {

    SubscriptionPayment.open(
        currentSubscription
    );

}

function getRemainingDays(
    endDate
) {

    const today =
        new Date();

    const end =
        new Date(endDate);

    today.setHours(
        0,
        0,
        0,
        0
    );

    end.setHours(
        0,
        0,
        0,
        0
    );

    const diff =
        Math.ceil(
            (
                end - today
            ) / 86400000
        );

    return diff;

}

function formatDate(date) {

    return new Date(date)
        .toLocaleDateString(

            "en-IN",

            {

                day: "2-digit",

                month: "short",

                year: "numeric"

            }

        );

}
loadSubscription();

let subscriptionRefreshTimer = null;

function startSubscriptionAutoRefresh() {

    if (subscriptionRefreshTimer) {

        clearInterval(
            subscriptionRefreshTimer
        );

    }

    subscriptionRefreshTimer =
        setInterval(
            async () => {

                await loadSubscription();

            },
            10000
        );

}
async function downloadPOS() {

    const token =
        localStorage.getItem(
            "token"
        );

    const response =
        await fetch(
            "/api/pos/download",
            {
                headers: {
                    Authorization:
                        `Bearer ${token}`
                }
            }
        );

    if (!response.ok) {

        const error =
            await response.json();

        Notify.error(
            error.message
        );

        return;

    }

    const blob =
        await response.blob();

    const url =
        window.URL.createObjectURL(
            blob
        );

    const a =
        document.createElement(
            "a"
        );

    a.href =
        url;

    a.download =
        "AlignPOS.zip";

    document.body.appendChild(
        a
    );

    a.click();

    a.remove();

    window.URL.revokeObjectURL(
        url
    );

}
loadSubscription();

startSubscriptionAutoRefresh();

function logout() {

    localStorage.clear();

    window.location.href =
        "/login.html";

}

document
    .getElementById(
        "logoutBtnMobile"
    )
    ?.addEventListener(
        "click",
        logout
    );

document
    .getElementById(
        "logoutBtnDesktop"
    )
    ?.addEventListener(
        "click",
        logout
    );