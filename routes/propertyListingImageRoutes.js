const express =
    require("express");

const router =
    express.Router();

const propertyListingImageController =
    require("../controllers/propertyListingImageController");

const propertyAuthMiddleware =
    require("../middlewares/propertyAuthMiddleware");

const uploadPropertyImages =
    require("../middlewares/propertyListingUploadMiddleware");


// Upload multiple images
router.post(
    "/:id/images",
    propertyAuthMiddleware,
    uploadPropertyImages,
    propertyListingImageController.upload
);


// Get seller's listing images
router.get(
    "/:id/images",
    propertyAuthMiddleware,
    propertyListingImageController.getImages
);


// Change cover image
router.patch(
    "/:id/images/:imageId/cover",
    propertyAuthMiddleware,
    propertyListingImageController.setCover
);


// Delete image
router.delete(
    "/:id/images/:imageId",
    propertyAuthMiddleware,
    propertyListingImageController.remove
);


module.exports =
    router;
