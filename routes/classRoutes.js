const express =
    require("express");

const router =
    express.Router();

const authMiddleware =
    require("../middlewares/authMiddleware");

const classController =
    require("../controllers/classController");

router.get(
    "/",
    authMiddleware,
    classController.getClasses
);


router.get(
    "/attendance-summary",
    authMiddleware,
    classController.getClassesWithAttendance
);


router.get(
    "/:id",
    authMiddleware,
    classController.getClass
);

router.post(
    "/",
    authMiddleware,
    classController.createClass
);

router.put(
    "/:id",
    authMiddleware,
    classController.updateClass
);

router.patch(
    "/:id/status",
    authMiddleware,
    classController.updateClassStatus
);

module.exports =
    router;
