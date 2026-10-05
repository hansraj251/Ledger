function loadSuperAdminLayout(options) {

    const title = options.title;

    const active = options.active;


    document.body.innerHTML = `

<div
    class="flex h-screen overflow-hidden bg-slate-100">

    <div
        id="sidebarOverlay"
        class="fixed inset-0 z-40 hidden bg-black/40 md:hidden">
    </div>


    <aside
        id="sidebar"
        class="
            fixed
            left-0
            top-0
            z-50
            flex
            h-full
            w-72
            max-w-[85vw]
            -translate-x-full
            flex-col
            overflow-y-auto
            border-r
            border-slate-200
            bg-white
            text-slate-800
            shadow-lg
            transition-transform
            duration-300
            md:static
            md:w-64
            md:translate-x-0
            md:shadow-sm
        ">

        <div
            class="
                border-b
                border-slate-200
                px-6
                py-5
            ">

            <div
    class="
        border-b
        border-slate-200
        px-6
        py-5
    ">

    <a
        href="/index.html"
        class="flex items-center gap-2">

        <img
            src="/images/logo.png"
            alt="AlignOS Logo"
            class="h-10 w-10 object-contain">

        <span
            class="
                bg-gradient-to-r
                from-[#c13bbd]
                via-[#7b3fc6]
                to-[#2454c7]
                bg-clip-text
                text-2xl
                font-bold
                text-transparent
            ">

            AlignOS

        </span>

    </a>

</div>
        </div>


        <nav
            class="
                flex-1
                space-y-1.5
                p-4
            ">


            <div>

                <a
                    href="/super-admin/dashboard.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "dashboard"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Dashboard

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/restaurants.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "restaurants"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Users

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/plans.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "plans"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Food Plans

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/school-plans.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "school-plans"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    School Plans

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/pricing.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "pricing"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Pricing

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/accounts.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "accounts"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Accounts

                </a>

            </div>


            <div>

                <a
                    href="/super-admin/payments.html"
                    class="
                        block
                        rounded-xl
                        px-4
                        py-3
                        text-sm
                        font-medium
                        transition
                        ${
                            active === "payments"

                            ? "bg-indigo-500 text-white shadow-sm"

                            : "text-slate-600 hover:bg-indigo-50 hover:text-indigo-700"
                        }
                    ">

                    Payments

                </a>

            </div>


        </nav>


        <div
            class="
                border-t
                border-slate-200
                p-4
            ">

            <a
                href="#"
                onclick="SuperAdminAuth.logout(); return false;"
                class="
                    block
                    rounded-xl
                    bg-red-50
                    px-4
                    py-3
                    text-center
                    text-sm
                    font-semibold
                    text-red-600
                    transition
                    hover:bg-red-100
                ">

                Logout

            </a>

        </div>

    </aside>


    <div
        class="
            fixed
            left-0
            right-0
            top-0
            z-40
            flex
            h-16
            items-center
            justify-between
            border-b
            border-slate-200
            bg-white
            px-4
            shadow-sm
            md:hidden
        ">

        <button
            id="menuButton"
            class="
                rounded-xl
                border
                border-slate-200
                bg-white
                p-2
                text-slate-700
                shadow-sm
                transition
                hover:bg-indigo-50
                hover:text-indigo-700
            ">

            ☰

        </button>


        <a
    href="/index.html"
    class="flex items-center gap-2">

    <img
        src="/images/logo.png"
        alt="AlignOS Logo"
        class="h-8 w-8 object-contain">

    <span
        class="
            bg-gradient-to-r
            from-[#c13bbd]
            via-[#7b3fc6]
            to-[#2454c7]
            bg-clip-text
            text-lg
            font-bold
            text-transparent
        ">

        AlignOS

    </span>

</a>

    </div>


    <main
        class="
            flex
            h-screen
            flex-1
            flex-col
            overflow-hidden
        ">

        <header
            class="
                hidden
                h-20
                items-center
                justify-between
                border-b
                border-slate-200
                bg-white
                px-6
                shadow-sm
                md:flex
            ">

            <h1
                class="
                    text-3xl
                    font-bold
                    text-slate-800
                ">

                ${title}

            </h1>

        </header>


        <div
            id="pageContent"
            class="
                flex-1
                overflow-y-auto
                p-4
                pt-20
                md:p-8
                md:pt-8
            ">

        </div>


        <div id="modalContainer"></div>

    </main>

</div>

`;


    const menuButton =
        document.getElementById("menuButton");

    const sidebar =
        document.getElementById("sidebar");

    const overlay =
        document.getElementById("sidebarOverlay");


    if (menuButton && sidebar && overlay) {

        menuButton.addEventListener(
            "click",
            () => {

                sidebar.classList.remove(
                    "-translate-x-full"
                );

                overlay.classList.remove(
                    "hidden"
                );

            }
        );


        loadComponent(
            "modalContainer",
            "/components/modal.html"
        );


        overlay.addEventListener(
            "click",
            () => {

                sidebar.classList.add(
                    "-translate-x-full"
                );

                overlay.classList.add(
                    "hidden"
                );

            }
        );

    }

}