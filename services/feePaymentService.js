const feePaymentRepository =
    require("../repositories/feePaymentRepository");

const studentRepository =
    require("../repositories/studentRepository");
const feeStructureRepository =
    require("../repositories/feeStructureRepository");
exports.getPayments =
async (
    schoolId,
    studentId,
    academicYear
) => {

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

    return await feePaymentRepository.getByStudent(
        schoolId,
        studentId,
        academicYear
    );

};
exports.getReceiptData =

async (

    schoolId,

    paymentId

) => {

    const payment =

        await feePaymentRepository
            .getReceiptData(

                schoolId,

                paymentId

            );

    if (
        !payment
    ) {

        throw new Error(
            "Fee payment not found"
        );

    }

    return payment;

};
exports.getTotalPaid =
async (
    schoolId,
    studentId,
    academicYear
) => {

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

    const result =
        await feePaymentRepository.getTotalPaidByStudent(
            schoolId,
            studentId,
            academicYear
        );

    return Number(
        result?.total_paid || 0
    );

};
exports.getPaidByFeeStructure =
async (
    schoolId,
    studentId,
    academicYear
) => {

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

    return await feePaymentRepository
        .getPaidByFeeStructure(
            schoolId,
            studentId,
            academicYear
        );

};
exports.getAllPayments =
async (
    schoolId,
    academicYear
) => {

    return await feePaymentRepository.getAllBySchool(
        schoolId,
        academicYear
    );

};

exports.getPayment =
async (
    schoolId,
    paymentId
) => {

    const payment =
        await feePaymentRepository.getById(
            schoolId,
            paymentId
        );

    if (
        !payment
    ) {

        throw new Error(
            "Fee payment not found"
        );

    }

    return payment;

};

exports.createPayment =
async (
    schoolId,
    data
) => {

    if (
        !data.studentId
    ) {

        throw new Error(
            "Student is required"
        );

    }

    if (
        !data.academicYear ||
        !String(
            data.academicYear
        ).trim()
    ) {

        throw new Error(
            "Academic year is required"
        );

    }

    if (
        data.amount === undefined ||
        data.amount === null ||
        data.amount === ""
    ) {

        throw new Error(
            "Payment amount is required"
        );

    }

    const amount =
        Number(
            data.amount
        );

    if (
        !Number.isFinite(
            amount
        ) ||
        amount <= 0
    ) {

        throw new Error(
            "Payment amount must be greater than zero"
        );

    }

    if (
        !data.paymentDate
    ) {

        throw new Error(
            "Payment date is required"
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
    const feeStructures =
    await feeStructureRepository.getActiveByStudent(
        schoolId,
        data.studentId,
        String(
            data.academicYear
        ).trim()
    );

const totalFee =
    feeStructures.reduce(
        (
            total,
            fee
        ) =>
            total +
            Number(
                fee.amount || 0
            ),
        0
    );

const paidResult =
    await feePaymentRepository.getTotalPaidByStudent(
        schoolId,
        data.studentId,
        String(
            data.academicYear
        ).trim()
    );

const totalPaid =
    Number(
        paidResult?.total_paid || 0
    );

const balance =
    totalFee -
    totalPaid;

if (
    amount > balance
) {

    throw new Error(
        `Payment amount cannot exceed outstanding balance of ${balance}`
    );

} 
        if (
        !Array.isArray(
            data.items
        ) ||
        data.items.length === 0
    ) {

        throw new Error(
            "At least one fee head is required"
        );

    }
        const paidByFeeStructure =

        await feePaymentRepository
            .getPaidByFeeStructure(
                schoolId,
                data.studentId,
                String(
                    data.academicYear
                ).trim()
            );

    const paidMap =
        new Map();

    paidByFeeStructure.forEach(

        item => {

            paidMap.set(

                Number(
                    item.fee_structure_id
                ),

                Number(
                    item.paid_amount || 0
                )

            );

        }

    );

    const feeStructureMap =
        new Map();

    feeStructures.forEach(

        fee => {

            feeStructureMap.set(
                Number(fee.id),
                fee
            );

        }

    );

    const paymentItems = [];

    let itemsTotal = 0;
    const selectedFeeStructures =
    new Set();

    for (
        const item of data.items
    ) {
        const feeStructureId =
    Number(
        item.feeStructureId
    );

if (
    selectedFeeStructures.has(
        feeStructureId
    )
) {

    throw new Error(
        "The same fee head cannot be selected more than once"
    );

}

selectedFeeStructures.add(
    feeStructureId
);

        const feeStructure =
    feeStructureMap.get(
        feeStructureId
    );

        if (
            !feeStructure
        ) {

            throw new Error(
                "Invalid fee head selected"
            );

        }

        const itemAmount =
            Number(
                item.amount
            );

        if (
            !Number.isFinite(
                itemAmount
            ) ||
            itemAmount <= 0
        ) {

            throw new Error(
                `Invalid amount for ${feeStructure.fee_head}`
            );

        }

        const alreadyPaid =
    paidMap.get(
        Number(
            feeStructure.id
        )
    ) || 0;

const outstandingForHead =
    Math.max(
        0,
        Number(
            feeStructure.amount || 0
        ) -
        alreadyPaid
    );

        if (
            itemAmount >
            outstandingForHead
        ) {

            throw new Error(
                `${feeStructure.fee_head} amount cannot exceed ${outstandingForHead}`
            );

        }

        paymentItems.push({

            feeStructureId:
                feeStructure.id,

            feeHead:
                feeStructure.fee_head,

            amount:
                itemAmount

        });

        itemsTotal +=
            itemAmount;

    }

    if (
        Math.abs(
            itemsTotal - amount
        ) > 0.01
    ) {

        throw new Error(
            "Payment amount must equal the total of selected fee heads"
        );

    }

    return await feePaymentRepository.create({

    schoolId,

    studentId:
        data.studentId,

    academicYear:
        String(
            data.academicYear
        ).trim(),

    amount,

    paymentDate:
        data.paymentDate,

    paymentMode:
        data.paymentMode?.trim() || null,

    receiptNo:
        data.receiptNo?.trim() || null,

    remarks:
        data.remarks?.trim() || null,

    items:
        paymentItems

});

};
exports.getPendingFees =
async (
    schoolId
) => {

    const today =
        new Date();

    const year =
        today.getFullYear();

    const month =
        today.getMonth() + 1;

    const startYear =
        month >= 4
            ? year
            : year - 1;

    const academicYear =
        `${startYear}-${String(
            startYear + 1
        ).slice(-2)}`;

    const result =
        await feePaymentRepository.getPendingFees(
            schoolId,
            academicYear
        );

    const totalFee =
        Number(
            result?.total_fee || 0
        );

    const totalPaid =
        Number(
            result?.total_paid || 0
        );

    const pendingAmount =
        Math.max(
            totalFee - totalPaid,
            0
        );

    return {
        academicYear,
        totalFee,
        totalPaid,
        pendingAmount
    };
};