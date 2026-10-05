const db =
    require("../db");
const salaryStructureRepository =
    require("../repositories/salaryStructureRepository");
const teacherRepository =
    require("../repositories/teacherRepository");
exports.getActiveSalaryStructure =
async (
    schoolId,
    teacherId
) => {

    return await salaryStructureRepository.getActiveByStaffId(
        schoolId,
        teacherId
    );

};

exports.getSalaryStructures =
async (
    schoolId,
    teacherId
) => {

    return await salaryStructureRepository.getAllByStaffId(
        schoolId,
        teacherId
    );

};

exports.getSalaryStructure =
async (
    schoolId,
    salaryStructureId
) => {

    const salaryStructure =
        await salaryStructureRepository.getById(
            schoolId,
            salaryStructureId
        );

    if (
        !salaryStructure
    ) {

        throw new Error(
            "Salary structure not found"
        );

    }

    return salaryStructure;

};

exports.createSalaryStructure =
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

    if (
        data.basicSalary === undefined ||
        data.basicSalary === null ||
        data.basicSalary === ""
    ) {

        throw new Error(
            "Basic salary is required"
        );

    }

    const basicSalary =
        Number(
            data.basicSalary
        );

    if (
        !Number.isFinite(
            basicSalary
        ) ||
        basicSalary < 0
    ) {

        throw new Error(
            "Invalid basic salary"
        );

    }

    const hra =
        Number(
            data.hra || 0
        );

    const otherAllowances =
        Number(
            data.otherAllowances || 0
        );

    const pf =
        Number(
            data.pf || 0
        );

    const esi =
        Number(
            data.esi || 0
        );

    const professionalTax =
        Number(
            data.professionalTax || 0
        );

    const otherDeductions =
        Number(
            data.otherDeductions || 0
        );

    const effectiveFrom =
        data.effectiveFrom || null;
    if (
    !effectiveFrom
) {

    throw new Error(
        "Effective date is required"
    );

}

if (
    !/^\d{4}-\d{2}-\d{2}$/.test(
        effectiveFrom
    )
) {

    throw new Error(
        "Invalid effective date"
    );

}

    const values = [
        hra,
        otherAllowances,
        pf,
        esi,
        professionalTax,
        otherDeductions
    ];

    if (
        values.some(
            value =>
                !Number.isFinite(
                    value
                ) ||
                value < 0
        )
    ) {

        throw new Error(
            "Salary amounts cannot be negative or invalid"
        );

    }

    const salaryStructureId =
    await db.transaction(
        async () => {

            await salaryStructureRepository.deactivate(
                schoolId,
                data.teacherId
            );

            return await salaryStructureRepository.create({

                schoolId,

                teacherId:
                    data.teacherId,

                basicSalary,

                hra,

                otherAllowances,

                pf,

                esi,

                professionalTax,

                otherDeductions,

                effectiveFrom

            });

        }
    );

    return await salaryStructureRepository.getById(
        schoolId,
        salaryStructureId
    );

};
exports.getAllActiveSalaryStructures =
async (
    schoolId
) => {

    return await salaryStructureRepository.getAllActiveBySchool(
        schoolId
    );

};