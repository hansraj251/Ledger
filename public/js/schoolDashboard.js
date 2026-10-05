Auth.requireSchoolOwner();

async function loadSchool() {

    const sidebarSchoolLogo =
        document.getElementById(
            "sidebarSchoolLogo"
        );

    const savedSchoolLogo =
        localStorage.getItem(
            "schoolLogo"
        );

    if (
        sidebarSchoolLogo &&
        savedSchoolLogo
    ) {

        sidebarSchoolLogo.src =
            savedSchoolLogo;

        sidebarSchoolLogo.classList.remove(
            "hidden"
        );

    }


    try {

        const data =
            await API.get(
                "/api/schools/me"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message
            );

        }


        const school =
            data.school;


        document.getElementById(
            "schoolName"
        ).textContent =
            school.name;


        if (
            sidebarSchoolLogo &&
            school.logo
        ) {

            sidebarSchoolLogo.src =
                school.logo;

            sidebarSchoolLogo.classList.remove(
                "hidden"
            );


            localStorage.setItem(
                "schoolLogo",
                school.logo
            );

        }

    }
    catch (err) {

        console.error(err);

        window.location.href =
            "/login.html";

    }

}

async function loadClasses() {

    try {

        const today =
    getLocalDate();

const data =
    await API.get(
        `/api/classes/attendance-summary?date=${today}`
    );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load classes"
            );

        }

        const activeClasses =
    data.classes;

        const classesContainer =
            document.getElementById(
                "dashboardClasses"
            );

        if (
            !classesContainer
        ) {

            return;

        }

        if (
            !activeClasses.length
        ) {

            classesContainer.innerHTML = `

                <p
                    class="text-sm text-slate-500">

                    No active classes found.

                </p>

            `;

            return;

        }

        classesContainer.innerHTML =
    activeClasses.map(
        classItem => `

            <a
    href="/school/attendance.html?classId=${encodeURIComponent(
        classItem.id
    )}"
                class="group rounded-2xl border border-slate-200 bg-gradient-to-br from-white via-purple-50/40 to-indigo-50/50 p-4 shadow-sm transition-all duration-200 hover:-translate-y-1 hover:border-purple-200 hover:shadow-md">

                <div
                    class="flex items-start justify-between gap-3">

                    <div class="flex items-center gap-3">

    <p
        class="text-lg font-bold"
        style="
            background: linear-gradient(
                90deg,
                #c13bbd 0%,
                #7b3fc6 50%,
                #2454c7 100%
            );
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            color: transparent;
        ">

        ${classItem.name || "-"}

    </p>

    <span class="text-indigo-200">
        |
    </span>

    <p
        class="text-sm font-medium text-indigo-600">

        Section:
        ${classItem.section || "-"}

    </p>

</div>

                </div>


                ${
    classItem.isHoliday

    ?

    `
        
    `

    :

    `
        <div
            class="mt-4 grid grid-cols-2 gap-2">


            <div
    class="rounded-xl border border-slate-200 bg-white/80 p-3 text-center shadow-sm">

    <p
        class="text-xs font-semibold text-green-600">

        Present

    </p>

    <p
        class="mt-1 text-xl font-bold"
        style="
            background: linear-gradient(
                90deg,
                #c13bbd 0%,
                #7b3fc6 50%,
                #2454c7 100%
            );
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            color: transparent;
        ">

        ${
            classItem.presentStudents ||
            0
        }

    </p>

</div>


            <div
    class="rounded-xl border border-slate-200 bg-white/80 p-3 text-center shadow-sm">

    <p
        class="text-xs font-semibold text-red-600">

        Absent

    </p>

    <p
        class="mt-1 text-xl font-bold"
        style="
            background: linear-gradient(
                90deg,
                #7b3fc6 0%,
                #4f46a5 50%,
                #2454c7 100%
            );
            -webkit-background-clip: text;
            background-clip: text;
            -webkit-text-fill-color: transparent;
            color: transparent;
        ">

        ${
            classItem.absentStudents ||
            0
        }

    </p>

</div>

        </div>
    `
}

            </a>

        `
    ).join("");

    }
    catch (err) {

        console.error(err);

        const classesContainer =
            document.getElementById(
                "dashboardClasses"
            );

        if (
            classesContainer
        ) {

            classesContainer.innerHTML = `

                <p
                    class="text-sm text-red-600">

                    Unable to load classes.

                </p>

            `;

        }

    }

}

