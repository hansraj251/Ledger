Auth.requireSchoolOwner();

const studentsTableBody =
    document.getElementById(
        "studentsTableBody"
    );

const studentForm =
    document.getElementById(
        "studentForm"
    );

const studentFormElement =
    document.getElementById(
        "studentFormElement"
    );

const addStudentBtn =
    document.getElementById(
        "addStudentBtn"
    );

const closeFormBtn =
    document.getElementById(
        "closeFormBtn"
    );

const cancelStudentBtn =
    document.getElementById(
        "cancelStudentBtn"
    );

const formResult =
    document.getElementById(
        "formResult"
    );
let schoolClasses = [];
let students = [];
let editingStudentId =
    null;

addStudentBtn.addEventListener(
    "click",
    () => {

        formResult.textContent = "";

        studentForm.classList.remove(
            "hidden"
        );

    }
);

closeFormBtn.addEventListener(
    "click",
    closeStudentForm
);

cancelStudentBtn.addEventListener(
    "click",
    closeStudentForm
);

studentFormElement.addEventListener(
    "submit",
    createStudent
);
loadClasses();

loadStudents();

async function loadStudents() {

    try {

        const data =
            await API.get(
                "/api/students"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load students"
            );

        }
        students =
    data.students;

        renderStudents(
            data.students
        );

    }
    catch (err) {

        console.error(err);

        studentsTableBody.innerHTML = `
            <tr>
                <td
                    colspan="7"
                    class="px-4 py-10 text-center text-red-600 sm:px-6">
                    Unable to load students.
                </td>
            </tr>
        `;

    }

}

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
                data.message ||
                "Unable to load classes"
            );

        }

        schoolClasses =
            data.classes.filter(
                classItem =>
                    classItem.status ===
                    "active"
            );

        renderClassOptions();
        document.getElementById(
    "className"
).addEventListener(
    "change",
    renderSectionOptions
);

    }
    catch (err) {

        console.error(err);

    }

}
function renderClassOptions() {

    const classSelect =
        document.getElementById(
            "className"
        );

    classSelect.innerHTML = `
        <option value="">
            Select Class
        </option>
    `;

const uniqueClassNames =
    [
        ...new Set(
            schoolClasses.map(
                classItem =>
                    classItem.name
            )
        )
    ];

uniqueClassNames.forEach(
    className => {

        const option =
            document.createElement(
                "option"
            );

        option.value =
            className;

        option.textContent =
            className;

        classSelect.appendChild(
            option
        );

    }
);

}
function renderSectionOptions() {

    const classSelect =
        document.getElementById(
            "className"
        );

    const sectionSelect =
        document.getElementById(
            "section"
        );

    const selectedClass =
        classSelect.value;

    sectionSelect.innerHTML = `
        <option value="">
            Select Section
        </option>
    `;

    if (
        !selectedClass
    ) {

        return;

    }

    const sections =
        schoolClasses.filter(
            classItem =>
                classItem.name ===
                selectedClass
        );

    sections.forEach(
        classItem => {

            const option =
                document.createElement(
                    "option"
                );

            option.value =
                classItem.section;

            option.textContent =
                classItem.section;

            sectionSelect.appendChild(
                option
            );

        }
    );

}

