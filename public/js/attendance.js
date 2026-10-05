Auth.requireAttendanceUser();
let currentSubscription = null;

async function loadSubscription() {
    try {
        const data = await API.get("/api/subscription/school");

        if (!data.success) {
            Notify.error(data.message || "Unable to load subscription");
            return;
        }

        currentSubscription = data.subscription || null;
    } catch (err) {
        console.error("Subscription load error:", err);
    }
}

function isSubscriptionBlocked() {
    const status = (
        currentSubscription?.subscription_status || ""
    ).toLowerCase();

    return status === "expired" || status === "suspended";
}

function getSubscriptionBlockMessage() {
    const status = (
        currentSubscription?.subscription_status || ""
    ).toLowerCase();

    if (status === "suspended") {
        return "Your subscription is suspended. Please contact Align Support.";
    }

    if (status === "expired") {
        return "Your subscription has expired. Please renew your subscription.";
    }

    return "Your subscription does not allow this action.";
}
let isAttendanceUser =
    false;
const token =
    localStorage.getItem(
        "token"
    );

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

    }
    catch (err) {

        console.error(err);

    }

}
const attendanceDate =
    document.getElementById(
        "attendanceDate"
    );

const attendanceStudentTableBody =
    document.getElementById(
        "attendanceStudentTableBody"
    );

const attendanceSummary =
    document.getElementById(
        "attendanceSummary"
    );

const attendanceResult =
    document.getElementById(
        "attendanceResult"
    );

const studentSearch =
    document.getElementById(
        "studentSearch"
    );

const markAllPresentBtn =
    document.getElementById(
        "markAllPresentBtn"
    );
const holidayBtn =
    document.getElementById(
        "holidayBtn"
    );


if (
    isAttendanceUser &&
    holidayBtn
) {

    holidayBtn.classList.add(
        "hidden"
    );

}
const attendanceDateContainer =
    document.getElementById(
        "attendanceDateContainer"
    );

if (
    isAttendanceUser &&
    attendanceDateContainer
) {

    attendanceDateContainer.classList.add(
        "hidden"
    );

}

let students = [];

let filteredStudents = [];

let attendanceRecords = [];
let isHoliday = false;
let isLoadingAttendance =
    false;

const urlParams =
    new URLSearchParams(
        window.location.search
    );

const classId =
    urlParams.get(
        "classId"
    );

let selectedClass =
    null;


function getToday() {

    const date =
        new Date();

    const year =
        date.getFullYear();

    const month =
        String(
            date.getMonth() + 1
        ).padStart(
            2,
            "0"
        );

    const day =
        String(
            date.getDate()
        ).padStart(
            2,
            "0"
        );

    return `${year}-${month}-${day}`;

}


if (
    attendanceDate
) {

    attendanceDate.value =
        getToday();

    attendanceDate.max =
        getToday();

}



