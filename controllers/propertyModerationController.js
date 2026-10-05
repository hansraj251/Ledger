const propertyModerationService =
    require("../services/propertyModerationService");


exports.getPending =
async (
    req,
    res
) => {

    try {

        const listings =
            await propertyModerationService
                .getPendingListings();

        return res.json({

            success: true,

            listings

        });

    }
    catch (err) {

        console.error(
            "Property moderation list error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to load pending listings"

        });

    }

};


exports.getOne =
async (
    req,
    res
) => {

    try {

        const listing =
            await propertyModerationService
                .getListing(
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
                "Property listing not found"

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
            await propertyModerationService
                .updateModeration(
                    Number(
                        req.params.id
                    ),
                    req.body.moderationStatus
                );

        return res.json({

            success: true,

            message:
                `Listing ${listing.moderation_status} successfully.`,

            listing

        });

    }
    catch (err) {

        console.error(
            "Property moderation update error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update listing moderation"

        });

    }

};
