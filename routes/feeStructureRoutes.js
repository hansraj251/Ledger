const express =
    require("express");

const router =
    express.Router();

const feeStructureController =
    require("../controllers/feeStructureController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.get(
    "/student/:studentId",
    authMiddleware,
    feeStructureController.getFeeStructures
);

router.get(
    "/student/:studentId/active",
    authMiddleware,
    feeStructureController.getActiveFeeStructures
);

router.get(
    "/",
    authMiddleware,
    feeStructureController.getAllActiveFeeStructures
);

router.get(
    "/:id",
    authMiddleware,
    feeStructureController.getFeeStructure
);

router.post(
    "/",
    authMiddleware,
    feeStructureController.createFeeStructure
);

router.put(
    "/:id",
    authMiddleware,
    feeStructureController.updateFeeStructure
);

router.put(
    "/:id/status",
    authMiddleware,
    feeStructureController.updateFeeStructureStatus
);

module.exports =
    router;