let allProperties = [];
const PAGE_SIZE = 40;

let currentPage = 1;

let currentProperties = [];
const API = "/api/property/listings";



const $ = id =>
    document.getElementById(id);


/*
|--------------------------------------------------------------------------
| Helpers
|--------------------------------------------------------------------------
*/

const escapeHtml = value => {

    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");

};


const formatPrice = (
    price,
    priceType
) => {

    if (
        price === null ||
        price === undefined ||
        price === ""
    ) {

        return "Price on request";

    }


    const amount =
        Number(price);


    if (
        !Number.isFinite(
            amount
        )
    ) {

        return "Price on request";

    }


    return new Intl.NumberFormat(
        "en-IN",
        {
            style: "currency",
            currency: "INR",
            maximumFractionDigits: 0
        }
    ).format(
        amount
    );

};
/*
|--------------------------------------------------------------------------
| Random Property Feed
|--------------------------------------------------------------------------
*/

const shuffleProperties = properties => {

    const result = [...properties];

    for (
        let i = result.length - 1;
        i > 0;
        i--
    ) {

        const j =
            Math.floor(
                Math.random() * (i + 1)
            );

        [
            result[i],
            result[j]
        ] =
        [
            result[j],
            result[i]
        ];

    }

    return result;

};


const getTotalPages = properties => {
    return Math.max(
        1,
        Math.ceil(properties.length / PAGE_SIZE)
    );
};


const getPageProperties = (
    properties,
    page
) => {

    const start =
        (page - 1) * PAGE_SIZE;

    return properties.slice(
        start,
        start + PAGE_SIZE
    );

};

/*
|--------------------------------------------------------------------------
| Property Card
|--------------------------------------------------------------------------
*/

const renderPropertyCard = listing => {

    const images =
        Array.isArray(
            listing.images
        )
            ? listing.images
            : [];


    const cover =
        images.find(
            image =>
                Number(
                    image.is_cover
                ) === 1
        ) ||
        images[0];


    let imageHtml = `
        <div
            class="
                property-card-placeholder
            "
        >
            🏠
        </div>
    `;


    if (
        cover &&
        cover.image_url
    ) {

        imageHtml = `
            <img
                src="${escapeHtml(
                    cover.image_url
                )}"
                alt="${escapeHtml(
                    listing.title ||
                    "Property"
                )}"
                class="property-card-image"
                onerror="
                    this.onerror=null;
                    this.style.display='none';
                    this.nextElementSibling.style.display='flex';
                "
            >

            <div
                class="
                    property-card-placeholder
                "
                style="display:none;"
            >
                🏠
            </div>
        `;

    }


    /*
    |--------------------------------------------------------------------------
    | Property Details URL
    |--------------------------------------------------------------------------
    */

    const detailsUrl =
        `/property/details.html?id=${encodeURIComponent(
            listing.id
        )}`;

    let displayPrice = "";

if (
    listing.rent_amount !== null &&
    listing.rent_amount !== undefined &&
    listing.rent_amount !== ""
) {

    displayPrice =
        `${formatPrice(
            listing.rent_amount
        )} / month`;

}
else if (
    listing.price !== null &&
    listing.price !== undefined &&
    listing.price !== ""
) {

    displayPrice =
        formatPrice(
            listing.price,
            listing.price_type
        );

}    


    return `
        <a
            href="${detailsUrl}"
            class="block no-underline"
            aria-label="View ${escapeHtml(
                listing.title ||
                "Property"
            )}"
        >

            <article
                class="
                    property-card
                    h-full
                    cursor-pointer
                    transition
                    duration-200
                    hover:-translate-y-1
                    hover:shadow-xl
                "
            >

                ${imageHtml}


                <div class="p-5">

                    <h2
                        class="
                            truncate
                            text-lg
                            font-bold
                            text-slate-900
                        "
                    >
                        ${escapeHtml(
                            listing.title ||
                            "Untitled Property"
                        )}
                    </h2>

                    ${
    listing.city
        ? `
            <div class="mt-2 flex items-center gap-1 text-sm text-slate-500">
                <span>📍</span>
                <span>${escapeHtml(listing.city)}</span>
            </div>
          `
        : ""
}


                    ${
                        listing.subtitle
                            ? `
                                <p
                                    class="
                                        mt-1
                                        line-clamp-2
                                        text-sm
                                        text-slate-500
                                    "
                                >
                                    ${escapeHtml(
                                        listing.subtitle
                                    )}
                                </p>
                            `
                            : ""
                    }


                    <div
                        class="
                            mt-4
                            text-xl
                            font-bold
                            text-slate-900
                        "
                    >
                        ${escapeHtml(
    displayPrice
)}
                    </div>

                </div>

            </article>

        </a>
    `;

};


