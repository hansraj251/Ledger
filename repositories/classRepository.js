const db =
    require("../db");

exports.getAll =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM classes
        WHERE school_id = ?
        ORDER BY name, section
        `,
        [
            schoolId
        ]
    );

};

exports.getById =
async (
    schoolId,
    classId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM classes
        WHERE school_id = ?
        AND id = ?
        `,
        [
            schoolId,
            classId
        ]
    );

};

exports.getAttendanceSummary =
async (
    schoolId,
    classId,
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

        INNER JOIN classes
            ON classes.school_id =
                students.school_id

            AND classes.name =
                students.class_name

            AND classes.section =
                students.section

        LEFT JOIN attendance
            ON attendance.student_id =
                students.id

            AND attendance.school_id = ?

            AND attendance.attendance_date = ?

        WHERE students.school_id = ?

        AND classes.id = ?

        AND students.status = 'active'
        `,
        [
            schoolId,
            attendanceDate,
            schoolId,
            classId
        ]
    );

};

exports.getByNameAndSection =
async (
    schoolId,
    name,
    section
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM classes
        WHERE school_id = ?
        AND name = ?
        AND section = ?
        `,
        [
            schoolId,
            name,
            section
        ]
    );

};

exports.create =
async (
    classData
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO classes (
                school_id,
                name,
                section
            )
            VALUES (
                ?,
                ?,
                ?
            )
            `,
            [
                classData.schoolId,
                classData.name,
                classData.section
            ]
        );

    return result.lastID;

};
exports.update =
async (
    schoolId,
    classId,
    classData
) => {

    await db.runAsync(
        `
        UPDATE classes
        SET
            name = ?,
            section = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE school_id = ?
        AND id = ?
        `,
        [
            classData.name,
            classData.section,
            schoolId,
            classId
        ]
    );

};
exports.updateStatus =
async (
    schoolId,
    classId,
    status
) => {

    await db.runAsync(
        `
        UPDATE classes
        SET
            status = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE school_id = ?
        AND id = ?
        `,
        [
            status,
            schoolId,
            classId
        ]
    );

};