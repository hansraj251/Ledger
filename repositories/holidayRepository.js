const db =
    require("../db");

exports.getByDate =
async (
    schoolId,
    holidayDate
) => {

    return await db.getAsync(
        `
        SELECT *
        FROM holidays
        WHERE school_id = ?
        AND holiday_date = ?
        `,
        [
            schoolId,
            holidayDate
        ]
    );

};
exports.getByDateRange =
async (
    schoolId,
    startDate,
    endDate
) => {

    return await db.allAsync(

        `
        SELECT *
        FROM holidays
        WHERE school_id = ?
        AND holiday_date >= ?
        AND holiday_date <= ?
        ORDER BY holiday_date DESC
        `,

        [
            schoolId,
            startDate,
            endDate
        ]

    );

};
exports.create =
async (
    schoolId,
    holidayDate
) => {

    const result =
        await db.runAsync(
            `
            INSERT INTO holidays
            (
                school_id,
                holiday_date
            )
            VALUES
            (
                ?,
                ?
            )
            `,
            [
                schoolId,
                holidayDate
            ]
        );

    return await exports.getByDate(
        schoolId,
        holidayDate
    );

};

exports.delete =
async (
    schoolId,
    holidayDate
) => {

    const result =
        await db.runAsync(
            `
            DELETE FROM holidays
            WHERE school_id = ?
            AND holiday_date = ?
            `,
            [
                schoolId,
                holidayDate
            ]
        );

    return result.changes;

};