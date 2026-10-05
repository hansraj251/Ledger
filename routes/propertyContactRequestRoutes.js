const express =
    require("express");

const router =
    express.Router();

const propertyContactRequestController =
    require("../controllers/propertyContactRequestController");

const propertyAuthMiddleware =
    require("../middlewares/propertyAuthMiddleware");


// Buyer — no login required

router.post(
    "/:id",
    propertyContactRequestController.create
);


// Seller — view own contact requests

router.get(
    "/mine",
    propertyAuthMiddleware,
    propertyContactRequestController.getMine
);


// Seller — update request status

router.patch(
    "/:id/status",
    propertyAuthMiddleware,
    propertyContactRequestController.updateStatus
);


// Seller — delete closed contact request

router.delete(
    "/:id",
    propertyAuthMiddleware,
    propertyContactRequestController.deleteRequest
);


module.exports =
    router;