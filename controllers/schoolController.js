const schoolService =
    require("../services/schoolService");

exports.getSchool =
async (
    req,
    res
) => {

    try {

        const school =
            await schoolService.getSchool(
                req.user.schoolId
            );

        return res.json({

            success: true,

            school

        });

    }
    catch (err) {

        return res.status(404).json({

            success: false,

            message:
                err.message

        });

    }

};
exports.updateSchoolProfile =
async (
    req,
    res
) => {

    try {

        const school =
            await schoolService.updateProfile(
                req.user.schoolId,
                req.body
            );

        return res.json({

            success: true,

            school

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
exports.uploadSchoolLogo =
async (
    req,
    res
) => {

    try {

        if (
            !req.file
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "School logo is required"

            });

        }

        const school =
            await schoolService.updateLogo(
                req.user.schoolId,
                `/uploads/${req.file.filename}`
            );

        return res.json({

            success: true,

            school

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