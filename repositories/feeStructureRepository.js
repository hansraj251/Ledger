const db =
    require("../db");

exports.getById =
async (
    schoolId,
    feeStructureId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM fee_structures
        WHERE id = ?
        AND school_id = ?
        `,
        [
            feeStructureId,
            schoolId
        ]
    );

};

exports.getByStudent =
async (
    schoolId,
    studentId,
    academicYear
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM fee_structures
        WHERE school_id = ?
        AND student_id = ?
        AND academic_year = ?
        ORDER BY id ASC
        `,
        [
            schoolId,
            studentId,
            academicYear
        ]
    );

};

exports.getActiveByStudent =
async (
    schoolId,
    studentId,
    academicYear
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM fee_structures
        WHERE school_id = ?
        AND student_id = ?
        AND academic_year = ?
        AND status = 'active'
        ORDER BY id ASC
        `,
        [
            schoolId,
            studentId,
            academicYear
        ]
    );

};

exports.getAllActiveBySchool =
async (
    schoolId,
    academicYear
) => {

    return await db.allAsync(
        `
        SELECT
            fee_structures.*,

            students.admission_no,

            students.name AS student_name,

            students.class_name,

            students.section

        FROM fee_structures

        INNER JOIN students
            ON students.id =
                fee_structures.student_id

        WHERE fee_structures.school_id = ?

        AND fee_structures.academic_year = ?

        AND fee_structures.status = 'active'

        AND students.school_id = ?

        AND students.status = 'active'

        ORDER BY
            students.name,

            fee_structures.id
        `,
        [
            schoolId,
            academicYear,
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
            INSERT INTO fee_structures
            (
                school_id,
                student_id,
                academic_year,
                fee_head,
                amount,
                effective_from
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?
            )
            `,
            [
                data.schoolId,
                data.studentId,
                data.academicYear,
                data.feeHead,
                data.amount,
                data.effectiveFrom
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
    feeStructureId,
    data
) => {

    await db.runAsync(
        `
        UPDATE fee_structures
        SET
            academic_year = ?,
            fee_head = ?,
            amount = ?,
            effective_from = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
        AND school_id = ?
        `,
        [
            data.academicYear,
            data.feeHead,
            data.amount,
            data.effectiveFrom,
            feeStructureId,
            schoolId
        ]
    );

    return await exports.getById(
        schoolId,
        feeStructureId
    );

};

exports.updateStatus =
async (
    schoolId,
    feeStructureId,
    status
) => {

    const result =
        await db.runAsync(
            `
            UPDATE fee_structures
            SET
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = ?
            AND school_id = ?
            `,
            [
                status,
                feeStructureId,
                schoolId
            ]
        );

    return result.changes;

};