async function loadStudentCount() {

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

        const activeStudents =
    data.students.filter(
        student =>
            student.status ===
            "active"
    );

const studentsCount =
    document.querySelector(
        "#studentsCount"
    );

if (
    studentsCount
) {

    studentsCount.textContent =
        activeStudents.length;

}

    }
    catch (err) {

        console.error(err);

    }

}
async function loadTodayAttendance() {

    try {

        const data =
            await API.get(
                "/api/attendance/today"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load today's attendance"
            );

        }

        const attendanceElement =
            document.getElementById(
                "todayAttendance"
            );

   if (
    attendanceElement
) {

    if (
        data.isHoliday
    ) {

        attendanceElement.textContent =
            "Holiday";

    } else {

        attendanceElement.textContent =
            `${Number(
                data.attendancePercentage || 0
            ).toFixed(1)}%`;

    }

}

    }
    catch (err) {

        console.error(err);

        const attendanceElement =
            document.getElementById(
                "todayAttendance"
            );

        if (
            attendanceElement
        ) {

            attendanceElement.textContent =
                "--";

        }

    }

}
function getCurrentAcademicYear() {

    const today =
        new Date();

    const year =
        today.getFullYear();

    const month =
        today.getMonth() + 1;

    const startYear =
        month >= 4
            ? year
            : year - 1;

    return `${startYear}-${String(
        startYear + 1
    ).slice(-2)}`;
}

async function loadPendingFees() {

    try {

        const academicYear =
            getCurrentAcademicYear();

        const data =
            await API.get(
                "/api/fee-payments/pending"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load pending fees"
            );

        }

        const pendingFees =
            document.getElementById(
                "pendingFees"
            );

        if (
            pendingFees
        ) {

            pendingFees.textContent =
                `₹${Number(
                    data.pendingAmount || 0
                ).toLocaleString(
                    "en-IN"
                )}`;

        }

    }
    catch (err) {

        console.error(err);

        const pendingFees =
            document.getElementById(
                "pendingFees"
            );

        if (
            pendingFees
        ) {

            pendingFees.textContent =
                "₹0";

        }

    }
}
async function loadPendingSalaries() {

    try {

        const data =
            await API.get(
                "/api/salary-payments/pending"
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load pending salaries"
            );

        }

        const pendingSalaries =
            document.getElementById(
                "pendingSalaries"
            );

        if (
            pendingSalaries
        ) {

            pendingSalaries.textContent =
                `₹${Number(
                    data.pendingAmount || 0
                ).toLocaleString(
                    "en-IN"
                )}`;

        }

    }
    catch (err) {

        console.error(err);

        const pendingSalaries =
            document.getElementById(
                "pendingSalaries"
            );

        if (
            pendingSalaries
        ) {

            pendingSalaries.textContent =
                "₹0";

        }

    }

}
async function loadStaffCount() {

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
                "Unable to load staff"
            );

        }

        const activeStaff =
            data.teachers.filter(
                teacher =>
                    teacher.status ===
                    "active"
            );

        const staffCount =
            document.querySelector(
                "#staffCount"
            );

        if (
            staffCount
        ) {

            staffCount.textContent =
                activeStaff.length;

        }

    }
    catch (err) {

        console.error(err);

        const staffCount =
            document.querySelector(
                "#staffCount"
            );

        if (
            staffCount
        ) {

            staffCount.textContent =
                "0";

        }

    }

}

document.getElementById(
    "logoutBtn"
).addEventListener(
    "click",
    () => {

        Auth.logout();

    }
);

loadSchool();
loadClasses();
loadStudentCount();
loadStaffCount();
loadTodayAttendance();
loadPendingFees();
loadPendingSalaries();
window.addEventListener(
    "pageshow",
    () => {

        loadClasses();

    }
);

setInterval(
    () => {

        loadTodayAttendance();

        loadClasses();
        loadPendingFees();
        loadPendingSalaries();

    },
    10000
);