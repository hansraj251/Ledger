const planRepository =
    require("../repositories/planRepository");

const planPricingRepository =
    require("../repositories/planPricingRepository");

exports.getAllPricing = async () => {

    return await planPricingRepository
        .getAll();

};    

exports.getPricing = async (
    planId
) => {

    const plan =
        await planRepository.getById(
            planId
        );

    if (!plan) {

        throw new Error(
            "Plan not found."
        );

    }

    return await planPricingRepository
        .getByPlanId(
            planId
        );

};

exports.createPricing = async (

    planId,

    durationDays,

    price,

    currency,

    status

) => {

    const plan =
        await planRepository.getById(
            planId
        );

    if (!plan) {

        throw new Error(
            "Plan not found."
        );

    }

    durationDays =
        Number(durationDays);

    price =
        Number(price);

    if (durationDays <= 0) {

        throw new Error(
            "Duration must be greater than zero."
        );

    }

    if (price <= 0) {

        throw new Error(
            "Price must be greater than zero."
        );

    }

    if (!currency) {

        currency = "INR";

    }

    if (
        status !== "active" &&
        status !== "inactive"
    ) {

        status = "active";

    }

    const existingPricing =
    await planPricingRepository
        .getByPlanAndDuration(
            planId,
            durationDays
        );

if (existingPricing) {

    throw new Error(
        "Pricing already exists for this duration."
    );

}

    return await
        planPricingRepository
            .create(

                planId,

                durationDays,

                price,

                currency,

                status

            );

};

exports.updatePricing = async (

    pricingId,

    durationDays,

    price,

    currency,

    status

) => {

    const pricing =
        await planPricingRepository
            .getById(
                pricingId
            );

    if (!pricing) {

        throw new Error(
            "Pricing not found."
        );

    }

    durationDays =
        Number(durationDays);

    price =
        Number(price);

    if (durationDays <= 0) {

        throw new Error(
            "Duration must be greater than zero."
        );

    }

    if (price <= 0) {

        throw new Error(
            "Price must be greater than zero."
        );

    }

    const duplicatePricing =
    await planPricingRepository
        .getByPlanAndDuration(

            pricing.plan_id,

            durationDays

        );

if (

    duplicatePricing &&

    duplicatePricing.id !== pricingId

) {

    throw new Error(

        "Pricing already exists for this duration."

    );

}

    await planPricingRepository
        .update(

            pricingId,

            durationDays,

            price,

            currency,

            status

        );

};

exports.deletePricing = async (
    pricingId
) => {

    const pricing =
        await planPricingRepository
            .getById(
                pricingId
            );

    if (!pricing) {

        throw new Error(
            "Pricing not found."
        );

    }

    await planPricingRepository
        .remove(
            pricingId
        );

};