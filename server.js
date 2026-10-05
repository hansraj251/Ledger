require("dotenv").config();

const http =
    require("http");

require("./db");

const path =
    require("path");

const express =
    require("express");

const cors =
    require("cors");

const initializeDatabase =
    require("./database/init");

const app =
    express();

const server =
    http.createServer(app);

app.use(cors());

app.use(
    "/api/subscription/webhook",
    express.raw({
        type: "application/json"
    })
);

app.use(
    express.json()
);

app.use(
    express.urlencoded({
        extended: true
    })
);

app.use(
    express.static("public")
);

const uploadsPath =
    process.env.RENDER
        ? "/var/data/uploads"
        : path.join(
            __dirname,
            "uploads"
        );

app.use(
    "/uploads",
    express.static(
        uploadsPath
    )
);

/* -------------------- ROUTES -------------------- */

app.use(
    "/api/auth",
    require("./routes/authRoutes")
);

app.use(
    "/api/property/auth",
    require("./routes/propertyAuthRoutes")
);
app.use(
    "/api/property/listings",
    require("./routes/propertyListingRoutes")
);
app.use(
    "/api/property/listings",
    require("./routes/propertyListingRoutes")
);
app.use(
    "/api/music",
    require("./routes/musicRoutes")
);

app.use(

    "/api/ledger/business",

    require("./routes/ledgerBusinessRoutes")

);

app.use(

    "/api/ledger/parties",

    require("./routes/ledgerPartyRoutes")

);

app.use(

    "/api/ledger/transactions",

    require("./routes/ledgerTransactionRoutes")

);

app.use(
    "/api/property/contact-requests",
    require("./routes/propertyContactRequestRoutes")
);
app.use(
    "/api/property/moderation",
    require("./routes/propertyModerationRoutes")
);

app.use(
    "/api/restaurants",
    require("./routes/restaurantRoutes")
);

app.use(
    "/api/schools",
    require("./routes/schoolRoutes")
);

app.use(
    "/api/students",
    require("./routes/studentRoutes")
);

app.use(
    "/api/teachers",
    require("./routes/teacherRoutes")
);

app.use(
    "/api/classes",
    require("./routes/classRoutes")
);
app.use(
    "/api/users",
    require("./routes/userRoutes")
);


app.use(
    "/api/designations",
    require("./routes/designationRoutes")
);

app.use(
    "/api/salary-structures",
    require("./routes/salaryStructureRoutes")
);

app.use(
    "/api/fee-structures",
    require("./routes/feeStructureRoutes")
);

app.use(
    "/api/fee-payments",
    require("./routes/feePaymentRoutes")
);
app.use(
    "/api/salary-payments",
    require("./routes/salaryPaymentRoutes")
);
app.use(
    "/api/attendance",
    require("./routes/attendanceRoutes")
);

app.use(
    "/api/holidays",
    require("./routes/holidayRoutes")
);
app.use(
    "/api/subscription",
    require("./routes/subscriptionRoutes")
);

app.use(
    "/api/super-admin",
    require("./routes/superAdminRoutes")
);

app.use(
    "/api/super-admin/accounts",
    require("./routes/superAdminAccountRoutes")
);

app.use(
    "/api/pos",
    require("./routes/posAuthRoutes")
);

app.use(
    "/api/pos",
    require("./routes/posRoutes")
);

app.get(
    "/",
    (
        req,
        res
    ) => {

        res.redirect(
            "/login.html"
        );

    }
);

initializeDatabase()

    .then(() => {

        const PORT =
            process.env.PORT
            || 3000;

        server.listen(
            PORT,
            () => {

                console.log(
                    `🚀 Align Cloud running on http://localhost:${PORT}`
                );

            }
        );

    })

    .catch(
        console.error
    );