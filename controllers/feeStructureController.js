const feeStructureService =
    require("../services/feeStructureService");

exports.getFeeStructures =
async (
    req,
    res
) => {

    try {

        const feeStructures =
    await feeStructureService.getFeeStructures(
        req.user.schoolId,
        req.params.studentId,
        req.query.academicYear
    );

        return res.json({

            success: true,

            feeStructures

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

exports.getActiveFeeStructures =
async (
    req,
    res
) => {

    try {

        const feeStructures =
    await feeStructureService.getActiveFeeStructures(
        req.user.schoolId,
        req.params.studentId,
        req.query.academicYear
    );

        return res.json({

            success: true,

            feeStructures

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

exports.getAllActiveFeeStructures =
async (
    req,
    res
) => {

    try {

        const feeStructures =
    await feeStructureService.getAllActiveFeeStructures(
        req.user.schoolId,
        req.query.academicYear
    );

        return res.json({

            success: true,

            feeStructures

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

exports.getFeeStructure =
async (
    req,
    res
) => {

    try {

        const feeStructure =
            await feeStructureService.getFeeStructure(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            feeStructure

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

exports.createFeeStructure =
async (
    req,
    res
) => {

    try {

        const feeStructure =
            await feeStructureService.createFeeStructure(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Fee structure created successfully",

            feeStructure

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

exports.updateFeeStructure =
async (
    req,
    res
) => {

    try {

        const feeStructure =
            await feeStructureService.updateFeeStructure(
                req.user.schoolId,
                req.params.id,
                req.body
            );

        return res.json({

            success: true,

            message:
                "Fee structure updated successfully",

            feeStructure

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

exports.updateFeeStructureStatus =
async (
    req,
    res
) => {

    try {

        const feeStructure =
            await feeStructureService.updateFeeStructureStatus(
                req.user.schoolId,
                req.params.id,
                req.body.status
            );

        return res.json({

            success: true,

            message:
                "Fee structure status updated successfully",

            feeStructure

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