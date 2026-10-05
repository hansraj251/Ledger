Auth.requireSchoolOwner();

const attendanceUserForm =
    document.getElementById(
        "attendanceUserForm"
    );

const attendanceUserFormElement =
    document.getElementById(
        "attendanceUserFormElement"
    );

const addAttendanceUserBtn =
    document.getElementById(
        "addAttendanceUserBtn"
    );

const closeAttendanceUserFormBtn =
    document.getElementById(
        "closeAttendanceUserFormBtn"
    );

const cancelAttendanceUserBtn =
    document.getElementById(
        "cancelAttendanceUserBtn"
    );

const attendanceUserFormResult =
    document.getElementById(
        "attendanceUserFormResult"
    );

const attendanceUsersBody =
    document.getElementById(
        "attendanceUsersBody"
    );


function openAttendanceUserForm() {

    attendanceUserForm.classList.remove(
        "hidden"
    );

    attendanceUserFormElement.reset();

    attendanceUserFormResult.textContent =
        "";

    attendanceUserFormResult.className =
        "mt-4 text-sm";

}


function closeAttendanceUserForm() {

    attendanceUserForm.classList.add(
        "hidden"
    );

}


async function saveAttendanceUser(
    event
) {

    event.preventDefault();

    attendanceUserFormResult.textContent =
        "";

    attendanceUserFormResult.className =
        "mt-4 text-sm";

    const teacherId =
    document.getElementById(
        "attendanceTeacher"
    ).value;

const classSelect =
    document.getElementById(
        "attendanceClasses"
    );

const classIds =
    Array.from(
        classSelect.selectedOptions
    ).map(
        option =>
            Number(
                option.value
            )
    );

    const username =
        document.getElementById(
            "attendanceUsername"
        ).value.trim();

    const password =
        document.getElementById(
            "attendanceUserPassword"
        ).value;

    if (
    !teacherId
) {

    attendanceUserFormResult.textContent =
        "Staff is required.";

    attendanceUserFormResult.className =
        "mt-4 text-sm text-red-600";

    return;

}


if (
    !classIds.length
) {

    attendanceUserFormResult.textContent =
        "Select at least one class.";

    attendanceUserFormResult.className =
        "mt-4 text-sm text-red-600";

    return;

}

    if (
        !username
    ) {

        attendanceUserFormResult.textContent =
            "User ID is required.";

        attendanceUserFormResult.className =
            "mt-4 text-sm text-red-600";

        return;

    }

    if (
        password.length < 8
    ) {

        attendanceUserFormResult.textContent =
            "Password must be at least 8 characters.";

        attendanceUserFormResult.className =
            "mt-4 text-sm text-red-600";

        return;

    }

    try {

        const response =
    await API.post(
        "/api/users/attendance",
        {
            teacherId:
                Number(
                    teacherId
                ),

            classIds,

            username,

            password
        }
    );

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to create attendance user."
            );

        }

        attendanceUserFormResult.textContent =
            "Attendance user created successfully.";

        attendanceUserFormResult.className =
            "mt-4 text-sm text-emerald-600";

        attendanceUserFormElement.reset();

        await loadAttendanceUsers();

    }
    catch (err) {

        console.error(err);

        attendanceUserFormResult.textContent =
            err.message ||
            "Unable to create attendance user.";

        attendanceUserFormResult.className =
            "mt-4 text-sm text-red-600";

    }

}


async function loadAttendanceUsers() {

    attendanceUsersBody.innerHTML = `

        <tr>

            <td
                colspan="3"
                class="px-5 py-8 text-center text-sm text-slate-500">

                Loading attendance users...

            </td>

        </tr>

    `;

    try {

        const response =
            await API.get(
                "/api/users/attendance"
            );

        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to load attendance users."
            );

        }

        const users =
            response.users || [];

        if (
            !users.length
        ) {

            attendanceUsersBody.innerHTML = `

                <tr>

                    <td
                        colspan="3"
                        class="px-5 py-8 text-center text-sm text-slate-500">

                        No attendance users found.

                    </td>

                </tr>

            `;

            return;

        }

        attendanceUsersBody.innerHTML =
            users.map(
                user => `

                    <tr
                        class="border-t border-slate-100">

                        <td
                            class="px-5 py-4 text-sm font-medium text-slate-800">

                            ${user.name || "-"}

                        </td>

                        <td
                            class="px-5 py-4 text-sm text-slate-600">

                            ${user.username || "-"}

                        </td>

                        <td
                            class="px-5 py-4">

                            <span
                                class="${
                                    user.status === "active"
                                        ? "bg-emerald-50 text-emerald-700"
                                        : "bg-slate-100 text-slate-600"
                                } rounded-full px-2.5 py-1 text-xs font-medium">

                                ${
                                    user.status === "active"
                                        ? "Active"
                                        : "Inactive"
                                }

                            </span>

                        </td>

                    </tr>

                `
            ).join("");

    }
    catch (err) {

        console.error(err);

        attendanceUsersBody.innerHTML = `

            <tr>

                <td
                    colspan="3"
                    class="px-5 py-8 text-center text-sm text-red-600">

                    ${
                        err.message ||
                        "Unable to load attendance users."
                    }

                </td>

            </tr>

        `;

    }

}


addAttendanceUserBtn.addEventListener(
    "click",
    openAttendanceUserForm
);


closeAttendanceUserFormBtn.addEventListener(
    "click",
    closeAttendanceUserForm
);


cancelAttendanceUserBtn.addEventListener(
    "click",
    closeAttendanceUserForm
);


attendanceUserFormElement.addEventListener(
    "submit",
    saveAttendanceUser
);
async function loadAttendanceTeachers() {

    const response =
        await API.get(
            "/api/teachers"
        );

    if (
        !response.success
    ) {

        throw new Error(
            response.message ||
            "Unable to load staff."
        );

    }

    const teachers =
        response.teachers || [];

    const activeTeachers =
        teachers.filter(
            teacher =>
                teacher.status ===
                "active"
        );

    const teacherSelect =
        document.getElementById(
            "attendanceTeacher"
        );

    teacherSelect.innerHTML = `

        <option value="">
            Select staff
        </option>

    `;

    activeTeachers.forEach(
        teacher => {

            const option =
                document.createElement(
                    "option"
                );

            option.value =
                teacher.id;

            option.textContent =
                teacher.name;

            teacherSelect.appendChild(
                option
            );

        }
    );

}


async function loadAttendanceClasses() {

    const response =
        await API.get(
            "/api/classes"
        );

    if (
        !response.success
    ) {

        throw new Error(
            response.message ||
            "Unable to load classes."
        );

    }

    const classes =
        response.classes || [];

    const activeClasses =
        classes.filter(
            classItem =>
                classItem.status ===
                "active"
        );

    const classSelect =
        document.getElementById(
            "attendanceClasses"
        );

    classSelect.innerHTML = "";

    activeClasses.forEach(
        classItem => {

            const option =
                document.createElement(
                    "option"
                );

            option.value =
                classItem.id;

            option.textContent =
                `${classItem.name} - ${classItem.section}`;

            classSelect.appendChild(
                option
            );

        }
    );

}

async function initializePage() {

    try {

        await loadAttendanceTeachers();

        await loadAttendanceClasses();

        await loadAttendanceUsers();

    }
    catch (err) {

        console.error(err);

    }

}

initializePage();