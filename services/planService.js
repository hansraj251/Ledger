const planRepository =
    require("../repositories/planRepository");

const planLimitRepository =
    require("../repositories/planLimitRepository");

exports.getPlans = async () => {

    const plans =
    await planRepository.getByType(
        "food"
    );

    for (const plan of plans) {

        if (
            plan.plan_type === "school"
        ) {

            plan.active_students =
                await planLimitRepository
                    .getActiveStudentLimit(
                        plan.id
                    );

            continue;

        }

        plan.waiter_devices =
            await planLimitRepository
                .getWaiterDeviceLimit(
                    plan.id
                );

    }

    return plans;

};

exports.getSchoolPlans = async () => {

    const plans =
        await planRepository.getByType(
            "school"
        );

    for (const plan of plans) {

        plan.active_students =
            await planLimitRepository
                .getActiveStudentLimit(
                    plan.id
                );

    }

    return plans;

};

exports.getAllPlans = async () => {

    const plans =
        await planRepository.getAll();

    for (const plan of plans) {

        if (
            plan.plan_type === "school"
        ) {

            plan.active_students =
                await planLimitRepository
                    .getActiveStudentLimit(
                        plan.id
                    );

            continue;

        }

        plan.waiter_devices =
            await planLimitRepository
                .getWaiterDeviceLimit(
                    plan.id
                );

    }

    return plans;

};

exports.getActivePlans = async () => {

    const plans =
        await planRepository.getActive();

    for (const plan of plans) {

        if (
            plan.plan_type === "school"
        ) {

            plan.active_students =
                await planLimitRepository
                    .getActiveStudentLimit(
                        plan.id
                    );

            continue;

        }

        plan.waiter_devices =
            await planLimitRepository
                .getWaiterDeviceLimit(
                    plan.id
                );

    }

    return plans;

};

exports.getPlan = async (
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

    if (
        plan.plan_type === "school"
    ) {

        plan.active_students =
            await planLimitRepository
                .getActiveStudentLimit(
                    planId
                );

    } else {

        plan.waiter_devices =
            await planLimitRepository
                .getWaiterDeviceLimit(
                    planId
                );

    }

    return plan;

};

exports.updatePlan = async (

    planId,

    displayName,

    description,

    sortOrder,

    limitValue,

    status

) => {

    const existingPlan =
        await planRepository.getById(
            planId
        );

    if (!existingPlan) {

        throw new Error(
            "Plan not found."
        );

    }

    await planRepository.update(

        planId,

        displayName,

        description,

        sortOrder,

        status,

        existingPlan.plan_type

    );

    if (
        existingPlan.plan_type === "school"
    ) {

        await planLimitRepository
            .updateActiveStudentLimit(

                planId,

                limitValue

            );

    } else {

        await planLimitRepository
            .updateWaiterDeviceLimit(

                planId,

                limitValue

            );

    }

};

exports.createPlan = async (

    slug,

    displayName,

    description,

    sortOrder,

    limitValue,

    status = "active",

    planType = "food"

) => {

    const existingPlan =
        await planRepository.getBySlug(
            slug
        );

    if (existingPlan) {

        throw new Error(
            "Plan slug already exists."
        );

    }

    if (
        planType !== "food" &&
        planType !== "school"
    ) {

        throw new Error(
            "Invalid plan type."
        );

    }

    const planId =
        await planRepository.create(

            slug,

            displayName,

            description,

            sortOrder,

            status,

            planType

        );

    if (
        planType === "school"
    ) {

        await planLimitRepository
            .updateActiveStudentLimit(

                planId,

                limitValue

            );

    } else {

        await planLimitRepository
            .updateWaiterDeviceLimit(

                planId,

                limitValue

            );

    }

    return planId;

};

exports.deletePlan = async (
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

    await planRepository.remove(
        planId
    );

};