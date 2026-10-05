const propertyContactRequestService =
    require("../services/propertyContactRequestService");


exports.create =
async (
    req,
    res
) => {

    try {

        const {
            request,
            buyerAccessToken
        } =
            await propertyContactRequestService
                .createRequest(
                    Number(
                        req.params.id
                    ),
                    req.body.buyerName,
                    req.body.buyerMobile,
                    req.body.message
                );

        return res.status(201).json({

            success: true,

            message:
                "Contact request sent successfully.",

            request: {

                id:
                    request.id,

                listing_id:
                    request.listing_id,

                status:
                    request.status,

                created_at:
                    request.created_at

            },

            buyerAccessToken

        });

    }
    catch (err) {

        console.error(
            "Property contact request error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to send contact request"

        });

    }

};

exports.getMine =
async (
    req,
    res
) => {

    try {

        const requests =
            await propertyContactRequestService
                .getSellerRequests(
                    req.propertyUserId
                );

        return res.json({

            success: true,

            requests

        });

    }
    catch (err) {

        console.error(
            "Property contact requests error:",
            err
        );

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to load contact requests"

        });

    }

};


exports.updateStatus =
async (
    req,
    res
) => {

    try {

        const request =
            await propertyContactRequestService
                .updateStatus(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId,
                    req.body.status
                );

        return res.json({

            success: true,

            message:
                "Contact request status updated successfully.",

            request

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update contact request"

        });

    }

};
exports.deleteRequest =
async (
    req,
    res
) => {

    try {

        await propertyContactRequestService
            .deleteRequest(
                Number(
                    req.params.id
                ),
                req.propertyUserId
            );

        return res.json({
            success: true,
            message:
                "Contact request deleted successfully."
        });

    }
    catch (err) {

        return res.status(400).json({
            success: false,
            message:
                err.message ||
                "Unable to delete contact request"
        });

    }
};
exports.shareContact =
async (
    req,
    res
) => {

    try {

        const request =
            await propertyContactRequestService
                .shareContact(
                    Number(
                        req.params.id
                    ),
                    req.propertyUserId
                );

        return res.json({

            success: true,

            message:
                "Contact details shared successfully.",

            request

        });

    }
    catch (err) {

        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to share contact details"

        });

    }

};
exports.getSharedContact =
async (
    req,
    res
) => {

    try {

        const authHeader =
            req.headers.authorization || "";

        if (
            !authHeader.startsWith(
                "Bearer "
            )
        ) {
            return res.status(401).json({

                success: false,

                message:
                    "Buyer authentication required"

            });
        }

        const buyerAccessToken =
            authHeader
                .substring(7)
                .trim();

        if (
            !buyerAccessToken
        ) {
            return res.status(401).json({

                success: false,

                message:
                    "Buyer authentication required"

            });
        }

        const request =
            await propertyContactRequestService
                .getSharedContact(
                    Number(
                        req.params.id
                    ),
                    buyerAccessToken
                );

        return res.json({

            success: true,

            contact: request

        });

    }
    catch (err) {

        return res.status(404).json({

            success: false,

            message:
                err.message ||
                "Contact details are not available"

        });

    }

};