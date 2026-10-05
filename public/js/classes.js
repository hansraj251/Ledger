Auth.requireSchoolOwner();

const classForm =
    document.getElementById(
        "classForm"
    );

const classFormElement =
    document.getElementById(
        "classFormElement"
    );

const formResult =
    document.getElementById(
        "formResult"
    );

let classes = [];
let editingClassId =
    null;

async function loadClasses() {

    try {

        const data =
            await API.get(
                "/api/classes"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message
            );

        }
        classes =
    data.classes;

        renderClasses(
            data.classes
        );

    }
    catch (err) {

        console.error(err);

        document.getElementById(
            "classesTableBody"
        ).innerHTML = `
            <tr>
                <td
                    colspan="5"
                    class="px-4 py-10 text-center text-red-500 sm:px-6">

                    Unable to load classes.

                </td>
            </tr>
        `;

    }

}

function renderClasses(
    classes
) {

    const tbody =
        document.getElementById(
            "classesTableBody"
        );

    if (
        !classes.length
    ) {

        tbody.innerHTML = `
            <tr>
                <td
                    colspan="4"
                    class="px-4 py-10 text-center text-slate-500 sm:px-6">

                    No classes found.

                </td>
            </tr>
        `;

        return;

    }

    tbody.innerHTML =
        classes.map(
            (
                classItem,
                index
            ) => `
                <tr
    class="border-b last:border-b-0 align-middle transition-colors hover:bg-slate-200">

                    <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
    ${index + 1}
</td>

                    <td
    class="whitespace-nowrap px-3 py-3 font-medium text-slate-800 sm:px-6 sm:py-4">

    ${escapeHtml(
        classItem.name
    )}

</td>

                   <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">

    ${escapeHtml(
        classItem.section
    )}

</td>

                    <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">

    <span
       class="whitespace-nowrap rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">

        ${escapeHtml(
            classItem.status
        )}

    </span>

</td>
                  <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">

    <div class="flex flex-wrap gap-2">

        <button
            type="button"
            onclick="editClass(${classItem.id})"
            class="whitespace-nowrap rounded-lg bg-blue-100 px-3 py-2 text-sm font-medium text-blue-700 hover:bg-blue-200">

            Edit

        </button>

        <button
            type="button"
            onclick="toggleClassStatus(${classItem.id}, '${classItem.status}')"
            class="whitespace-nowrap rounded-lg px-3 py-2 text-sm font-medium ${
                classItem.status === "active"
                    ? "bg-red-100 text-red-700 hover:bg-red-200"
                    : "bg-emerald-600 text-white hover:bg-emerald-700"
            }">

            ${
                classItem.status === "active"
                    ? "Deactivate"
                    : "Activate"
            }

        </button>

    </div>

</td>

                </tr>
            `
        )
        .join("");

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

function editClass(
    classId
) {

    const classItem =
        classes.find(
            item =>
                Number(item.id) ===
                Number(classId)
        );

    if (
        !classItem
    ) {

        return;

    }
    editingClassId =
    classItem.id;

    document.getElementById(
        "classFormTitle"
    ).textContent =
        "Edit Class";

    document.getElementById(
        "className"
    ).value =
        classItem.name;

    document.getElementById(
        "section"
    ).value =
        classItem.section;

    classForm.classList.remove(
        "hidden"
    );

    document.getElementById(
        "className"
    ).focus();

}
async function toggleClassStatus(
    classId,
    currentStatus
) {

    const newStatus =
        currentStatus === "active"
            ? "inactive"
            : "active";

    try {

        const response =
            await API.patch(
                `/api/classes/${classId}/status`,
                {
                    status: newStatus
                }
            );

        if (
            !response.success
        ) {

            throw new Error(
                response.message
            );

        }

        await loadClasses();

    }
    catch (err) {

        console.error(err);

        alert(
            err.message ||
            "Unable to update class status."
        );

    }

}

function openClassForm() {

    classForm.classList.remove(
        "hidden"
    );

    document.getElementById(
        "className"
    ).focus();

}

function closeClassForm() {

    classForm.classList.add(
        "hidden"
    );

    classFormElement.reset();

    formResult.textContent =
        "";

    editingClassId =
    null;

document.getElementById(
    "classFormTitle"
).textContent =
    "Add Class";

}


classFormElement.addEventListener(
    "submit",
    async (
        event
    ) => {

        event.preventDefault();

        formResult.textContent =
            "";

        const data = {

            name:
                document.getElementById(
                    "className"
                ).value.trim(),

            section:
                document.getElementById(
                    "section"
                ).value.trim()

        };

        try {

            let response;

if (
    editingClassId
) {

    response =
        await API.put(
            `/api/classes/${editingClassId}`,
            data
        );

}
else {

    response =
        await API.post(
            "/api/classes",
            data
        );

}

            if (
                !response.success
            ) {

                throw new Error(
                    response.message
                );

            }

            formResult.textContent =
    editingClassId
        ? "Class updated successfully."
        : "Class created successfully.";

            formResult.className =
                "mt-4 text-sm font-medium text-emerald-600";

            classFormElement.reset();

            await loadClasses();

            setTimeout(
                closeClassForm,
                500
            );

        }
        catch (err) {

            console.error(err);

            formResult.textContent =
                err.message ||
                "Unable to create class.";

            formResult.className =
                "mt-4 text-sm font-medium text-red-600";

        }

    }
);

document.getElementById(
    "addClassBtn"
).addEventListener(
    "click",
    openClassForm
);

document.getElementById(
    "closeFormBtn"
).addEventListener(
    "click",
    closeClassForm
);

document.getElementById(
    "cancelFormBtn"
).addEventListener(
    "click",
    closeClassForm
);



loadClasses();
