const holidayRepository =
    require("../repositories/holidayRepository");

exports.getHoliday =
async (
    schoolId,
    holidayDate
) => {

    return await holidayRepository.getByDate(
        schoolId,
        holidayDate
    );

};

exports.setHoliday =
async (
    schoolId,
    holidayDate
) => {

    const existing =
        await holidayRepository.getByDate(
            schoolId,
            holidayDate
        );

    if (
        existing
    ) {

        return existing;

    }

    return await holidayRepository.create(
        schoolId,
        holidayDate
    );

};

exports.removeHoliday =
async (
    schoolId,
    holidayDate
) => {

    await holidayRepository.delete(
        schoolId,
        holidayDate
    );

};