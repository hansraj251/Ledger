const subscriptionService =
    require("../services/subscriptionService"); 

exports.getSubscription =
async (req, res) => {

    try {

        const subscription =
            await subscriptionService
                .getSubscription(
                    req.user.restaurantId
                );

        return res.json({

            success: true,

            subscription

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.createOrder =
    async (req, res) => {

        try {

            const pricingId =
                Number(req.body.pricingId);

            if (!pricingId) {

                return res.status(400).json({

                    success: false,

                    message: "Pricing is required."

                });

            }

            const payment =
                await subscriptionService
                    .createOrder(
                        req.user.restaurantId,
                        pricingId
                    );

            return res.json({

                success: true,

                ...payment

            });

        } catch (err) {

            console.error(err);

            return res.status(500).json({

                success: false,

                message: err.message

            });

        }

    };
exports.verifyPayment =
    async (req, res) => {

        try {

            const result =
                await subscriptionService
                    .verifyPayment(
                        req.user.restaurantId,
                        req.body
                    );

            return res.json({

                success: true,

                data: result

            });

        } catch (err) {

            console.error(err);

            return res.status(500).json({

                success: false,

                message: err.message

            });

        }

    };    
exports.getPlans =
    async (req, res) => {

        try {

            const plans =
                await subscriptionService
                    .getPlans();

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
exports.getSchoolPlans =
    async (req, res) => {

        try {

            const plans =
                await subscriptionService
                    .getSchoolPlans();

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
exports.getSchoolSubscription =
async (req, res) => {

    try {

        const subscription =
            await subscriptionService
                .getSchoolSubscription(
                    req.user.schoolId
                );

        return res.json({

            success: true,

            subscription

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.createSchoolOrder =
async (req, res) => {

    try {

        const pricingId =
            Number(req.body.pricingId);

        if (!pricingId) {

            return res.status(400).json({

                success: false,

                message:
                    "Pricing is required."

            });

        }

        const payment =
            await subscriptionService
                .createSchoolOrder(
                    req.user.schoolId,
                    pricingId
                );

        return res.json({

            success: true,

            ...payment

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};

exports.verifySchoolPayment =
async (req, res) => {

    try {

        const result =
            await subscriptionService
                .verifySchoolPayment(
                    req.user.schoolId,
                    req.body
                );

        return res.json({

            success: true,

            data: result

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};
exports.webhook =
    async (req, res) => {

        try {

            await subscriptionService.processWebhook(

    req.body,

    req.headers[
        "x-razorpay-signature"
    ]

);

            return res.json({

                success: true

            });

        } catch (err) {

            console.error(err);

            return res.status(400).json({

                success: false,

                message: err.message

            });

        }

    };    

exports.getActiveDevices = async (req, res) => {

    try {

        return res.json({
            success: true,
            sessions
        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({
            success: false,
            message: "Failed to load active devices."
        });

    }

};
exports.logoutActiveDevice = async (
    req,
    res
) => {

    try {

        return res.json({
            success: true,
            message: "Device logged out successfully."
        });

    } catch (err) {

        console.error(err);

        return res.status(400).json({
            success: false,
            message: err.message
        });

    }

};
exports.getSubscriptionSyncData = async (
    req,
    res
) => {

    try {

        const data =
    await subscriptionService
        .getSubscriptionSyncData(
            req.user.restaurantId
        );

        return res.json({

            success: true,

            data

        });

    } catch (err) {

        console.error(err);

        return res.status(500).json({

            success: false,

            message: err.message

        });

    }

};