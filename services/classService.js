const classRepository =
    require("../repositories/classRepository");

const holidayRepository =
    require("../repositories/holidayRepository");

exports.getClasses =
async (
    schoolId
) => {

    return await classRepository.getAll(
        schoolId
    );

};

exports.getClass =
async (
    schoolId,
    classId
) => {

    const classItem =
        await classRepository.getById(
            schoolId,
            classId
        );

    if (
        !classItem
    ) {

        throw new Error(
            "Class not found"
        );

    }

    return classItem;

};
exports.getClassesWithAttendance =
async (
    schoolId,
    attendanceDate
) => {

    if (
        !attendanceDate
    ) {

        throw new Error(
            "Attendance date is required"
        );

    }

    const classes =
        await classRepository.getAll(
            schoolId
        );

    const activeClasses =
        classes.filter(
            classItem =>
                classItem.status ===
                "active"
        );
    const holiday =
    await holidayRepository.getByDate(
        schoolId,
        attendanceDate
    );

const isHoliday =
    Boolean(
        holiday
    );

    const result = [];

    for (
        const classItem
        of activeClasses
    ) {

        const summary =
            await classRepository
                .getAttendanceSummary(
                    schoolId,
                    classItem.id,
                    attendanceDate
                );

        result.push({

            ...classItem,

            totalStudents:
                Number(
                    summary.total_students ||
                    0
                ),

            presentStudents:
                Number(
                    summary.present_students ||
                    0
                ),

            absentStudents:
                Number(
                    summary.absent_students ||
                    0
                ),
            isHoliday

        });

    }

    return result;

};
exports.createClass =
async (
    schoolId,
    data
) => {

    if (
        !data.name ||
        !data.name.trim()
    ) {

        throw new Error(
            "Class name is required"
        );

    }

    if (
        !data.section ||
        !data.section.trim()
    ) {

        throw new Error(
            "Section is required"
        );

    }

    const name =
        data.name.trim();

    const section =
        data.section.trim();

    const existingClass =
        await classRepository.getByNameAndSection(
            schoolId,
            name,
            section
        );

    if (
        existingClass
    ) {

        throw new Error(
            "Class and section already exists"
        );

    }

    const classId =
        await classRepository.create({

            schoolId,

            name,

            section

        });

    return await classRepository.getById(
        schoolId,
        classId
    );

};
exports.updateClass =
async (
    schoolId,
    classId,
    data
) => {

    if (
        !data.name ||
        !data.name.trim()
    ) {

        throw new Error(
            "Class name is required"
        );

    }

    if (
        !data.section ||
        !data.section.trim()
    ) {

        throw new Error(
            "Section is required"
        );

    }

    const name =
        data.name.trim();

    const section =
        data.section.trim();

    const existingClass =
        await classRepository.getByNameAndSection(
            schoolId,
            name,
            section
        );

    if (
        existingClass &&
        Number(
            existingClass.id
        ) !== Number(
            classId
        )
    ) {

        throw new Error(
            "Class and section already exists"
        );

    }

    const existingClassById =
        await classRepository.getById(
            schoolId,
            classId
        );

    if (
        !existingClassById
    ) {

        throw new Error(
            "Class not found"
        );

    }

    await classRepository.update(
        schoolId,
        classId,
        {
            name,
            section
        }
    );

    return await classRepository.getById(
        schoolId,
        classId
    );

};
exports.updateClassStatus =
async (
    schoolId,
    classId,
    status
) => {

    if (
        status !== "active" &&
        status !== "inactive"
    ) {

        throw new Error(
            "Invalid status"
        );

    }

    const existingClass =
        await classRepository.getById(
            schoolId,
            classId
        );

    if (
        !existingClass
    ) {

        throw new Error(
            "Class not found"
        );

    }

    await classRepository.updateStatus(
        schoolId,
        classId,
        status
    );

    return await classRepository.getById(
        schoolId,
        classId
    );

};