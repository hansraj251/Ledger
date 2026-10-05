const attendanceService =
    require("../services/attendanceService");
const dateUtil =
    require("../utils/date");
exports.getByDate =
async (
    req,
    res
) => {

    try {

        const attendance =
            await attendanceService.getByDate(
                req.user.schoolId,
                req.query.attendanceDate
            );

        return res.json({

            success: true,

            attendance

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

exports.saveAttendance =
async (
    req,
    res
) => {

    try {

        const attendance =
    await attendanceService.saveAttendance(
        req.user.schoolId,
        req.body,
        req.user.userId,
        req.user.role
    );

return res.json({

    success: true,

    message:
        "Attendance saved successfully",

    attendance

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
exports.getStudentHistory =
async (
    req,
    res
) => {

    try {

        const attendance =
            await attendanceService.getStudentHistory(
                req.user.schoolId,
                req.params.studentId,
                req.query.startDate,
                req.query.endDate
            );

        return res.json({

            success: true,

            attendance

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
exports.getTodaySummary =
async (
    req,
    res
) => {

    try {

        const today =
    dateUtil.formatDate(
        new Date()
    );

        const summary =
            await attendanceService
                .getTodaySummary(
                    req.user.schoolId,
                    today
                );

        return res.json({

            success: true,

            ...summary

        });

    }
    catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message:
                err.message ||
                "Unable to load today's attendance"

        });

    }

};