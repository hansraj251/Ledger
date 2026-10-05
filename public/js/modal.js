const Modal = {

    saveHandler: null,

    open(
        title,
        bodyHtml,
        onSave,
        options = {}
    ) {

        document.getElementById("modalTitle").textContent =
            title;

        document.getElementById("modalBody").innerHTML =
            bodyHtml;

        const overlay =
            document.getElementById("modalOverlay");

        overlay.classList.remove("hidden");
        overlay.classList.add("flex");

        const saveBtn =
            document.getElementById("modalSave");

        saveBtn.disabled = false;

        saveBtn.textContent =
            options.buttonText ?? "Save";

        saveBtn.className =
            `rounded-lg px-5 py-2 text-white ${
                options.buttonClass ?? "bg-slate-600"
            }`;

        this.saveHandler = async () => {

            if (saveBtn.disabled) {

                return;

            }

            saveBtn.disabled = true;

            const originalText =
                saveBtn.textContent;

            saveBtn.textContent =
                options.loadingText ??
                "Processing...";

            try {

                await onSave();
                Modal.close();

            }

            catch (err) {

    console.error(err);

    document.getElementById(
        "modalBody"
    ).innerHTML = `
        <p class="text-red-600">
            ${err.message}
        </p>
    `;

}

            finally {

                if (
                    !document
                        .getElementById("modalOverlay")
                        .classList.contains("hidden")
                ) {

                    saveBtn.disabled = false;

                    saveBtn.textContent =
                        originalText;

                }

            }

        };

        saveBtn.onclick =
            this.saveHandler;

    },

    confirm(
        title,
        message,
        onConfirm,
        options = {}
    ) {

        this.open(

            title,

            `
<p class="text-slate-600">
${message}
</p>
`,

            onConfirm,

            {

                buttonText:
                    options.buttonText ??
                    "Confirm",

                buttonClass:
                    options.buttonClass ??
                    "bg-slate-600",

                loadingText:
                    options.loadingText ??
                    "Processing..."

            }

        );

    },

    close() {

        const overlay =
            document.getElementById(
                "modalOverlay"
            );

        overlay.classList.add("hidden");

        overlay.classList.remove("flex");

        this.saveHandler = null;

    }

};

document.addEventListener(
    "click",
    e => {

        if (
            e.target.id ===
            "modalCancel"
        ) {

            Modal.close();

        }

        if (
            e.target.id ===
            "modalOverlay"
        ) {

            Modal.close();

        }

    }
);

document.addEventListener(
    "keydown",
    e => {

        const overlay =
            document.getElementById(
                "modalOverlay"
            );

        if (
            overlay.classList.contains("hidden")
        ) {

            return;

        }

        if (e.key === "Escape") {

            Modal.close();

        }

        if (
            e.key === "Enter" &&
            Modal.saveHandler
        ) {

            e.preventDefault();

            Modal.saveHandler();

        }

    }
);