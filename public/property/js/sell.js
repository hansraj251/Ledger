/*
|--------------------------------------------------------------------------
| Align Properties - Sell Page
|--------------------------------------------------------------------------
*/

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
                "/property/login.html?redirect=sell";

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

        const priceTypeInput =
            document.getElementById(
                "propertyPriceType"
            );
        const rentInput =
            document.getElementById(
             "propertyRent"
            );   
        const salePriceInput =
    document.getElementById(
        "propertyPrice"
    );


if (
    salePriceInput &&
    rentInput
) {

    salePriceInput.addEventListener(
        "input",
        () => {

            if (
                salePriceInput.value
            ) {

                rentInput.value = "";

                rentInput.disabled =
                    true;

            }
            else {

                rentInput.disabled =
                    false;

            }

        }
    );


    rentInput.addEventListener(
        "input",
        () => {

            if (
                rentInput.value
            ) {

                salePriceInput.value =
                    "";

                salePriceInput.disabled =
                    true;

            }
            else {

                salePriceInput.disabled =
                    false;

            }

        }
    );

}
if (
    salePriceInput &&
    rentInput
) {

    if (
        salePriceInput.value
    ) {

        rentInput.disabled = true;

    }

    if (
        rentInput.value
    ) {

        salePriceInput.disabled = true;

    }

}     

        

        const photosInput =
            document.getElementById(
                "propertyPhotos"
            );

        const photoPreview =
            document.getElementById(
                "propertyPhotoPreview"
            );
        const contactPreferenceInputs =
    document.querySelectorAll(
        'input[name="contactPreference"]'
    );    

        const stateInput =
            document.getElementById(
                "state"
            );

        const cityInput =
            document.getElementById(
                "city"
            );

        const locationData =
            window.PROPERTY_LOCATIONS || {};

        if (stateInput && cityInput) {

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

            stateInput.addEventListener(
                "change",
                () => {

                    cityInput.innerHTML = "";

                    const state =
                        stateInput.value;

                    if (!state) {

                        cityInput.disabled = true;

                        cityInput.innerHTML =
                            '<option value="">Select State First</option>';

                        return;
                    }

                    cityInput.disabled = false;

                    cityInput.innerHTML =
                        '<option value="">Select City</option>';

                    (locationData[state] || [])
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
                }
            );
        }

        const submitButton =
            document.getElementById(
                "propertyFormSubmitBtn"
            );

        const submitText =
            document.getElementById(
                "propertyFormSubmitText"
            );

        const errorBox =
            document.getElementById(
                "propertyFormError"
            );


        /*
        |--------------------------------------------------------------------------
        | Error helpers
        |--------------------------------------------------------------------------
        */

        const showError =
            message => {

                errorBox.textContent =
                    message ||
                    "Something went wrong.";

                errorBox.classList.remove(
                    "hidden"
                );

                window.scrollTo({
                    top: 0,
                    behavior: "smooth"
                });

            };


        const clearError =
            () => {

                errorBox.textContent =
                    "";

                errorBox.classList.add(
                    "hidden"
                );

            };


        /*
        |--------------------------------------------------------------------------
        | Photo Preview
        |--------------------------------------------------------------------------
        */

        photosInput.addEventListener(
            "change",
            () => {

                clearError();

                photoPreview.innerHTML =
                    "";


                const files =
                    Array.from(
                        photosInput.files || []
                    );


                for (
                    const file of files
                ) {

                    if (
                        file.size >
                        5 * 1024 * 1024
                    ) {

                        photosInput.value =
                            "";

                        photoPreview.innerHTML =
                            "";

                        showError(
                            `"${file.name}" is larger than 5 MB.`
                        );

                        return;

                    }


                    if (
                        !file.type.startsWith(
                            "image/"
                        )
                    ) {

                        photosInput.value =
                            "";

                        photoPreview.innerHTML =
                            "";

                        showError(
                            `"${file.name}" is not a valid image.`
                        );

                        return;

                    }


                    const image =
                        document.createElement(
                            "img"
                        );


                    image.src =
                        URL.createObjectURL(
                            file
                        );

                    image.alt =
                        file.name;

                    image.className =
                        `
                            h-32
                            w-full
                            rounded-xl
                            border
                            border-slate-200
                            object-cover
                        `;


                    photoPreview.appendChild(
                        image
                    );

                }

            }
        );


        /*
        |--------------------------------------------------------------------------
        | Submit
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


                const priceType =
    priceTypeInput.value;

const rentValue =
    rentInput.value.trim();


                const files =
                    Array.from(
                        photosInput.files || []
                    );


                /*
                |--------------------------------------------------------------------------
                | Validation
                |--------------------------------------------------------------------------
                */

                if (!title) {

                    showError(
                        "Please enter a title."
                    );

                    titleInput.focus();

                    return;

                }


                if (
                    title.length >
                    150
                ) {

                    showError(
                        "Title cannot be more than 150 characters."
                    );

                    titleInput.focus();

                    return;

                }


                if (
                    priceValue !== "" &&
                    Number(priceValue) < 0
                ) {

                    showError(
                        "Price cannot be negative."
                    );

                    priceInput.focus();

                    return;

                }


                for (
                    const file of files
                ) {

                    if (
                        file.size >
                        5 * 1024 * 1024
                    ) {

                        showError(
                            `"${file.name}" is larger than 5 MB.`
                        );

                        return;

                    }

                }


                /*
                |--------------------------------------------------------------------------
                | Request body
                |--------------------------------------------------------------------------
                */

                const body = {

    title:
        title,

    subtitle:
        subtitle,

    description:
        description,

    price:
    priceValue === ""
        ? null
        : Number(
            priceValue
        ),

priceType:
    priceType,

rentAmount:
    rentValue === ""
        ? null
        : Number(
            rentValue
        ),
    location:
    document.getElementById("location")?.value.trim() || null,

address:
    document.getElementById("address")?.value.trim() || null,

city:
    document.getElementById("city")?.value.trim() || null,

state:
    document.getElementById("state")?.value.trim() || null,

pincode:
    document.getElementById("pincode")?.value.trim() || null,    

    contactPreference:
        document.querySelector(
            'input[name="contactPreference"]:checked'
        )?.value || "show"

};


                /*
                |--------------------------------------------------------------------------
                | Disable button
                |--------------------------------------------------------------------------
                */

                submitButton.disabled =
                    true;

                submitText.textContent =
                    "Creating...";


                try {

                    /*
                    |--------------------------------------------------------------------------
                    | Create listing
                    |--------------------------------------------------------------------------
                    */

                    const response =
                        await fetch(
                            "/api/property/listings",
                            {
                                method: "POST",

                                headers: {
                                    "Content-Type":
                                        "application/json",

                                    "Authorization":
                                        `Bearer ${token}`,

                                    "Accept":
                                        "application/json"
                                },

                                body:
                                    JSON.stringify(
                                        body
                                    )
                            }
                        );


                    /*
                    |--------------------------------------------------------------------------
                    | Parse response safely
                    |--------------------------------------------------------------------------
                    */

                    let data = {};

                    try {

                        data =
                            await response.json();

                    }

                    catch {

                        throw new Error(
                            "Server returned an invalid response."
                        );

                    }


                    /*
                    |--------------------------------------------------------------------------
                    | Authentication failure
                    |--------------------------------------------------------------------------
                    */

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
                            "/property/login.html?redirect=sell";

                        return;

                    }


                    /*
                    |--------------------------------------------------------------------------
                    | Create failure
                    |--------------------------------------------------------------------------
                    */

                    if (
                        !response.ok ||
                        !data.success
                    ) {

                        throw new Error(
                            data.message ||
                            "Unable to create property."
                        );

                    }


                    if (
                        !data.listing ||
                        !data.listing.id
                    ) {

                        throw new Error(
                            "Property was created but listing ID was not returned."
                        );

                    }


                    const listingId =
                        Number(
                            data.listing.id
                        );


                    /*
                    |--------------------------------------------------------------------------
                    | Upload photos
                    |--------------------------------------------------------------------------
                    */

                    if (
                        files.length > 0
                    ) {

                        submitText.textContent =
                            "Uploading Photos...";


                        const imageData =
                            new FormData();


                        files.forEach(
                            file => {

                                imageData.append(
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
                                            `Bearer ${token}`,

                                        "Accept":
                                            "application/json"
                                    },

                                    body:
                                        imageData
                                }
                            );


                        let imageResult =
                            {};

                        try {

                            imageResult =
                                await imageResponse.json();

                        }

                        catch {

                            throw new Error(
                                "Server returned an invalid image upload response."
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
                                "/property/login.html?redirect=sell";

                            return;

                        }


                        if (
                            !imageResponse.ok ||
                            !imageResult.success
                        ) {

                            throw new Error(
                                imageResult.message ||
                                "Property created but photos could not be uploaded."
                            );

                        }

                    }


                    /*
                    |--------------------------------------------------------------------------
                    | Success
                    |--------------------------------------------------------------------------
                    */

                    submitText.textContent =
                        "Published";


                    Toast.show(
    "Property published successfully.",
    "success"
);

setTimeout(
    () => {

        window.location.href =
            "/property/dashboard.html";

    },
    800
);


                    window.location.href =
                        "/property/dashboard.html";

                }

                catch (error) {

                    console.error(
                        "Sell property error:",
                        error
                    );


                    showError(
                        error.message ||
                        "Unable to create property."
                    );

                }

                finally {

                    submitButton.disabled =
                        false;

                    submitText.textContent =
                        "Create Property";

                }

            }
        );

    }
);