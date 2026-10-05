let currentPlans = [];


async function loadPlans() {

    const token =
        SuperAdminAuth.token();

    const response =
        await fetch(

            "/api/super-admin/school-plans",

            {
                headers: {
                    Authorization:
                        `Bearer ${token}`
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
    currentPlans =
    data.plans.filter(
        plan =>
            plan.plan_type ===
            "school"
    );

    renderPlans(
    currentPlans
);

}
function renderPlans(plans) {

    const tbody =
        document.getElementById(
            "plansTable"
        );

    tbody.innerHTML = "";

    plans.forEach(plan => {

        tbody.innerHTML += `

<tr>

<td class="p-4 font-semibold">

${plan.display_name}

</td>

<td class="p-4">

${plan.sort_order}

</td>

<td class="p-4">

${plan.description || "-"}

</td>

<td class="p-4">

${plan.active_students == -1
    ? "Unlimited"
    : plan.active_students}

</td>

<td class="p-4">

<span class="rounded-full px-3 py-1 text-sm
${plan.status === "active"
    ? "bg-green-100 text-green-700"
    : "bg-red-100 text-red-700"}">

${plan.status}

</span>

</td>

<td class="p-4">

<button

onclick="editPlan(${plan.id})"

class="rounded bg-slate-600 px-3 py-2 text-white hover:bg-blue-700">

Edit

</button>

</td>

</tr>

`;

    });

}
function editPlan(planId) {

    const plan =
        currentPlans.find(
            p => p.id === planId
        );

    if (!plan) {

        Notify.error(
            "Plan not found."
        );

        return;

    }

    openPlanModal(
        plan
    );

}
function openPlanModal(
    plan = null
) {

    const isEdit =
        plan !== null;

    Modal.open(

        isEdit
            ? "Edit Plan"
            : "Add Plan",

        `

<div class="space-y-4">

<div>

<label class="mb-1 block text-sm font-medium">

Slug

</label>

<input

id="planSlug"

type="text"

class="w-full rounded-lg border p-3"

value="${plan?.slug ?? ""}"

${isEdit ? "readonly" : ""}>

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Display Name

</label>

<input

id="planDisplayName"

type="text"

class="w-full rounded-lg border p-3"

value="${plan?.display_name ?? ""}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Description

</label>

<textarea

id="planDescription"

class="w-full rounded-lg border p-3"

rows="3">${plan?.description ?? ""}</textarea>

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Sort Order

</label>

<input

id="planSortOrder"

type="number"

min="0"

class="w-full rounded-lg border p-3"

value="${plan?.sort_order ?? 1}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Active Students

</label>

<input

id="planActiveStudents"

type="number"

class="w-full rounded-lg border p-3"

value="${plan?.active_students ?? 1}">

</div>

<div>

<label class="mb-1 block text-sm font-medium">

Status

</label>

<select

id="planStatus"

class="w-full rounded-lg border p-3">

<option
value="active"
${plan?.status === "inactive" ? "" : "selected"}>

Active

</option>

<option
value="inactive"
${plan?.status === "inactive" ? "selected" : ""}>

Inactive

</option>

</select>

</div>

</div>

`,

        async () => {

    const payload = {

        slug:
            document.getElementById(
                "planSlug"
            ).value.trim(),

        display_name:
            document.getElementById(
                "planDisplayName"
            ).value.trim(),

        description:
            document.getElementById(
                "planDescription"
            ).value.trim(),
        sort_order:

    Number(

        document.getElementById(

            "planSortOrder"

        ).value

    ),

        limit_value:
    Number(
        document.getElementById(
            "planActiveStudents"
        ).value
    ),

        status:
    document.getElementById(
        "planStatus"
    ).value,

plan_type:
    "school"

    };

    if (!payload.display_name) {

        Notify.error(
            "Display name is required."
        );

        return;

    }

    if (!isEdit && !payload.slug) {

        Notify.error(
            "Slug is required."
        );

        return;

    }

    let response;

    if (isEdit) {

        response =
            await API.put(

                `/api/super-admin/plans/${plan.id}`,

                {

                    display_name:
                        payload.display_name,

                    description:
                        payload.description,
                    sort_order:

                    payload.sort_order,

                    limit_value:
                    payload.limit_value,

                    status:
                        payload.status

                }

            );

    } else {

        response =
            await API.post(

                "/api/super-admin/plans",

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
            ? "Plan updated successfully."
            : "Plan created successfully."

    );

    await loadPlans();

},

        {

            buttonText:
                isEdit
                    ? "Update Plan"
                    : "Create Plan",

            loadingText:
                isEdit
                    ? "Updating..."
                    : "Creating..."

        }

    );

}


loadPlans();