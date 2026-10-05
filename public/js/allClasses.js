Auth.requireLogin();

let classes = [];

let students = [];
function setupHeader(
    isAttendanceUser
) {

    const backBtn =
        document.getElementById(
            "backBtn"
        );

    const logoutBtn =
        document.getElementById(
            "logoutBtn"
        );

    if (
        isAttendanceUser
    ) {

        backBtn.classList.add(
            "hidden"
        );

        logoutBtn.classList.remove(
            "hidden"
        );

        logoutBtn.addEventListener(
            "click",
            () => {

                Auth.logout();

            }
        );

        return;

    }

    backBtn.classList.remove(
        "hidden"
    );

    logoutBtn.classList.add(
        "hidden"
    );

}

async function loadData() {

    try {
        const token =
    localStorage.getItem(
        "token"
    );

let isAttendanceUser =
    false;

if (
    token
) {

    try {

        const payload =
            JSON.parse(
                atob(
                    token
                        .split(".")[1]
                )
            );

        isAttendanceUser =
            payload.role ===
            "attendance";
            setupHeader(
    isAttendanceUser
);

    }
    catch (err) {

        console.error(err);

    }

}


        let classesData;

if (
    isAttendanceUser
) {

    classesData =
        await API.get(
            "/api/users/attendance/classes"
        );

}
else {

    classesData =
        await API.get(
            "/api/classes"
        );

}

const studentsData =
    await API.get(
        "/api/students"
    );

        if (
            !classesData.success
        ) {

            throw new Error(
                classesData.message ||
                "Unable to load classes"
            );

        }

        if (
            !studentsData.success
        ) {

            throw new Error(
                studentsData.message ||
                "Unable to load students"
            );

        }

        classes =
            classesData.classes.filter(
                classItem =>
                    classItem.status ===
                    "active"
            );

        students =
            studentsData.students.filter(
                student =>
                    student.status ===
                    "active"
            );

        renderClasses();

    }
    catch (err) {

        console.error(err);

        document.getElementById(
            "classesSummary"
        ).textContent =
            "Unable to load classes.";

        document.getElementById(
            "classesGrid"
        ).innerHTML = `
            <div
                class="col-span-full rounded-2xl border border-red-200 bg-red-50 p-6 text-center text-sm text-red-600">

                Unable to load classes.

            </div>
        `;

    }

}


function renderClasses() {

    const grid =
        document.getElementById(
            "classesGrid"
        );

    const summary =
        document.getElementById(
            "classesSummary"
        );

    summary.textContent =
        `${classes.length} active class${
            classes.length === 1
                ? ""
                : "es"
        }`;


    if (
        !classes.length
    ) {

        grid.innerHTML = `
            <div
                class="col-span-full rounded-2xl border border-slate-200 bg-white p-10 text-center shadow-sm">

                <p
                    class="text-sm text-slate-500">

                    No active classes found.

                </p>

            </div>
        `;

        return;

    }


    grid.innerHTML =
        classes.map(
            classItem => {

                const studentCount =
                    students.filter(
                        student =>
                            student.class_name ===
                            classItem.name &&
                            student.section ===
                            classItem.section
                    ).length;


                return `
                    <button
                        type="button"
                        onclick="openClassAttendance(
                            ${classItem.id}
                        )"
                        class="group rounded-2xl border border-slate-200 bg-white p-6 text-left shadow-sm transition duration-200 hover:-translate-y-1 hover:border-blue-200 hover:shadow-lg">


                        <div
                            class="mt-5">

                            <h3
                                class="text-lg font-semibold text-slate-800">

                                ${escapeHtml(
                                    classItem.name
                                )}

                            </h3>

                            <p
                                class="mt-1 text-sm text-slate-500">

                                Section
                                ${escapeHtml(
                                    classItem.section
                                )}

                            </p>

                        </div>


                        <div
                            class="mt-5 flex items-end justify-between border-slate-100 pt-4">

                            <div>

                                <p
                                    class="text-xs text-slate-500">

                                    Students

                                </p>

                                <p
                                    class="mt-1 text-2xl font-bold text-slate-800">

                                    ${studentCount}

                                </p>

                            </div>

                        </div>

                    </button>
                `;

            }
        ).join("");

}


function getClassInitial(
    className
) {

    const value =
        String(
            className || ""
        ).trim();

    return value
        ? value.charAt(0).toUpperCase()
        : "C";

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


function openClassAttendance(
    classId
) {

    window.location.href =
        `/school/attendance.html?classId=${encodeURIComponent(
            classId
        )}`;

}


loadData();
