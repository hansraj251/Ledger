Auth.requireSchoolOwner();

const studentName =
    document.getElementById(
        "studentName"
    );

const studentDetails =
    document.getElementById(
        "studentDetails"
    );

const startDate =
    document.getElementById(
        "startDate"
    );

const endDate =
    document.getElementById(
        "endDate"
    );

const historyResult =
    document.getElementById(
        "historyResult"
    );

const historyTableBody =
    document.getElementById(
        "historyTableBody"
    );

const totalDays =
    document.getElementById(
        "totalDays"
    );

const presentDays =
    document.getElementById(
        "presentDays"
    );

const absentDays =
    document.getElementById(
        "absentDays"
    );


const params =
    new URLSearchParams(
        window.location.search
    );

const studentId =
    params.get(
        "studentId"
    );


function getDateString(
    date
) {

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


function setDefaultDates() {

    const today =
        new Date();

    const firstDay =
        new Date(
            today.getFullYear(),
            today.getMonth(),
            1
        );

    startDate.value =
        getDateString(
            firstDay
        );

    endDate.value =
        getDateString(
            today
        );

}


async function loadStudent() {

    try {

        const data =
            await API.get(
                `/api/students/${studentId}`
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load student"
            );

        }

        const student =
            data.student;

        studentName.textContent =
            student.name ||
            "Student";

        studentDetails.textContent =
            `Admission No: ${
                student.admission_no ||
                "-"
            } • ${
                student.class_name ||
                "-"
            } • Section ${
                student.section ||
                "-"
            }`;

    }
    catch (err) {

        console.error(err);

        studentName.textContent =
            "Student";

        studentDetails.textContent =
            "Unable to load student details";

    }

}


async function loadHistory() {

    try {

        historyResult.textContent =
            "";

        historyResult.className =
            "mt-4 text-sm";

        if (
            !studentId
        ) {

            throw new Error(
                "Student ID is missing"
            );

        }

        if (
            !startDate.value ||
            !endDate.value
        ) {

            throw new Error(
                "Please select both dates"
            );

        }

        if (
            startDate.value >
            endDate.value
        ) {

            throw new Error(
                "Start date cannot be after end date"
            );

        }


        const data =
            await API.get(
                `/api/attendance/student/${studentId}/history?startDate=${encodeURIComponent(
                    startDate.value
                )}&endDate=${encodeURIComponent(
                    endDate.value
                )}`
            );

        if (
            !data.success
        ) {

            throw new Error(
                data.message ||
                "Unable to load attendance history"
            );

        }

        renderHistory(
    data.attendance?.records ||
    [],
    data.attendance?.holidays ||
    []
);

    }
    catch (err) {

        console.error(err);

        historyResult.textContent =
            err.message;

        historyResult.className =
            "mt-4 text-sm text-red-600";

        historyTableBody.innerHTML = `

            <tr>

                <td
                    colspan="3"
                    class="px-6 py-10 text-center text-sm text-red-600">

                    Unable to load attendance history.

                </td>

            </tr>

        `;

        totalDays.textContent =
            "0";

        presentDays.textContent =
            "0";

        absentDays.textContent =
            "0";

    }
    finally {

    }

}


function renderHistory(
    records,
    holidays
) {
    const holidaySet =
    new Set(
        holidays
    );

const attendanceRecords =
    records.filter(
        record =>
            !holidaySet.has(
                record.attendance_date
            )
    );

    const presentCount =
    attendanceRecords.filter(
            record =>
                record.status ===
                "present"
        ).length;

    const absentCount =
    attendanceRecords.filter(
            record =>
                record.status ===
                "absent"
        ).length;

    totalDays.textContent =
    attendanceRecords.length;

    presentDays.textContent =
        presentCount;

    absentDays.textContent =
        absentCount;


    if (
    !attendanceRecords.length
) {

        historyTableBody.innerHTML = `

            <tr>

                <td
                    colspan="3"
                    class="px-6 py-10 text-center text-sm text-slate-500">

                    No attendance records found for the selected dates.

                </td>

            </tr>

        `;

        return;

    }


    historyTableBody.innerHTML =
    attendanceRecords.map(
            record => {

                const isPresent =
                    record.status ===
                    "present";

                return `

                    <tr
                        class="border-slate-100 hover:bg-slate-200">

                        <td
                            class="px-6 py-4 text-sm font-medium text-slate-700">

                            ${formatDate(
                                record.attendance_date
                            )}

                        </td>

                        <td
                            class="px-6 py-4">

                            <span
                                class="${
                                    isPresent
                                        ? "rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700"
                                        : "rounded-full bg-red-100 px-3 py-1 text-xs font-medium text-red-700"
                                }">

                                ${
                                    isPresent
                                        ? "Present"
                                        : "Absent"
                                }

                            </span>

                        </td>

                    </tr>

                `;

            }
        ).join("");

}


function formatDate(
    dateString
) {

    if (
        !dateString
    ) {

        return "-";

    }

    const parts =
        dateString.split(
            "-"
        );

    if (
        parts.length !== 3
    ) {

        return dateString;

    }

    return `${parts[2]}-${parts[1]}-${parts[0]}`;

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

startDate.addEventListener(
    "change",
    loadHistory
);

endDate.addEventListener(
    "change",
    loadHistory
);

setDefaultDates();

if (
    studentId
) {

    loadStudent();

    loadHistory();

}
else {

    historyResult.textContent =
        "Student ID is missing.";

    historyResult.className =
        "mt-4 text-sm text-red-600";

}