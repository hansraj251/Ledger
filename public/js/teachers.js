Auth.requireSchoolOwner();

const teachersTableBody =
    document.getElementById(
        "teachersTableBody"
    );

const teacherForm =
    document.getElementById(
        "teacherForm"
    );

const teacherFormElement =
    document.getElementById(
        "teacherFormElement"
    );

const addTeacherBtn =
    document.getElementById(
        "addTeacherBtn"
    );

const closeFormBtn =
    document.getElementById(
        "closeFormBtn"
    );

const cancelTeacherBtn =
    document.getElementById(
        "cancelTeacherBtn"
    );

const formResult =
    document.getElementById(
        "formResult"
    );

const addDesignationBtn =
    document.getElementById(
        "addDesignationBtn"
    );

const designationForm =
    document.getElementById(
        "designationForm"
    );

const closeDesignationBtn =
    document.getElementById(
        "closeDesignationBtn"
    );

const cancelDesignationBtn =
    document.getElementById(
        "cancelDesignationBtn"
    );

const designationFormElement =
    document.getElementById(
        "designationFormElement"
    );

const designationResult =
    document.getElementById(
        "designationResult"
    );
const designationSelect =
    document.getElementById(
        "designation"
    );

addTeacherBtn.addEventListener(
    "click",
    () => {

        formResult.textContent = "";

        teacherForm.classList.remove(
            "hidden"
        );

    }
);
addDesignationBtn.addEventListener(
    "click",
    () => {

        designationResult.textContent =
            "";

        designationForm.classList.remove(
            "hidden"
        );

        document.getElementById(
            "designationName"
        ).focus();

    }
);
closeDesignationBtn.addEventListener(
    "click",
    closeDesignationForm
);

cancelDesignationBtn.addEventListener(
    "click",
    closeDesignationForm
);
designationFormElement.addEventListener(
    "submit",
    createDesignation
);

function closeDesignationForm() {

    designationForm.classList.add(
        "hidden"
    );

    designationFormElement.reset();

    designationResult.textContent =
        "";

}

closeFormBtn.addEventListener(
    "click",
    closeTeacherForm
);

cancelTeacherBtn.addEventListener(
    "click",
    closeTeacherForm
);

teacherFormElement.addEventListener(
    "submit",
    createTeacher
);
async function createDesignation(
    event
) {

    event.preventDefault();

    designationResult.textContent =
        "Saving designation...";

    designationResult.className =
        "mt-4 text-sm font-medium text-slate-500";

    const name =
        document
            .getElementById(
                "designationName"
            )
            .value
            .trim();

    try {

        const data =
            await API.post(
                "/api/designations",
                {
                    name
                }
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to create designation"
            );

        }

        designationResult.textContent =
            "Designation created successfully.";

        designationResult.className =
            "mt-4 text-sm font-medium text-emerald-600";

        designationFormElement.reset();

        await loadDesignations();

        setTimeout(
            closeDesignationForm,
            500
        );

    }
    catch (err) {

        console.error(err);

        designationResult.textContent =
            err.message ||
            "Unable to create designation.";

        designationResult.className =
            "mt-4 text-sm font-medium text-red-600";

    }

}

loadTeachers();
loadDesignations();

async function loadTeachers() {

    try {

        const data =
            await API.get(
                "/api/teachers"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load teachers"
            );

        }

        renderTeachers(
            data.teachers
        );

    }
    catch (err) {

        console.error(err);

        teachersTableBody.innerHTML = `
            <tr>
                <td
                    colspan="8"
                    class="px-4 py-10 text-center text-red-600 sm:px-6">
                    Unable to load teachers.
                </td>
            </tr>
        `;

    }

}

