const superAdminAccountService =
    require("../services/superAdminAccountService");

exports.getAll =
async (
    req,
    res
) => {

    try {

        const accounts =
            await superAdminAccountService
                .getAll();

        res.json({

            success: true,

            accounts

        });

    }
    catch (error) {

        res.status(500).json({

            success: false,

            message: error.message

        });

    }

};

exports.create =
async (
    req,
    res
) => {

    try {

        await superAdminAccountService
            .create(
                req.body
            );

        res.json({

            success: true,

            message:
                "Super Admin created successfully"

        });

    }
    catch (error) {

        res.status(400).json({

            success: false,

            message: error.message

        });

    }

};

exports.update =
async (
    req,
    res
) => {

    try {

        await superAdminAccountService
    .update(
        req.params.id,
        req.body,
        req.user.id
    );

        res.json({

            success: true,

            message:
                "Super Admin updated successfully"

        });

    }
    catch (error) {

        res.status(400).json({

            success: false,

            message: error.message

        });

    }

};

exports.changePassword =
async (
    req,
    res
) => {

    try {

        await superAdminAccountService
            .changePassword(
                req.params.id,
                req.body.password
            );

        res.json({

            success: true,

            message:
                "Password updated successfully"

        });

    }
    catch (error) {

        res.status(400).json({

            success: false,

            message: error.message

        });

    }

};

exports.delete =
async (
    req,
    res
) => {

    try {

        await superAdminAccountService
    .delete(
        req.params.id,
        req.user.id
    );

        res.json({

            success: true,

            message:
                "Super Admin deleted successfully"

        });

    }
    catch (error) {

        res.status(400).json({

            success: false,

            message: error.message

        });

    }

};