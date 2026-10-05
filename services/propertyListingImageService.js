const propertyListingRepository =
    require("../repositories/propertyListingRepository");

const propertyListingImageRepository =
    require("../repositories/propertyListingImageRepository");


const MAX_IMAGES =
    8;


exports.addImages =
async (
    listingId,
    sellerId,
    files
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

    if (
        !files ||
        files.length === 0
    ) {

        throw new Error(
            "At least one image is required"
        );

    }

    const existingImages =
        await propertyListingImageRepository
            .getByListingId(
                listingId
            );

    if (
        existingImages.length +
        files.length >
        MAX_IMAGES
    ) {

        throw new Error(
            `A listing can have maximum ${MAX_IMAGES} images`
        );

    }

    let hasCover =
        existingImages.some(
            image =>
                Number(
                    image.is_cover
                ) === 1
        );

    const createdImages = [];

    for (
        let index = 0;
        index < files.length;
        index++
    ) {

        const file =
            files[index];

        const isCover =
            hasCover
                ? 0
                : index === 0
                    ? 1
                    : 0;

        if (
            isCover === 1
        ) {

            hasCover = true;

        }

        const image =
            await propertyListingImageRepository
                .create(

                    listingId,

                    `/uploads/${file.filename}`,

                    existingImages.length +
                        index,

                    isCover

                );

        createdImages.push(
            image
        );

    }

    return createdImages;

};


exports.getImages =
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

    return await propertyListingImageRepository
        .getByListingId(
            listingId
        );

};


exports.setCover =
async (
    listingId,
    imageId,
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

    const image =
        await propertyListingImageRepository
            .getById(
                imageId
            );

    if (
        !image ||
        Number(
            image.listing_id
        ) !== Number(
            listingId
        )
    ) {

        throw new Error(
            "Image not found"
        );

    }

    await propertyListingImageRepository
        .clearCover(
            listingId
        );

    await propertyListingImageRepository
        .setCover(
            imageId,
            listingId
        );

    return await propertyListingImageRepository
        .getByListingId(
            listingId
        );

};


exports.deleteImage =
async (
    listingId,
    imageId,
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

    const image =
        await propertyListingImageRepository
            .getById(
                imageId
            );

    if (
        !image ||
        Number(
            image.listing_id
        ) !== Number(
            listingId
        )
    ) {

        throw new Error(
            "Image not found"
        );

    }

    const wasCover =
        Number(
            image.is_cover
        ) === 1;

    const changes =
        await propertyListingImageRepository
            .delete(
                imageId,
                listingId
            );

    if (
        changes === 0
    ) {

        throw new Error(
            "Unable to delete image"
        );

    }

    if (
        wasCover
    ) {

        const remainingImages =
            await propertyListingImageRepository
                .getByListingId(
                    listingId
                );

        if (
            remainingImages.length > 0
        ) {

            await propertyListingImageRepository
                .setCover(
                    remainingImages[0].id,
                    listingId
                );

        }

    }

    return await propertyListingImageRepository
        .getByListingId(
            listingId
        );

};
