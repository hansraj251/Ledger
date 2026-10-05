const teacherService =
    require("../services/teacherService");

exports.getTeachers =
async (
    req,
    res
) => {

    try {

        const teachers =
            await teacherService.getTeachers(
                req.user.schoolId
            );

        return res.json({

            success: true,

            teachers

        });

    }
    catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.getTeacher =
async (
    req,
    res
) => {

    try {

        const teacher =
            await teacherService.getTeacher(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            teacher

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
exports.createTeacher =
async (
    req,
    res
) => {

    try {

        const teacher =
            await teacherService.createTeacher(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Teacher created successfully",

            teacher

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
exports.updateTeacher =
async (
    req,
    res
) => {

    try {

        const teacher =
            await teacherService.updateTeacher(
                req.user.schoolId,
                req.params.id,
                req.body
            );

        return res.json({

            success: true,

            message:
                "Teacher updated successfully",

            teacher

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
exports.updateTeacherStatus =
async (
    req,
    res
) => {

    try {

        const teacher =
            await teacherService.updateTeacherStatus(
                req.user.schoolId,
                req.params.id,
                req.body.status
            );

        return res.json({

            success: true,

            message:
                "Teacher status updated successfully",

            teacher

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