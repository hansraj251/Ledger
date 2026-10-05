const fs =
    require("fs");

const path =
    require("path");

const crypto =
    require("crypto");

const privateKeyPath =
    process.env
        .SUBSCRIPTION_PRIVATE_KEY_PATH ||
    "./keys/private.pem";
console.log(
    process.env.SUBSCRIPTION_PRIVATE_KEY_PATH
);
const privateKey =
    fs.readFileSync(
        path.resolve(
            privateKeyPath
        ),
        "utf8"
    );

exports.sign =
    payload => {

        return crypto
            .sign(

                "sha256",

                Buffer.from(payload),

                privateKey

            )
            .toString(
                "base64"
            );

    };