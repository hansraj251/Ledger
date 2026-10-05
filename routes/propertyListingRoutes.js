const express =
    require("express");

const router =
    express.Router();

const propertyListingController =
    require("../controllers/propertyListingController");

const propertyAuthMiddleware =
    require("../middlewares/propertyAuthMiddleware");


// Public listings
router.get(
    "/",
    propertyListingController.getPublished
);
router.get(
    "/public/:id",
    propertyListingController.getPublishedListingById
);

// Seller listings
router.post(
    "/",
    propertyAuthMiddleware,
    propertyListingController.create
);

router.get(
    "/mine",
    propertyAuthMiddleware,
    propertyListingController.getMine
);
router.get(

    "/saved",

    propertyAuthMiddleware,

    propertyListingController.getSavedListings

);
router.post(
    "/:id/save",
    propertyAuthMiddleware,
    propertyListingController.saveListing
);

router.delete(
    "/:id/save",
    propertyAuthMiddleware,
    propertyListingController.unsaveListing
);
router.get(
    "/mine/:id",
    propertyAuthMiddleware,
    propertyListingController.getMineById
);

router.put(
    "/:id",
    propertyAuthMiddleware,
    propertyListingController.update
);

router.patch(
    "/:id/status",
    propertyAuthMiddleware,
    propertyListingController.updateStatus
);

router.delete(
    "/:id",
    propertyAuthMiddleware,
    propertyListingController.remove
);


module.exports =
    router;
