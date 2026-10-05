const db =
    require("../db");

exports.getById =
async (
    schoolId,
    paymentId
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM fee_payments
        WHERE id = ?
        AND school_id = ?
        `,
        [
            paymentId,
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
        FROM fee_payments
        WHERE school_id = ?
        AND student_id = ?
        AND academic_year = ?
        ORDER BY
            payment_date DESC,
            id DESC
        `,
        [
            schoolId,
            studentId,
            academicYear
        ]
    );

};

exports.getTotalPaidByStudent =
async (
    schoolId,
    studentId,
    academicYear
) => {

    return await db.getAsync(
        `
        SELECT
            COALESCE(
                SUM(amount),
                0
            ) AS total_paid
        FROM fee_payments
        WHERE school_id = ?
        AND student_id = ?
        AND academic_year = ?
        `,
        [
            schoolId,
            studentId,
            academicYear
        ]
    );

};
exports.getPaidByFeeStructure =

async (

    schoolId,

    studentId,

    academicYear

) => {

    return await db.allAsync(

        `
        SELECT

            fee_payment_items.fee_structure_id,

            COALESCE(

                SUM(
                    fee_payment_items.amount
                ),

                0

            ) AS paid_amount

        FROM fee_payment_items

        INNER JOIN fee_payments

            ON fee_payments.id =
                fee_payment_items.payment_id

        WHERE fee_payments.school_id = ?

        AND fee_payments.student_id = ?

        AND fee_payments.academic_year = ?

        GROUP BY

            fee_payment_items.fee_structure_id

        `,

        [

            schoolId,

            studentId,

            academicYear

        ]

    );

};
exports.getAllBySchool =
async (
    schoolId,
    academicYear
) => {

    return await db.allAsync(
        `
        SELECT
            fee_payments.*,

            students.admission_no,

            students.name AS student_name,

            students.class_name,

            students.section

        FROM fee_payments

        INNER JOIN students
            ON students.id =
                fee_payments.student_id

        WHERE fee_payments.school_id = ?

        AND fee_payments.academic_year = ?

        AND students.school_id = ?

        ORDER BY
            fee_payments.payment_date DESC,
            fee_payments.id DESC
        `,
        [
            schoolId,
            academicYear,
            schoolId
        ]
    );

};
async function generateReceiptNo(

    schoolId

) {

    const currentYear =

        new Date()

            .getFullYear();

    const prefix =

        `REC-${currentYear}-`;

    const result =

        await db.getAsync(

            `

            SELECT

                MAX(

                    CAST(

                        SUBSTR(
                            receipt_no,
                            ?
                        ) AS INTEGER

                    )

                ) AS last_number

            FROM fee_payments

            WHERE school_id = ?

            AND receipt_no LIKE ?

            `,

            [

                prefix.length + 1,

                schoolId,

                `${prefix}%`

            ]

        );

    const nextNumber =

        Number(
            result?.last_number || 0
        ) + 1;

    return (

        prefix +

        String(
            nextNumber
        ).padStart(
            6,
            "0"
        )

    );

}
exports.create =

async (

    data

) => {

    await db.runAsync(

        `
        BEGIN TRANSACTION
        `

    );

    try {
        const receiptNo =

    await generateReceiptNo(

        data.schoolId

    );

        const result =

            await db.runAsync(

                `
                INSERT INTO fee_payments

                (

                    school_id,

                    student_id,

                    academic_year,

                    amount,

                    payment_date,

                    payment_mode,

                    receipt_no,

                    remarks

                )

                VALUES

                (

                    ?, ?, ?, ?, ?, ?, ?, ?

                )

                `,

                [

                    data.schoolId,

                    data.studentId,

                    data.academicYear,

                    data.amount,

                    data.paymentDate,

                    data.paymentMode,

                    receiptNo,

                    data.remarks

                ]

            );

        const paymentId =

            result.lastID;

        for (

            const item of
                data.items

        ) {

            await db.runAsync(

                `
                INSERT INTO
                    fee_payment_items

                (

                    payment_id,

                    fee_structure_id,

                    fee_head,

                    amount

                )

                VALUES

                (

                    ?, ?, ?, ?

                )

                `,

                [

                    paymentId,

                    item.feeStructureId,

                    item.feeHead,

                    item.amount

                ]

            );

        }

        await db.runAsync(

            `
            COMMIT
            `

        );

        return await exports.getById(

            data.schoolId,

            paymentId

        );

    }
    catch (err) {

        await db.runAsync(

            `
            ROLLBACK
            `

        );

        throw err;

    }

};
exports.getPendingFees =
async (
    schoolId,
    academicYear
) => {

    return await db.getAsync(
        `
        SELECT
            COALESCE(
                SUM(fee_structures.amount),
                0
            ) AS total_fee,

            COALESCE(
                (
                    SELECT
                        SUM(fee_payments.amount)
                    FROM fee_payments
                    WHERE fee_payments.school_id = ?
                    AND fee_payments.academic_year = ?
                ),
                0
            ) AS total_paid

        FROM fee_structures

        INNER JOIN students
            ON students.id =
                fee_structures.student_id

        WHERE fee_structures.school_id = ?
        AND fee_structures.academic_year = ?
        AND fee_structures.status = 'active'
        AND students.school_id = ?
        AND students.status = 'active'
        `,
        [
            schoolId,
            academicYear,
            schoolId,
            academicYear,
            schoolId
        ]
    );
};

exports.getReceiptData =

async (

    schoolId,

    paymentId

) => {

    const payment =

        await db.getAsync(

            `
            SELECT

                fee_payments.id,

                fee_payments.receipt_no,

                fee_payments.payment_date,

                fee_payments.amount,

                fee_payments.payment_mode,

                fee_payments.remarks,

                students.name AS student_name,

                students.father_name,

                students.mobile AS student_mobile,

                students.admission_no,

                students.class_name,

                students.section,

                schools.name AS school_name,

                schools.address AS school_address,

                schools.city AS school_city,

                schools.state AS school_state,

                schools.pincode AS school_pincode,

                schools.mobile AS school_mobile,
                schools.logo AS school_logo,
                schools.receipt_footer_message
                    AS receipt_footer_message

            FROM fee_payments

            INNER JOIN students

                ON students.id =
                    fee_payments.student_id

            INNER JOIN schools

                ON schools.id =
                    fee_payments.school_id

            WHERE fee_payments.id = ?

            AND fee_payments.school_id = ?

            `,

            [

                paymentId,

                schoolId

            ]

        );

    if (
        !payment
    ) {

        return null;

    }

    const items =

        await db.allAsync(

            `
            SELECT

                fee_payment_items.id,

                fee_payment_items.fee_structure_id,

                fee_payment_items.fee_head,

                fee_payment_items.amount

            FROM fee_payment_items

            INNER JOIN fee_payments

                ON fee_payments.id =
                    fee_payment_items.payment_id

            WHERE fee_payment_items.payment_id = ?

            AND fee_payments.school_id = ?

            ORDER BY
                fee_payment_items.id ASC

            `,

            [

                paymentId,

                schoolId

            ]

        );

    payment.items =
        items || [];

    return payment;

};