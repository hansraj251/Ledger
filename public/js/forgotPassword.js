const email =
    document.getElementById(
        "email"
    );

const sendOtpBtn =
    document.getElementById(
        "sendOtpBtn"
    );

sendOtpBtn.addEventListener(

    "click",

    async () => {

        if (
            !email.value.trim()
        ) {

            Notify.error(
                "Email is required"
            );

            return;

        }

        sendOtpBtn.disabled =
            true;

        try {

            const response =
                await API.post(

                    "/api/auth/forgot-password",

                    {

                        email:
                            email.value.trim()

                    }

                );

            Notify.success(

                response.message

            );

            sessionStorage.setItem(

                "resetEmail",

                email.value.trim()

            );

            setTimeout(

                () => {

                    window.location.href =
                        "/admin/resetPassword.html";

                },

                1000

            );

        }
        catch (err) {

            Notify.error(

                err.message

            );

        }
        finally {

            sendOtpBtn.disabled =
                false;

        }

    }

);