const savedLoading =
    document.getElementById(
        "savedLoading"
    );

const savedError =
    document.getElementById(
        "savedError"
    );

const savedEmpty =
    document.getElementById(
        "savedEmpty"
    );

const savedContainer =
    document.getElementById(
        "savedContainer"
    );

const logoutBtn =
    document.getElementById(
        "logoutBtn"
    );


/*
|--------------------------------------------------------------------------
| Auth
|--------------------------------------------------------------------------
*/

const getPropertyToken =
    () => {

        return localStorage.getItem(
            "propertyToken"
        );

    };


/*
|--------------------------------------------------------------------------
| Logout
|--------------------------------------------------------------------------
*/

if (logoutBtn) {

    logoutBtn.addEventListener(
        "click",
        () => {

            localStorage.removeItem(
                "propertyToken"
            );

            localStorage.removeItem(
                "propertyUser"
            );

            window.location.href =
                "/property/login.html";

        }
    );

}


/*
|--------------------------------------------------------------------------
| Escape HTML
|--------------------------------------------------------------------------
*/

const escapeHtml =
    value => {

        return String(
            value ?? ""
        )
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );

    };


/*
|--------------------------------------------------------------------------
| Format Price
|--------------------------------------------------------------------------
*/

const formatPrice =
    value => {

        if (
            value === null ||
            value === undefined ||
            value === ""
        ) {

            return "";

        }

        const number =
            Number(value);

        if (
            !Number.isFinite(number)
        ) {

            return "";

        }

        return new Intl.NumberFormat(
            "en-IN",
            {
                maximumFractionDigits: 2
            }
        ).format(
            number
        );

    };


/*
|--------------------------------------------------------------------------
| Get Price Text
|--------------------------------------------------------------------------
*/

const getPriceText =
    listing => {

        const priceType =
            String(
                listing.price_type ||
                ""
            ).toLowerCase();


        if (
            priceType === "rent"
        ) {

            if (
                listing.rent_amount !== null &&
                listing.rent_amount !== undefined &&
                listing.rent_amount !== ""
            ) {

                return (
                    "₹" +
                    formatPrice(
                        listing.rent_amount
                    ) +
                    " / month"
                );

            }

            return "Rent";

        }


        if (
            listing.price !== null &&
            listing.price !== undefined &&
            listing.price !== ""
        ) {

            return (
                "₹" +
                formatPrice(
                    listing.price
                )
            );

        }


        return "Price on request";

    };


/*
|--------------------------------------------------------------------------
| Get Type
|--------------------------------------------------------------------------
*/

const getListingType =
    listing => {

        const priceType =
            String(
                listing.price_type ||
                ""
            ).toLowerCase();


        if (
            priceType === "rent"
        ) {

            return "For Rent";

        }


        return "For Sale";

    };


/*
|--------------------------------------------------------------------------
| Get Image
|--------------------------------------------------------------------------
*/

const getCoverImage =
    listing => {

        if (
            Array.isArray(listing.images) &&
            listing.images.length
        ) {

            const cover =
                listing.images.find(
                    image =>
                        Number(
                            image.is_cover
                        ) === 1
                ) ||
                listing.images[0];


            if (
                cover &&
                cover.image_url
            ) {

                return cover.image_url;

            }

        }


        return "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='800' height='500' viewBox='0 0 800 500'%3E%3Crect width='800' height='500' fill='%23f1f5f9'/%3E%3Ctext x='400' y='250' text-anchor='middle' dominant-baseline='middle' font-family='Arial' font-size='28' fill='%2394a3b8'%3ENo Image Available%3C/text%3E%3C/svg%3E";

    };

/*
|--------------------------------------------------------------------------
| Render Saved Property
|--------------------------------------------------------------------------
*/

const renderSavedProperty =
    listing => {

        const card =
            document.createElement(
                "article"
            );

        card.className =
            "saved-card";


        card.dataset.listingId =
            String(
                listing.id
            );


        const image =
            escapeHtml(
                getCoverImage(
                    listing
                )
            );

        const title =
            escapeHtml(
                listing.title ||
                "Untitled Property"
            );

        const subtitle =
            escapeHtml(
                listing.subtitle ||
                ""
            );

        const price =
            escapeHtml(
                getPriceText(
                    listing
                )
            );

        const type =
            escapeHtml(
                getListingType(
                    listing
                )
            );


        card.innerHTML = `

            <img
                src="${image}"
                alt="${title}"
                class="saved-image"
                
            >

            <div class="p-5">

                <div
                    class="
                        flex
                        items-start
                        justify-between
                        gap-3
                    "
                >

                    <span
                        class="
                            inline-flex
                            rounded-full
                            bg-indigo-50
                            px-3
                            py-1
                            text-xs
                            font-semibold
                            text-indigo-700
                        "
                    >
                        ${type}
                    </span>

                </div>


                <h2
                    class="
                        mt-3
                        line-clamp-2
                        text-lg
                        font-bold
                        text-slate-800
                    "
                >
                    ${title}
                </h2>


                ${
                    subtitle
                        ? `
                            <p
                                class="
                                    mt-1
                                    line-clamp-2
                                    text-sm
                                    text-slate-500
                                "
                            >
                                ${subtitle}
                            </p>
                        `
                        : ""
                }


                <p
                    class="
                        mt-4
                        text-xl
                        font-bold
                        text-slate-800
                    "
                >
                    ${price}
                </p>


                <div
                    class="
                        mt-5
                        grid
                        grid-cols-2
                        gap-3
                    "
                >

                    <a
                        href="/property/details.html?id=${encodeURIComponent(
                            listing.id
                        )}"
                        class="
                            inline-flex
                            items-center
                            justify-center
                            rounded-xl
                            px-4
                            py-3
                            text-sm
                            font-semibold
                            text-white
                        "
                        style="
                            background:
                                linear-gradient(
                                    90deg,
                                    #c13bbd,
                                    #7b3fc6,
                                    #2454c7
                                );
                        "
                    >
                        View Property
                    </a>


                    <button
                        type="button"
                        class="
                            saved-remove-button
                            inline-flex
                            items-center
                            justify-center
                            rounded-xl
                            px-4
                            py-3
                            text-sm
                            font-semibold
                        "
                        data-remove-saved
                    >
                        Remove
                    </button>

                </div>

            </div>

        `;


        const removeBtn =
            card.querySelector(
                "[data-remove-saved]"
            );


        removeBtn.addEventListener(
            "click",
            () => {

                confirmRemoveSaved(
                    listing,
                    card,
                    removeBtn
                );

            }
        );


        savedContainer.appendChild(
            card
        );

    };


