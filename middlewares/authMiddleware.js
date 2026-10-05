const jwt = require("jsonwebtoken");

module.exports = async (req, res, next) => {

    const authHeader = req.headers.authorization;
    

    if (!authHeader) {
        return res.status(401).json({
            success: false,
            message: "Token missing"
        });
    }

    const token = authHeader.split(" ")[1];
    

    try {


        const decoded = jwt.verify(
            token,
            process.env.JWT_SECRET
        );

        if (decoded.sessionId) {

    const session =
        await staffSessionService.getSessionById(
            decoded.sessionId
        );

    if (!session || !session.is_active) {

        return res.status(401).json({
            success: false,
            message: "Session expired"
        });

    }

    

}

        req.user = decoded;

        next();

    } catch (err) {

    console.error(err);

    return res.status(401).json({
        success: false,
        message: "Invalid token"
    });

}

};