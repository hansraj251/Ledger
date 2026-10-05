async function loadOtpModal() {

    const container =
        document.getElementById(
            "modalContainer"
        );

    const response =
        await fetch(
            "/components/otpModal.html"
        );

    container.innerHTML =
        await response.text();
    bindOtpModalEvents();    

}
function openOtpModal() {

    document
        .getElementById("otpModal")
        .classList.remove("hidden");

    document
        .getElementById("otpModal")
        .classList.add("flex");

}

function closeOtpModal() {

    document
        .getElementById("otpModal")
        .classList.remove("flex");

    document
        .getElementById("otpModal")
        .classList.add("hidden");

}
function bindOtpModalEvents() {

    document

        .getElementById("verifyOtpBtn")

        .addEventListener(

            "click",

            verifyOtp

        );

}
async function verifyOtp() {

    const otp =
        document
            .getElementById("otpInput")
            .value
            .trim();

    const result =
        document
            .getElementById("otpResult");

    result.textContent = "";

    if (!otp) {

        result.textContent =
            "Please enter OTP";

        return;

    }

    const verifyEndpoint =
    window.otpVerifyEndpoint ||
    "/api/auth/verify-otp";

const data = await API.post(
    verifyEndpoint,
    {
        email: window.signupEmail,
        otp
    }
);

    if (!data.success) {

        result.classList.remove(
            "text-green-600"
        );

        result.classList.add(
            "text-red-600"
        );

        result.textContent =
            data.message;

        return;

    }

    result.classList.remove(
        "text-red-600"
    );

    result.classList.add(
        "text-green-600"
    );

    result.textContent =
        "Signup successful. Redirecting...";

    setTimeout(
    () => {

        window.location.href =
            window.otpSuccessRedirect ||
            "/login.html";

    },
    1500
);

}