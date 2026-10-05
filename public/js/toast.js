const Toast = {

    show(
        message,
        type = "success"
    ) {

        const toast =
            document.createElement(
                "div"
            );

        toast.className =
            `fixed top-5 right-5 z-50 rounded-lg px-5 py-3 text-white shadow-lg transition-all`;

        if (
            type === "success"
        ) {

            toast.style.background =
                "linear-gradient(90deg, #c13bbd 0%, #7b3fc6 50%, #2454c7 100%)";

        } else {

            toast.style.background =
                "linear-gradient(90deg, #b83280 0%, #7b3fc6 50%, #4338ca 100%)";

        }

        toast.textContent =
            message;

        document.body.appendChild(
            toast
        );

        setTimeout(
            () => {

                toast.remove();

            },
            3000
        );

    }

};