/*
|--------------------------------------------------------------------------
| Authentication Button
|--------------------------------------------------------------------------
*/

const setupAuthButton = () => {

    const authButton =
        $("propertyAuthButton");


    if (!authButton) {
        return;
    }


    const token =
        localStorage.getItem(
            "propertyToken"
        );


    /*
    |--------------------------------------------------------------------------
    | Logged in
    |--------------------------------------------------------------------------
    */

    if (token) {

        authButton.textContent =
            "Logout";

        authButton.href =
            "#";


        authButton.onclick =
            event => {

                event.preventDefault();


                localStorage.removeItem(
                    "propertyToken"
                );

                localStorage.removeItem(
                    "propertyUser"
                );


                authButton.textContent =
                    "Login";

                authButton.href =
                    "/property/login.html";


                /*
                |--------------------------------------------------------------------------
                | Keep user on dashboard
                |--------------------------------------------------------------------------
                */

                window.location.reload();

            };


        return;

    }


    /*
    |--------------------------------------------------------------------------
    | Logged out
    |--------------------------------------------------------------------------
    */

    authButton.textContent =
        "Login";

    authButton.href =
        "/property/login.html";

};


/*
|--------------------------------------------------------------------------
| Load Properties
|--------------------------------------------------------------------------
*/

const loadProperties =
    async () => {

        const loading =
            $("propertiesLoading");

        const errorBox =
            $("propertiesError");

        const empty =
            $("propertiesEmpty");

        const list =
            $("propertiesList");


        /*
        |--------------------------------------------------------------------------
        | Initial state
        |--------------------------------------------------------------------------
        */

        loading.classList.remove(
            "hidden"
        );

        errorBox.classList.add(
            "hidden"
        );

        empty.classList.add(
            "hidden"
        );

        list.classList.add(
            "hidden"
        );


        try {

            const response =
                await fetch(
                    API,
                    {
                        method: "GET",

                        headers: {
                            "Accept":
                                "application/json"
                        }
                    }
                );


            const data =
                await response.json();


            console.log(
                "Property API response:",
                data
            );


            if (
                !response.ok ||
                !data.success
            ) {

                throw new Error(
                    data.message ||
                    "Unable to load properties."
                );

            }


            /*
            |--------------------------------------------------------------------------
            | API response
            |--------------------------------------------------------------------------
            |
            | {
            |     success: true,
            |     listings: [...]
            | }
            |
            */

            const listings =
                Array.isArray(
                    data.listings
                )
                    ? data.listings
                    : [];


            loading.classList.add(
                "hidden"
            );
            allProperties = listings;


currentPage = 1;

/*
|--------------------------------------------------------------------------
| Populate State Filter
|--------------------------------------------------------------------------
*/

populateStateFilter();

const stateFilter =
    document.getElementById(
        "propertyStateFilter"
    );

const cityFilter =
    document.getElementById(
        "propertyCityFilter"
    );

/*
|--------------------------------------------------------------------------
| Restore saved State
|--------------------------------------------------------------------------
*/

const savedState =
    localStorage.getItem(
        "propertyStateFilter"
    ) || "";

if (stateFilter) {

    const stateExists =
        [...stateFilter.options]
            .some(
                option =>
                    option.value ===
                    savedState
            );

    stateFilter.value =
        stateExists
            ? savedState
            : "";

}

/*
|--------------------------------------------------------------------------
| Populate City according to State
|--------------------------------------------------------------------------
*/

populateCityFilter(
    stateFilter?.value || ""
);

/*
|--------------------------------------------------------------------------
| Restore saved City
|--------------------------------------------------------------------------
*/

const savedCity =
    localStorage.getItem(
        "propertyCityFilter"
    ) || "";

if (cityFilter) {

    const cityExists =
        [...cityFilter.options]
            .some(
                option =>
                    option.value ===
                    savedCity
            );

    cityFilter.value =
        cityExists
            ? savedCity
            : "";

    if (!cityExists) {

        localStorage.removeItem(
            "propertyCityFilter"
        );

    }

}

/*
|--------------------------------------------------------------------------
| Apply restored filters
|--------------------------------------------------------------------------
*/

const searchInput =
    document.getElementById(
        "propertySearch"
    );

if (searchInput) {

    searchInput.dispatchEvent(
        new Event("input")
    );

} else {

    renderProperties(
        listings,
        1
    );

}


            /*
            |--------------------------------------------------------------------------
            | Empty
            |--------------------------------------------------------------------------
            */

            if (
                listings.length === 0
            ) {

                empty.classList.remove(
                    "hidden"
                );

                return;

            }


            /*
            |--------------------------------------------------------------------------
            | Render
            |--------------------------------------------------------------------------
            */


            list.classList.remove(
                "hidden"
            );


        }

        catch (error) {

            console.error(
                "Property listings error:",
                error
            );


            loading.classList.add(
                "hidden"
            );


            errorBox.textContent =
                error.message ||
                "Unable to load properties.";


            errorBox.classList.remove(
                "hidden"
            );

        }

    };

