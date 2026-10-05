const bcrypt =
    require("bcrypt");

const userRepository =
    require("../repositories/userRepository");
const attendanceUserClassRepository =
    require("../repositories/attendanceUserClassRepository");
const teacherRepository =
    require("../repositories/teacherRepository");
exports.createAttendanceUser =
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

    if (
        !Array.isArray(
            data.classIds
        ) ||
        !data.classIds.length
    ) {

        throw new Error(
            "At least one class is required"
        );

    }

    if (
        !data.username ||
        !data.username.trim()
    ) {

        throw new Error(
            "User ID is required"
        );

    }

    if (
        !data.password ||
        data.password.length < 8
    ) {

        throw new Error(
            "Password must be at least 8 characters"
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
            "Staff not found or inactive"
        );

    }

    const username =
        data.username
            .trim()
            .toLowerCase();

    const existingUser =
        await userRepository.getByUsername(
            username
        );

    if (
        existingUser
    ) {

        throw new Error(
            "User ID already exists"
        );

    }

    const classIds =
        [
            ...new Set(
                data.classIds.map(
                    classId =>
                        Number(
                            classId
                        )
                )
            )
        ];

    if (
        classIds.some(
            classId =>
                !Number.isInteger(
                    classId
                ) ||
                classId <= 0
        )
    ) {

        throw new Error(
            "Invalid class selection"
        );

    }
    const validClasses =
    await attendanceUserClassRepository
        .getValidClassIds(
            schoolId,
            classIds
        );

    if (
    validClasses.length !==
    classIds.length
    ) {

        throw new Error(
        "One or more selected classes are invalid"
        );

    }

    const passwordHash =
        await bcrypt.hash(
            data.password,
            10
        );

    const user =
        await userRepository.create({

            schoolId,

            name:
                teacher.name,

            username,

            passwordHash,

            role:
                "attendance",

            status:
                "active"

        });

    await attendanceUserClassRepository
        .assignClasses(
            user.id,
            classIds
        );

    return user;

};
exports.getAttendanceUsers =
async (
    schoolId
) => {

    return await userRepository
        .getAttendanceUsersBySchool(
            schoolId
        );

};
exports.getAttendanceClasses =
async (
    userId
) => {

    return await attendanceUserClassRepository
        .getClassesByUser(
            userId
        );

};