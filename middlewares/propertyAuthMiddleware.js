const jwt =
    require("jsonwebtoken");

module.exports =
async (
    req,
    res,
    next
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
                    "Authentication required"

            });

        }

        const token =
            authHeader.substring(7).trim();

        if (
            !token
        ) {

            return res.status(401).json({

                success: false,

                message:
                    "Authentication required"

            });

        }

        const decoded =
            jwt.verify(
                token,
                process.env.JWT_SECRET
            );

        if (
            !decoded.propertyUserId ||
            decoded.propertyRole !==
                "seller"
        ) {

            return res.status(403).json({

                success: false,

                message:
                    "Property seller access required"

            });

        }

        req.propertyUserId =
            decoded.propertyUserId;

        req.propertyRole =
            decoded.propertyRole;

        next();

    }
    catch (err) {

        console.error(
            "Property auth error:",
            err.message
        );

        return res.status(401).json({

            success: false,

            message:
                "Invalid or expired authentication token"

        });

    }

};
