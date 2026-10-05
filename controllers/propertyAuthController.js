const bcrypt =
    require("bcrypt");

const propertyAuthService =
    require("../services/propertyAuthService");

const otpService =
    require("../services/otpService");

exports.login =
async (
    req,
    res
) => {

    try {

        const {
            identifier,
            password
        } = req.body;

        const result =
            await propertyAuthService
                .login(
                    identifier,
                    password
                );

        return res.json({

            success: true,

            token:
                result.token,

            user:
                result.user

        });

    }
    catch (err) {

        console.error(
            "Property login error:",
            err
        );

        return res.status(401).json({

            success: false,

            message:
                err.message ||
                "Unable to login"

        });

    }

};


exports.signup =
async (
    req,
    res
) => {

    try {

        const {
            name,
            email,
            mobile,
            password
        } = req.body;


        const cleanName =
            String(
                name || ""
            ).trim();

        const cleanEmail =
            String(
                email || ""
            )
            .trim()
            .toLowerCase();

        const cleanMobile =
            String(
                mobile || ""
            ).trim();

        const cleanPassword =
            String(
                password || ""
            ).trim();


        if (!cleanName) {

            return res.status(400).json({

                success: false,

                message:
                    "Name is required"

            });

        }


        if (!cleanEmail) {

            return res.status(400).json({

                success: false,

                message:
                    "Email is required"

            });

        }


        if (!cleanMobile) {

            return res.status(400).json({

                success: false,

                message:
                    "Mobile number is required"

            });

        }


        if (
            cleanPassword.length < 8
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Password must be at least 8 characters"

            });

        }


        const existingUser =
            await propertyAuthService
                .getByEmailOrMobile(
                    cleanEmail,
                    cleanMobile
                );


        if (existingUser) {

            return res.status(400).json({

                success: false,

                message:
                    "Email or Mobile already registered"

            });

        }


        const passwordHash =
            await bcrypt.hash(
                cleanPassword,
                10
            );


        const otp =
            otpService.generateOtp();

        const expiresAt =
            otpService.generateExpiry();


        await otpService.saveOtp({

    email:
        cleanEmail,

    otp,

    purpose:
        "property_signup",

    ownerName:
        cleanName,

    mobile:
        cleanMobile,

    passwordHash,

    expiresAt

});


        await otpService.sendOtpEmail(

            cleanEmail,

            otp

        );


        return res.json({

            success: true,

            message:
                "OTP sent successfully"

        });

    }
    catch (err) {

        console.error(
            "Property signup OTP error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                err.message ||
                "Unable to send OTP"

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

                String(email)
                    .trim()
                    .toLowerCase(),

                String(otp).trim(),

                "property_signup"

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


        const otpData =
            verificationResult.data;


        const existingUser =
            await propertyAuthService
                .getByEmailOrMobile(
                    otpData.email,
                    otpData.mobile
                );


        if (existingUser) {

            await otpService.deleteOtp(
                otpData.email,
                "property_signup"
            );

            return res.status(400).json({

                success: false,

                message:
                    "Email or Mobile already registered"

            });

        }


        const user =
    await propertyAuthService
        .createUserFromOtp(

            otpData.owner_name,

            otpData.email,

            otpData.mobile,

            otpData.password_hash

        );


        await otpService.deleteOtp(

            otpData.email,

            "property_signup"

        );


        return res.status(201).json({

            success: true,

            message:
                "Signup successful",

            user

        });

    }
    catch (err) {

        console.error(
            "Property OTP verification error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                err.message ||
                "Unable to verify OTP"

        });

    }

};
exports.getProfile = async (
    req,
    res
) => {

    try {

        const user =
            await propertyAuthService
                .getProfile(
                    req.propertyUserId
                );


        return res.json({

            success: true,

            user

        });

    }

    catch (err) {

        console.error(
            "Property profile error:",
            err
        );


        return res.status(404).json({

            success: false,

            message:
                err.message ||
                "Unable to load profile"

        });

    }

};


exports.updateProfile = async (
    req,
    res
) => {

    try {

        const user =
            await propertyAuthService
                .updateProfile(
                    req.propertyUserId,
                    req.body
                );


        return res.json({

            success: true,

            message:
                "Profile updated successfully.",

            user

        });

    }

    catch (err) {

        console.error(
            "Property profile update error:",
            err
        );


        return res.status(400).json({

            success: false,

            message:
                err.message ||
                "Unable to update profile"

        });

    }

};

exports.forgotPassword = async (
    req,
    res
) => {

    try {

        const email =
            String(
                req.body.email || ""
            )
            .trim()
            .toLowerCase();

        if (!email) {

            return res.status(400).json({

                success: false,

                message:
                    "Email is required"

            });

        }

        const user =
            await propertyAuthService
                .getByEmail(email);

        if (!user) {

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
                "property_reset_password",

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

        console.error(
            "Property forgot password error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                err.message ||
                "Unable to send OTP"

        });

    }

};


exports.resetPassword = async (
    req,
    res
) => {

    try {

        const {
            email,
            otp,
            password
        } = req.body;

        const cleanEmail =
            String(
                email || ""
            )
            .trim()
            .toLowerCase();

        const cleanOtp =
            String(
                otp || ""
            ).trim();

        const cleanPassword =
            String(
                password || ""
            );

        if (
            !cleanEmail ||
            !cleanOtp ||
            !cleanPassword
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Email, OTP and Password are required"

            });

        }

        if (
            cleanPassword.length < 8
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Password must be at least 8 characters"

            });

        }

        const verificationResult =
            await otpService.verifyOtp(

                cleanEmail,

                cleanOtp,

                "property_reset_password"

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

        const user =
            await propertyAuthService
                .getByEmail(
                    cleanEmail
                );

        if (!user) {

            return res.status(400).json({

                success: false,

                message:
                    "Email not found"

            });

        }

        await propertyAuthService
            .updatePassword(
                user.id,
                cleanPassword
            );

        await otpService.deleteOtp(

            cleanEmail,

            "property_reset_password"

        );

        return res.json({

            success: true,

            message:
                "Password reset successfully"

        });

    }
    catch (err) {

        console.error(
            "Property reset password error:",
            err
        );

        return res.status(500).json({

            success: false,

            message:
                err.message ||
                "Unable to reset password"

        });

    }

};