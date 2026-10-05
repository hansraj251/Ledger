const classService =
    require("../services/classService");
const dateUtil =
    require("../utils/date");
exports.getClasses =
async (
    req,
    res
) => {

    try {

        const classes =
            await classService.getClasses(
                req.user.schoolId
            );

        return res.json({

            success: true,

            classes

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};
exports.getClassesWithAttendance =
async (
    req,
    res
) => {

    try {

        const today =
    dateUtil.formatDate(
        new Date()
    );

        const classes =
            await classService
                .getClassesWithAttendance(
                    req.user.schoolId,
                    req.query.date ||
                        today
                );

        return res.json({

            success: true,

            classes

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};
exports.getClass =
async (
    req,
    res
) => {

    try {

        const classItem =
            await classService.getClass(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            class: classItem

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.createClass =
async (
    req,
    res
) => {

    try {

        const classItem =
            await classService.createClass(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Class created successfully",

            class: classItem

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};
exports.updateClass =
async (
    req,
    res
) => {

    try {

        const classItem =
            await classService.updateClass(
                req.user.schoolId,
                req.params.id,
                req.body
            );

        return res.json({

            success: true,

            message:
                "Class updated successfully",

            class: classItem

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.updateClassStatus =
async (
    req,
    res
) => {

    try {

        const classItem =
            await classService.updateClassStatus(
                req.user.schoolId,
                req.params.id,
                req.body.status
            );

        return res.json({

            success: true,

            message:
                "Class status updated successfully",

            class: classItem

        });

    }
    catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};
