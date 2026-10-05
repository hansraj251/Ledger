document.addEventListener(
    "DOMContentLoaded",
    () => {

        /*
        |--------------------------------------------------------------------------
        | Authentication
        |--------------------------------------------------------------------------
        */

        const token =
            localStorage.getItem(
                "propertyToken"
            );



        if (!token) {

            window.location.href =
                "/property/login.html?redirect=profile";

            return;

        }
        


        /*
        |--------------------------------------------------------------------------
        | Listing ID
        |--------------------------------------------------------------------------
        */

        const params =
            new URLSearchParams(
                window.location.search
            );

        const listingId =
            Number(
                params.get("id")
            );


        if (
            !Number.isInteger(listingId) ||
            listingId <= 0
        ) {

            window.location.href =
                "/property/profile.html";

            return;

        }


        /*
        |--------------------------------------------------------------------------
        | Elements
        |--------------------------------------------------------------------------
        */

        const form =
            document.getElementById(
                "propertyForm"
            );

        const errorBox =
            document.getElementById(
                "propertyFormError"
            );

        const submitBtn =
            document.getElementById(
                "propertyFormSubmitBtn"
            );

        const submitText =
            document.getElementById(
                "propertyFormSubmitText"
            );
        const photoInput =
    document.getElementById(
        "propertyPhotos"
    );    
    photoInput.addEventListener(
    "change",
    () => {

        const existingPhotoCount =
            document.querySelectorAll(
                "#propertyPhotoPreview [data-existing-image]"
            ).length;

        const newlySelected =
            Array.from(
                photoInput.files || []
            );

        const combinedFiles =
            Array.from(
                selectedPhotoFiles.files
            ).concat(
                newlySelected
            );

        /*
         * Existing + new must not exceed 8.
         */
        if (
            existingPhotoCount +
            combinedFiles.length >
            8
        ) {

            showError(
                `A listing can have maximum 8 photos. You currently have ${existingPhotoCount} existing photo(s) and selected ${combinedFiles.length} new photo(s).`
            );

            photoInput.value = "";

            return;
        }


        /*
         * Replace DataTransfer with combined files.
         */
        selectedPhotoFiles.items.clear();

        combinedFiles.forEach(
            file => {

                selectedPhotoFiles.items.add(
                    file
                );

            }
        );


        photoInput.files =
            selectedPhotoFiles.files;


        clearError();

        renderNewPhotoPreview();

    }
);

        const titleInput =
            document.getElementById(
                "propertyTitle"
            );

        const subtitleInput =
            document.getElementById(
                "propertySubtitle"
            );

        const descriptionInput =
            document.getElementById(
                "propertyDescription"
            );

        const priceInput =
            document.getElementById(
                "propertyPrice"
            );

        const rentInput =
            document.getElementById(
                "propertyRent"
            );

        const priceTypeInput =
            document.getElementById(
                "propertyPriceType"
            );

        const contactShow =
            document.getElementById(
                "contactShow"
            );

        const contactRequest =
            document.getElementById(
                "contactRequest"
            );

        const stateInput =
            document.getElementById(
                "propertyState"
            );

        const cityInput =
            document.getElementById(
                "propertyCity"
            );

        const locationData =
            window.PROPERTY_LOCATIONS || {};

        const populateCities = (
            selectedState = "",
            selectedCity = ""
        ) => {

            if (!stateInput || !cityInput) {
                return;
            }

            if (
                stateInput.options.length <= 1
            ) {

                Object.keys(locationData)
                    .sort((a, b) =>
                        a.localeCompare(
                            b,
                            undefined,
                            {
                                sensitivity: "base"
                            }
                        )
                    )
                    .forEach(state => {

                        const option =
                            document.createElement(
                                "option"
                            );

                        option.value = state;
                        option.textContent = state;

                        stateInput.appendChild(
                            option
                        );
                    });
            }

            stateInput.value =
                selectedState || "";

            cityInput.innerHTML = "";

            const cities =
                locationData[
                    selectedState
                ] || [];

            if (!selectedState) {

                cityInput.disabled = true;

                cityInput.innerHTML =
                    '<option value="">Select State First</option>';

                return;
            }

            cityInput.disabled = false;

            cityInput.innerHTML =
                '<option value="">Select City</option>';

            cities
                .slice()
                .sort((a, b) =>
                    a.localeCompare(
                        b,
                        undefined,
                        {
                            sensitivity: "base"
                        }
                    )
                )
                .forEach(city => {

                    const option =
                        document.createElement(
                            "option"
                        );

                    option.value = city;
                    option.textContent = city;

                    cityInput.appendChild(
                        option
                    );
                });

            cityInput.value =
                selectedCity || "";
        };

        stateInput?.addEventListener(
            "change",
            () => {

                populateCities(
                    stateInput.value,
                    ""
                );
            }
        );

        const addressInput =
            document.getElementById(
                "propertyAddress"
            );
        const locationInput =
    document.getElementById(
        "propertyLocation"
    );

const pincodeInput =
    document.getElementById(
        "propertyPincode"
    );    


        /*
        |--------------------------------------------------------------------------
        | Helpers
        |--------------------------------------------------------------------------
        */

        const clearError =
            () => {

                errorBox.textContent =
                    "";

                errorBox.classList.add(
                    "hidden"
                );

            };


        const showError =
            message => {

                errorBox.textContent =
                    message ||
                    "Something went wrong.";

                errorBox.classList.remove(
                    "hidden"
                );

                errorBox.scrollIntoView({
                    behavior: "smooth",
                    block: "center"
                });

            };


        const escapeHtml =
            value => {

                const div =
                    document.createElement(
                        "div"
                    );

                div.textContent =
                    value === null ||
                    value === undefined
                        ? ""
                        : String(value);

                return div.innerHTML;

            };


        /*
        |--------------------------------------------------------------------------
        | Load Listing
        |--------------------------------------------------------------------------
        */

        const loadListing =
            async () => {

                try {

                    const response =
                        await fetch(
                            `/api/property/listings/mine/${listingId}`,
                            {
                                method: "GET",

                                headers: {
                                    "Accept":
                                        "application/json",

                                    "Authorization":
                                        `Bearer ${token}`
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
                            `/property/login.html?redirect=${encodeURIComponent(
                                window.location.pathname +
                                window.location.search
                            )}`;

                        return;

                    }


                    if (
                        !response.ok ||
                        !data.success ||
                        !data.listing
                    ) {

                        throw new Error(
                            data.message ||
                            "Unable to load property."
                        );

                    }


                    const listing =
                        data.listing;


                    /*
                    |--------------------------------------------------------------------------
                    | Fill form
                    |--------------------------------------------------------------------------
                    */

                    titleInput.value =
                        listing.title ||
                        "";

                    subtitleInput.value =
                        listing.subtitle ||
                        "";

                    descriptionInput.value =
                        listing.description ||
                        "";

                    populateCities(
                        listing.state || "",
                        listing.city || ""
                    );
                    if (locationInput) {
    locationInput.value =
        listing.location || "";
}

if (pincodeInput) {
    pincodeInput.value =
        listing.pincode || "";
}

                    if (addressInput) {
                        addressInput.value =
                            listing.address ||
                            "";
                    }


                    if (
                        listing.price !== null &&
                        listing.price !== undefined
                    ) {

                        priceInput.value =
                            listing.price;

                    }
                    else {

                        priceInput.value =
                            "";

                    }


                    if (
                        listing.rent_amount !== null &&
                        listing.rent_amount !== undefined
                    ) {

                        rentInput.value =
                            listing.rent_amount;

                    }
                    else {

                        rentInput.value =
                            "";

                    }


                    priceTypeInput.value =
                        listing.price_type ||
                        "fixed";


                    const contactPreference =
                        listing.contact_preference ||
                        "show";


                    if (
                        contactPreference ===
                        "request"
                    ) {

                        contactRequest.checked =
                            true;

                    }
                    else {

                        contactShow.checked =
                            true;

                    }


                    /*
                    |--------------------------------------------------------------------------
                    | Existing photos
                    |--------------------------------------------------------------------------
                    */

                    await loadExistingPhotos(listing);

                }
                catch (error) {

                    console.error(
                        "Load listing error:",
                        error
                    );

                    showError(
                        error.message ||
                        "Unable to load property."
                    );

                    submitBtn.disabled =
                        true;

                }

            };


        /*
        |--------------------------------------------------------------------------
        | Existing Photos
        |--------------------------------------------------------------------------
        */
const loadExistingPhotos = async listing => {

    const preview =
        document.getElementById(
            "propertyPhotoPreview"
        );

    if (!preview) {
        return;
    }

    try {

        const response =
            await fetch(
                `/api/property/listings/${listingId}/images`,
                {
                    method: "GET",

                    headers: {
                        "Accept":
                            "application/json",

                        "Authorization":
                            `Bearer ${token}`
                    }
                }
            );

        let data = {};

        try {
            data = await response.json();
        }
        catch {
            throw new Error(
                "Invalid image response."
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
                `/property/login.html?redirect=${encodeURIComponent(
                    window.location.pathname +
                    window.location.search
                )}`;

            return;
        }

        if (
            !response.ok ||
            !data.success
        ) {
            throw new Error(
                data.message ||
                "Unable to load property photos."
            );
        }

        listing.photos =
            Array.isArray(data.images)
                ? data.images
                : [];

        renderExistingPhotos(listing);

    }
    catch (error) {

        console.error(
            "Load property images error:",
            error
        );

        preview.innerHTML = `
            <p class="col-span-full text-sm text-red-500">
                Unable to load property photos.
            </p>
        `;
    }
};
      const renderExistingPhotos = listing => {

    const preview =
        document.getElementById(
            "propertyPhotoPreview"
        );

    if (!preview) {
        return;
    }

    const photos =
        Array.isArray(listing.photos)
            ? listing.photos
            : [];

    if (photos.length === 0) {

        preview.innerHTML = `
            <p class="col-span-full text-sm text-slate-400">
                No photos added yet.
            </p>
        `;

        return;
    }

    preview.innerHTML =
        photos
            .map(
                photo => {

                    const imageId =
                        photo &&
                        typeof photo === "object"
                            ? Number(photo.id)
                            : null;

                    const url =
                        typeof photo === "string"
                            ? photo
                            : photo?.url ||
                              photo?.path ||
                              photo?.image_url ||
                              "";

                    if (!url) {
                        return "";
                    }

                    return `
                        <div
                            class="
                                relative
                                overflow-hidden
                                rounded-xl
                                border
                                border-slate-200
                                bg-slate-50
                            "
                            data-existing-image="true"
                            data-image-id="${imageId || ""}"
                        >

                            <img
                                src="${escapeHtml(url)}"
                                alt="Property photo"
                                class="
                                    h-32
                                    w-full
                                    object-cover
                                "
                            >

                            ${
                                imageId
                                    ? `
                                        <button
    type="button"
    data-delete-existing-image="${imageId}"
    title="Remove photo"
    aria-label="Remove photo"
    style="
        position:absolute;
        top:8px;
        right:8px;
        z-index:50;
        width:32px;
        height:32px;
        padding:0;
        border:0;
        border-radius:9999px;
        background:rgba(255,255,255,0.95);
        color:#334155;
        font-size:22px;
        font-weight:700;
        line-height:32px;
        text-align:center;
        cursor:pointer;
        box-shadow:0 2px 8px rgba(0,0,0,0.2);
    "
>
    ×
</button>
                                    `
                                    : ""
                            }

                        </div>
                    `;
                }
            )
            .join("");


    /*
     * Bind X buttons for EXISTING photos.
     */
    preview
        .querySelectorAll(
            "[data-delete-existing-image]"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    async () => {

                        const imageId =
                            Number(
                                button.dataset
                                    .deleteExistingImage
                            );

                        if (
                            !Number.isInteger(
                                imageId
                            ) ||
                            imageId <= 0
                        ) {
                            return;
                        }


                        const confirmed =
                            window.confirm(
                                "Remove this photo?"
                            );

                        if (!confirmed) {
                            return;
                        }


                        button.disabled =
                            true;


                        button.textContent =
                            "…";


                        try {


                            const response =
                                await fetch(
                                    `/api/property/listings/${listingId}/images/${imageId}`,
                                    {
                                        method: "DELETE",

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
                                    "propertyUser"
                                );

                                window.location.href =
                                    `/property/login.html?redirect=${encodeURIComponent(
                                        window.location.pathname +
                                        window.location.search
                                    )}`;

                                return;
                            }


                            if (
                                !response.ok ||
                                !data.success
                            ) {

                                throw new Error(
                                    data.message ||
                                    "Unable to remove photo."
                                );

                            }


                            /*
                             * Remove card immediately.
                             */
                            const card =
                                button.closest(
                                    "[data-existing-image]"
                                );

                            if (card) {
                                card.remove();
                            }


                            /*
                             * If no photos remain,
                             * show empty message.
                             */
                            const remaining =
                                preview.querySelectorAll(
                                    "[data-existing-image]"
                                ).length;

                            if (
                                remaining === 0 &&
                                selectedPhotoFiles.files.length === 0
                            ) {

                                preview.innerHTML = `
                                    <p class="col-span-full text-sm text-slate-400">
                                        No photos added yet.
                                    </p>
                                `;

                            }

                        }
                        catch (error) {

                            console.error(
                                "Delete existing property photo error:",
                                error
                            );


                            button.disabled =
                                false;

                            button.textContent =
                                "×";


                            showError(
                                error.message ||
                                "Unable to remove photo."
                            );

                        }

                    }
                );

            }
        );

};
    const selectedPhotoFiles = new DataTransfer();

const renderNewPhotoPreview = () => {

    const preview =
        document.getElementById(
            "propertyPhotoPreview"
        );

    if (!preview) {
        return;
    }

    /*
     * Preserve existing photo cards.
     */
    const existingCards =
        Array.from(
            preview.querySelectorAll(
                "[data-existing-image]"
            )
        );

    /*
     * Remove only previously rendered
     * new-photo cards.
     */
    preview
        .querySelectorAll(
            "[data-new-photo]"
        )
        .forEach(
            card => card.remove()
        );


    /*
     * Render selected new photos.
     */
    Array.from(
        selectedPhotoFiles.files
    ).forEach(
        (file, index) => {

            const url =
                URL.createObjectURL(file);

            const card =
                document.createElement(
                    "div"
                );

            card.className =
                "relative overflow-hidden rounded-xl border border-slate-200 bg-slate-50";

            card.setAttribute(
                "data-new-photo",
                "true"
            );

            card.innerHTML = `

                <img
                    src="${url}"
                    alt="New property photo"
                    class="
                        h-32
                        w-full
                        object-cover
                    "
                >

                <button
                    type="button"
                    data-remove-new-photo="${index}"
                    title="Remove photo"
                    aria-label="Remove photo"
                    style="
        position:absolute;
        top:8px;
        right:8px;
        z-index:50;
        width:32px;
        height:32px;
        padding:0;
        border:0;
        border-radius:9999px;
        background:rgba(255,255,255,0.95);
        color:#334155;
        font-size:22px;
        font-weight:700;
        line-height:32px;
        text-align:center;
        cursor:pointer;
        box-shadow:0 2px 8px rgba(0,0,0,0.2);
    "
                >
                    ×
                </button>

            `;

            preview.appendChild(
                card
            );
        }
    );


    /*
     * Bind X buttons for NEW photos only.
     */
    preview
        .querySelectorAll(
            "[data-remove-new-photo]"
        )
        .forEach(
            button => {

                button.addEventListener(
                    "click",
                    () => {

                        const removeIndex =
                            Number(
                                button.dataset
                                    .removeNewPhoto
                            );

                        if (
                            !Number.isInteger(
                                removeIndex
                            )
                        ) {
                            return;
                        }


                        const newTransfer =
                            new DataTransfer();


                        Array.from(
                            selectedPhotoFiles.files
                        ).forEach(
                            (
                                file,
                                fileIndex
                            ) => {

                                if (
                                    fileIndex !==
                                    removeIndex
                                ) {

                                    newTransfer
                                        .items
                                        .add(file);

                                }

                            }
                        );


                        selectedPhotoFiles.items
                            .clear();


                        Array.from(
                            newTransfer.files
                        ).forEach(
                            file => {

                                selectedPhotoFiles
                                    .items
                                    .add(file);

                            }
                        );


                        photoInput.files =
                            selectedPhotoFiles.files;


                        renderNewPhotoPreview();

                    }
                );

            }
        );

};

        /*
        |--------------------------------------------------------------------------
        | Submit / Update
        |--------------------------------------------------------------------------
        */

        form.addEventListener(
            "submit",
            async event => {

                event.preventDefault();

                clearError();


                const title =
                    titleInput.value.trim();

                const subtitle =
                    subtitleInput.value.trim();

                const description =
                    descriptionInput.value.trim();

                const priceValue =
                    priceInput.value.trim();

                const rentValue =
                    rentInput.value.trim();

                const priceType =
                    priceTypeInput.value;

                const contactPreference =
                    document.querySelector(
                        'input[name="contactPreference"]:checked'
                    )?.value ||
                    "show";


                /*
                |--------------------------------------------------------------------------
                | Validation
                |--------------------------------------------------------------------------
                */

                if (!title) {

                    showError(
                        "Please enter a property title."
                    );

                    titleInput.focus();

                    return;

                }


                if (
                    priceValue &&
                    rentValue
                ) {

                    showError(
                        "Please enter either Sale Price or Rent / Month, not both."
                    );

                    return;

                }


                if (
                    !priceValue &&
                    !rentValue
                ) {

                    showError(
                        "Please enter either Sale Price or Rent / Month."
                    );

                    return;

                }


                if (
                    priceValue &&
                    Number(priceValue) < 0
                ) {

                    showError(
                        "Sale price cannot be negative."
                    );

                    return;

                }


                if (
                    rentValue &&
                    Number(rentValue) < 0
                ) {

                    showError(
                        "Rent cannot be negative."
                    );

                    return;

                }


                /*
                |--------------------------------------------------------------------------
                | Request body
                |--------------------------------------------------------------------------
                */

                const body = {

                    title,

                    subtitle,

                    description,

                    price:
                        priceValue
                            ? Number(
                                priceValue
                            )
                            : null,

                    rentAmount:
                        rentValue
                            ? Number(
                                rentValue
                            )
                            : null,

                    priceType,

                    contactPreference,

                    location:
    locationInput?.value.trim() || null,

pincode:
    pincodeInput?.value.trim() || null,

                    state:
                        stateInput?.value.trim() || null,

                    city:
                        cityInput?.value.trim() || null,

                    address:
                        addressInput?.value.trim() || null

                };
                const newPhotos =
    photoInput?.files
        ? Array.from(photoInput.files)
        : [];
        const existingPhotoCount =
    document.querySelectorAll(
        "#propertyPhotoPreview [data-existing-image]"
    ).length;

if (
    existingPhotoCount +
    newPhotos.length >
    8
) {

    showError(
        `A listing can have maximum 8 photos. You currently have ${existingPhotoCount} existing photo(s) and selected ${newPhotos.length} new photo(s).`
    );

    submitBtn.disabled =
        false;

    submitText.textContent =
        "Save Changes";

    return;
}
        


                /*
                |--------------------------------------------------------------------------
                | Loading state
                |--------------------------------------------------------------------------
                */

                submitBtn.disabled =
                    true;

                submitText.textContent =
                    "Saving...";


                try {

                    const response =
                        await fetch(
                            `/api/property/listings/${listingId}`,
                            {
                                method: "PUT",

                                headers: {

                                    "Content-Type":
                                        "application/json",

                                    "Accept":
                                        "application/json",

                                    "Authorization":
                                        `Bearer ${token}`

                                },

                                body:
                                    JSON.stringify(
                                        body
                                    )

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
                            "propertyUser"
                        );

                        window.location.href =
                            `/property/login.html?redirect=${encodeURIComponent(
                                window.location.pathname +
                                window.location.search
                            )}`;

                        return;

                    }


                    if (
                        !response.ok ||
                        !data.success
                    ) {

                        throw new Error(
                            data.message ||
                            "Unable to update property."
                        );

                    }


                    /*
                    |--------------------------------------------------------------------------
                    | Success
                    |--------------------------------------------------------------------------
                    */

                    /*
|--------------------------------------------------------------------------
| Upload new photos
|--------------------------------------------------------------------------
*/

if (newPhotos.length > 0) {

    submitText.textContent =
        "Uploading Photos...";

    const formData =
        new FormData();

    newPhotos.forEach(
        file => {

            formData.append(
    "photos",
    file
);

        }
    );

    const imageResponse =
        await fetch(
            `/api/property/listings/${listingId}/images`,
            {
                method: "POST",

                headers: {
                    "Authorization":
                        `Bearer ${token}`
                },

                body: formData
            }
        );

    let imageData = {};

    try {

        imageData =
            await imageResponse.json();

    }
    catch {

        throw new Error(
            "Invalid image upload response."
        );

    }

    if (
        imageResponse.status === 401 ||
        imageResponse.status === 403
    ) {

        localStorage.removeItem(
            "propertyToken"
        );

        localStorage.removeItem(
            "propertyUser"
        );

        window.location.href =
            `/property/login.html?redirect=${encodeURIComponent(
                window.location.pathname +
                window.location.search
            )}`;

        return;

    }

    if (
        !imageResponse.ok ||
        !imageData.success
    ) {

        throw new Error(
            imageData.message ||
            "Property details saved, but photos could not be uploaded."
        );

    }

}


submitText.textContent =
    "Saved";


window.location.href =
    "/property/profile.html";

                }
                catch (error) {

                    console.error(
                        "Update listing error:",
                        error
                    );

                    showError(
                        error.message ||
                        "Unable to update property."
                    );

                    submitBtn.disabled =
                        false;

                    submitText.textContent =
                        "Save Changes";

                }

            }
        );


        /*
        |--------------------------------------------------------------------------
        | Initial load
        |--------------------------------------------------------------------------
        */

        loadListing();

    }
);