async function loadAttendance() {
    if (
    isLoadingAttendance
) {

    return;

}

isLoadingAttendance =
    true;

    try {

        attendanceResult.textContent =
            "";

        attendanceResult.className =
            "mt-4 text-sm";

        const [
            studentsResponse,
            attendanceResponse
        ] =
            await Promise.all([

                API.get(
                    "/api/students"
                ),

                API.get(
                    `/api/attendance?attendanceDate=${encodeURIComponent(
                        attendanceDate.value
                    )}`
                )

            ]);

        if (
            !studentsResponse.success
        ) {

            throw new Error(
                studentsResponse.message ||
                "Unable to load students"
            );

        }

        if (
            !attendanceResponse.success
        ) {

            throw new Error(
                attendanceResponse.message ||
                "Unable to load attendance"
            );

        }

        students =
            (
                studentsResponse.students ||
                []
            ).filter(
                student =>
                    student.status === "active"
            );


        if (
            classId
        ) {

            let classesResponse;

if (
    isAttendanceUser
) {

    classesResponse =
        await API.get(
            "/api/users/attendance/classes"
        );

}
else {

    classesResponse =
        await API.get(
            "/api/classes"
        );

}

            if (
                !classesResponse.success
            ) {

                throw new Error(
                    classesResponse.message ||
                    "Unable to load classes"
                );

            }

            selectedClass =
                (
                    classesResponse.classes ||
                    []
                ).find(
                    classItem =>
                        Number(
                            classItem.id
                        ) ===
                        Number(
                            classId
                        )
                );

            if (
                !selectedClass
            ) {

                throw new Error(
                    "Class not found"
                );

            }

            students =
                students.filter(
                    student =>
                        student.class_name ===
                            selectedClass.name &&
                        student.section ===
                            selectedClass.section
                );

        }


        attendanceRecords =
            attendanceResponse.attendance ||
            [];
        const holidayResponse =
    await API.get(
        `/api/holidays?date=${encodeURIComponent(
            attendanceDate.value
        )}`
    );

isHoliday =
    holidayResponse.success &&
    !!holidayResponse.holiday;

updateHolidayButton();

renderStudents();

if (
    markAllPresentBtn
) {

    markAllPresentBtn.disabled =
        isHoliday;

    markAllPresentBtn.classList.toggle(
        "opacity-50",
        isHoliday
    );

    markAllPresentBtn.classList.toggle(
        "cursor-not-allowed",
        isHoliday
    );

}

    }
    catch (err) {

        console.error(err);

        attendanceStudentTableBody.innerHTML = `

            <tr>

                <td
                    colspan="4"
                    class="px-4 py-10 text-center text-sm text-red-600 sm:px-6">

                    ${escapeHtml(
                        err.message
                    )}

                </td>

            </tr>

        `;

        attendanceSummary.textContent =
            "Unable to load attendance";

    }
    finally {

    isLoadingAttendance =
        false;

}

}
async function loadHoliday() {

    try {

        const data =
            await API.get(
                `/api/holidays?date=${attendanceDate.value}`
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load holiday"
            );

        }

        isHoliday =
            Boolean(
                data.holiday
            );

        updateHolidayButton();

    }
    catch (err) {

        console.error(err);

        isHoliday =
            false;

        updateHolidayButton();

    }

}

function updateHolidayButton() {

    if (
        !holidayBtn
    ) {

        return;

    }

    if (
        isHoliday
    ) {

        holidayBtn.textContent =
             "Remove Holiday";

        holidayBtn.classList.remove(
            "bg-orange-500",
            "hover:bg-orange-600"
        );

        holidayBtn.classList.add(
            "bg-slate-600",
            "hover:bg-slate-700"
        );

    }
    else {

        holidayBtn.textContent =
            "Mark Holiday";

        holidayBtn.classList.remove(
            "bg-slate-600",
            "hover:bg-slate-700"
        );

        holidayBtn.classList.add(
            "bg-orange-500",
            "hover:bg-orange-600"
        );

    }

}
async function removeHoliday() {

    try {

        holidayBtn.disabled =
            true;

        holidayBtn.textContent =
            "Removing...";

        const data =
            await API.delete(
                "/api/holidays",
                {
                    date:
                        attendanceDate.value
                }
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to remove holiday"
            );

        }

        await loadHoliday();

        await loadAttendance();

        Notify.success(
            "Holiday removed successfully"
        );

    }
    catch (err) {

        console.error(err);

        Notify.error(
            err.message ||
            "Unable to remove holiday"
        );

    }
    finally {

        holidayBtn.disabled =
            false;

    }

}
holidayBtn?.addEventListener(
    "click",
    async () => {

        try {

            if (
                isHoliday
            ) {

                await API.delete(
                    "/api/holidays",
                    {
                        date:
                            attendanceDate.value
                    }
                );

            }
            else {

                await API.post(
                    "/api/holidays",
                    {
                        date:
                            attendanceDate.value
                    }
                );

            }

            await loadAttendance();

            await loadHoliday();

            renderStudents();

        }
        catch (err) {

            console.error(err);

            attendanceResult.textContent =
                err.message ||
                "Unable to update holiday.";

            attendanceResult.className =
                "mt-4 text-sm text-red-600";

        }

    }
);


