const posAuthService =
    require("../services/posAuthService");

exports.login = async (
    req,
    res
) => {

    try {

        const {

    email,
    password,
    deviceId

} = req.body;

        if (
            !email ||
            !password
        ) {

            return res.status(400).json({

                success: false,

                message:
                    "Email and Password are required"

            });

        }

        const result =
    await posAuthService.login(

        email,

        password,

        deviceId

    );

        return res.json({

    success: true,

    message: "Login Successful",

    token: result.token,

    restaurant: result.restaurant,

    user: result.user,

    plan: result.plan,

    planLimit: result.planLimit

});

    }
    catch (err) {

    return res.status(401).json({

        success: false,

        code:
            err.code,

        message:
            err.message

    });

}

};
exports.replaceDevice = async (
    req,
    res
) => {

    try {

        const {

            email,
            password,
            deviceId

        } = req.body;

        await posAuthService
            .replaceDevice(

                email,

                password,

                deviceId

            );

        res.json({

            success: true

        });

    }
    catch (
        err
    ) {

        res.status(401)
            .json({

                success: false,

                message:
                    err.message

            });

    }

};
