const loginBtn =
    document.getElementById("loginBtn");

loginBtn.addEventListener(
    "click",
    login
);

async function login() {

    const username =
        document.getElementById("loginId").value.trim();

    const password =
        document.getElementById("password").value;

    const result =
        document.getElementById("result");

    result.textContent = "";

    if (!username || !password) {

        result.textContent =
            "Username and Password are required.";

        return;

    }

    try {

        const response =
            await fetch(
                "/api/super-admin/login",
                {
                    method: "POST",

                    headers: {
                        "Content-Type":
                            "application/json"
                    },

                    body: JSON.stringify({

                        username,

                        password

                    })

                }
            );

        const data =
            await response.json();

        if (!data.success) {

            result.textContent =
                data.message;

            return;

        }

        SuperAdminAuth.save(

            data.token,

            data.admin

        );

        location.href =
            "/super-admin/dashboard.html";

    } catch (err) {

        console.error(err);

        result.textContent =
            "Login failed.";

    }

}