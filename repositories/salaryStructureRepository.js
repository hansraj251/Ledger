const db =
    require("../db");

exports.getActiveByStaffId =
async (
    schoolId,
    teacherId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM salary_structures
        WHERE school_id = ?
        AND teacher_id = ?
        AND status = 'active'
        ORDER BY effective_from DESC, id DESC
        LIMIT 1
        `,
        [
            schoolId,
            teacherId
        ]
    );

};

exports.getById =
async (
    schoolId,
    salaryStructureId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM salary_structures
        WHERE school_id = ?
        AND id = ?
        `,
        [
            schoolId,
            salaryStructureId
        ]
    );

};

exports.getAllByStaffId =
async (
    schoolId,
    teacherId
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM salary_structures
        WHERE school_id = ?
        AND teacher_id = ?
        ORDER BY effective_from DESC, id DESC
        `,
        [
            schoolId,
            teacherId
        ]
    );

};

exports.create =
async (
    salaryStructure
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO salary_structures (

                school_id,
                teacher_id,
                basic_salary,
                hra,
                other_allowances,
                pf,
                esi,
                professional_tax,
                other_deductions,
                effective_from

            )
            VALUES (
                ?, ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            `,
            [
                salaryStructure.schoolId,
                salaryStructure.teacherId,
                salaryStructure.basicSalary,
                salaryStructure.hra,
                salaryStructure.otherAllowances,
                salaryStructure.pf,
                salaryStructure.esi,
                salaryStructure.professionalTax,
                salaryStructure.otherDeductions,
                salaryStructure.effectiveFrom
            ]
        );

    return result.lastID;

};

exports.deactivate =
async (
    schoolId,
    teacherId
) => {

    const result =
        await db.runAsync(
            `
            UPDATE salary_structures
            SET
                status = 'inactive',
                updated_at = CURRENT_TIMESTAMP
            WHERE school_id = ?
            AND teacher_id = ?
            AND status = 'active'
            `,
            [
                schoolId,
                teacherId
            ]
        );

    return result.changes;

};
exports.getAllActiveBySchool =
async (
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT
            salary_structures.*,

            teachers.employee_id,

            teachers.name AS teacher_name,

            designations.name AS designation_name

        FROM salary_structures

        INNER JOIN teachers
            ON teachers.id =
                salary_structures.teacher_id

        LEFT JOIN designations
            ON designations.id =
                teachers.designation_id

        WHERE salary_structures.school_id = ?

        AND salary_structures.status = 'active'

        AND teachers.school_id = ?

        AND teachers.status = 'active'

        ORDER BY
            teachers.name
        `,
        [
            schoolId,
            schoolId
        ]
    );

};