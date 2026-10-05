const express =
    require("express");

const router =
    express.Router();

const authMiddleware =
    require("../middlewares/authMiddleware");

const superAdminMiddleware =
    require("../middlewares/superAdminMiddleware");

const superAdminAccountController =
    require("../controllers/superAdminAccountController");

router.get(
    "/",
    authMiddleware,
    superAdminMiddleware,
    superAdminAccountController.getAll
);

router.post(
    "/",
    authMiddleware,
    superAdminMiddleware,
    superAdminAccountController.create
);

router.put(
    "/:id",
    authMiddleware,
    superAdminMiddleware,
    superAdminAccountController.update
);

router.patch(
    "/:id/password",
    authMiddleware,
    superAdminMiddleware,
    superAdminAccountController.changePassword
);

router.delete(
    "/:id",
    authMiddleware,
    superAdminMiddleware,
    superAdminAccountController.delete
);

module.exports =
    router;