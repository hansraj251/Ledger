const bcrypt =
    require("bcrypt");

const jwt =
    require("jsonwebtoken");

const propertyAuthRepository =
    require("../repositories/propertyAuthRepository");


exports.login =
async (
    identifier,
    password
) => {

    const cleanIdentifier =
        String(
            identifier || ""
        ).trim();

    const cleanPassword =
        String(
            password || ""
        ).trim();

    if (
        !cleanIdentifier ||
        !cleanPassword
    ) {

        throw new Error(
            "Email/mobile and password are required"
        );

    }

    let user =
        await propertyAuthRepository
            .getByEmail(
                cleanIdentifier
            );

    if (
        !user
    ) {

        user =
            await propertyAuthRepository
                .getByMobile(
                    cleanIdentifier
                );

    }

    if (
        !user
    ) {

        throw new Error(
            "Invalid email/mobile or password"
        );

    }

    if (
        user.status !== "active"
    ) {

        throw new Error(
            "Your account is not active"
        );

    }

    const matched =
        await bcrypt.compare(
            cleanPassword,
            user.password
        );

    if (
        !matched
    ) {

        throw new Error(
            "Invalid email/mobile or password"
        );

    }

    await propertyAuthRepository
        .updateLastLogin(
            user.id
        );

    const token =
        jwt.sign(
            {
                propertyUserId:
                    user.id,

                propertyRole:
                    "seller"
            },

            process.env.JWT_SECRET,

            {
                expiresIn:
                    "7d"
            }
        );

    return {

        token,

        user: {

            id:
                user.id,

            name:
                user.name,

            email:
                user.email,

            mobile:
                user.mobile,

            status:
                user.status

        }

    };

};


exports.userExists =
async (
    email,
    mobile
) => {

    return await propertyAuthRepository
        .getByEmailOrMobile(
            email,
            mobile
        );

};

exports.getByEmailOrMobile = async (

    email,

    mobile

) => {

    return await propertyAuthRepository
        .getByEmailOrMobile(
            email,
            mobile
        );

};

exports.createUser =
async (
    name,
    email,
    mobile,
    password
) => {

    const cleanName =
        String(
            name || ""
        ).trim();

    const cleanEmail =
        String(
            email || ""
        ).trim()
        .toLowerCase();

    const cleanMobile =
        String(
            mobile || ""
        ).trim();

    const cleanPassword =
        String(
            password || ""
        ).trim();

    if (
        !cleanName
    ) {

        throw new Error(
            "Name is required"
        );

    }

    if (
        !cleanEmail &&
        !cleanMobile
    ) {

        throw new Error(
            "Email or mobile is required"
        );

    }

    if (
        cleanPassword.length < 8
    ) {

        throw new Error(
            "Password must be at least 8 characters"
        );

    }

    const existing =
        await propertyAuthRepository
            .getByEmailOrMobile(
                cleanEmail || null,
                cleanMobile || null
            );

    if (
        existing
    ) {

        throw new Error(
            "An account with this email or mobile already exists"
        );

    }

    const hashedPassword =
        await bcrypt.hash(
            cleanPassword,
            10
        );

    return await propertyAuthRepository
        .create(
            cleanName,
            cleanEmail || null,
            cleanMobile || null,
            hashedPassword
        );

};
exports.getProfile = async (
    userId
) => {

    const user =
        await propertyAuthRepository
            .getProfile(
                userId
            );


    if (!user) {

        throw new Error(
            "User profile not found"
        );

    }


    return user;

};


exports.updateProfile = async (
    userId,
    data
) => {

    const name =
        String(
            data.name || ""
        ).trim();

    const email =
        String(
            data.email || ""
        ).trim();

    const mobile =
        String(
            data.mobile || ""
        ).trim();


    if (!name) {

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
        email &&
        !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
            email
        )
    ) {

        throw new Error(
            "Invalid email address"
        );

    }


    if (
        mobile &&
        !/^[0-9+\-\s()]{7,20}$/.test(
            mobile
        )
    ) {

        throw new Error(
            "Invalid mobile number"
        );

    }


    const emailUser =
        email
            ? await propertyAuthRepository
                .getByEmail(email)
            : null;


    if (
        emailUser &&
        emailUser.id !== userId
    ) {

        throw new Error(
            "Email is already in use"
        );

    }


    const mobileUser =
        mobile
            ? await propertyAuthRepository
                .getByMobile(mobile)
            : null;


    if (
        mobileUser &&
        mobileUser.id !== userId
    ) {

        throw new Error(
            "Mobile number is already in use"
        );

    }


    return await propertyAuthRepository
        .updateProfile(
            userId,
            name,
            email || null,
            mobile || null
        );

};
exports.createUserFromOtp = async (

    name,
    email,
    mobile,
    passwordHash

) => {

    const cleanName =
        String(name || "").trim();

    const cleanEmail =
        String(email || "")
            .trim()
            .toLowerCase();

    const cleanMobile =
        String(mobile || "").trim();

    if (!cleanName) {

        throw new Error(
            "Name is required"
        );

    }

    if (!cleanEmail) {

        throw new Error(
            "Email is required"
        );

    }

    if (!passwordHash) {

        throw new Error(
            "Password is required"
        );

    }

    const existing =
        await propertyAuthRepository
            .getByEmailOrMobile(
                cleanEmail,
                cleanMobile || null
            );

    if (existing) {

        throw new Error(
            "An account with this email or mobile already exists"
        );

    }

    return await propertyAuthRepository.create(

        cleanName,

        cleanEmail,

        cleanMobile || null,

        passwordHash

    );

};
exports.getByEmail = async (
    email
) => {

    return await propertyAuthRepository
        .getByEmail(
            String(email || "")
                .trim()
                .toLowerCase()
        );

};


exports.updatePassword = async (
    userId,
    password
) => {

    const cleanPassword =
        String(
            password || ""
        );

    if (
        cleanPassword.length < 8
    ) {

        throw new Error(
            "Password must be at least 8 characters"
        );

    }

    const passwordHash =
        await bcrypt.hash(
            cleanPassword,
            10
        );

    return await propertyAuthRepository
        .updatePassword(
            userId,
            passwordHash
        );

};