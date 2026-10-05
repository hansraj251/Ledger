const propertyListingRepository =
    require("../repositories/propertyListingRepository");

const propertyContactRequestRepository =
    require("../repositories/propertyContactRequestRepository");
const crypto =
    require("crypto");

const cleanText = (
    value
) => {

    if (
        value === null ||
        value === undefined
    ) {
        return "";
    }

    return String(
        value
    ).trim();

};


exports.createRequest =
async (
    listingId,
    buyerName,
    buyerMobile,
    message
) => {

    const listing =
        await propertyListingRepository
            .getPublishedById(
                listingId
            );

    if (
        !listing
    ) {

        throw new Error(
            "Listing not found"
        );

    }

    const name =
        cleanText(
            buyerName
        );

    const mobile =
        cleanText(
            buyerMobile
        );

    const cleanMessage =
        cleanText(
            message
        );

    if (
        !name
    ) {

        throw new Error(
            "Name is required"
        );

    }

    if (
        name.length > 100
    ) {

        throw new Error(
            "Name cannot exceed 100 characters"
        );

    }

    if (
        !mobile
    ) {

        throw new Error(
            "Mobile number is required"
        );

    }

    if (
        !/^[0-9+\-\s()]{7,20}$/.test(
            mobile
        )
    ) {

        throw new Error(
            "Invalid mobile number"
        );

    }

    if (
        cleanMessage.length > 1000
    ) {

        throw new Error(
            "Message cannot exceed 1000 characters"
        );

    }
        const buyerAccessToken =
        crypto.randomBytes(
            32
        ).toString("hex");

    const buyerAccessTokenHash =
        crypto
            .createHash("sha256")
            .update(
                buyerAccessToken
            )
            .digest("hex");

    const buyerAccessTokenExpiresAt =
        new Date(
            Date.now() +
            7 * 24 * 60 * 60 * 1000
        ).toISOString();

        const request =
        await propertyContactRequestRepository
            .create(
                listingId,
                name,
                mobile,
                cleanMessage || null,
                buyerAccessTokenHash,
                buyerAccessTokenExpiresAt
            );
                return {
        request,
        buyerAccessToken
    };

};


exports.getSellerRequests =
async (
    sellerId
) => {

    return await propertyContactRequestRepository
        .getSellerRequests(
            sellerId
        );

};


exports.updateStatus =
async (
    requestId,
    sellerId,
    status
) => {

    const allowedStatuses = [
        "new",
        "contacted",
        "closed"
    ];

    if (
        !allowedStatuses.includes(
            status
        )
    ) {

        throw new Error(
            "Invalid contact request status"
        );

    }

    const request =
        await propertyContactRequestRepository
            .updateStatus(
                requestId,
                sellerId,
                status
            );

    if (
        !request
    ) {

        throw new Error(
            "Contact request not found"
        );

    }

    return request;

};
exports.shareContact =
async (
    requestId,
    sellerId
) => {

    const request =
        await propertyContactRequestRepository
            .updateContactShared(
                requestId,
                sellerId
            );

    if (
        !request
    ) {
        throw new Error(
            "Contact request not found"
        );
    }

    return {
        id:
            request.id,

        listing_id:
            request.listing_id,

        buyer_name:
            request.buyer_name,

        buyer_mobile:
            request.buyer_mobile,

        message:
            request.message,

        status:
            request.status,

        contacted_at:
            request.contacted_at,

        created_at:
            request.created_at,

        updated_at:
            request.updated_at,

        contact_shared:
            request.contact_shared,

        contact_shared_at:
            request.contact_shared_at,

        listing_title:
            request.listing_title,

        seller_id:
            request.seller_id
    };
};
exports.deleteRequest =
async (
    requestId,
    sellerId
) => {

    const deleted =
        await propertyContactRequestRepository
            .deleteRequest(
                requestId,
                sellerId
            );

    if (!deleted) {

        throw new Error(
            "Only your closed contact requests can be deleted"
        );

    }

    return true;
};
exports.getSharedContact =
async (
    requestId,
    buyerAccessToken
) => {

    const token =
        cleanText(
            buyerAccessToken
        );

    if (
        !token
    ) {
        throw new Error(
            "Buyer authentication required"
        );
    }

    const request =
        await propertyContactRequestRepository
            .getSharedContact(
                requestId
            );

    if (
        !request
    ) {
        throw new Error(
            "Contact details are not available"
        );
    }

    const tokenHash =
        crypto
            .createHash("sha256")
            .update(
                token
            )
            .digest("hex");

    if (
        tokenHash !==
        request.buyer_access_token_hash
    ) {
        throw new Error(
            "Invalid buyer access token"
        );
    }

    if (
        !request.buyer_access_token_expires_at ||
        new Date(
            request.buyer_access_token_expires_at
        ).getTime() <=
            Date.now()
    ) {
        throw new Error(
            "Buyer access token has expired"
        );
    }

    return {
        request_id:
            request.request_id,

        listing_id:
            request.listing_id,

        contact_shared:
            request.contact_shared,

        contact_shared_at:
            request.contact_shared_at,

        seller_name:
            request.seller_name,

        seller_email:
            request.seller_email,

        seller_mobile:
            request.seller_mobile
    };
};