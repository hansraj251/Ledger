const express =
    require("express");

const router =
    express.Router();

const ledgerBusinessController =
    require("../controllers/ledgerBusinessController");

const authMiddleware =
    require("../middlewares/authMiddleware");

const alignAccountMiddleware =
    require("../middlewares/alignAccountMiddleware");


// Get Ledger business

router.get(
    "/",
    authMiddleware,
    alignAccountMiddleware,
    ledgerBusinessController.getBusiness
);


// Create Ledger business

router.post(
    "/",
    authMiddleware,
    alignAccountMiddleware,
    ledgerBusinessController.createBusiness
);


// Update Ledger business

router.put(
    "/:id",
    authMiddleware,
    alignAccountMiddleware,
    ledgerBusinessController.updateBusiness
);


module.exports =
    router;
