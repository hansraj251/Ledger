const restaurantRepository =
    require("../repositories/restaurantRepository");

exports.getRestaurant =
async (
    restaurantId
) => {

    const restaurant =
        await restaurantRepository
            .getById(
                restaurantId
            );

    if (
        !restaurant
    ) {

        throw new Error(
            "Account not found"
        );

    }

    return restaurant;

};