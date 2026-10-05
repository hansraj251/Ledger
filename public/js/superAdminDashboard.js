async function loadDashboard() {

    const token =
        SuperAdminAuth.token();

    if (!token) {

        location.href =
            "/login.html";

        return;

    }

    try {

        const response =
            await fetch(
                "/api/super-admin/dashboard",
                {
                    headers: {
                        Authorization:
                            `Bearer ${token}`
                    }
                }
            );

        const data =
            await response.json();

        if (!data.success) {

            Notify.error(
                data.message
            );

            return;

        }

        document.getElementById(
            "totalRestaurants"
        ).textContent =
            data.totalRestaurants;

        document.getElementById(
            "totalSchools"
        ).textContent =
            data.totalSchools;

    } catch (err) {

        console.error(err);

        Notify.error(
            "Failed to load dashboard."
        );

    }

}

loadDashboard();