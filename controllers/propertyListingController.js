const propertyListingService =
    require("../services/propertyListingService");


exports.create =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyListingService
                .createListing(
                    req.propertyUserId,
                    req.body
                );

        return res.status(201).json({

            success: true,

            message:
                "Listing created successfully.",

            listing

        });

    }
    catch (err) {

        console.error(
            "Property listing create error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to create listing"

        });

    }

};


exports.getMine =
async (
    req,
    res
) => {

    try {

        const listings =
            await propertyListingService
                .getSellerListings(
                    req.propertyUserId
                );

        return res.json({

            success: true,

            listings

        });

    }
    catch (err) {

        console.error(
            "Property seller listings error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                "Unable to load listings"

        });

    }

};


exports.getMineById =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyListingService
                .getListingForSeller(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId
                );

        return res.json({

            success: true,

            listing

        });

    }
    catch (err) {

        return res.status(404).json({

            success: false,

            message:
                err.message ||
                "Listing not found"

        });

    }

};


exports.update =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyListingService
                .updateListing(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId,
                    req.body
                );

        return res.json({

            success: true,

            message:
                "Listing updated successfully.",

            listing

        });

    }
    catch (err) {

        console.error(
            "Property listing update error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update listing"

        });

    }

};


exports.updateStatus =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyListingService
                .updateListingStatus(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId,
                    req.body.status
                );

        return res.json({

            success: true,

            message:
                "Listing status updated successfully.",

            listing

        });

    }
    catch (err) {

        console.error(
            "Property listing status error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update listing status"

        });

    }

};


exports.remove =
async (
    req,
    res
) => {

    try {

        await propertyListingService
            .deleteListing(
                Number(
                    req.params.id
                ),
                req.propertyUserId
            );

        return res.json({

            success: true,

            message:
                "Listing deleted successfully."

        });

    }
    catch (err) {

        console.error(
            "Property listing delete error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to delete listing"

        });

    }

};


exports.getPublished =
async (
    req,
    res
) => {

    try {

        const listings =
            await propertyListingService
                .getPublishedListings();

        return res.json({

            success: true,

            listings

        });

    }
    catch (err) {

        console.error(
            "Property public listings error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                "Unable to load listings"

        });

    }

};


exports.getPublishedListingById =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyListingService
                .getPublishedListingById(
                    Number(
                        req.params.id
                    )
                );

        return res.json({

            success: true,

            listing

        });

    }
    catch (err) {

        return res.status(404).json({

            success: false,

            message:
                err.message ||
                "Listing not found"

        });

    }

};
/*
|--------------------------------------------------------------------------
| Save Listing
|--------------------------------------------------------------------------
*/

exports.saveListing =
async (
    req,
    res
) => {

    try {

        const listingId =
            Number(
                req.params.id
            );

        if (
            !Number.isInteger(listingId) ||
            listingId <= 0
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Invalid listing ID."

            });

        }


        const result =
            await propertyListingService
                .saveListing(
                    req.propertyUserId,
                    listingId
                );


        return res.json({

            success: true,

            message:
                "Saved successfully.",

            saved:
                result.saved

        });

    }
    catch (err) {

        console.error(
            "Property listing save error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to save property."

        });

    }

};


/*
|--------------------------------------------------------------------------
| Unsave Listing
|--------------------------------------------------------------------------
*/

exports.unsaveListing =
async (
    req,
    res
) => {

    try {

        const listingId =
            Number(
                req.params.id
            );

        if (
            !Number.isInteger(listingId) ||
            listingId <= 0
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Invalid listing ID."

            });

        }


        const result =
            await propertyListingService
                .unsaveListing(
                    req.propertyUserId,
                    listingId
                );


        return res.json({

            success: true,

            message:
                "Property removed from saved.",

            saved:
                result.saved

        });

    }
    catch (err) {

        console.error(
            "Property listing unsave error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to remove saved property."

        });

    }

};


/*
|--------------------------------------------------------------------------
| Get Saved Listings
|--------------------------------------------------------------------------
*/

exports.getSavedListings =
async (
    req,
    res
) => {

    try {

        const listings =
            await propertyListingService
                .getSavedListings(
                    req.propertyUserId
                );


        return res.json({

            success: true,

            listings

        });

    }
    catch (err) {

        console.error(
            "Property saved listings error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                "Unable to load saved properties."

        });

    }

};