const express =
    require("express");

const router =
    express.Router();

const authMiddleware =
    require("../middlewares/authMiddleware");

const salaryPaymentController =
    require("../controllers/salaryPaymentController");

router.get(
    "/pending",
    authMiddleware,
    salaryPaymentController.getPendingSalary
);

router.get(
    "/history",
    authMiddleware,
    salaryPaymentController.getPaymentHistory
);

router.post(
    "/",
    authMiddleware,
    salaryPaymentController.createPayment
);

module.exports =
    router;