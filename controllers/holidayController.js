const holidayService =
    require("../services/holidayService");

exports.getHoliday =
async (
    req,
    res
) => {

    try {

        const holiday =
            await holidayService.getHoliday(
                req.user.schoolId,
                req.query.date
            );

        return res.json({

            success: true,

            holiday

        });

    }
    catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.setHoliday =
async (
    req,
    res
) => {
    if (
    req.user.role ===
    "attendance"
) {

    return res.status(403).json({

        success: false,

        message:
            "Attendance users cannot manage holidays"

    });

}

    try {

        const holiday =
            await holidayService.setHoliday(
                req.user.schoolId,
                req.body.date
            );

        return res.json({

            success: true,

            holiday

        });

    }
    catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.removeHoliday =
async (
    req,
    res
) => {
    if (
    req.user.role ===
    "attendance"
) {

    return res.status(403).json({

        success: false,

        message:
            "Attendance users cannot manage holidays"

    });

}

    try {

        await holidayService.removeHoliday(
            req.user.schoolId,
            req.body.date
        );

        return res.json({

            success: true

        });

    }
    catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};