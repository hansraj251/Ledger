const bcrypt =
    require("bcrypt");

const userRepository =
    require("../repositories/userRepository");

exports.resetPassword =
async (
    email,
    password
) => {

    const passwordHash =
        await bcrypt.hash(
            password,
            10
        );

    await userRepository
        .updatePassword(
            email,
            passwordHash
        );

};