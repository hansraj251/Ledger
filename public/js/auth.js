const Auth = {

    requireLogin() {

        const token =
            localStorage.getItem(
                "token"
            );

        const superAdminToken =
            localStorage.getItem(
                "superAdminToken"
            );

        if (
            !token &&
            !superAdminToken
        ) {

            window.location.href =
                "/login.html";

            return false;

        }

        return true;

    },
    requireAttendanceUser() {

    if (
        !this.requireLogin()
    ) {

        return false;

    }

    const token =
        localStorage.getItem(
            "token"
        );

    if (
        !token
    ) {

        return false;

    }

    try {

        const payload =
            JSON.parse(
                atob(
                    token
                        .split(".")[1]
                )
            );

        if (
            payload.role ===
            "attendance"
        ) {

            return true;

        }

        if (
            payload.businessType ===
            "school"
        ) {

            return true;

        }

        window.location.replace(
            "/school/dashboard.html"
        );

        return false;

    }
    catch (err) {

        console.error(err);

        window.location.replace(
            "/login.html"
        );

        return false;

    }

},
requireSchoolOwner() {

    if (
        !this.requireLogin()
    ) {

        return false;

    }

    const token =
        localStorage.getItem(
            "token"
        );

    if (
        !token
    ) {

        return false;

    }

    try {

        const payload =
            JSON.parse(
                atob(
                    token
                        .split(".")[1]
                )
            );

        if (
            payload.businessType !==
            "school"
        ) {

            window.location.replace(
                "/login.html"
            );

            return false;

        }

        if (
            payload.role ===
            "attendance"
        ) {

            window.location.replace(
                "/school/all-classes.html"
            );

            return false;

        }

        return true;

    }
    catch (err) {

        console.error(err);

        window.location.replace(
            "/login.html"
        );

        return false;

    }

},

    logout() {

        localStorage.removeItem(
            "token"
        );

        localStorage.removeItem(
            "superAdminToken"
        );

        localStorage.removeItem(
            "superAdmin"
        );

        sessionStorage.clear();

        window.location.replace(
            "/login.html"
        );

    },

    isLoggedIn() {

        return !!(

            localStorage.getItem(
                "token"
            ) ||

            localStorage.getItem(
                "superAdminToken"
            )

        );

    },

    redirectIfLoggedIn() {

        if (

            localStorage.getItem(
                "superAdminToken"
            )

        ) {

            window.location.href =
                "/super-admin/dashboard.html";

            return;

        }

       if (

    localStorage.getItem(
        "token"
    )

) {

    const token =
        localStorage.getItem(
            "token"
        );

    try {

        const payload =
            JSON.parse(
                atob(
                    token
                        .split(".")[1]
                )
            );

        if (
            payload.businessType ===
            "school"
        ) {

            window.location.href =
                "/school/dashboard.html";

            return;

        }

    }
    catch (err) {

        console.error(err);

    }

    window.location.href =
        "/admin/subscription.html";

}
    }

};