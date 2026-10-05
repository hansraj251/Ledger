const salaryPaymentService =
    require("../services/salaryPaymentService");

exports.getPendingSalary =
async (
    req,
    res
) => {

    try {

        const result =
            await salaryPaymentService.getPendingSalary(
                req.user.schoolId
            );

        return res.json({

            success: true,

            ...result

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

exports.createPayment =
async (
    req,
    res
) => {

    try {

        const payment =
            await salaryPaymentService.createPayment(
                req.user.schoolId,
                req.body
            );

        return res.json({

            success: true,

            payment

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

exports.getPaymentHistory =
async (
    req,
    res
) => {

    try {

        const history =
            await salaryPaymentService.getPaymentHistory(
                req.user.schoolId,
                req.query.salaryMonth
            );

        return res.json({

            success: true,

            history

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