async function loadComponent(elementId, filePath) {

    const response = await fetch(filePath);

    const html = await response.text();

    document.getElementById(elementId).innerHTML = html;
    const currentPage =
    window.location.pathname
        .split("/")
        .pop()
        .replace(".html", "");

const activeLink = document.querySelector(
    `[data-page="${currentPage}"]`
);

if (activeLink) {

    activeLink.classList.remove("hover:bg-slate-100");

    activeLink.classList.add(
        "bg-slate-600",
        "text-white"
    );

}
if (elementId === "sidebar") {

    if (typeof loadSidebarRestaurant === "function") {

        loadSidebarRestaurant();

    }

    if (typeof initializeSidebar === "function") {

        initializeSidebar();

    }

}
if (elementId === "sidebar") {

    if (typeof loadSidebarRestaurant === "function") {

        loadSidebarRestaurant();

    }

    if (typeof initializeSidebar === "function") {

        initializeSidebar();

    }

    if (typeof MobileSidebar !== "undefined") {

        MobileSidebar.init();

    }

}

}
