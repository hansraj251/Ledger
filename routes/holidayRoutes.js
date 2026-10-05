const express =
    require("express");

const router =
    express.Router();

const holidayController =
    require("../controllers/holidayController");

const authMiddleware =
    require("../middlewares/authMiddleware");


router.get(
    "/",
    authMiddleware,
    holidayController.getHoliday
);


router.post(
    "/",
    authMiddleware,
    holidayController.setHoliday
);


router.delete(
    "/",
    authMiddleware,
    holidayController.removeHoliday
);


module.exports =
    router;