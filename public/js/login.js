Auth.redirectIfLoggedIn();

document
    .getElementById("loginBtn")
    .addEventListener(
        "click",
        login
    );

document.addEventListener(
    "keydown",
    (event) => {

        if (
            event.key !== "Enter"
        ) {

            return;

        }

        event.preventDefault();

        login();

    }
);

async function login() {

    const loginId =
        document
            .getElementById(
                "loginId"
            )
            .value
            .trim();

    const password =
        document
            .getElementById(
                "password"
            )
            .value;

    const result =
        document.getElementById(
            "result"
        );

    result.textContent = "";

    if (
        !loginId ||
        !password
    ) {

        result.textContent =
            "Email/User ID and Password are required.";

        return;

    }

    try {

        const superAdmin =
            await API.post(
                "/api/super-admin/login",
                {
                    username:
                        loginId,
                    password
                }
            );

        if (
            superAdmin.success
        ) {

            localStorage.setItem(
                "superAdminToken",
                superAdmin.token
            );

            localStorage.setItem(
                "superAdmin",
                JSON.stringify(
                    superAdmin.admin
                )
            );

            window.location.href =
                "/super-admin/dashboard.html";

            return;

        }

        const data =
    await API.post(
        "/api/auth/login",
        {
            identifier:
                loginId,
            password
        }
    );

        if (
            !data.success
        ) {

            result.textContent =
                data.message;

            return;

        }

        localStorage.setItem(
            "token",
            data.token
        );

        const payload =
            JSON.parse(
                atob(
                    data.token
                        .split(".")[1]
                )
            );

        if (
    payload.businessType === "school"
) {

    localStorage.setItem(
        "school_id",
        payload.schoolId
    );

    if (
        payload.role === "attendance"
    ) {

        window.location.href =
    "/school/all-classes.html";
        return;

    }

    window.location.href =
        "/school/dashboard.html";

    return;

}

localStorage.setItem(
    "restaurant_id",
    payload.restaurantId
);

window.location.href =
    "/admin/subscription.html";

    }
    catch (err) {

        console.error(err);

        result.textContent =
            "Unable to connect to server.";

    }

}

const togglePassword =
    document.getElementById(
        "togglePassword"
    );

const passwordInput =
    document.getElementById(
        "password"
    );

togglePassword.addEventListener(
    "click",
    () => {

        passwordInput.type =
            passwordInput.type ===
            "password"
                ? "text"
                : "password";

    }
);