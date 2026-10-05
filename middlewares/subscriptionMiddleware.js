const restaurantService =
    require("../services/restaurantService");

exports.getRestaurant =
async (
    req,
    res
) => {

    try {

        const restaurant =
            await restaurantService
                .getRestaurant(
                    req.user.restaurantId
                );

        return res.json({

            success: true,

            restaurant

        });

    }
    catch (err) {

        return res.status(500).json({

            success: false,

            message:
                err.message

        });

    }

};