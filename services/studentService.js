const studentRepository =
    require("../repositories/studentRepository");
const classRepository =
    require("../repositories/classRepository");
const planLimitRepository =
    require("../repositories/planLimitRepository");
const subscriptionService =
    require("./subscriptionService");
const attendanceUserClassRepository =
    require("../repositories/attendanceUserClassRepository");
exports.getStudents =
async (
    schoolId,
    userId,
    userRole
) => {

    if (
        userRole ===
        "attendance"
    ) {

        return await attendanceUserClassRepository
            .getStudentsByUser(
                userId,
                schoolId
            );

    }

    return await studentRepository.getAll(
        schoolId
    );

};

exports.getStudent =
async (
    schoolId,
    studentId
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

    return student;

};
exports.createStudent =
async (
    schoolId,
    data
) => {

    if (
        !data.admissionNo ||
        !data.name
    ) {

        throw new Error(
            "Admission number and student name are required"
        );

    }

    const existing =
        await studentRepository.getByAdmissionNo(
            schoolId,
            data.admissionNo
        );
        if (
        data.className &&
        data.section &&
        data.rollNo
    ) {

        const existingRollNo =
    await studentRepository.getByClassSectionAndRollNo(
        schoolId,
        data.className.trim(),
        data.section.trim(),
        data.rollNo.trim()
    );

if (
    existingRollNo &&
    Number(
        existingRollNo.id
    ) !==
    Number(
        studentId
    )
) {

    throw new Error(
        "Roll number already exists in this class and section"
    );

}

    }

    if (
        existing
    ) {

        throw new Error(
            "Admission number already exists"
        );

    }
    const subscription =
    await subscriptionService.getSchoolSubscription(
        schoolId
    );

if (
    subscription.plan_id
) {

    const activeStudentLimit =
        await planLimitRepository
            .getActiveStudentLimit(
                subscription.plan_id
            );

    if (
        activeStudentLimit !== null &&
        activeStudentLimit !== -1
    ) {

        const activeStudentCount =
            await studentRepository.getActiveCount(
                schoolId
            );

        if (
            activeStudentCount >=
            activeStudentLimit
        ) {

            throw new Error(
                `Active student limit reached. Your plan allows ${activeStudentLimit} active students.`
            );

        }

    }

}
    if (
    data.className &&
    data.section
) {

    const classItem =
        await classRepository.getByNameAndSection(
            schoolId,
            data.className.trim(),
            data.section.trim()
        );

    if (
        !classItem
    ) {

        throw new Error(
            "Selected class and section does not exist"
        );

    }

}

    return await studentRepository.create({

        schoolId,

        admissionNo:
            data.admissionNo.trim(),

        name:
            data.name.trim(),

        fatherName:
            data.fatherName?.trim() || null,

        motherName:
            data.motherName?.trim() || null,

        dob:
            data.dob || null,

        gender:
            data.gender || null,

        mobile:
            data.mobile?.trim() || null,

        address:
            data.address?.trim() || null,

        className:
            data.className?.trim() || null,

        section:
            data.section?.trim() || null,

        rollNo:
            data.rollNo?.trim() || null

    });

};

exports.updateStudent =
async (
    schoolId,
    studentId,
    data
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

    if (
        !data.admissionNo ||
        !data.name
    ) {

        throw new Error(
            "Admission number and student name are required"
        );

    }

    const existing =
        await studentRepository.getByAdmissionNo(
            schoolId,
            data.admissionNo
        );
        if (
        data.className &&
        data.section &&
        data.rollNo
    ) {

        const existingRollNo =
    await studentRepository.getByClassSectionAndRollNo(
        schoolId,
        data.className.trim(),
        data.section.trim(),
        data.rollNo.trim()
    );

if (
    existingRollNo &&
    Number(
        existingRollNo.id
    ) !==
    Number(
        studentId
    )
) {

    throw new Error(
        "Roll number already exists in this class and section"
    );

}

    }

    if (
        existing &&
        Number(existing.id) !==
        Number(studentId)
    ) {

        throw new Error(
            "Admission number already exists"
        );

    }

    if (
        data.className &&
        data.section
    ) {

        const classItem =
            await classRepository.getByNameAndSection(
                schoolId,
                data.className.trim(),
                data.section.trim()
            );

        if (
            !classItem
        ) {

            throw new Error(
                "Selected class and section does not exist"
            );

        }

    }

    return await studentRepository.update(
        schoolId,
        studentId,
        {

            admissionNo:
                data.admissionNo.trim(),

            name:
                data.name.trim(),

            fatherName:
                data.fatherName?.trim() || null,

            motherName:
                data.motherName?.trim() || null,

            dob:
                data.dob || null,

            gender:
                data.gender || null,

            mobile:
                data.mobile?.trim() || null,

            address:
                data.address?.trim() || null,

            className:
                data.className?.trim() || null,

            section:
                data.section?.trim() || null,

            rollNo:
                data.rollNo?.trim() || null

        }
    );

};
exports.updateStudentStatus =
async (
    schoolId,
    studentId,
    status
) => {

    if (
        status !== "active" &&
        status !== "inactive"
    ) {

        throw new Error(
            "Invalid student status"
        );

    }

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
    if (
    status === "active" &&
    student.status !== "active"
) {

    const subscription =
        await subscriptionService
            .getSchoolSubscription(
                schoolId
            );

    if (
        subscription.plan_id
    ) {

        const activeStudentLimit =
            await planLimitRepository
                .getActiveStudentLimit(
                    subscription.plan_id
                );

        if (
            activeStudentLimit !== null &&
            activeStudentLimit !== -1
        ) {

            const activeStudentCount =
                await studentRepository
                    .getActiveCount(
                        schoolId
                    );

            if (
                activeStudentCount >=
                activeStudentLimit
            ) {

                throw new Error(
                    `Active student limit reached. Your plan allows ${activeStudentLimit} active students.`
                );

            }

        }

    }

}

    await studentRepository.updateStatus(
        schoolId,
        studentId,
        status
    );

    return await studentRepository.getById(
        schoolId,
        studentId
    );

};