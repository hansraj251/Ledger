const express = require("express");

const router = express.Router();

const superAdminController =
    require("../controllers/superAdminController");
const planController =
    require("../controllers/planController");   
const planPricingController =
    require("../controllers/planPricingController");  
const authMiddleware =
require("../middlewares/authMiddleware");       
const superAdminMiddleware =
require("../middlewares/superAdminMiddleware");  
const appVersionController =
    require("../controllers/appVersionController");  

router.post(
    "/login",
    superAdminController.login
);

router.get(
    "/dashboard",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.dashboard
);

router.get(
    "/restaurants",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.getRestaurants
);
router.get(

    "/schools",

    authMiddleware,

    superAdminMiddleware,

    superAdminController.getSchools

);
router.get(

    "/schools/:schoolId",

    authMiddleware,

    superAdminMiddleware,

    superAdminController.getSchool

);

router.put(

    "/schools/:schoolId/subscription",

    authMiddleware,

    superAdminMiddleware,

    superAdminController.updateSchoolSubscription

);
router.get(
    "/payments",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.getPaymentHistory
);
router.get(
    "/restaurants/:restaurantId",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.getRestaurant
);

router.put(
    "/restaurants/:restaurantId/subscription",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.updateRestaurantSubscription
);

router.get(
    "/restaurants/:restaurantId/active-sessions",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.getActiveSessions
);

router.post(
    "/sessions/:sessionId/force-logout",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.forceLogout
);

router.get(
    "/plans",
    authMiddleware,
    superAdminMiddleware,
    planController.getPlans
);
router.get(
    "/school-plans",
    authMiddleware,
    superAdminMiddleware,
    planController.getSchoolPlans
);
router.get(
    "/all-plans",
    authMiddleware,
    superAdminMiddleware,
    planController.getAllPlans
);
router.post(
    "/plans",
    authMiddleware,
    superAdminMiddleware,
    planController.createPlan
);

router.get(
    "/plans/:id",
    authMiddleware,
    superAdminMiddleware,
    planController.getPlan
);

router.put(
    "/plans/:id",
    authMiddleware,
    superAdminMiddleware,
    planController.updatePlan
);

router.get(
    "/plans/:planId/pricing",
    authMiddleware,
    superAdminMiddleware,
    planPricingController.getPricing
);

router.get(
    "/pricing",
    authMiddleware,
    superAdminMiddleware,
    planPricingController.getAllPricing
);

router.post(
    "/pricing",
    authMiddleware,
    superAdminMiddleware,
    planPricingController.createPricing
);

router.put(
    "/pricing/:id",
    authMiddleware,
    superAdminMiddleware,
    planPricingController.updatePricing
);

router.get(
    "/backup",
    authMiddleware,
    superAdminMiddleware,
    superAdminController.downloadBackup
);
router.get(
    "/app-version",
    authMiddleware,
    superAdminMiddleware,
    appVersionController.getLatestVersion
);


router.put(
    "/app-version",
    authMiddleware,
    superAdminMiddleware,
    appVersionController.setLatestVersion
);

module.exports = router;