const bcrypt =
    require("bcrypt");

const jwt =
    require("jsonwebtoken");

const authRepository =
    require("../repositories/authRepository");

exports.login =
async (
    identifier,
    password
) => {

    let user =
        await authRepository
            .getByEmail(
                identifier
            );

    if (
        !user
    ) {

        user =
            await authRepository
                .getByUsername(
                    identifier
                );

    }

    if (
        !user
    ) {

        throw new Error(
            "Invalid user ID or password"
        );

    }

    const matched =
        await bcrypt.compare(
            password.trim(),
            user.password.trim()
        );

    if (
        !matched
    ) {

        throw new Error(
    "Invalid user ID or password"
);

    }

    const token =
    jwt.sign(
        {
            userId:
                user.id,

            restaurantId:
                user.restaurant_id,

            schoolId:
                user.school_id,

            businessType:
                user.business_type,

            role:
                user.role
        },
        process.env.JWT_SECRET,
        {
            expiresIn:
                "7d"
        }
    );

    return {

        token,

        user

    };

};

exports.userExists =
async (
    email,
    mobile
) => {

    return await authRepository
        .getByEmailOrMobile(
            email,
            mobile
        );

};
exports.existsByEmail =
async (
    email
) => {

    return await authRepository
        .existsByEmail(
            email
        );

};