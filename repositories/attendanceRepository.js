const db =
    require("../db");

exports.getByStudentAndDate =
async (
    schoolId,
    studentId,
    attendanceDate
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM attendance
        WHERE school_id = ?
        AND student_id = ?
        AND attendance_date = ?
        `,
        [
            schoolId,
            studentId,
            attendanceDate
        ]
    );

};

exports.getByDate =
async (
    schoolId,
    attendanceDate
) => {

    return await db.allAsync(
        `
        SELECT
            attendance.*,

            students.admission_no,

            students.name AS student_name,

            students.class_name,

            students.section

        FROM attendance

        INNER JOIN students
            ON students.id =
                attendance.student_id

        WHERE attendance.school_id = ?

        AND attendance.attendance_date = ?

        AND students.school_id = ?

        AND students.status = 'active'

        ORDER BY
            students.name
        `,
        [
            schoolId,
            attendanceDate,
            schoolId
        ]
    );

};

exports.create =
async (
    data
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO attendance
            (
                school_id,
                student_id,
                attendance_date,
                status,
                remarks
            )
            VALUES
            (
                ?, ?, ?, ?, ?
            )
            `,
            [
                data.schoolId,
                data.studentId,
                data.attendanceDate,
                data.status,
                data.remarks
            ]
        );

    return await exports.getByStudentAndDate(
        data.schoolId,
        data.studentId,
        data.attendanceDate
    );

};

exports.update =
async (
    schoolId,
    studentId,
    attendanceDate,
    data
) => {

    await db.runAsync(
        `
        UPDATE attendance
        SET
            status = ?,
            remarks = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE school_id = ?
        AND student_id = ?
        AND attendance_date = ?
        `,
        [
            data.status,
            data.remarks,
            schoolId,
            studentId,
            attendanceDate
        ]
    );

    return await exports.getByStudentAndDate(
        schoolId,
        studentId,
        attendanceDate
    );

};
exports.getStudentHistory =
async (
    schoolId,
    studentId,
    startDate,
    endDate
) => {

    return await db.allAsync(
        `
        SELECT
            attendance.*,

            students.admission_no,

            students.name AS student_name,

            students.class_name,

            students.section

        FROM attendance

        INNER JOIN students
            ON students.id =
                attendance.student_id

        WHERE attendance.school_id = ?

        AND attendance.student_id = ?

        AND attendance.attendance_date >= ?

        AND attendance.attendance_date <= ?

        AND students.school_id = ?

        ORDER BY
            attendance.attendance_date DESC
        `,
        [
            schoolId,
            studentId,
            startDate,
            endDate,
            schoolId
        ]
    );

};
exports.getTodaySummary =
async (
    schoolId,
    attendanceDate
) => {

    return await db.getAsync(
        `
        SELECT

            COUNT(
                students.id
            ) AS total_students,

            COUNT(
                CASE
                    WHEN attendance.status = 'present'
                    THEN 1
                END
            ) AS present_students,

            COUNT(
                CASE
                    WHEN attendance.status = 'absent'
                    THEN 1
                END
            ) AS absent_students

        FROM students

        LEFT JOIN attendance
            ON attendance.student_id =
                students.id

            AND attendance.school_id = ?

            AND attendance.attendance_date = ?

        WHERE students.school_id = ?

        AND students.status = 'active'
        `,
        [
            schoolId,
            attendanceDate,
            schoolId
        ]
    );

};