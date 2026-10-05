Auth.requireSchoolOwner();

const studentsContainer =
    document.getElementById(
        "studentsContainer"
    );

const studentSummary =
    document.getElementById(
        "studentSummary"
    );

const studentSearch =
    document.getElementById(
        "studentSearch"
    );

let students = [];


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
            (
                data.students ||
                []
            ).filter(
                student =>
                    student.status ===
                    "active"
            );

        renderStudents(
            students
        );

    }
    catch (err) {

        console.error(err);

        studentsContainer.innerHTML = `

            <div
                class="col-span-full rounded-2xl border border-red-200 bg-white p-8 text-center text-sm text-red-600">

                Unable to load students.

            </div>

        `;

        studentSummary.textContent =
            "Unable to load students";

    }

}


function renderStudents(
    studentList
) {

    studentSummary.textContent =
        `${studentList.length} active student${studentList.length === 1 ? "" : "s"}`;


    if (
        !studentList.length
    ) {

        studentsContainer.innerHTML = `

            <div
                class="col-span-full rounded-2xl border border-slate-200 bg-white p-8 text-center text-sm text-slate-500">

                No active students found.

            </div>

        `;

        return;

    }


    studentsContainer.innerHTML =
        studentList.map(
            student => `

                <article
                    class="rounded-2xl border border-slate-200 bg-white p-6 shadow-sm transition hover:-translate-y-1 hover:shadow-md">

                    <div
                        class="flex items-start justify-between gap-4">

                        <div>

                            <h3
                                class="text-lg font-semibold text-slate-800">

                                ${escapeHtml(
                                    student.name
                                )}

                            </h3>

                            <p
                                class="mt-1 text-sm text-slate-500">

                                Admission No:
                                ${escapeHtml(
                                    student.admission_no ||
                                    "-"
                                )}

                            </p>

                        </div>

                        <span
                            class="rounded-full bg-emerald-100 px-3 py-1 text-xs font-medium text-emerald-700">

                            Active

                        </span>

                    </div>


                    <div
                        class="mt-5 border-slate-100 pt-4">

                        <div
                            class="grid grid-cols-2 gap-4">

                            <div>

                                <p
                                    class="text-xs text-slate-500">

                                    Class

                                </p>

                                <p
                                    class="mt-1 text-sm font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.class_name ||
                                        "-"
                                    )}

                                </p>

                            </div>

                            <div>

                                <p
                                    class="text-xs text-slate-500">

                                    Section

                                </p>

                                <p
                                    class="mt-1 text-sm font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.section ||
                                        "-"
                                    )}

                                </p>

                            </div>

                            <div>

                                <p
                                    class="text-xs text-slate-500">

                                    Roll No.

                                </p>

                                <p
                                    class="mt-1 text-sm font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.roll_no ||
                                        "-"
                                    )}

                                </p>

                            </div>

                            <div>

                                <p
                                    class="text-xs text-slate-500">

                                    Gender

                                </p>

                                <p
                                    class="mt-1 text-sm font-medium capitalize text-slate-800">

                                    ${escapeHtml(
                                        student.gender ||
                                        "-"
                                    )}

                                </p>

                            </div>

                        </div>

                    </div>


                    <div
                        class="mt-5 border-slate-100 pt-4">

                        <p
                            class="text-xs font-semibold uppercase tracking-wide text-slate-500">

                            Parent Details

                        </p>

                        <div
                            class="mt-3 space-y-2">

                            <div
                                class="flex justify-between gap-4 text-sm">

                                <span
                                    class="text-slate-500">

                                    Father

                                </span>

                                <span
                                    class="text-right font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.father_name ||
                                        "-"
                                    )}

                                </span>

                            </div>

                            <div
                                class="flex justify-between gap-4 text-sm">

                                <span
                                    class="text-slate-500">

                                    Mother

                                </span>

                                <span
                                    class="text-right font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.mother_name ||
                                        "-"
                                    )}

                                </span>

                            </div>

                            <div
                                class="flex justify-between gap-4 text-sm">

                                <span
                                    class="text-slate-500">

                                    Mobile

                                </span>

                                <span
                                    class="text-right font-medium text-slate-800">

                                    ${escapeHtml(
                                        student.mobile ||
                                        "-"
                                    )}

                                </span>

                            </div>

                        </div>

                    </div>


                    <div
                        class="mt-5">

                        <button
                            type="button"
                            onclick="openAttendanceHistory(${student.id})"
                            class="w-full rounded-xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-blue-700">

                            Attendance History

                        </button>

                    </div>

                </article>

            `
        ).join("");

}


studentSearch.addEventListener(
    "input",
    () => {

        const search =
            studentSearch.value
                .trim()
                .toLowerCase();

        const filteredStudents =
            students.filter(
                student =>
                    String(
                        student.name ||
                        ""
                    )
                        .toLowerCase()
                        .includes(search) ||

                    String(
                        student.admission_no ||
                        ""
                    )
                        .toLowerCase()
                        .includes(search) ||

                    String(
                        student.class_name ||
                        ""
                    )
                        .toLowerCase()
                        .includes(search) ||

                    String(
                        student.section ||
                        ""
                    )
                        .toLowerCase()
                        .includes(search)
            );

        renderStudents(
            filteredStudents
        );

    }
);


function openAttendanceHistory(
    studentId
) {

    window.location.href =
        `/school/attendance-history.html?studentId=${studentId}`;

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


loadStudents();