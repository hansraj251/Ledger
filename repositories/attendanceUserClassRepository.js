const db =
    require("../db");


exports.getClassesByUser =
async (
    userId
) => {

    return await db.allAsync(
        `
        SELECT
            classes.*

        FROM attendance_user_classes

        INNER JOIN classes
            ON classes.id =
                attendance_user_classes.class_id

        WHERE attendance_user_classes.user_id = ?

        AND classes.status = 'active'

        ORDER BY
            classes.name,
            classes.section
        `,
        [
            userId
        ]
    );

};


exports.getUsersByClass =
async (
    schoolId,
    classId
) => {

    return await db.allAsync(
        `
        SELECT
            users.id,
            users.name,
            users.username,
            users.status

        FROM attendance_user_classes

        INNER JOIN users
            ON users.id =
                attendance_user_classes.user_id

        WHERE users.school_id = ?

        AND attendance_user_classes.class_id = ?

        AND users.role = 'attendance'

        ORDER BY
            users.name
        `,
        [
            schoolId,
            classId
        ]
    );

};
exports.getValidClassIds =
async (
    schoolId,
    classIds
) => {

    if (
        !classIds.length
    ) {

        return [];

    }

    const placeholders =
        classIds
            .map(
                () => "?"
            )
            .join(",");

    return await db.allAsync(
        `
        SELECT
            id

        FROM classes

        WHERE school_id = ?

        AND status = 'active'

        AND id IN (${placeholders})
        `,
        [
            schoolId,
            ...classIds
        ]
    );

};
exports.isStudentInAssignedClass =
async (
    userId,
    studentId,
    schoolId
) => {

    const result =
        await db.getAsync(
            `
            SELECT
                students.id

            FROM attendance_user_classes

            INNER JOIN classes
                ON classes.id =
                    attendance_user_classes.class_id

            INNER JOIN students
                ON students.class_name =
                    classes.name

                AND students.section =
                    classes.section

                AND students.school_id =
                    classes.school_id

            WHERE attendance_user_classes.user_id = ?

            AND students.id = ?

            AND students.school_id = ?

            AND classes.school_id = ?

            AND classes.status = 'active'

            AND students.status = 'active'
            `,
            [
                userId,
                studentId,
                schoolId,
                schoolId
            ]
        );

    return Boolean(
        result
    );

};
exports.assignClasses =
async (
    userId,
    classIds
) => {

    for (
        const classId
        of classIds
    ) {

        await db.runAsync(
            `
            INSERT OR IGNORE INTO attendance_user_classes
            (
                user_id,
                class_id
            )
            VALUES
            (
                ?,
                ?
            )
            `,
            [
                userId,
                classId
            ]
        );

    }

};


exports.deleteByUser =
async (
    userId
) => {

    await db.runAsync(
        `
        DELETE FROM attendance_user_classes

        WHERE user_id = ?
        `,
        [
            userId
        ]
    );

};

exports.getStudentsByUser =
async (
    userId,
    schoolId
) => {

    return await db.allAsync(
        `
        SELECT
            students.*

        FROM attendance_user_classes

        INNER JOIN classes
            ON classes.id =
                attendance_user_classes.class_id

        INNER JOIN students
            ON students.class_name =
                classes.name

            AND students.section =
                classes.section

            AND students.school_id =
                classes.school_id

        WHERE attendance_user_classes.user_id = ?

        AND classes.school_id = ?

        AND classes.status = 'active'

        AND students.school_id = ?

        AND students.status = 'active'

        ORDER BY
            students.name
        `,
        [
            userId,
            schoolId,
            schoolId
        ]
    );

};