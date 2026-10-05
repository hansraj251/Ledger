const express = require("express");

const router =
    express.Router();

const authMiddleware =
    require("../middlewares/authMiddleware");

const restaurantController =
    require("../controllers/restaurantController");

router.get(
    "/me",
    authMiddleware,
    restaurantController.getRestaurant
);

module.exports =
    router;