const express =
    require("express");

const router =
    express.Router();

const teacherController =
    require("../controllers/teacherController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.post(
    "/",
    authMiddleware,
    teacherController.createTeacher
);
router.put(
    "/:id",
    authMiddleware,
    teacherController.updateTeacher
);
router.patch(
    "/:id/status",
    authMiddleware,
    teacherController.updateTeacherStatus
);

router.get(
    "/",
    authMiddleware,
    teacherController.getTeachers
);

router.get(
    "/:id",
    authMiddleware,
    teacherController.getTeacher
);

module.exports =
    router;
