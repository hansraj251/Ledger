const express =
    require("express");

const router =
    express.Router();

const posController =
    require("../controllers/posController");

const auth =
    require("../middlewares/authMiddleware");

router.get(
    "/latest",
    auth,
    posController.getLatest
);

router.get(
    "/download",
    auth,
    posController.download
);

module.exports =
    router;