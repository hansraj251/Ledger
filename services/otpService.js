const db =
    require("../db");
const Brevo =
    require("@getbrevo/brevo");
   
function generateOtp() {

    return Math.floor(
        100000 +
        Math.random() * 900000
    ).toString();

}
function generateExpiry() {

    const expiry =
        new Date();

    expiry.setMinutes(
        expiry.getMinutes() + 10
    );

    return expiry.toISOString();

}
const brevo =
    new Brevo.BrevoClient({

        apiKey:
            process.env.BREVO_API_KEY

    });
async function sendOtpEmail(
    email,
    otp
) {

    const emailData = {

    sender: {
    name: "AlignOS",
    email: "alignos.in@gmail.com"
},

    to: [

        {

            email

        }

    ],

    subject:
        "Verify your Align account"

};

    emailData.htmlContent =
        `
<div style="font-family:Arial,sans-serif;max-width:600px;margin:auto">

<h2>
Align Smart OS
</h2>

<p>
Your verification code is:
</p>

<h1 style="letter-spacing:6px">
${otp}
</h1>

<p>
This code will expire in 10 minutes.
</p>

<p>
If you did not request this code, you can safely ignore this email.
</p>

</div>
`;

    await brevo
    .transactionalEmails
    .sendTransacEmail(
        emailData
    );

}

async function saveOtp(
    data
) {

    return new Promise(
        (
            resolve,
            reject
        ) => {

            db.run(
                `
                DELETE FROM email_otps
WHERE email = ?
AND purpose = ?
                `,
                [
    data.email,
    data.purpose
],
                err => {

                    if (err) {

                        return reject(err);

                    }

                    db.run(
                        `
                        INSERT INTO email_otps
                        (
    email,
    otp,
    purpose,
    business_type,
    restaurant_name,
    owner_name,
    mobile,
    password_hash,
    expires_at
)
                        VALUES
(
    ?, ?, ?, ?, ?, ?, ?, ?, ?
)
                        `,
                        [
    data.email,
    data.otp,
    data.purpose,
    data.businessType,
    data.restaurantName,
    data.ownerName,
    data.mobile,
    data.passwordHash,
    data.expiresAt
],
                        err => {

                            if (err) {

                                return reject(err);

                            }

                            resolve();

                        }
                    );

                }
            );

        }
    );

}

async function verifyOtp(
    email,
    otp,
    purpose
) {

    return new Promise(

        (
            resolve,
            reject
        ) => {

            db.get(

    `
    SELECT *
    FROM email_otps
    WHERE email = ?
    AND purpose = ?
    `,

    [
        email,
        purpose
    ],

    (
        err,
        otpRecord
    ) => {

                    if (err) {

                        return reject(err);

                    }

                    if (!otpRecord) {

                        return resolve({

                            success: false,

                            message:
                                "Invalid OTP"

                        });

                    }

                    if (
                        otpRecord.otp !== otp
                    ) {

                        return resolve({

                            success: false,

                            message:
                                "Invalid OTP"

                        });

                    }

                    if (
                        new Date(
                            otpRecord.expires_at
                        ) < new Date()
                    ) {

                        return resolve({

                            success: false,

                            message:
                                "OTP expired"

                        });

                    }

                    resolve({

                        success: true,

                        data:
                            otpRecord

                    });

                }

            );

        }

    );

}

async function deleteOtp(
    email,
    purpose
) {

    return new Promise(

        (
            resolve,
            reject
        ) => {

            db.run(

                `
                DELETE FROM email_otps
WHERE email = ?
AND purpose = ?
                `,
              [
    email,
    purpose
],

                function (err) {

                    if (err) {

                        return reject(err);

                    }

                    resolve();

                }

            );

        }

    );

}
module.exports = {

    generateOtp,

    generateExpiry,

    saveOtp,

    verifyOtp,

    deleteOtp,

    sendOtpEmail

};