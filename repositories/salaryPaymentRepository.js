const db =
    require("../db");

exports.getTotalPaidByTeacher =
async (
    schoolId,
    teacherId,
    salaryMonth
) => {

    return await db.getAsync(
        `
        SELECT
            COALESCE(
                SUM(amount_paid),
                0
            ) AS total_paid
        FROM salary_payments
        WHERE school_id = ?
        AND teacher_id = ?
        AND salary_month = ?
        `,
        [
            schoolId,
            teacherId,
            salaryMonth
        ]
    );

};

exports.getByTeacherAndMonth =
async (
    schoolId,
    teacherId,
    salaryMonth
) => {

    return await db.allAsync(
        `
        SELECT *
        FROM salary_payments
        WHERE school_id = ?
        AND teacher_id = ?
        AND salary_month = ?
        ORDER BY
            payment_date DESC,
            id DESC
        `,
        [
            schoolId,
            teacherId,
            salaryMonth
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
            INSERT INTO salary_payments
            (
                school_id,
                teacher_id,
                salary_month,
                salary_amount,
                amount_paid,
                payment_date,
                payment_mode,
                reference_no,
                remarks
            )
            VALUES
            (
                ?, ?, ?, ?, ?, ?, ?, ?, ?
            )
            `,
            [
                data.schoolId,
                data.teacherId,
                data.salaryMonth,
                data.salaryAmount,
                data.amountPaid,
                data.paymentDate,
                data.paymentMode,
                data.referenceNo,
                data.remarks
            ]
        );

    return await db.getAsync(
        `
        SELECT *
        FROM salary_payments
        WHERE id = ?
        `,
        [
            result.lastID
        ]
    );

};

exports.getHistoryBySchool =
async (
    schoolId,
    salaryMonth
) => {

    return await db.allAsync(
        `
        SELECT
            salary_payments.*,
            teachers.name AS teacher_name,
            teachers.employee_id,
            designations.name AS designation_name
        FROM salary_payments

        INNER JOIN teachers
            ON teachers.id =
                salary_payments.teacher_id

        LEFT JOIN designations
            ON designations.id =
                teachers.designation_id

        WHERE salary_payments.school_id = ?
        AND salary_payments.salary_month = ?

        ORDER BY
            salary_payments.payment_date DESC,
            salary_payments.id DESC
        `,
        [
            schoolId,
            salaryMonth
        ]
    );

};