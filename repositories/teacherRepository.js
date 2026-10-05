const db =
    require("../db");

exports.getById =
async (
    schoolId,
    teacherId
) => {

    return await db.getAsync(
        `
        SELECT
            teachers.*,
            designations.name AS designation_name
        FROM teachers
        LEFT JOIN designations
            ON designations.id =
                teachers.designation_id
            AND designations.school_id =
                teachers.school_id
        WHERE teachers.school_id = ?
        AND teachers.id = ?
        `,
        [
            schoolId,
            teacherId
        ]
    );

};

exports.getActiveById =
async (
    schoolId,
    teacherId
) => {

    return await db.getAsync(
        `
        SELECT
            teachers.*,

            designations.name AS designation_name

        FROM teachers

        LEFT JOIN designations
            ON designations.id =
                teachers.designation_id

            AND designations.school_id =
                teachers.school_id

        WHERE teachers.school_id = ?

        AND teachers.id = ?

        AND teachers.status = 'active'
        `,
        [
            schoolId,
            teacherId
        ]
    );

};

exports.getAll =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT
            teachers.*,
            designations.name AS designation_name
        FROM teachers
        LEFT JOIN designations
            ON designations.id =
                teachers.designation_id
            AND designations.school_id =
                teachers.school_id
        WHERE teachers.school_id = ?
        ORDER BY teachers.id DESC
        `,
        [
            schoolId
        ]
    );

};

exports.getByEmployeeId =
async (
    schoolId,
    employeeId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM teachers
        WHERE school_id = ?
        AND employee_id = ?
        `,
        [
            schoolId,
            employeeId
        ]
    );

};

exports.create =
async (
    teacher
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO teachers (

                school_id,
                employee_id,
                name,
                father_name,
                mother_name,
                dob,
                gender,
                mobile,
                email,
                address,
                qualification,
                subject,
                joining_date,
                designation_id

            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            `,
            [
                teacher.schoolId,
                teacher.employeeId,
                teacher.name,
                teacher.fatherName,
                teacher.motherName,
                teacher.dob,
                teacher.gender,
                teacher.mobile,
                teacher.email,
                teacher.address,
                teacher.qualification,
                teacher.subject,
                teacher.joiningDate,
                teacher.designationId
            ]
        );

    return result.lastID;

};

exports.update =
async (
    schoolId,
    teacherId,
    teacher
) => {

    const result =
        await db.runAsync(
            `
            UPDATE teachers
            SET
                employee_id = ?,
                name = ?,
                father_name = ?,
                mother_name = ?,
                dob = ?,
                gender = ?,
                mobile = ?,
                email = ?,
                address = ?,
                qualification = ?,
                subject = ?,
                joining_date = ?,
                designation_id = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE school_id = ?
            AND id = ?
            `,
            [
                teacher.employeeId,
                teacher.name,
                teacher.fatherName,
                teacher.motherName,
                teacher.dob,
                teacher.gender,
                teacher.mobile,
                teacher.email,
                teacher.address,
                teacher.qualification,
                teacher.subject,
                teacher.joiningDate,
                teacher.designationId,
                schoolId,
                teacherId
            ]
        );

    return result.changes;

};
exports.updateStatus =
async (
    schoolId,
    teacherId,
    status
) => {

    const result =
        await db.runAsync(
            `
            UPDATE teachers
            SET
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE school_id = ?
            AND id = ?
            `,
            [
                status,
                schoolId,
                teacherId
            ]
        );

    return result.changes;

};