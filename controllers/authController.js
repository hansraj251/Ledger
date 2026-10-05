const bcrypt = require("bcrypt");

const authService =
    require("../services/authService");

const authSignupService =
    require("../services/authSignupService");

const alignAccountService =

    require("../services/alignAccountService");
const otpService =
    require("../services/otpService");  
const passwordResetService =
    require("../services/passwordResetService");      
exports.signup = async (req, res) => {

    const {
        businessType,
        restaurantName,
        ownerName,
        email,
        mobile,
        password
    } = req.body;

    if (
    businessType !== "food" &&
    businessType !== "school"
) {

    return res.status(400).json({

        success: false,

        message:
            "Invalid business type"

    });

}

    if (!restaurantName || restaurantName.trim() === "") {
        return res.status(400).json({
            success: false,
            message: "Restaurant name is required"
        });
    }

    if (!ownerName || ownerName.trim() === "") {
        return res.status(400).json({
            success: false,
            message: "Owner name is required"
        });
    }

    if (!email || email.trim() === "") {
        return res.status(400).json({
            success: false,
            message: "Email is required"
        });
    }

    if (!mobile || mobile.trim() === "") {
        return res.status(400).json({
            success: false,
            message: "Mobile number is required"
        });
    }

    if (!password || password.length < 8) {
        return res.status(400).json({
            success: false,
            message: "Password must be at least 8 characters"
        });
    }
try {

    const existingUser =
        await authService
            .userExists(
                email,
                mobile
            );

    if (
        existingUser
    ) {

        return res.status(400).json({

            success: false,

            message:
                "Email or Mobile already registered"

        });

    }

    const hashedPassword =
        await bcrypt.hash(
            password,
            10
        );

    const otp =
        otpService.generateOtp();

    const expiresAt =
        otpService.generateExpiry();

    await otpService.saveOtp({

        email,

        otp,

        purpose: "signup",

        businessType,

        restaurantName,

        ownerName,

        mobile,

        passwordHash: hashedPassword,

        expiresAt

    });

    await otpService.sendOtpEmail(
        email,
        otp
    );

    return res.json({

        success: true,

        message:
            "OTP sent successfully"

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
exports.login =
async (
    req,
    res
) => {

    const {
    email,
    identifier,
    password
} = req.body;

const loginIdentifier =
    identifier ||
    email;

if (
    !loginIdentifier ||
    !password
) {

    return res.status(400).json({

        success: false,

        message:
            "Email/User ID and Password are required"

    });

}

    try {

        const result =
    await authService.login(

        loginIdentifier,

        password

    );

        return res.json({

    success: true,

    message:
        "Login Successful",

    token:
        result.token,

    businessType:
        result.user.business_type,

    restaurantId:
        result.user.restaurant_id || null,

    schoolId:
        result.user.school_id || null,

    user: {

        id:
            result.user.id,

        name:
            result.user.name,

        email:
            result.user.email,

        role:
            result.user.role

    }

});

    }
    catch (err) {

        return res.status(401).json({

            success: false,

            message:
                err.message

        });

    }

};
exports.verifyOtp =
    async (
        req,
        res
    ) => {
     try {    
        const {

    email,

    otp

} = req.body;
if (!email || !otp) {

    return res.status(400).json({

        success: false,

        message:
            "Email and OTP are required"

    });

}
const verificationResult =
    await otpService.verifyOtp(

        email,

        otp,

        "signup"

    );

if (!verificationResult.success) {

    return res.status(400).json({

        success: false,

        message:
            verificationResult.message

    });

}
const otpData =
    verificationResult.data;

let result;

if (
    otpData.business_type === "school"
) {

    result =
        await authSignupService.createSchoolAccount({

            schoolName:
                otpData.restaurant_name,

            ownerName:
                otpData.owner_name,

            email:
                otpData.email,

            mobile:
                otpData.mobile,

            passwordHash:
                otpData.password_hash

        });

}
else {

    result =
        await authSignupService.createRestaurantAccount({

            restaurantName:
                otpData.restaurant_name,

            ownerName:
                otpData.owner_name,

            email:
                otpData.email,

            mobile:
                otpData.mobile,

            passwordHash:
                otpData.password_hash

        });

}

await alignAccountService.ensureModuleAccount(
        otpData.business_type === "school"
            ? "school"
            : "food",
        result.userId,
        otpData.owner_name,
        otpData.email,
        otpData.mobile,
        otpData.password_hash
    );

try {

    await otpService.deleteOtp(

    otpData.email,

    "signup"

);

}
catch (err) {

    console.error(err);

}

return res.json({

    success: true,

    message:
        "Signup Successful",

    businessType:
        otpData.business_type,

    restaurantId:
        result.restaurantId || null,

    schoolId:
        result.schoolId || null,

    userId:
        result.userId

});

    }
    catch (err) {

        return res.status(500).json({
            success: false,
            message: err.message
        });

    }

};
exports.forgotPassword =
async (
    req,
    res
) => {

    const {

        email

    } = req.body;

    if (
        !email
    ) {

        return res.status(400).json({

            success: false,

            message:
                "Email is required"

        });

    }

    try {

        const user =
            await authService
                .existsByEmail(
                    email
                );

        if (
            !user
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Email not found"

            });

        }

        const otp =
            otpService.generateOtp();

        const expiresAt =
            otpService.generateExpiry();

        await otpService.saveOtp({

            email,

            otp,

            purpose:
                "reset_password",

            restaurantName:
                null,

            ownerName:
                null,

            mobile:
                null,

            passwordHash:
                null,

            expiresAt

        });

        await otpService.sendOtpEmail(

            email,

            otp

        );

        return res.json({

            success: true,

            message:
                "OTP sent successfully"

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
exports.resetPassword =
    async (
        req,
        res
    ) => {

        try {

            const {

                email,

                otp,

                password

            } = req.body;

            if (
                !email ||
                !otp ||
                !password
            ) {

                return res.status(400).json({

                    success: false,

                    message:
                        "Email, OTP and Password are required"

                });

            }

            const verificationResult =
                await otpService.verifyOtp(

                    email,

                    otp,

                    "reset_password"

                );

            if (
                !verificationResult.success
            ) {

                return res.status(400).json({

                    success: false,

                    message:
                        verificationResult.message

                });

            }

            await passwordResetService.resetPassword(

                email,

                password

            );

            await otpService.deleteOtp(

                email,

                "reset_password"

            );

            return res.json({

                success: true,

                message:
                    "Password reset successfully"

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
