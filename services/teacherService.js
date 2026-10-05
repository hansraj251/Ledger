const teacherRepository =
    require("../repositories/teacherRepository");
const designationRepository =
    require("../repositories/designationRepository");
exports.getTeachers =
async (
    schoolId
) => {

    return await teacherRepository.getAll(
        schoolId
    );

};

exports.getTeacher =
async (
    schoolId,
    teacherId
) => {

    const teacher =
        await teacherRepository.getById(
            schoolId,
            teacherId
        );

    if (
        !teacher
    ) {

        throw new Error(
            "Teacher not found"
        );

    }

    return teacher;

};

exports.createTeacher =
async (
    schoolId,
    data
) => {

    if (
        !data.name ||
        !data.name.trim()
    ) {

        throw new Error(
            "Teacher name is required"
        );

    }

    if (
        data.employeeId
    ) {

        const existingTeacher =
            await teacherRepository.getByEmployeeId(
                schoolId,
                data.employeeId
            );

        if (
            existingTeacher
        ) {

            throw new Error(
                "Employee ID already exists"
            );

        }

    }
    if (
    data.designationId
) {

    const designation =
        await designationRepository.getById(
            schoolId,
            data.designationId
        );

    if (
        !designation ||
        designation.status !== "active"
    ) {

        throw new Error(
            "Invalid designation"
        );

    }

}

    const teacherId =
        await teacherRepository.create({

            schoolId,

            employeeId:
                data.employeeId ||
                null,

            name:
                data.name.trim(),

            fatherName:
                data.fatherName ||
                null,

            motherName:
                data.motherName ||
                null,

            dob:
                data.dob ||
                null,

            gender:
                data.gender ||
                null,

            mobile:
                data.mobile ||
                null,

            email:
                data.email ||
                null,

            address:
                data.address ||
                null,

            qualification:
                data.qualification ||
                null,

            subject:
                data.subject ||
                null,

            joiningDate:
    data.joiningDate ||
    null,

designationId:
    data.designationId ||
    null

        });

    return await teacherRepository.getById(
        schoolId,
        teacherId
    );

};
exports.updateTeacher =
async (
    schoolId,
    teacherId,
    data
) => {

    const existingTeacher =
        await teacherRepository.getById(
            schoolId,
            teacherId
        );

    if (
        !existingTeacher
    ) {

        throw new Error(
            "Teacher not found"
        );

    }

    if (
        !data.name ||
        !data.name.trim()
    ) {

        throw new Error(
            "Teacher name is required"
        );

    }

    if (
        data.employeeId
    ) {

        const duplicateTeacher =
            await teacherRepository.getByEmployeeId(
                schoolId,
                data.employeeId
            );

        if (
            duplicateTeacher &&
            duplicateTeacher.id !==
                Number(teacherId)
        ) {

            throw new Error(
                "Employee ID already exists"
            );

        }

    }
    if (
    data.designationId
) {

    const designation =
        await designationRepository.getById(
            schoolId,
            data.designationId
        );

    if (
        !designation ||
        designation.status !== "active"
    ) {

        throw new Error(
            "Invalid designation"
        );

    }

}

    await teacherRepository.update(
        schoolId,
        teacherId,
        {

            employeeId:
                data.employeeId ||
                null,

            name:
                data.name.trim(),

            fatherName:
                data.fatherName ||
                null,

            motherName:
                data.motherName ||
                null,

            dob:
                data.dob ||
                null,

            gender:
                data.gender ||
                null,

            mobile:
                data.mobile ||
                null,

            email:
                data.email ||
                null,

            address:
                data.address ||
                null,

            qualification:
                data.qualification ||
                null,

            subject:
                data.subject ||
                null,

            joiningDate:
    data.joiningDate ||
    null,

designationId:
    data.designationId ||
    null

        }
    );

    return await teacherRepository.getById(
        schoolId,
        teacherId
    );

};
exports.updateTeacherStatus =
async (
    schoolId,
    teacherId,
    status
) => {

    const teacher =
        await teacherRepository.getById(
            schoolId,
            teacherId
        );

    if (
        !teacher
    ) {

        throw new Error(
            "Teacher not found"
        );

    }

    if (
        ![
            "active",
            "inactive"
        ].includes(
            status
        )
    ) {

        throw new Error(
            "Invalid teacher status"
        );

    }

    await teacherRepository.updateStatus(
        schoolId,
        teacherId,
        status
    );

    return await teacherRepository.getById(
        schoolId,
        teacherId
    );

};