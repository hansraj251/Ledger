const express =
    require("express");

const router =
    express.Router();

const attendanceController =
    require("../controllers/attendanceController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.get(
    "/today",
    authMiddleware,
    attendanceController.getTodaySummary
);
router.get(
    "/student/:studentId/history",
    authMiddleware,
    attendanceController.getStudentHistory
);
router.get(
    "/",
    authMiddleware,
    attendanceController.getByDate
);

router.post(
    "/",
    authMiddleware,
    attendanceController.saveAttendance
);

module.exports =
    router;
