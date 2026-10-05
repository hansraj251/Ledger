const feePaymentService =
    require("../services/feePaymentService");

exports.getPayments =
async (
    req,
    res
) => {

    try {

        const payments =
            await feePaymentService.getPayments(
                req.user.schoolId,
                req.params.studentId,
                req.query.academicYear
            );

        return res.json({

            success: true,

            payments

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

exports.getTotalPaid =
async (
    req,
    res
) => {

    try {

       const totalPaid =
    await feePaymentService.getTotalPaid(
        req.user.schoolId,
        req.params.studentId,
        req.query.academicYear

    );

        return res.json({

            success: true,

            totalPaid

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

exports.getPaidByFeeStructure =
async (
    req,
    res
) => {

    try {

        const paidByFeeStructure =
            await feePaymentService
                .getPaidByFeeStructure(
                    req.user.schoolId,
                    req.params.studentId,
                    req.query.academicYear
                );

        return res.json({

            success: true,

            paidByFeeStructure

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

exports.getAllPayments =
async (
    req,
    res
) => {

    try {

        const payments =
    await feePaymentService.getAllPayments(
        req.user.schoolId,
        req.query.academicYear
    );

        return res.json({

            success: true,

            payments

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

exports.getPayment =
async (
    req,
    res
) => {

    try {

        const payment =
            await feePaymentService.getPayment(
                req.user.schoolId,
                req.params.id
            );

        return res.json({

            success: true,

            payment

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
exports.getReceiptData =

async (

    req,

    res

) => {

    try {

        const payment =

            await feePaymentService
                .getReceiptData(

                    req.user.schoolId,

                    req.params.paymentId

                );

        return res.json({

            success: true,

            payment

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
exports.createPayment =
async (
    req,
    res
) => {

    try {

        const payment =
            await feePaymentService.createPayment(
                req.user.schoolId,
                req.body
            );

        return res.status(201).json({

            success: true,

            message:
                "Fee payment recorded successfully",

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
exports.getPendingFees =
async (
    req,
    res
) => {

    try {

        const pendingFees =
            await feePaymentService.getPendingFees(
                req.user.schoolId
            );

        return res.json({

            success: true,

            ...pendingFees

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