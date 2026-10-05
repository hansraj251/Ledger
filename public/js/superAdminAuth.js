const SuperAdminAuth = {

    save(token, admin) {

        localStorage.setItem(
            "superAdminToken",
            token
        );

        localStorage.setItem(
            "superAdmin",
            JSON.stringify(admin)
        );

    },

    token() {

        return localStorage.getItem(
            "superAdminToken"
        );

    },

    admin() {

        return JSON.parse(

            localStorage.getItem(
                "superAdmin"
            ) || "{}"

        );

    },

    logout() {

        localStorage.removeItem(
            "superAdminToken"
        );

        localStorage.removeItem(
            "superAdmin"
        );

        location.href =
    "/login.html";

    }

};