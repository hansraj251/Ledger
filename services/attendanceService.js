const attendanceRepository =
    require("../repositories/attendanceRepository");

const studentRepository =
    require("../repositories/studentRepository");
const holidayRepository =
    require("../repositories/holidayRepository");
const attendanceUserClassRepository =
    require("../repositories/attendanceUserClassRepository");
const dateUtil =
    require("../utils/date");    
exports.getByDate =
async (
    schoolId,
    attendanceDate
) => {

    if (
        !attendanceDate
    ) {

        throw new Error(
            "Attendance date is required"
        );

    }
    const holiday =
    await holidayRepository.getByDate(
        schoolId,
        attendanceDate
    );

if (
    holiday
) {

    return [];

}

    return await attendanceRepository.getByDate(
        schoolId,
        attendanceDate
    );

};

exports.saveAttendance =
async (
    schoolId,
    data,
    userId,
    userRole
) => {

    if (
        !data.studentId
    ) {

        throw new Error(
            "Student is required"
        );

    }

    if (
        !data.attendanceDate
    ) {

        throw new Error(
            "Attendance date is required"
        );

    }
    const today =
    dateUtil.formatDate(
        new Date()
    );

if (
    data.attendanceDate >
    today
) {

    throw new Error(
        "Attendance cannot be marked for a future date"
    );

}
    const holiday =
    await holidayRepository.getByDate(
        schoolId,
        data.attendanceDate
    );

if (
    holiday
) {

    throw new Error(
        "Attendance cannot be marked on a holiday"
    );

}

    if (
        ![
            "present",
            "absent"
        ].includes(
            data.status
        )
    ) {

        throw new Error(
            "Attendance status must be present or absent"
        );

    }

    const student =
    await studentRepository.getById(
        schoolId,
        data.studentId
    );

if (
    !student
) {

    throw new Error(
        "Student not found"
    );

}

if (
    userRole ===
    "attendance"
) {

    const assigned =
        await attendanceUserClassRepository
            .isStudentInAssignedClass(
                userId,
                data.studentId,
                schoolId
            );

    if (
        !assigned
    ) {

        throw new Error(
            "You are not authorized to mark attendance for this student"
        );

    }

}

    if (
        student.status !== "active"
    ) {

        throw new Error(
            "Only active students can have attendance"
        );

    }


    const existingAttendance =
        await attendanceRepository.getByStudentAndDate(
            schoolId,
            data.studentId,
            data.attendanceDate
        );

    if (
        existingAttendance
    ) {

        return await attendanceRepository.update(
            schoolId,
            data.studentId,
            data.attendanceDate,
            {
                status:
                    data.status,

                remarks:
                    data.remarks?.trim() || null
            }
        );

    }

    return await attendanceRepository.create({

        schoolId,

        studentId:
            data.studentId,

        attendanceDate:
            data.attendanceDate,

        status:
            data.status,

        remarks:
            data.remarks?.trim() || null

    });

};
exports.getStudentHistory =
async (
    schoolId,
    studentId,
    startDate,
    endDate
) => {

    if (
        !studentId
    ) {

        throw new Error(
            "Student is required"
        );

    }

    if (
        !startDate
    ) {

        throw new Error(
            "Start date is required"
        );

    }

    if (
        !endDate
    ) {

        throw new Error(
            "End date is required"
        );

    }

    const student =
        await studentRepository.getById(
            schoolId,
            studentId
        );

    if (
        !student
    ) {

        throw new Error(
            "Student not found"
        );

    }
    const holidays =
    await holidayRepository.getByDateRange(
        schoolId,
        startDate,
        endDate
    );

    const records =
    await attendanceRepository.getStudentHistory(
        schoolId,
        studentId,
        startDate,
        endDate
    );

return {

    records,

    holidays:
        holidays.map(
            holiday =>
                holiday.holiday_date
        )

};

};
exports.getTodaySummary =
async (
    schoolId,
    attendanceDate
) => {

    const holiday =
        await holidayRepository.getByDate(
            schoolId,
            attendanceDate
        );

    if (
        holiday
    ) {

        return {

            totalStudents: 0,

            presentStudents: 0,

            attendancePercentage: 0,

            isHoliday: true

        };

    }


    const summary =
        await attendanceRepository
            .getTodaySummary(
                schoolId,
                attendanceDate
            );

    const totalStudents =
        Number(
            summary.total_students || 0
        );

    const presentStudents =
        Number(
            summary.present_students || 0
        );

    let attendancePercentage = 0;

    if (
        totalStudents > 0
    ) {

        attendancePercentage =
            (
                presentStudents /
                totalStudents
            ) * 100;

    }

    return {

        totalStudents,

        presentStudents,

        attendancePercentage,

        isHoliday: false

    };

};