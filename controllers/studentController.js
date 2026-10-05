const studentService =
    require("../services/studentService");

exports.getStudents =
async (
    req,
    res
) => {

    try {

        const students =
    await studentService.getStudents(
        req.user.schoolId,
        req.user.userId,
        req.user.role
    );

        return res.json({

            success: true,

            students

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

exports.getStudent =
async (
    req,
    res
) => {

    try {

        const student =
            await studentService.getStudent(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            student

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

exports.createStudent =
async (
    req,
    res
) => {

    try {

        const student =
            await studentService.createStudent(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Student created successfully",

            student

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

exports.updateStudent =
async (
    req,
    res
) => {

    try {

        const student =
            await studentService.updateStudent(
                req.user.schoolId,
                req.params.id,
                req.body
            );

        return res.json({

            success: true,

            message:
                "Student updated successfully",

            student

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
exports.updateStudentStatus =
async (
    req,
    res
) => {

    try {

        const student =
            await studentService.updateStudentStatus(
                req.user.schoolId,
                req.params.id,
                req.body.status
            );

        return res.json({

            success: true,

            message:
                "Student status updated successfully",

            student

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