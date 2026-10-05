const db =
    require("../db");

exports.getById =
async (
    schoolId,
    studentId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM students
        WHERE id = ?
        AND school_id = ?
        `,
        [
            studentId,
            schoolId
        ]
    );

};

exports.getAll =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM students
        WHERE school_id = ?
        ORDER BY id DESC
        `,
        [
            schoolId
        ]
    );

};

exports.getByAdmissionNo =
async (
    schoolId,
    admissionNo
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM students
        WHERE school_id = ?
        AND admission_no = ?
        `,
        [
            schoolId,
            admissionNo
        ]
    );

};
exports.getActiveCount =
async (
    schoolId
) => {

    const result =
        await db.getAsync(
            `
            SELECT
                COUNT(*) AS count
            FROM students
            WHERE school_id = ?
            AND status = 'active'
            `,
            [
                schoolId
            ]
        );

    return result.count;

};
exports.getByClassSectionAndRollNo =
async (
    schoolId,
    className,
    section,
    rollNo
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM students
        WHERE school_id = ?
        AND class_name = ?
        AND section = ?
        AND roll_no = ?
        `,
        [
            schoolId,
            className,
            section,
            rollNo
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
            INSERT INTO students
            (
                school_id,
                admission_no,
                name,
                father_name,
                mother_name,
                dob,
                gender,
                mobile,
                address,
                class_name,
                section,
                roll_no
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            `,
            [
                data.schoolId,
                data.admissionNo,
                data.name,
                data.fatherName,
                data.motherName,
                data.dob,
                data.gender,
                data.mobile,
                data.address,
                data.className,
                data.section,
                data.rollNo
            ]
        );

    return await exports.getById(
        data.schoolId,
        result.lastID
    );

};
exports.update =
async (
    schoolId,
    studentId,
    data
) => {

    await db.runAsync(
        `
        UPDATE students
        SET
            admission_no = ?,
            name = ?,
            father_name = ?,
            mother_name = ?,
            dob = ?,
            gender = ?,
            mobile = ?,
            address = ?,
            class_name = ?,
            section = ?,
            roll_no = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        AND school_id = ?
        `,
        [
            data.admissionNo,
            data.name,
            data.fatherName,
            data.motherName,
            data.dob,
            data.gender,
            data.mobile,
            data.address,
            data.className,
            data.section,
            data.rollNo,
            studentId,
            schoolId
        ]
    );

    return await exports.getById(
        schoolId,
        studentId
    );

};
exports.updateStatus =
async (
    schoolId,
    studentId,
    status
) => {

    const result =
        await db.runAsync(
            `
            UPDATE students
            SET
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE school_id = ?
            AND id = ?
            `,
            [
                status,
                schoolId,
                studentId
            ]
        );

    return result.changes;

};