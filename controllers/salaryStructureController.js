const salaryStructureService =
    require("../services/salaryStructureService");

exports.getActiveSalaryStructure =
async (
    req,
    res
) => {

    try {

        const salaryStructure =
            await salaryStructureService.getActiveSalaryStructure(
                req.user.schoolId,
                req.params.teacherId
            );

        return res.json({

    success: true,

    salaryStructure:
        salaryStructure || null

});

    }
    catch (err) {

        console.error(err);

        return res.status(404).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.getSalaryStructures =
async (
    req,
    res
) => {

    try {

        const salaryStructures =
            await salaryStructureService.getSalaryStructures(
                req.user.schoolId,
                req.params.teacherId
            );

        return res.json({

            success: true,

            salaryStructures

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

exports.getSalaryStructure =
async (
    req,
    res
) => {

    try {

        const salaryStructure =
            await salaryStructureService.getSalaryStructure(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            salaryStructure

        });

    }
    catch (err) {

        console.error(err);

        return res.status(404).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.createSalaryStructure =
async (
    req,
    res
) => {

    try {

        const salaryStructure =
            await salaryStructureService.createSalaryStructure(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Salary structure created successfully",

            salaryStructure

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

exports.getAllActiveSalaryStructures =
async (
    req,
    res
) => {

    try {

        const salaryStructures =
            await salaryStructureService.getAllActiveSalaryStructures(
                req.user.schoolId
            );

        return res.json({

            success: true,

            salaryStructures

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