const renderProperties = listings => {

    const list =
        document.getElementById(
            "propertiesList"
        );

    const empty =
        document.getElementById(
            "propertiesEmpty"
        );

    const searchResult =
        document.getElementById(
            "propertySearchResult"
        );


    currentProperties =
        listings;


    const totalPages =
        Math.max(
            1,
            Math.ceil(
                listings.length /
                PAGE_SIZE
            )
        );


    if (
        currentPage >
        totalPages
    ) {

        currentPage =
            totalPages;

    }


    const start =
        (
            currentPage - 1
        ) *
        PAGE_SIZE;


    const pageProperties =
        listings.slice(
            start,
            start + PAGE_SIZE
        );


    if (list) {

        list.innerHTML =
            pageProperties
                .map(
                    listing =>
                        renderPropertyCard(
                            listing
                        )
                )
                .join("");

    }


    if (
        listings.length === 0
    ) {

        list.classList.add(
            "hidden"
        );

        empty.classList.remove(
            "hidden"
        );

    }
    else {

        empty.classList.add(
            "hidden"
        );

        list.classList.remove(
            "hidden"
        );

    }


    if (searchResult) {

        searchResult.textContent =
            `${listings.length} ${
                listings.length === 1
                    ? "property"
                    : "properties"
            } found`;

        searchResult.classList.remove(
            "hidden"
        );

    }


    renderPagination(
        listings.length,
        currentPage
    );

};
const renderPagination = (
    totalItems,
    page
) => {

    const pagination =
        document.getElementById(
            "propertyPagination"
        );

    if (!pagination) {
        return;
    }

    const totalPages =
        Math.ceil(
            totalItems /
            PAGE_SIZE
        );

    if (totalPages <= 1) {

        pagination.innerHTML = "";

        return;

    }

    const createButton = (
        label,
        targetPage,
        disabled = false,
        active = false
    ) => {

        return `
            <button
                type="button"
                data-page="${targetPage}"
                ${disabled ? "disabled" : ""}
                class="
                    min-w-[42px]
                    h-10
                    px-3
                    rounded-xl
                    border
                    text-sm
                    font-semibold
                    transition
                    ${
                        active
                            ? "border-indigo-500 bg-indigo-500 text-white shadow-md"
                            : "border-slate-200 bg-white text-slate-700 hover:border-indigo-300 hover:bg-indigo-50"
                    }
                    ${
                        disabled
                            ? "cursor-not-allowed opacity-40"
                            : "cursor-pointer"
                    }
                "
            >
                ${label}
            </button>
        `;

    };


    let html = `

        <div
            class="
                flex
                w-full
                flex-col
                items-center
                justify-between
                gap-4
                rounded-2xl
                border
                border-slate-200
                bg-white
                px-4
                py-4
                shadow-sm
                sm:flex-row
            "
        >

            <div
                class="
                    text-sm
                    font-medium
                    text-slate-600
                "
            >
                Page
                <span
                    class="font-bold text-slate-900"
                >
                    ${page}
                </span>

                of

                <span
                    class="font-bold text-slate-900"
                >
                    ${totalPages}
                </span>
            </div>

            <div
                class="
                    flex
                    flex-wrap
                    items-center
                    justify-center
                    gap-2
                "
            >

    `;


    html += createButton(
        "‹",
        page - 1,
        page === 1
    );


    /*
     * Page numbers
     */

    for (
        let i = 1;
        i <= totalPages;
        i++
    ) {

        /*
         * Don't show every number when
         * there are many pages.
         */

        if (
            totalPages > 7 &&
            i !== 1 &&
            i !== totalPages &&
            Math.abs(i - page) > 2
        ) {

            if (
                i === 2 ||
                i === totalPages - 1
            ) {

                html += `
                    <span
                        class="
                            px-1
                            text-slate-400
                        "
                    >
                        …
                    </span>
                `;

            }

            continue;

        }


        html += createButton(
            i,
            i,
            false,
            i === page
        );

    }


    html += createButton(
        "›",
        page + 1,
        page === totalPages
    );


    html += `

            </div>

        </div>

    `;


    pagination.innerHTML =
        html;


    pagination
        .querySelectorAll(
            "button[data-page]"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    () => {

                        const targetPage =
                            Number(
                                button.dataset.page
                            );

                        if (
                            targetPage < 1 ||
                            targetPage > totalPages ||
                            targetPage === page
                        ) {
                            return;
                        }

                        currentPage =
                            targetPage;

                        renderProperties(
                            currentProperties
                        );

                        window.scrollTo({
                            top: 0,
                            behavior: "smooth"
                        });

                    }
                );

            }
        );

};
const populateStateFilter = () => {

    const stateFilter =
        document.getElementById(
            "propertyStateFilter"
        );

    if (!stateFilter) {
        return;
    }

    const states = [
        ...new Set(
            allProperties
                .map(
                    listing =>
                        String(
                            listing.state || ""
                        ).trim()
                )
                .filter(Boolean)
        )
    ].sort(
        (a, b) =>
            a.localeCompare(
                b,
                undefined,
                {
                    sensitivity: "base"
                }
            )
    );

    stateFilter.innerHTML = `
        <option value="">
            All States
        </option>
    `;

    states.forEach(state => {

        const option =
            document.createElement(
                "option"
            );

        option.value = state;
        option.textContent = state;

        stateFilter.appendChild(
            option
        );

    });

};


