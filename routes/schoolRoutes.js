const express =
    require("express");

const router =
    express.Router();

const schoolController =
    require("../controllers/schoolController");

const authMiddleware =
    require("../middlewares/authMiddleware");
const uploadSchoolLogo =
    require("../middlewares/uploadMiddleware");
router.get(
    "/me",
    authMiddleware,
    schoolController.getSchool
);
router.put(
    "/me",
    authMiddleware,
    schoolController.updateSchoolProfile
);
router.put(

    "/me/logo",

    authMiddleware,

    uploadSchoolLogo,

    schoolController.uploadSchoolLogo

);

module.exports =
    router;
