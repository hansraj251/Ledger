const ledgerBusinessService =
    require("../services/ledgerBusinessService");

exports.getBusiness =
async (
    req,
    res
) => {

    try {

        const business =
            await ledgerBusinessService
                .getBusiness(
                    req.alignAccountId
                );

        return res.json({

            success: true,

            business

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

exports.createBusiness =
async (
    req,
    res
) => {

    try {

        const business =
            await ledgerBusinessService
                .createBusiness(

                    req.alignAccountId,

                    req.body.businessName,

                    req.body.mobile,

                    req.body.address,

                    req.body.currency

                );

        return res.status(201).json({

            success: true,

            business

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};

exports.updateBusiness =
async (
    req,
    res
) => {

    try {

        const business =
            await ledgerBusinessService
                .updateBusiness(

                    req.params.id,

                    req.alignAccountId,

                    req.body.businessName,

                    req.body.mobile,

                    req.body.address,

                    req.body.currency

                );

        return res.json({

            success: true,

            business

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message

        });

    }

};