function renderTeachers(
    teachers
) {

    if (
        !teachers ||
        teachers.length === 0
    ) {

        teachersTableBody.innerHTML = `
            <tr>
                <td
                    colspan="8"
                    class="px-4 py-10 text-center text-slate-500 sm:px-6">
                    No teachers found.
                </td>
            </tr>
        `;

        return;

    }

    teachersTableBody.innerHTML =
        teachers
            .map(
                (
                    teacher
                ) => `
                    <tr class="border-slate-200 align-middle transition-colors hover:bg-slate-200">

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                teacher.employee_id ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 font-medium text-slate-800 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                teacher.name
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
    ${escapeHtml(
        teacher.designation_name ||
        "-"
    )}
</td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                teacher.qualification ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                teacher.mobile ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                teacher.joining_date ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
    <span
        class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">
        ${escapeHtml(
            teacher.status
        )}
    </span>
</td>

<td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">

    <div class="flex flex-wrap gap-2">

        <button
            type="button"
            onclick="editTeacher(${teacher.id})"
            class="shrink-0 rounded-lg bg-blue-600 px-3 py-2 text-sm font-medium text-white transition hover:bg-blue-700">

            Edit

        </button>

        <button
            type="button"
            onclick="toggleTeacherStatus(${teacher.id}, '${teacher.status}')"
            class="shrink-0 rounded-lg px-3 py-2 text-sm font-medium text-white transition ${
                teacher.status === "active"
                    ? "bg-red-600 hover:bg-red-700"
                    : "bg-emerald-600 hover:bg-emerald-700"
            }">

            ${
                teacher.status === "active"
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

async function createTeacher(
    event
) {

    event.preventDefault();

    formResult.textContent =
        "Saving teacher...";

    formResult.className =
        "mt-4 text-sm font-medium text-slate-500";

    const teacher = {

        employeeId:
            document
                .getElementById(
                    "employeeId"
                )
                .value
                .trim(),

        name:
            document
                .getElementById(
                    "teacherName"
                )
                .value
                .trim(),
        designationId:
    document
        .getElementById(
            "designation"
        )
        .value ||
        null,

        fatherName:
            document
                .getElementById(
                    "fatherName"
                )
                .value
                .trim(),

        motherName:
            document
                .getElementById(
                    "motherName"
                )
                .value
                .trim(),

        dob:
            document
                .getElementById(
                    "dob"
                )
                .value,

        gender:
            document
                .getElementById(
                    "gender"
                )
                .value,

        mobile:
            document
                .getElementById(
                    "mobile"
                )
                .value
                .trim(),

        email:
            document
                .getElementById(
                    "email"
                )
                .value
                .trim(),

        address:
            document
                .getElementById(
                    "address"
                )
                .value
                .trim(),

        qualification:
            document
                .getElementById(
                    "qualification"
                )
                .value
                .trim(),

        subject:
            document
                .getElementById(
                    "subject"
                )
                .value
                .trim(),

        joiningDate:
            document
                .getElementById(
                    "joiningDate"
                )
                .value

    };

    try {

        const editId =
    teacherForm.dataset.editId;

let data;

if (
    editId
) {

    data =
        await API.put(
            `/api/teachers/${editId}`,
            teacher
        );

}
else {

    data =
        await API.post(
            "/api/teachers",
            teacher
        );

}

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to create teacher"
            );

        }

        formResult.textContent =
    editId
        ? "Teacher updated successfully."
        : "Teacher created successfully.";

        formResult.className =
            "mt-4 text-sm font-medium text-emerald-600";

        delete teacherForm.dataset.editId;

        teacherFormElement.reset();

        await loadTeachers();

        setTimeout(
            closeTeacherForm,
            500
        );

    }
    catch (err) {

        console.error(err);

        formResult.textContent =
            err.message ||
            "Unable to create teacher.";

        formResult.className =
            "mt-4 text-sm font-medium text-red-600";

    }

}

function closeTeacherForm() {

    teacherForm.classList.add(
        "hidden"
    );

    formResult.textContent = "";

    teacherFormElement.reset();

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
async function editTeacher(
    teacherId
) {

    try {

        const data =
            await API.get(
                "/api/teachers"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load teacher"
            );

        }

        const teacher =
            data.teachers.find(
                item =>
                    Number(item.id) ===
                    Number(teacherId)
            );

        if (
            !teacher
        ) {

            throw new Error(
                "Teacher not found"
            );

        }

        document.getElementById(
            "employeeId"
        ).value =
            teacher.employee_id ||
            "";

        document.getElementById(
            "teacherName"
        ).value =
            teacher.name ||
            "";
        document.getElementById(
    "designation"
).value =
    teacher.designation_id ||
    "";

        document.getElementById(
            "fatherName"
        ).value =
            teacher.father_name ||
            "";

        document.getElementById(
            "motherName"
        ).value =
            teacher.mother_name ||
            "";

        document.getElementById(
            "dob"
        ).value =
            teacher.dob ||
            "";

        document.getElementById(
            "gender"
        ).value =
            teacher.gender ||
            "";

        document.getElementById(
            "mobile"
        ).value =
            teacher.mobile ||
            "";

        document.getElementById(
            "email"
        ).value =
            teacher.email ||
            "";

        document.getElementById(
            "address"
        ).value =
            teacher.address ||
            "";

        document.getElementById(
            "qualification"
        ).value =
            teacher.qualification ||
            "";

        document.getElementById(
            "subject"
        ).value =
            teacher.subject ||
            "";

        document.getElementById(
            "joiningDate"
        ).value =
            teacher.joining_date ||
            "";

        teacherForm.dataset.editId =
            teacher.id;

        teacherForm.classList.remove(
            "hidden"
        );

        formResult.textContent =
            "Editing teacher.";

        formResult.className =
            "mt-4 text-sm font-medium text-blue-600";

    }
    catch (err) {

        console.error(err);

        formResult.textContent =
            err.message ||
            "Unable to load teacher.";

        formResult.className =
            "mt-4 text-sm font-medium text-red-600";

    }

}
async function toggleTeacherStatus(
    teacherId,
    currentStatus
) {

    const newStatus =
        currentStatus === "active"
            ? "inactive"
            : "active";

    const action =
        newStatus === "active"
            ? "activate"
            : "deactivate";

    Modal.open(

        `${action === "activate" ? "Activate" : "Deactivate"} Teacher`,

        `
        <p class="text-slate-600">
            Are you sure you want to ${action} this teacher?
        </p>
        `,

        async () => {

            const data =
                await API.patch(
                    `/api/teachers/${teacherId}/status`,
                    {
                        status:
                            newStatus
                    }
                );

            if (
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to update teacher status"
                );

            }

            Modal.close();

            await loadTeachers();

        },

        {

            buttonText:
                action === "activate"
                    ? "Activate"
                    : "Deactivate",

            buttonClass:
                action === "activate"
                    ? "bg-emerald-600 hover:bg-emerald-700"
                    : "bg-red-600 hover:bg-red-700",

            loadingText:
                action === "activate"
                    ? "Activating..."
                    : "Deactivating..."

        }

    );

}

async function loadDesignations() {

    try {

        const data =
            await API.get(
                "/api/designations"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load designations"
            );

        }

        designationSelect.innerHTML = `
            <option value="">
                Select Designation
            </option>
        `;

        data.designations
            .filter(
                designation =>
                    designation.status ===
                    "active"
            )
            .forEach(
                designation => {

                    const option =
                        document.createElement(
                            "option"
                        );

                    option.value =
                        designation.id;

                    option.textContent =
                        designation.name;

                    designationSelect.appendChild(
                        option
                    );

                }
            );

    }
    catch (err) {

        console.error(err);

        designationSelect.innerHTML = `
            <option value="">
                Unable to load designations
            </option>
        `;

    }

}