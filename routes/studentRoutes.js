const express =
    require("express");

const router =
    express.Router();

const studentController =
    require("../controllers/studentController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.get(
    "/",
    authMiddleware,
    studentController.getStudents
);
router.post(
    "/",
    authMiddleware,
    studentController.createStudent
);
router.put(
    "/:id",
    authMiddleware,
    studentController.updateStudent
);
router.put(
    "/:id/status",
    authMiddleware,
    studentController.updateStudentStatus
);
router.get(
    "/:id",
    authMiddleware,
    studentController.getStudent
);


module.exports =
    router;
