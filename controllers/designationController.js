const designationService =
    require("../services/designationService");

exports.getDesignations =
async (
    req,
    res
) => {

    try {

        const designations =
            await designationService.getDesignations(
                req.user.schoolId
            );

        return res.json({

            success: true,

            designations

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

exports.getDesignation =
async (
    req,
    res
) => {

    try {

        const designation =
            await designationService.getDesignation(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            designation

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

exports.createDesignation =
async (
    req,
    res
) => {

    try {

        const designation =
            await designationService.createDesignation(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Designation created successfully",

            designation

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

exports.updateDesignationStatus =
async (
    req,
    res
) => {

    try {

        const designation =
            await designationService.updateDesignationStatus(
                req.user.schoolId,
                req.params.id,
                req.body.status
            );

        return res.json({

            success: true,

            message:
                "Designation status updated successfully",

            designation

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