function renderStudents() {

    const searchTerm =
        studentSearch?.value
            .trim()
            .toLowerCase() ||
        "";


    filteredStudents =
        students.filter(
            student => {

                if (
                    !searchTerm
                ) {

                    return true;

                }

                return (
                    String(
                        student.name ||
                        ""
                    )
                    .toLowerCase()
                    .includes(
                        searchTerm
                    ) ||
                    String(
                        student.admission_no ||
                        ""
                    )
                    .toLowerCase()
                    .includes(
                        searchTerm
                    )
                );

            }
        );


    if (
        !filteredStudents.length
    ) {

        attendanceStudentTableBody.innerHTML = `

            <tr>

                <td
                    colspan="4"
                    class="px-4 py-10 text-center text-sm text-slate-500 sm:px-6">

                    No students found.

                </td>

            </tr>

        `;

        attendanceSummary.textContent =
            "0 active students";

        return;

    }


    attendanceStudentTableBody.innerHTML =
        filteredStudents.map(
            student => {

                const attendance =
                    attendanceRecords.find(
                        record =>
                            Number(
                                record.student_id
                            ) ===
                            Number(
                                student.id
                            )
                    );

                const status =
                    attendance?.status ||
                    "absent";


                const isPresent =
                    status === "present";
                const attendanceDisabled =
    isHoliday
        ? "disabled"
        : "";


                return `

                    <tr
                        class="border-slate-100 hover:bg-slate-200">

                        <td
                            class="px-3 py-4 sm:px-6">

                            <p
                                class="text-sm font-medium text-slate-800">

                                ${escapeHtml(
                                    student.name ||
                                    "-"
                                )}

                            </p>

                        </td>


                        <td
    class="px-3 py-4 text-sm text-slate-600 sm:px-6">

    ${escapeHtml(
        student.class_name ||
        "-"
    )}
    -
    ${escapeHtml(
        student.section ||
        "-"
    )}

</td>


                        <td
    class="px-3 py-4 text-center sm:px-6">

    <div
        class="flex items-center justify-center gap-2">

        <button
            type="button"
            ${attendanceDisabled}
            class="attendance-present rounded-lg px-3 py-2 text-xs font-semibold transition ${
                isPresent
                    ? "bg-emerald-600 text-white shadow-sm"
                    : "bg-slate-100 text-slate-600 hover:bg-emerald-50 hover:text-emerald-700"
            }"
            data-student-id="${student.id}"
            data-status="present">

            P

        </button>

        <button
            type="button"
            ${attendanceDisabled}
            class="attendance-absent rounded-lg px-3 py-2 text-xs font-semibold transition ${
                !isPresent
                    ? "bg-red-600 text-white shadow-sm"
                    : "bg-slate-100 text-slate-600 hover:bg-red-50 hover:text-red-700"
            }"
            data-student-id="${student.id}"
            data-status="absent">

            A

        </button>

    </div>

</td>

                    </tr>

                `;

            }
        )
        .join("");


    updateSummary();

}


function updateSummary() {

    let presentCount =
        0;

    let absentCount =
        0;

    students.forEach(
        student => {

            const attendance =
                attendanceRecords.find(
                    record =>
                        Number(
                            record.student_id
                        ) ===
                        Number(
                            student.id
                        )
                );

            if (
                attendance?.status ===
                "present"
            ) {

                presentCount++;

            }
            else {

                absentCount++;

            }

        }
    );


    attendanceSummary.textContent =
        selectedClass
            ? `${selectedClass.name} • Section ${selectedClass.section} • ${students.length} active students • ${presentCount} present • ${absentCount} absent`
            : `${students.length} active students • ${presentCount} present • ${absentCount} absent`;

}


