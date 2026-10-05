const express =
    require("express");

const router =
    express.Router();

const propertyModerationController =
    require("../controllers/propertyModerationController");

const authMiddleware =
    require("../middlewares/authMiddleware");

const superAdminMiddleware =
    require("../middlewares/superAdminMiddleware");


// Super Admin — pending property listings
router.get(
    "/pending",
    authMiddleware,
    superAdminMiddleware,
    propertyModerationController.getPending
);


// Super Admin — view one listing
router.get(
    "/:id",
    authMiddleware,
    superAdminMiddleware,
    propertyModerationController.getOne
);


// Super Admin — approve/reject listing
router.patch(
    "/:id",
    authMiddleware,
    superAdminMiddleware,
    propertyModerationController.update
);


module.exports =
    router;
