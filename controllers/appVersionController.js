const appVersionService =
    require("../services/appVersionService");

exports.getLatestVersion =
async (
    req,
    res
) => {

    try {

        const data =
            await appVersionService
                .getLatestVersion();

        return res.json({

            success: true,

            ...data

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.setLatestVersion =
async (
    req,
    res
) => {

    try {

        const data =
            await appVersionService
                .setLatestVersion(
                    req.body.version
                );

        return res.json({

            success: true,

            message:
                "Latest version updated successfully.",

            ...data

        });

    } catch (err) {

        console.error(err);

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};