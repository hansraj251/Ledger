const planService =
    require("../services/planService");

exports.getPlans = async (
    req,
    res
) => {

    try {

        const plans =
            await planService.getPlans();

        return res.json({

            success: true,

            plans

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.getSchoolPlans = async (
    req,
    res
) => {

    try {

        const plans =
            await planService.getSchoolPlans();

        return res.json({

            success: true,

            plans

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.getAllPlans = async (
    req,
    res
) => {

    try {

        const plans =
            await planService.getAllPlans();

        return res.json({

            success: true,

            plans

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.getPlan = async (
    req,
    res
) => {

    try {

        const plan =
            await planService.getPlan(
                req.params.id
            );

        return res.json({

            success: true,

            plan

        });

    } catch (err) {

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.updatePlan = async (
    req,
    res
) => {

    try {

        await planService.updatePlan(

    req.params.id,

    req.body.display_name,

    req.body.description,

    req.body.sort_order,

    req.body.limit_value,

    req.body.status

);

        return res.json({

            success: true

        });

    } catch (err) {

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.createPlan = async (
    req,
    res
) => {

    try {

        const planId =
    await planService.createPlan(

        req.body.slug,

        req.body.display_name,

        req.body.description,

        req.body.sort_order,

        req.body.limit_value,

        req.body.status,

        req.body.plan_type

    );

        return res.json({

            success: true,

            planId

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};