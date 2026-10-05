const express =
    require("express");

const router =
    express.Router();

const propertyAuthController =
    require("../controllers/propertyAuthController");

const propertyAuthMiddleware =
    require("../middlewares/propertyAuthMiddleware");


router.post(
    "/signup",
    propertyAuthController.signup
);

router.post(
    "/verify-otp",
    propertyAuthController.verifyOtp
);

router.post(
    "/login",
    propertyAuthController.login
);

router.post(
    "/forgot-password",
    propertyAuthController.forgotPassword
);

router.post(
    "/reset-password",
    propertyAuthController.resetPassword
);


router.get(
    "/profile",
    propertyAuthMiddleware,
    propertyAuthController.getProfile
);


router.put(
    "/profile",
    propertyAuthMiddleware,
    propertyAuthController.updateProfile
);


module.exports =
    router;