attendanceStudentTableBody?.addEventListener(
    "click",
    async event => {

        const button =
            event.target.closest(
                "[data-status]"
            );

        if (
            !button
        ) {

            return;

        }

        const studentId =
            Number(
                button.dataset.studentId
            );

        const status =
            button.dataset.status;

        button.disabled =
            true;

        await saveStudentAttendance(
            studentId,
            status
        );

        button.disabled =
            false;

    }
);


studentSearch?.addEventListener(
    "input",
    renderStudents
);


attendanceDate?.addEventListener(
    "change",
    async () => {

        await loadAttendance();

    }
);


markAllPresentBtn?.addEventListener(
    "click",
    markAllPresent
);


async function saveStudentAttendance(
    studentId,
    status
) {
     if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    try {

        attendanceResult.textContent =
            "";

        attendanceResult.className =
            "mt-4 text-sm";


        const response =
            await API.post(
                "/api/attendance",
                {
                    studentId:
                        studentId,

                    attendanceDate:
                        attendanceDate.value,

                    status:
                        status
                }
            );


        if (
            !response.success
        ) {

            throw new Error(
                response.message ||
                "Unable to save attendance"
            );

        }


        const existingRecord =
            attendanceRecords.find(
                record =>
                    Number(
                        record.student_id
                    ) ===
                    Number(
                        studentId
                    )
            );


        if (
            existingRecord
        ) {

            existingRecord.status =
                status;

        }
        else {

            attendanceRecords.push({

                student_id:
                    studentId,

                status:
                    status

            });

        }


        renderStudents();


        attendanceResult.textContent =
            "Attendance saved.";

        attendanceResult.className =
            "mt-4 text-sm text-emerald-600";


    }
    catch (err) {

        console.error(err);

        attendanceResult.textContent =
            err.message ||
            "Unable to save attendance.";

        attendanceResult.className =
            "mt-4 text-sm text-red-600";

    }

}


async function markAllPresent() {
     if (isSubscriptionBlocked()) {
        Notify.error(getSubscriptionBlockMessage());
        return;
    }

    if (
        !students.length
    ) {

        return;

    }
    if (
    isHoliday
) {

    attendanceResult.textContent =
        "Attendance is disabled because this is a holiday.";

    attendanceResult.className =
        "mt-4 text-sm text-orange-600";

    return;

}


    markAllPresentBtn.disabled =
        true;

    const originalText =
        markAllPresentBtn.textContent;

    markAllPresentBtn.textContent =
        "Saving...";


    try {

        attendanceResult.textContent =
            "";

        attendanceResult.className =
            "mt-4 text-sm";


        for (
            const student of students
        ) {

            const response =
                await API.post(
                    "/api/attendance",
                    {
                        studentId:
                            student.id,

                        attendanceDate:
                            attendanceDate.value,

                        status:
                            "present"
                    }
                );


            if (
                !response.success
            ) {

                throw new Error(
                    response.message ||
                    "Unable to save attendance"
                );

            }


            const existingRecord =
                attendanceRecords.find(
                    record =>
                        Number(
                            record.student_id
                        ) ===
                        Number(
                            student.id
                        )
                );


            if (
                existingRecord
            ) {

                existingRecord.status =
                    "present";

            }
            else {

                attendanceRecords.push({

                    student_id:
                        student.id,

                    status:
                        "present"

                });

            }

        }


        renderStudents();


        attendanceResult.textContent =
            "All students marked present.";

        attendanceResult.className =
            "mt-4 text-sm text-emerald-600";

    }
    catch (err) {

        console.error(err);

        attendanceResult.textContent =
            err.message ||
            "Unable to mark all students present.";

        attendanceResult.className =
            "mt-4 text-sm text-red-600";

    }
    finally {

        markAllPresentBtn.disabled =
            false;

        markAllPresentBtn.textContent =
            originalText;

    }

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


(async function initAttendancePage() {
    await loadSubscription();
    await loadAttendance();
})();

setInterval(
    async () => {

        await loadAttendance();

    },
    15000
);