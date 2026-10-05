const express =
    require("express");

const router =
    express.Router();

const authMiddleware =
    require("../middlewares/authMiddleware");

const alignAccountMiddleware =
    require("../middlewares/alignAccountMiddleware");

const ledgerTransactionController =
    require("../controllers/ledgerTransactionController");

router.use(
    authMiddleware,
    alignAccountMiddleware
);

router.get(
    "/party/:partyId",
    ledgerTransactionController.getTransactions
);

router.get(
    "/party/:partyId/summary",
    ledgerTransactionController.getPartySummary
);

router.get(
    "/party/:partyId/:transactionId",
    ledgerTransactionController.getTransaction
);

router.post(
    "/party/:partyId",
    ledgerTransactionController.createTransaction
);

router.put(
    "/party/:partyId/:transactionId",
    ledgerTransactionController.updateTransaction
);

router.delete(
    "/party/:partyId/:transactionId",
    ledgerTransactionController.deleteTransaction
);

module.exports = router;
