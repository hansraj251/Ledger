const express =
    require("express");


const router =
    express.Router();


const designationController =
    require("../controllers/designationController");


const authMiddleware =
    require("../middlewares/authMiddleware");


router.get(
    "/",
    authMiddleware,
    designationController.getDesignations
);


router.get(
    "/:id",
    authMiddleware,
    designationController.getDesignation
);


router.post(
    "/",
    authMiddleware,
    designationController.createDesignation
);


router.patch(
    "/:id/status",
    authMiddleware,
    designationController.updateDesignationStatus
);


module.exports =
    router;