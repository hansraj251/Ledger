const db =
    require("../db");

exports.createOrder = async (

    restaurantId,

    planId,

    planPricingId,

    razorpayOrderId,

    amount,

    currency,

    durationDays

) => {

        await db.runAsync(
            `
            INSERT INTO
                subscription_orders
            (

                restaurant_id,
plan_id,
plan_pricing_id,
razorpay_order_id,
amount,
currency,
duration_days

            )
            VALUES
            (
                ?,
?,
?,
?,
?,
?,
?
            )
            `,
            [
    restaurantId,
    planId,
    planPricingId,
    razorpayOrderId,
    amount,
    currency,
    durationDays
]
        );

    };
exports.createSchoolOrder = async (

    schoolId,

    planId,

    planPricingId,

    razorpayOrderId,

    amount,

    currency,

    durationDays

) => {

    await db.runAsync(
        `
        INSERT INTO
            subscription_orders
        (

            school_id,
            plan_id,
            plan_pricing_id,
            razorpay_order_id,
            amount,
            currency,
            duration_days

        )
        VALUES
        (
            ?,
            ?,
            ?,
            ?,
            ?,
            ?,
            ?
        )
        `,
        [
            schoolId,
            planId,
            planPricingId,
            razorpayOrderId,
            amount,
            currency,
            durationDays
        ]
    );

};

exports.markPaid =
    async (
        razorpayOrderId,
        razorpayPaymentId,
        paymentMethod = null
    ) => {

        await db.runAsync(
            `
            UPDATE subscription_orders
            SET

                status = 'paid',

                razorpay_payment_id = ?,

                payment_method = ?,

                paid_at = CURRENT_TIMESTAMP

            WHERE razorpay_order_id = ?
            `,
            [
                razorpayPaymentId,
                paymentMethod,
                razorpayOrderId
            ]
        );

    };

exports.getByRazorpayOrderId =
    async (
        razorpayOrderId
    ) => {

        return await db.getAsync(
            `
            SELECT *
            FROM subscription_orders
            WHERE razorpay_order_id = ?
            `,
            [
                razorpayOrderId
            ]
        );

    };

exports.getPaymentHistory =
async () => {

    return await db.allAsync(
        `
        SELECT

            subscription_orders.id,

            subscription_orders.restaurant_id,

            restaurants.name
                AS restaurant_name,

            restaurants.restaurant_code,

            plans.display_name
                AS plan_name,

            plan_pricing.duration_days,

            subscription_orders.amount,

            subscription_orders.currency,

            subscription_orders.razorpay_order_id,

            subscription_orders.razorpay_payment_id,

            subscription_orders.payment_method,

            subscription_orders.status,

            subscription_orders.paid_at,

            subscription_orders.created_at

        FROM subscription_orders

        INNER JOIN restaurants
            ON restaurants.id =
                subscription_orders.restaurant_id

        INNER JOIN plans
            ON plans.id =
                subscription_orders.plan_id

        INNER JOIN plan_pricing
            ON plan_pricing.id =
                subscription_orders.plan_pricing_id

        ORDER BY
            subscription_orders.created_at DESC
        `
    );

};