const express =
    require("express");

const router =
    express.Router();

const feePaymentController =
    require("../controllers/feePaymentController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.get(
    "/student/:studentId",
    authMiddleware,
    feePaymentController.getPayments
);

router.get(
    "/student/:studentId/total",
    authMiddleware,
    feePaymentController.getTotalPaid
);
router.get(
    "/student/:studentId/paid-by-head",
    authMiddleware,
    feePaymentController.getPaidByFeeStructure
);

router.get(
    "/",
    authMiddleware,
    feePaymentController.getAllPayments
);


router.get(
    "/pending",
    authMiddleware,
    feePaymentController.getPendingFees
);
router.get(

    "/:paymentId/receipt",

    authMiddleware,

    feePaymentController.getReceiptData

);

router.get(
    "/:id",
    authMiddleware,
    feePaymentController.getPayment
);

router.post(
    "/",
    authMiddleware,
    feePaymentController.createPayment
);

module.exports =
    router;