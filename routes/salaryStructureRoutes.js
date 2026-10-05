const express =
    require("express");

const router =
    express.Router();

const salaryStructureController =
    require("../controllers/salaryStructureController");

const authMiddleware =
    require("../middlewares/authMiddleware");

router.get(
    "/",
    authMiddleware,
    salaryStructureController.getAllActiveSalaryStructures
);

router.get(
    "/staff/:teacherId",
    authMiddleware,
    salaryStructureController.getActiveSalaryStructure
);

router.get(
    "/staff/:teacherId/history",
    authMiddleware,
    salaryStructureController.getSalaryStructures
);

router.get(
    "/:id",
    authMiddleware,
    salaryStructureController.getSalaryStructure
);

router.post(
    "/",
    authMiddleware,
    salaryStructureController.createSalaryStructure
);

module.exports =
    router;