const propertyListingImageService =
    require("../services/propertyListingImageService");


exports.upload =
async (
    req,
    res
) => {

    try {

        const images =
            await propertyListingImageService
                .addImages(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId,
                    req.files
                );

        return res.status(201).json({

            success: true,

            message:
                "Images uploaded successfully.",

            images

        });

    }
    catch (err) {

        console.error(
            "Property image upload error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to upload images"

        });

    }

};


exports.getImages =
async (
    req,
    res
) => {

    try {

        const images =
            await propertyListingImageService
                .getImages(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId
                );

        return res.json({

            success: true,

            images

        });

    }
    catch (err) {

        return res.status(404).json({

            success: false,

            message:
                err.message ||
                "Unable to load images"

        });

    }

};


exports.setCover =
async (
    req,
    res
) => {

    try {

        const images =
            await propertyListingImageService
                .setCover(
                    Number(
                        req.params.id
                    ),
                    Number(
                        req.params.imageId
                    ),
                    req.propertyUserId
                );

        return res.json({

            success: true,

            message:
                "Cover image updated successfully.",

            images

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update cover image"

        });

    }

};


exports.remove =
async (
    req,
    res
) => {

    try {

        const images =
            await propertyListingImageService
                .deleteImage(
                    Number(
                        req.params.id
                    ),
                    Number(
                        req.params.imageId
                    ),
                    req.propertyUserId
                );

        return res.json({

            success: true,

            message:
                "Image deleted successfully.",

            images

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to delete image"

        });

    }

};
