const express =
    require("express");

const router =
    express.Router();

const userController =
    require("../controllers/userController");

const authMiddleware =
    require("../middlewares/authMiddleware");
router.get(
    "/attendance",
    authMiddleware,
    userController.getAttendanceUsers
);
router.get(
    "/attendance/classes",
    authMiddleware,
    userController.getAttendanceClasses
);
router.post(
    "/attendance",
    authMiddleware,
    userController.createAttendanceUser
);

module.exports =
    router;