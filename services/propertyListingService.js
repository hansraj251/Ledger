const propertyListingRepository =
    require("../repositories/propertyListingRepository");


const ALLOWED_PRICE_TYPES = [
    "fixed",
    "negotiable",
    "call_for_price"
];


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


/*
|--------------------------------------------------------------------------
| Validate Listing Data
|--------------------------------------------------------------------------
*/

const validateListingData = (
    data
) => {

    const title =
        cleanText(
            data.title
        );

    const subtitle =
        cleanText(
            data.subtitle
        );

    const description =
        cleanText(
            data.description
        );


    /*
    |--------------------------------------------------------------------------
    | Prohibited Text
    |--------------------------------------------------------------------------
    */

    const combinedText =
        `${title} ${subtitle} ${description}`
            .toLowerCase();


    const prohibitedPatterns = [

        /\bpay\s+token\b/,

        /\bpay\s+advance\b/,

        /\bsend\s+money\b/,

        /\btransfer\s+money\b/,

        /\bpay\s+first\b/,

        /\bpayment\s+first\b/,

        /\bguaranteed\s+return\b/,

        /\bguaranteed\s+profit\b/,

        /\bdouble\s+your\s+money\b/,

        /\b100%\s+guarantee\b/,

        /\bno\s+risk\b/,

        /\bfully\s+guaranteed\b/,

        /\burgent\s+payment\b/,

        /\bpay\s+immediately\b/,

        /\bpay\s+now\b/

    ];


    const hasProhibitedText =
        prohibitedPatterns.some(
            pattern =>
                pattern.test(
                    combinedText
                )
        );


    if (
        hasProhibitedText
    ) {

        throw new Error(
            "Listing contains prohibited payment or misleading text"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Title
    |--------------------------------------------------------------------------
    */

    if (
        !title
    ) {

        throw new Error(
            "Title is required"
        );

    }


    if (
        title.length > 150
    ) {

        throw new Error(
            "Title cannot exceed 150 characters"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Subtitle
    |--------------------------------------------------------------------------
    */

    if (
        subtitle.length > 200
    ) {

        throw new Error(
            "Subtitle cannot exceed 200 characters"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Description
    |--------------------------------------------------------------------------
    */

    if (
        description.length > 5000
    ) {

        throw new Error(
            "Description cannot exceed 5000 characters"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Price Type
    |--------------------------------------------------------------------------
    */

    const priceType =
        cleanText(
            data.priceType
        ) || "fixed";


    if (
        !ALLOWED_PRICE_TYPES.includes(
            priceType
        )
    ) {

        throw new Error(
            "Invalid price type"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Price
    |--------------------------------------------------------------------------
    */

    let price = null;


    if (
        data.price !== null &&
        data.price !== undefined &&
        data.price !== ""
    ) {

        price =
            Number(
                data.price
            );


        if (
            !Number.isFinite(
                price
            ) ||
            price < 0
        ) {

            throw new Error(
                "Price must be a valid non-negative amount"
            );

        }

    }
    /*
|--------------------------------------------------------------------------
| Rent / Month
|--------------------------------------------------------------------------
*/

let rentAmount = null;

if (
    data.rentAmount !== null &&
    data.rentAmount !== undefined &&
    data.rentAmount !== ""
) {

    rentAmount =
        Number(
            data.rentAmount
        );

    if (
        !Number.isFinite(
            rentAmount
        ) ||
        rentAmount < 0
    ) {

        throw new Error(
            "Rent must be a valid non-negative amount"
        );

    }

}
/*
|--------------------------------------------------------------------------
| Sale Price OR Rent
|--------------------------------------------------------------------------
*/

const hasSalePrice =
    data.price !== null &&
    data.price !== undefined &&
    data.price !== "";

const hasRentAmount =
    data.rentAmount !== null &&
    data.rentAmount !== undefined &&
    data.rentAmount !== "";

if (
    hasSalePrice &&
    hasRentAmount
) {

    throw new Error(
        "Enter either Sale Price or Rent / Month, not both"
    );

}

if (
    !hasSalePrice &&
    !hasRentAmount
) {

    throw new Error(
        "Enter either Sale Price or Rent / Month"
    );

}


    /*
    |--------------------------------------------------------------------------
    | Token
    |--------------------------------------------------------------------------
    */

    const tokenRequired =
        Boolean(
            data.tokenRequired
        );


    let tokenAmount = null;


    if (
        tokenRequired
    ) {

        if (
            data.tokenAmount === null ||
            data.tokenAmount === undefined ||
            data.tokenAmount === ""
        ) {

            throw new Error(
                "Token amount is required"
            );

        }


        tokenAmount =
            Number(
                data.tokenAmount
            );


        if (
            !Number.isFinite(
                tokenAmount
            ) ||
            tokenAmount <= 0
        ) {

            throw new Error(
                "Token amount must be greater than zero"
            );

        }

    }


    /*
    |--------------------------------------------------------------------------
    | Contact Preference
    |--------------------------------------------------------------------------
    |
    | show    = Seller contact can be shown directly
    | request = Buyer must request contact
    |
    */

    const contactPreference =
        data.contactPreference === "request"
            ? "request"
            : "show";


    return {

        title,

        subtitle:
            subtitle || null,

        description:
            description || null,

        price,

        priceType,

        rentAmount,

        tokenRequired:
            tokenRequired
                ? 1
                : 0,

        tokenAmount,

        contactPreference,
        location:
    data.location?.trim() || null,

address:
    data.address?.trim() || null,

city:
    data.city?.trim() || null,

state:
    data.state?.trim() || null,

pincode:
    data.pincode?.trim() || null

    };

};


/*
|--------------------------------------------------------------------------
| Create Listing
|--------------------------------------------------------------------------
*/

exports.createListing =
async (
    sellerId,
    data
) => {

    const listing =
        validateListingData(
            data
        );


    return await propertyListingRepository
        .create(

            sellerId,

            listing.title,

            listing.subtitle,

            listing.description,

            listing.price,

            listing.priceType,

            listing.rentAmount,

            listing.tokenRequired,

            listing.tokenAmount,

            "published",

            "approved",

            listing.contactPreference,
            listing.location,
listing.address,
listing.city,
listing.state,
listing.pincode

        );

};


/*
|--------------------------------------------------------------------------
| Get Seller Listings
|--------------------------------------------------------------------------
*/

exports.getSellerListings =
async (
    sellerId
) => {

    return await propertyListingRepository
        .getSellerListings(
            sellerId
        );

};


/*
|--------------------------------------------------------------------------
| Get Listing For Seller
|--------------------------------------------------------------------------
*/

exports.getListingForSeller =
async (
    listingId,
    sellerId
) => {

    const listing =
        await propertyListingRepository
            .getByIdForSeller(

                listingId,

                sellerId

            );


    if (
        !listing
    ) {

        throw new Error(
            "Listing not found"
        );

    }


    return listing;

};


/*
|--------------------------------------------------------------------------
| Update Listing
|--------------------------------------------------------------------------
*/

exports.updateListing =
async (
    listingId,
    sellerId,
    data
) => {

    await exports
        .getListingForSeller(

            listingId,

            sellerId

        );


    const listing =
        validateListingData(
            data
        );


    return await propertyListingRepository
        .update(

            listingId,

            sellerId,

            listing.title,

            listing.subtitle,

            listing.description,

            listing.price,

            listing.priceType,

            listing.rentAmount,

            listing.tokenRequired,

            listing.tokenAmount,

            listing.contactPreference,
            listing.location,
listing.address,
listing.city,
listing.state,
listing.pincode

        );

};


/*
|--------------------------------------------------------------------------
| Update Listing Status
|--------------------------------------------------------------------------
*/

exports.updateListingStatus =
async (
    listingId,
    sellerId,
    status
) => {

    const allowedStatuses = [

        "draft",

        "published",

        "hidden",

        "sold",

        "closed"

    ];


    if (
        !allowedStatuses.includes(
            status
        )
    ) {

        throw new Error(
            "Invalid listing status"
        );

    }


    /*
    |--------------------------------------------------------------------------
    | Verify ownership
    |--------------------------------------------------------------------------
    */

    await exports
        .getListingForSeller(

            listingId,

            sellerId

        );


    /*
    |--------------------------------------------------------------------------
    | Direct status update
    |--------------------------------------------------------------------------
    |
    | Moderation approval is currently disabled.
    |
    */

    return await propertyListingRepository
        .updateStatus(

            listingId,

            sellerId,

            status

        );

};


/*
|--------------------------------------------------------------------------
| Delete Listing
|--------------------------------------------------------------------------
*/

exports.deleteListing =
async (
    listingId,
    sellerId
) => {

    await exports
        .getListingForSeller(

            listingId,

            sellerId

        );


    const changes =
        await propertyListingRepository
            .delete(

                listingId,

                sellerId

            );


    if (
        changes === 0
    ) {

        throw new Error(
            "Unable to delete listing"
        );

    }


    return true;

};


/*
|--------------------------------------------------------------------------
| Get Published Listings
|--------------------------------------------------------------------------
*/

exports.getPublishedListings =
async () => {

    return await propertyListingRepository
        .getPublishedListings();

};


/*
|--------------------------------------------------------------------------
| Get Published Listing By ID
|--------------------------------------------------------------------------
*/

exports.getPublishedListingById =
async (
    listingId
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


    return listing;

};

/*
|--------------------------------------------------------------------------
| Save Listing
|--------------------------------------------------------------------------
*/

exports.saveListing = async (
    userId,
    listingId
) => {

    const listing =
        await propertyListingRepository
            .getById(
                listingId
            );

    if (!listing) {

        throw new Error(
            "Property not found."
        );

    }


    await propertyListingRepository
        .saveListing(
            userId,
            listingId
        );


    return {
        success: true,
        saved: true
    };

};


/*
|--------------------------------------------------------------------------
| Unsave Listing
|--------------------------------------------------------------------------
*/

exports.unsaveListing = async (
    userId,
    listingId
) => {

    const listing =
        await propertyListingRepository
            .getById(
                listingId
            );

    if (!listing) {

        throw new Error(
            "Property not found."
        );

    }


    await propertyListingRepository
        .unsaveListing(
            userId,
            listingId
        );


    return {
        success: true,
        saved: false
    };

};


/*
|--------------------------------------------------------------------------
| Check Saved Listing
|--------------------------------------------------------------------------
*/

exports.isListingSaved = async (
    userId,
    listingId
) => {

    const listing =
        await propertyListingRepository
            .getById(
                listingId
            );

    if (!listing) {

        throw new Error(
            "Property not found."
        );

    }


    const saved =
        await propertyListingRepository
            .isListingSaved(
                userId,
                listingId
            );


    return {
        success: true,
        saved
    };

};


/*
|--------------------------------------------------------------------------
| Get Saved Listings
|--------------------------------------------------------------------------
*/

exports.getSavedListings = async (
    userId
) => {

    return await propertyListingRepository
        .getSavedListings(
            userId
        );

};