Auth.redirectIfLoggedIn();
loadOtpModal();
document

    .getElementById("signupForm")

    .addEventListener("submit", async (e) => {

        e.preventDefault();

        await signup();

    });
const businessTypeInput =
    document.getElementById(
        "businessType"
    );

const restaurantNameInput =
    document.getElementById(
        "restaurantName"
    );

businessTypeInput.addEventListener(
    "change",
    () => {

        restaurantNameInput.placeholder =
            businessTypeInput.value === "school"
                ? "School Name"
                : "Business Name";

    }
);
async function signup() {

    const businessType =
    document.getElementById("businessType").value;

    const restaurantName =
        document.getElementById("restaurantName").value.trim();

    const ownerName =
        document.getElementById("ownerName").value.trim();

    const email =
        document.getElementById("email").value.trim();

    const mobile =
        document.getElementById("mobile").value.trim();

    const password =
        document.getElementById("password").value;

    const result =
        document.getElementById("result");

    result.textContent = "";

    const data = await API.post(
        "/api/auth/signup",
        {
            businessType,
            restaurantName,
            ownerName,
            email,
            mobile,
            password
        }
    );

    if (!data.success) {

        result.textContent =
            data.message;

        return;

    }

    result.classList.remove("text-red-600");

result.classList.add("text-green-600");

result.textContent =
    "OTP sent successfully. Please verify your email.";
    window.signupEmail =
    email;

openOtpModal();

}
