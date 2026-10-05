const express =
    require("express");

const router =
    express.Router();

const ledgerPartyController =
    require("../controllers/ledgerPartyController");

const authMiddleware =
    require("../middlewares/authMiddleware");

const alignAccountMiddleware =
    require("../middlewares/alignAccountMiddleware");


// Get all parties
router.get(
    "/",
    authMiddleware,
    alignAccountMiddleware,
    ledgerPartyController.getParties
);


// Get single party
router.get(
    "/:id",
    authMiddleware,
    alignAccountMiddleware,
    ledgerPartyController.getParty
);


// Create party
router.post(
    "/",
    authMiddleware,
    alignAccountMiddleware,
    ledgerPartyController.createParty
);


// Update party
router.put(
    "/:id",
    authMiddleware,
    alignAccountMiddleware,
    ledgerPartyController.updateParty
);


// Deactivate party
router.delete(
    "/:id",
    authMiddleware,
    alignAccountMiddleware,
    ledgerPartyController.deleteParty
);


module.exports =
    router;
