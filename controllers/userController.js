const userService =
    require("../services/userService");

exports.createAttendanceUser =
async (
    req,
    res
) => {

    try {

        const user =
            await userService
                .createAttendanceUser(
                    req.user.schoolId,
                    req.body
                );

        return res.status(201).json({

            success: true,

            message:
                "Attendance user created successfully",

            user

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
exports.getAttendanceUsers =
async (
    req,
    res
) => {

    try {

        const users =
            await userService
                .getAttendanceUsers(
                    req.user.schoolId
                );

        return res.json({

            success: true,

            users

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
exports.getAttendanceClasses =
async (
    req,
    res
) => {

    try {

        const classes =
            await userService
                .getAttendanceClasses(
                    req.user.userId
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