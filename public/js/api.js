let redirectingToLogin =
    false;
const API = {

   getToken() {

    return (

        localStorage.getItem(
            "superAdminToken"
        )

        ||

        localStorage.getItem(
            "token"
        )

        ||

        localStorage.getItem(
            "staffToken"
        )

    );

},
    async request(url, options = {}) {

        const headers = {
            ...(options.headers || {})
        };

        const token = this.getToken();

        if (token) {
            headers.Authorization = `Bearer ${token}`;
        }

        if (
            options.body &&
            !headers["Content-Type"]
        ) {
            headers["Content-Type"] = "application/json";
        }

        const response = await fetch(url, {
            ...options,
            headers
        });


  if (

    response.status === 401 &&

    !url.includes("/login")

) {

    const data =
        await response.json();

    if (!redirectingToLogin) {

        redirectingToLogin =
            true;

        localStorage.removeItem(
            "token"
        );

        localStorage.removeItem(
            "staffToken"
        );

        if (
    typeof Notify !== "undefined"
) {

    Notify.error(

        data.message ||

        "Session expired. Please login again."

    );

}

        setTimeout(() => {

            window.location.href =
                "/login.html";

        }, 1000);

    }

    return data;

}
       const data =
    await response.json();

return data;

    },

    async get(url) {

        return this.request(url);

    },

    async post(url, data) {

        return this.request(url, {

            method: "POST",

            body: JSON.stringify(data)

        });

    },

    async put(url, data) {

        return this.request(url, {

            method: "PUT",

            body: JSON.stringify(data)

        });

    },
    async patch(url, data) {

    return this.request(url, {

        method: "PATCH",

        body: JSON.stringify(data)

        });

    },

    async delete(url, data) {

    return this.request(url, {

        method: "DELETE",

        body:
            JSON.stringify(data)

    });

}


};
