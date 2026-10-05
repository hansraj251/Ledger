async function loadSchools() {

    const token =
        SuperAdminAuth.token();

    if (!token) {

        location.href =
            "/login.html";

        return [];

    }

    const response =
        await fetch(

            "/api/super-admin/schools",

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

        throw new Error(

            data.message ||
            "Unable to load schools"

        );

    }

    return data.schools || [];

}
async function loadRestaurants() {

    const token =
        SuperAdminAuth.token();

    if (!token) {

        location.href =
            "/login.html";

        return;

    }

    try {

        const [
            restaurantsResponse,
            schools
        ] =
            await Promise.all([

                fetch(
                    "/api/super-admin/restaurants",
                    {
                        headers: {
                            Authorization:
                                `Bearer ${token}`
                        }
                    }
                ).then(
                    response =>
                        response.json()
                ),

                loadSchools()

            ]);

        if (
            !restaurantsResponse.success
        ) {

            throw new Error(

                restaurantsResponse.message ||
                "Unable to load food businesses"

            );

        }

        const restaurants =
            (
                restaurantsResponse.restaurants ||
                []
            ).map(
                restaurant => ({

                    ...restaurant,

                    type:
                        "Food Business",

                    code:
                        restaurant.restaurant_code,

                    manageUrl:
                        `/super-admin/restaurant.html?id=${restaurant.id}`

                })
            );

        const schoolRows =
            (
                schools || []
            ).map(
                school => ({

                    ...school,

                    type:
                        "School",

                    code:
                        school.school_code,

                    manageUrl:
                        `/super-admin/school.html?id=${school.id}`

                })
            );

        const users = [

            ...restaurants,

            ...schoolRows

        ];

        users.sort(
            (
                a,
                b
            ) => {

                return Number(
                    b.id
                ) -
                Number(
                    a.id
                );

            }
        );

        const tbody =
            document.getElementById(
                "restaurantTable"
            );

        tbody.innerHTML = "";

        users.forEach(
            user => {

                tbody.innerHTML += `

<tr>

    <td class="whitespace-nowrap p-4">

        <span
            class="rounded-full px-3 py-1 text-xs font-medium ${
                user.type === "School"
                    ? "bg-indigo-100 text-indigo-700"
                    : "bg-blue-100 text-blue-700"
            }">

            ${user.type}

        </span>

    </td>

    <td class="whitespace-nowrap p-4">

        ${user.code || "-"}

    </td>

    <td class="p-4">

        ${user.name || "-"}

    </td>

    <td class="p-4">

        ${user.owner_name || "-"}

    </td>

    <td class="p-4">

        ${user.plan || "-"}

    </td>

    <td class="p-4">

        ${user.subscription_status || "-"}

    </td>

    <td class="p-4">

        <button
            type="button"
            onclick="location.href='${user.manageUrl}'"
            class="rounded bg-slate-600 px-3 py-2 text-white hover:bg-blue-700">

            Manage

        </button>

    </td>

</tr>

`;

            }

        );

    }
    catch (err) {

        console.error(err);

        Notify.error(
            err.message ||
            "Unable to load users"
        );

    }

}