function renderStudents(
    students
) {

    if (
        !students ||
        students.length === 0
    ) {

        studentsTableBody.innerHTML = `
            <tr>
                <td
                    colspan="7"
                    class="px-4 py-10 text-center text-slate-500 sm:px-6">
                    No students found.
                </td>
            </tr>
        `;

        return;

    }

    studentsTableBody.innerHTML =
        students
            .map(
                (
                    student
                ) => `
                    <tr class=" align-middle">

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                student.admission_no
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 font-medium text-slate-800 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                student.name
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                student.class_name ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                student.section ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            ${escapeHtml(
                                student.roll_no ||
                                "-"
                            )}
                        </td>

                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">
                            <span
                                class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">
                                ${escapeHtml(
                                    student.status
                                )}
                            </span>
                        </td>
                        <td class="whitespace-nowrap px-3 py-3 sm:px-6 sm:py-4">

    <button
    type="button"
    onclick="editStudent(${student.id})"
    class="whitespace-nowrap rounded-lg bg-blue-100 px-3 py-2 text-sm font-medium text-blue-700 hover:bg-blue-200">

    Edit

</button>

<button
    type="button"
    onclick="toggleStudentStatus(
        ${student.id},
        '${student.status}'
    )"
    class="${
        student.status === "active"
            ? "whitespace-nowrap rounded-lg bg-red-100 px-3 py-2 text-sm font-medium text-red-700 hover:bg-red-200"
            : "whitespace-nowrap rounded-lg bg-emerald-600 px-3 py-2 text-sm font-medium text-white hover:bg-emerald-700"
    }">

    ${
        student.status === "active"
            ? "Deactivate"
            : "Activate"
    }

</button>

</td>

                    </tr>
                `
            )
            .join("");

}
function editStudent(
    studentId
) {

    const student =
        students.find(
            item =>
                Number(item.id) ===
                Number(studentId)
        );

    if (
        !student
    ) {

        return;

    }
    editingStudentId =
    student.id;

    document.getElementById(
        "studentFormTitle"
    ).textContent =
        "Edit Student";

    document.getElementById(
        "admissionNo"
    ).value =
        student.admission_no || "";

    document.getElementById(
        "studentName"
    ).value =
        student.name || "";

    document.getElementById(
        "fatherName"
    ).value =
        student.father_name || "";

    document.getElementById(
        "motherName"
    ).value =
        student.mother_name || "";

    document.getElementById(
        "dob"
    ).value =
        student.dob || "";

    document.getElementById(
        "gender"
    ).value =
        student.gender || "";

    document.getElementById(
        "mobile"
    ).value =
        student.mobile || "";

    document.getElementById(
        "address"
    ).value =
        student.address || "";

    document.getElementById(
        "className"
    ).value =
        student.class_name || "";

    renderSectionOptions();

    document.getElementById(
        "section"
    ).value =
        student.section || "";

    document.getElementById(
        "rollNo"
    ).value =
        student.roll_no || "";

    studentForm.classList.remove(
        "hidden"
    );

}
async function createStudent(
    event
) {

    event.preventDefault();

    formResult.textContent =
        "Saving student...";

    formResult.className =
        "mt-4 text-sm font-medium text-slate-500";

    const student = {

        admissionNo:
            document
                .getElementById(
                    "admissionNo"
                )
                .value
                .trim(),

        name:
            document
                .getElementById(
                    "studentName"
                )
                .value
                .trim(),

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

        address:
            document
                .getElementById(
                    "address"
                )
                .value
                .trim(),

        className:
            document
                .getElementById(
                    "className"
                )
                .value
                .trim(),

        section:
            document
                .getElementById(
                    "section"
                )
                .value
                .trim(),

        rollNo:
            document
                .getElementById(
                    "rollNo"
                )
                .value
                .trim()

    };

    try {

        let data;

if (
    editingStudentId
) {

    data =
        await API.put(
            `/api/students/${editingStudentId}`,
            student
        );

}
else {

    data =
        await API.post(
            "/api/students",
            student
        );

}

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to create student"
            );

        }

        formResult.textContent =
    editingStudentId
        ? "Student updated successfully."
        : "Student created successfully.";

        formResult.className =
            "mt-4 text-sm font-medium text-emerald-600";

        studentFormElement.reset();

        await loadStudents();

        setTimeout(
            closeStudentForm,
            500
        );

    }
    catch (err) {

        console.error(err);

        formResult.textContent =
            err.message ||
            "Unable to create student.";

        formResult.className =
            "mt-4 text-sm font-medium text-red-600";

    }

}

function closeStudentForm() {

    studentForm.classList.add(
        "hidden"
    );

    formResult.textContent = "";

    studentFormElement.reset();
    editingStudentId =
    null;

document.getElementById(
    "studentFormTitle"
).textContent =
    "Add Student";

}
function toggleStudentStatus(
    studentId,
    currentStatus
) {

    const isActive =
        currentStatus === "active";

    const nextStatus =
        isActive
            ? "inactive"
            : "active";

    Modal.open(

        isActive
            ? "Deactivate Student"
            : "Activate Student",

        `
        <p class="text-slate-600">
            ${
                isActive
                    ? "Are you sure you want to deactivate this student?"
                    : "Are you sure you want to activate this student?"
            }
        </p>
        `,

        async () => {

            const data =
                await API.put(
                    `/api/students/${studentId}/status`,
                    {
                        status:
                            nextStatus
                    }
                );

            if (
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to update student status"
                );

            }

            Modal.close();

            await loadStudents();

        },

        {

            buttonText:
                isActive
                    ? "Deactivate"
                    : "Activate",

            buttonClass:
                isActive
                    ? "bg-red-600 hover:bg-red-700"
                    : "bg-emerald-600 hover:bg-emerald-700",

            loadingText:
                isActive
                    ? "Deactivating..."
                    : "Activating..."

        }

    );

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