const populateCityFilter = (
    selectedState = ""
) => {

    const cityFilter =
        document.getElementById(
            "propertyCityFilter"
        );

    if (!cityFilter) {
        return;
    }

    const normalizedState =
        String(
            selectedState || ""
        )
            .trim()
            .toLowerCase();

    const cities = [
        ...new Set(
            allProperties
                .filter(listing => {

                    if (!normalizedState) {
                        return true;
                    }

                    return (
                        String(
                            listing.state || ""
                        )
                            .trim()
                            .toLowerCase() ===
                        normalizedState
                    );

                })
                .map(
                    listing =>
                        String(
                            listing.city || ""
                        ).trim()
                )
                .filter(Boolean)
        )
    ].sort(
        (a, b) =>
            a.localeCompare(
                b,
                undefined,
                {
                    sensitivity: "base"
                }
            )
    );

    cityFilter.innerHTML = `
        <option value="">
            All Cities
        </option>
    `;

    cities.forEach(city => {

        const option =
            document.createElement(
                "option"
            );

        option.value = city;
        option.textContent = city;

        cityFilter.appendChild(
            option
        );

    });

};


const setupPropertySearch = () => {

    const searchInput =
        document.getElementById(
            "propertySearch"
        );

    const clearButton =
        document.getElementById(
            "clearPropertySearch"
        );

    const stateFilter =
        document.getElementById(
            "propertyStateFilter"
        );

    const cityFilter =
        document.getElementById(
            "propertyCityFilter"
        );


    if (!searchInput) {
        return;
    }


    /*
    |--------------------------------------------------------------------------
    | Search + State + City
    |--------------------------------------------------------------------------
    */

    const performSearch = () => {

        const keyword =
            searchInput.value
                .trim()
                .toLowerCase();


        const selectedState =
            stateFilter?.value
                .trim()
                .toLowerCase() || "";


        const selectedCity =
            cityFilter?.value
                .trim()
                .toLowerCase() || "";


        const hasAnyFilter =
            Boolean(
                keyword ||
                selectedState ||
                selectedCity
            );


        if (hasAnyFilter) {

            clearButton?.classList.remove(
                "hidden"
            );

        }
        else {

            clearButton?.classList.add(
                "hidden"
            );

        }


        const filteredProperties =
            allProperties.filter(
                listing => {

                    const title =
                        String(
                            listing.title || ""
                        ).toLowerCase();

                    const subtitle =
                        String(
                            listing.subtitle || ""
                        ).toLowerCase();

                    const description =
                        String(
                            listing.description || ""
                        ).toLowerCase();

                    const location =
                        String(
                            listing.location || ""
                        ).toLowerCase();

                    const address =
                        String(
                            listing.address || ""
                        ).toLowerCase();

                    const city =
                        String(
                            listing.city || ""
                        ).toLowerCase();

                    const state =
                        String(
                            listing.state || ""
                        ).toLowerCase();

                    const pincode =
                        String(
                            listing.pincode || ""
                        ).toLowerCase();


                    const searchMatches =
                        !keyword ||

                        title.includes(
                            keyword
                        ) ||

                        subtitle.includes(
                            keyword
                        ) ||

                        description.includes(
                            keyword
                        ) ||

                        location.includes(
                            keyword
                        ) ||

                        address.includes(
                            keyword
                        ) ||

                        city.includes(
                            keyword
                        ) ||

                        state.includes(
                            keyword
                        ) ||

                        pincode.includes(
                            keyword
                        );


                    const stateMatches =
                        !selectedState ||
                        state === selectedState;


                    const cityMatches =
                        !selectedCity ||
                        city === selectedCity;


                    return (
                        searchMatches &&
                        stateMatches &&
                        cityMatches
                    );

                }
            );


        currentPage = 1;

        renderProperties(
            filteredProperties,
            1
        );

    };


    /*
    |--------------------------------------------------------------------------
    | Events
    |--------------------------------------------------------------------------
    */

    searchInput.addEventListener(
        "input",
        performSearch
    );


    stateFilter?.addEventListener(
        "change",
        () => {

            const selectedState =
                stateFilter.value
                    .trim();


            localStorage.setItem(
                "propertyStateFilter",
                selectedState
            );


            /*
            |--------------------------------------------------------------
            | Rebuild City list for selected State
            |--------------------------------------------------------------
            */

            populateCityFilter(
                selectedState
            );


            /*
            |--------------------------------------------------------------
            | Reset City whenever State changes
            |--------------------------------------------------------------
            */

            if (cityFilter) {

                cityFilter.value = "";

            }


            localStorage.removeItem(
                "propertyCityFilter"
            );


            performSearch();

        }
    );


    cityFilter?.addEventListener(
        "change",
        () => {

            const selectedCity =
                cityFilter.value
                    .trim();


            localStorage.setItem(
                "propertyCityFilter",
                selectedCity
            );


            performSearch();

        }
    );


    clearButton?.addEventListener(
        "click",
        () => {

            searchInput.value = "";


            if (stateFilter) {

                stateFilter.value = "";

            }


            if (cityFilter) {

                populateCityFilter();

                cityFilter.value = "";

            }


            localStorage.removeItem(
                "propertyStateFilter"
            );

            localStorage.removeItem(
                "propertyCityFilter"
            );


            searchInput.focus();

            performSearch();

        }
    );


    /*
    |--------------------------------------------------------------------------
    | Initial filtering using restored State + City
    |--------------------------------------------------------------------------
    */

    performSearch();

};


/*
|--------------------------------------------------------------------------
| Initialisation
|--------------------------------------------------------------------------
*/

document.addEventListener(
    "DOMContentLoaded",
    () => {

        setupAuthButton();

setupPropertySearch();

loadProperties();

    }
);