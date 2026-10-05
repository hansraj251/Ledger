const signupForm =
    document.getElementById(
        "signupForm"
    );

const signupBtn =
    document.getElementById(
        "signupBtn"
    );

const result =
    document.getElementById(
        "result"
    );


signupForm.addEventListener(
    "submit",
    async event => {

        event.preventDefault();


        const name =
            document
                .getElementById("name")
                .value
                .trim();

        const email =
            document
                .getElementById("email")
                .value
                .trim()
                .toLowerCase();

        const mobile =
            document
                .getElementById("mobile")
                .value
                .trim();

        const password =
            document
                .getElementById("password")
                .value;


        result.textContent = "";

        result.className =
            "mt-5 text-center font-medium";


        /*
        |--------------------------------------------------------------------------
        | Validation
        |--------------------------------------------------------------------------
        */

        if (!name) {

            showError(
                "Please enter your name."
            );

            return;

        }


        if (!email) {

            showError(
                "Please enter your email address."
            );

            return;

        }


        if (
            password.length < 8
        ) {

            showError(
                "Password must be at least 8 characters."
            );

            return;

        }


        /*
        |--------------------------------------------------------------------------
        | Disable button
        |--------------------------------------------------------------------------
        */

        signupBtn.disabled = true;

        signupBtn.textContent =
            "Sending OTP...";


        try {

            /*
            |--------------------------------------------------------------------------
            | Start Signup
            |--------------------------------------------------------------------------
            */

            const response =
                await fetch(
                    "/api/property/auth/signup",
                    {

                        method: "POST",

                        headers: {

                            "Content-Type":
                                "application/json"

                        },

                        body:
                            JSON.stringify({

                                name,

                                email,

                                mobile,

                                password

                            })

                    }
                );


            const data =
                await response.json();


            if (
                !response.ok ||
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to send OTP."
                );

            }


            /*
            |--------------------------------------------------------------------------
            | Store email for OTP verification
            |--------------------------------------------------------------------------
            */

            window.signupEmail = email;

window.otpVerifyEndpoint =
    "/api/property/auth/verify-otp";

window.otpSuccessRedirect = "/music-auth/login.html";
                


            /*
            |--------------------------------------------------------------------------
            | OTP Modal
            |--------------------------------------------------------------------------
            */

            if (
                typeof openOtpModal !==
                "function"
            ) {

                throw new Error(
                    "OTP modal could not be loaded."
                );

            }


            openOtpModal();


            /*
            |--------------------------------------------------------------------------
            | Success message
            |--------------------------------------------------------------------------
            */

            Toast.show(
                "OTP sent successfully.",
                "success"
            );


        }
        catch (err) {

            console.error(
                "Music signup error:",
                err
            );


            showError(
                err.message ||
                "Unable to send OTP."
            );


            signupBtn.disabled =
                false;

            signupBtn.textContent =
                "Create Account";

        }

    }
);


/*
|--------------------------------------------------------------------------
| Error
|--------------------------------------------------------------------------
*/

function showError(
    message
) {

    result.textContent =
        message;

    result.className =
        "mt-5 text-center font-medium text-red-600";

}