/*
|--------------------------------------------------------------------------
| Remove Saved Confirmation
|--------------------------------------------------------------------------
*/

const confirmRemoveSaved =
    (
        listing,
        card,
        button
    ) => {

        Modal.confirm(

            "Remove Saved Property",

            `
                <p class="text-slate-600">
                    Are you sure you want to remove
                    <strong>
                        ${escapeHtml(
                            listing.title ||
                            "this property"
                        )}
                    </strong>
                    from your saved properties?
                </p>
            `,

            async () => {

                button.disabled =
                    true;


                const token =
                    getPropertyToken();


                if (!token) {

                    window.location.href =
                        "/property/login.html";

                    return;

                }


                const response =
                    await fetch(

                        `/api/property/listings/${encodeURIComponent(
                            listing.id
                        )}/save`,

                        {
                            method:
                                "DELETE",

                            headers: {

                                "Authorization":
                                    `Bearer ${token}`,

                                "Accept":
                                    "application/json"

                            }

                        }

                    );


                let data = {};

                try {

                    data =
                        await response.json();

                }
                catch {

                    throw new Error(
                        "Invalid server response."
                    );

                }


                if (
                    response.status === 401 ||
                    response.status === 403
                ) {

                    localStorage.removeItem(
                        "propertyToken"
                    );

                    localStorage.removeItem(
                        "propertyUser"
                    );

                    Modal.close();

                    window.location.href =
                        "/property/login.html";

                    return;

                }


                if (
                    !response.ok ||
                    !data.success
                ) {

                    throw new Error(
                        data.message ||
                        "Unable to remove saved property."
                    );

                }


                card.remove();

                Modal.close();

                Toast.show(
                    "Property removed from saved.",
                    "success"
                );


                if (
                    !savedContainer.children.length
                ) {

                    savedContainer.classList.add(
                        "hidden"
                    );

                    savedEmpty.classList.remove(
                        "hidden"
                    );

                }

            },

            {
                buttonText:
                    "Remove",

                buttonClass:
                    "bg-red-600",

                loadingText:
                    "Removing..."
            }

        );

    };


/*
|--------------------------------------------------------------------------
| Load Saved Properties
|--------------------------------------------------------------------------
*/

const loadSavedProperties =
    async () => {

        const token =
            getPropertyToken();


        if (!token) {

            window.location.href =
                "/property/login.html";

            return;

        }


        savedLoading.classList.remove(
            "hidden"
        );

        savedError.classList.add(
            "hidden"
        );

        savedEmpty.classList.add(
            "hidden"
        );

        savedContainer.classList.add(
            "hidden"
        );


        try {

            const response =
                await fetch(

                    "/api/property/listings/saved",

                    {
                        method:
                            "GET",

                        headers: {

                            "Authorization":
                                `Bearer ${token}`,

                            "Accept":
                                "application/json"

                        }

                    }

                );


            let data = {};

            try {

                data =
                    await response.json();

            }
            catch {

                throw new Error(
                    "Invalid server response."
                );

            }


            if (
                response.status === 401 ||
                response.status === 403
            ) {

                localStorage.removeItem(
                    "propertyToken"
                );

                localStorage.removeItem(
                    "propertyUser"
                );

                window.location.href =
                    "/property/login.html";

                return;

            }


            if (
                !response.ok ||
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to load saved properties."
                );

            }


            const listings =
                Array.isArray(
                    data.listings
                )
                    ? data.listings
                    : [];


            savedContainer.innerHTML =
                "";


            savedLoading.classList.add(
                "hidden"
            );


            if (
                !listings.length
            ) {

                savedEmpty.classList.remove(
                    "hidden"
                );

                return;

            }


            listings.forEach(
                listing => {

                    renderSavedProperty(
                        listing
                    );

                }
            );


            savedContainer.classList.remove(
                "hidden"
            );

        }
        catch (error) {

            console.error(
                "Load saved properties error:",
                error
            );


            savedLoading.classList.add(
                "hidden"
            );


            savedError.textContent =
                error.message ||
                "Unable to load saved properties.";


            savedError.classList.remove(
                "hidden"
            );

        }

    };


/*
|--------------------------------------------------------------------------
| Start
|--------------------------------------------------------------------------
*/

loadSavedProperties();