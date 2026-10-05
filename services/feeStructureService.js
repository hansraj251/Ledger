const feeStructureRepository =
    require("../repositories/feeStructureRepository");

const studentRepository =
    require("../repositories/studentRepository");
const feePaymentRepository =
    require("../repositories/feePaymentRepository");    

exports.getFeeStructures =
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

    return await feeStructureRepository.getByStudent(
        schoolId,
        studentId,
        academicYear
    );

};

exports.getActiveFeeStructures =
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

    return await feeStructureRepository.getActiveByStudent(
        schoolId,
        studentId,
        academicYear
    );

};

exports.getAllActiveFeeStructures =
async (
    schoolId,
    academicYear
) => {

    return await feeStructureRepository.getAllActiveBySchool(
        schoolId,
        academicYear
    );

};

exports.getFeeStructure =
async (
    schoolId,
    feeStructureId
) => {

    const feeStructure =
        await feeStructureRepository.getById(
            schoolId,
            feeStructureId
        );

    if (
        !feeStructure
    ) {

        throw new Error(
            "Fee structure not found"
        );

    }

    return feeStructure;

};

exports.createFeeStructure =
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
        !data.feeHead ||
        !String(
            data.feeHead
        ).trim()
    ) {

        throw new Error(
            "Fee head is required"
        );

    }

    if (
        data.amount === undefined ||
        data.amount === null ||
        data.amount === ""
    ) {

        throw new Error(
            "Fee amount is required"
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
        amount < 0
    ) {

        throw new Error(
            "Fee amount cannot be negative or invalid"
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

    return await feeStructureRepository.create({

        schoolId,

        studentId:
            data.studentId,

        academicYear:
            String(
                data.academicYear
            ).trim(),

        feeHead:
            String(
                data.feeHead
            ).trim(),

        amount,

        effectiveFrom:
            data.effectiveFrom || null

    });

};

exports.updateFeeStructure =
async (
    schoolId,
    feeStructureId,
    data
) => {

    const existing =
        await feeStructureRepository.getById(
            schoolId,
            feeStructureId
        );

    if (
        !existing
    ) {

        throw new Error(
            "Fee structure not found"
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
        !data.feeHead ||
        !String(
            data.feeHead
        ).trim()
    ) {

        throw new Error(
            "Fee head is required"
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
        amount < 0
    ) {

        throw new Error(
            "Fee amount cannot be negative or invalid"
        );

    }

    const paidResult =
    await feePaymentRepository
        .getPaidByFeeStructure(
            schoolId,
            existing.student_id,
            String(
                data.academicYear
            ).trim()
        );

const paidItem =
    (
        paidResult ||
        []
    ).find(
        item =>
            Number(
                item.fee_structure_id
            ) ===
            Number(
                feeStructureId
            )
    );

const paidAmount =
    Number(
        paidItem?.paid_amount ||
        0
    );

if (
    amount < paidAmount
) {

    throw new Error(
        `Fee amount cannot be less than the paid amount of ${paidAmount}`
    );

}

    return await feeStructureRepository.update(
        schoolId,
        feeStructureId,
        {

            academicYear:
                String(
                    data.academicYear
                ).trim(),

            feeHead:
                String(
                    data.feeHead
                ).trim(),

            amount,

            effectiveFrom:
                data.effectiveFrom || null

        }
    );

};

exports.updateFeeStructureStatus =
async (
    schoolId,
    feeStructureId,
    status
) => {

    if (
        status !== "active" &&
        status !== "inactive"
    ) {

        throw new Error(
            "Invalid fee structure status"
        );

    }

    const existing =
        await feeStructureRepository.getById(
            schoolId,
            feeStructureId
        );

    if (
        !existing
    ) {

        throw new Error(
            "Fee structure not found"
        );

    }

    await feeStructureRepository.updateStatus(
        schoolId,
        feeStructureId,
        status
    );

    return await feeStructureRepository.getById(
        schoolId,
        feeStructureId
    );

};