const salaryPaymentRepository =
    require("../repositories/salaryPaymentRepository");

const salaryStructureRepository =
    require("../repositories/salaryStructureRepository");

const teacherRepository =
    require("../repositories/teacherRepository");

function getSalaryMonth() {

    const today =
        new Date();

    return `${today.getFullYear()}-${String(
        today.getMonth() + 1
    ).padStart(
        2,
        "0"
    )}`;

}

function calculateNetSalary(
    salary
) {

    const totalEarnings =
        Number(
            salary.basic_salary || 0
        ) +
        Number(
            salary.hra || 0
        ) +
        Number(
            salary.other_allowances || 0
        );

    const totalDeductions =
        Number(
            salary.pf || 0
        ) +
        Number(
            salary.esi || 0
        ) +
        Number(
            salary.professional_tax || 0
        ) +
        Number(
            salary.other_deductions || 0
        );

    return Math.max(
        totalEarnings -
        totalDeductions,
        0
    );

}

exports.getPendingSalary =
async (
    schoolId
) => {

    const salaryMonth =
        getSalaryMonth();

    const salaryStructures =
        await salaryStructureRepository.getAllActiveBySchool(
            schoolId
        );

    const staff = [];

    for (
        const salary of salaryStructures
    ) {

        const salaryAmount =
            calculateNetSalary(
                salary
            );

        const paidResult =
            await salaryPaymentRepository.getTotalPaidByTeacher(
                schoolId,
                salary.teacher_id,
                salaryMonth
            );

        const totalPaid =
            Number(
                paidResult?.total_paid ||
                0
            );

        const pendingAmount =
            Math.max(
                salaryAmount -
                totalPaid,
                0
            );

        staff.push({

            teacherId:
                salary.teacher_id,

            employeeId:
                salary.employee_id,

            teacherName:
                salary.teacher_name,

            designationName:
                salary.designation_name,

            salaryMonth,

            salaryAmount,

            totalPaid,

            pendingAmount

        });

    }

    const totalSalary =
        staff.reduce(
            (
                total,
                member
            ) =>
                total +
                member.salaryAmount,
            0
        );

    const totalPaid =
        staff.reduce(
            (
                total,
                member
            ) =>
                total +
                member.totalPaid,
            0
        );

    const pendingAmount =
        staff.reduce(
            (
                total,
                member
            ) =>
                total +
                member.pendingAmount,
            0
        );

    return {

        salaryMonth,

        totalSalary,

        totalPaid,

        pendingAmount,

        staff

    };

};

exports.createPayment =
async (
    schoolId,
    data
) => {

    if (
        !data.teacherId
    ) {

        throw new Error(
            "Staff is required"
        );

    }

    const teacher =
        await teacherRepository.getActiveById(
            schoolId,
            data.teacherId
        );

    if (
        !teacher
    ) {

        throw new Error(
            "Active staff not found"
        );

    }

    const salary =
        await salaryStructureRepository.getActiveByStaffId(
            schoolId,
            data.teacherId
        );

    if (
        !salary
    ) {

        throw new Error(
            "Salary structure not found"
        );

    }

    const salaryAmount =
        calculateNetSalary(
            salary
        );

    if (
        salaryAmount <= 0
    ) {

        throw new Error(
            "Net salary must be greater than zero"
        );

    }

    const salaryMonth =
        String(
            data.salaryMonth ||
            getSalaryMonth()
        ).trim();

    const amountPaid =
        Number(
            data.amountPaid
        );

    if (
        !Number.isFinite(
            amountPaid
        ) ||
        amountPaid <= 0
    ) {

        throw new Error(
            "Payment amount must be greater than zero"
        );

    }

    const paidResult =
        await salaryPaymentRepository.getTotalPaidByTeacher(
            schoolId,
            data.teacherId,
            salaryMonth
        );

    const totalPaid =
        Number(
            paidResult?.total_paid ||
            0
        );

    const pendingAmount =
        Math.max(
            salaryAmount -
            totalPaid,
            0
        );

    if (
        pendingAmount <= 0
    ) {

        throw new Error(
            "Salary is already fully paid for this month"
        );

    }

    if (
        amountPaid >
        pendingAmount
    ) {

        throw new Error(
            `Payment amount cannot exceed outstanding salary of ${pendingAmount}`
        );

    }

    if (
        !data.paymentDate
    ) {

        throw new Error(
            "Payment date is required"
        );

    }

    return await salaryPaymentRepository.create({

        schoolId,

        teacherId:
            data.teacherId,

        salaryMonth,

        salaryAmount,

        amountPaid,

        paymentDate:
            data.paymentDate,

        paymentMode:
            data.paymentMode?.trim() ||
            null,

        referenceNo:
            data.referenceNo?.trim() ||
            null,

        remarks:
            data.remarks?.trim() ||
            null

    });

};

exports.getPaymentHistory =
async (
    schoolId,
    salaryMonth
) => {

    return await salaryPaymentRepository.getHistoryBySchool(
        schoolId,
        salaryMonth ||
        getSalaryMonth()
    );

};