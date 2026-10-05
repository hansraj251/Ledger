const propertyModerationRepository =
    require("../repositories/propertyModerationRepository");


exports.getPendingListings =
async () => {

    return await propertyModerationRepository
        .getPendingListings();

};


exports.getListing =
async (
    listingId
) => {

    const listing =
        await propertyModerationRepository
            .getById(
                listingId
            );

    if (
        !listing
    ) {

        throw new Error(
            "Property listing not found"
        );

    }

    return listing;

};


exports.updateModeration =
async (
    listingId,
    moderationStatus
) => {

    const allowedStatuses = [
        "approved",
        "rejected"
    ];

    if (
        !allowedStatuses.includes(
            moderationStatus
        )
    ) {

        throw new Error(
            "Invalid moderation status"
        );

    }

    const listing =
        await exports.getListing(
            listingId
        );

    if (
        listing.moderation_status !==
            "pending"
    ) {

        throw new Error(
            `Listing is already ${listing.moderation_status}`
        );

    }

    const updated =
        await propertyModerationRepository
            .updateModeration(
                listingId,
                moderationStatus
            );

    if (
        !updated
    ) {

        throw new Error(
            "Unable to update listing moderation status"
        );

    }

    return updated;

};
