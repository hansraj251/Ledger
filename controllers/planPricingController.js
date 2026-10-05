const planPricingService =
    require("../services/planPricingService");

exports.getPricing = async (
    req,
    res
) => {

    try {

        const pricing =
            await planPricingService.getPricing(
                req.params.planId
            );

        return res.json({

            success: true,

            pricing

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.createPricing = async (
    req,
    res
) => {

    try {

        const pricingId =
            await planPricingService.createPricing(

                req.body.plan_id,

                req.body.duration_days,

                req.body.price,

                req.body.currency,

                req.body.status

            );

        return res.json({

            success: true,

            pricingId

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.updatePricing = async (
    req,
    res
) => {

    try {

        await planPricingService.updatePricing(

            req.params.id,

            req.body.duration_days,

            req.body.price,

            req.body.currency,

            req.body.status

        );

        return res.json({

            success: true

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.getAllPricing = async (
    req,
    res
) => {

    try {

        const pricing =
            await planPricingService
                .getAllPricing();

        return res.json({

            success: true,

            pricing

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};