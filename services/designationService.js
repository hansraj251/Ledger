const designationRepository =
    require("../repositories/designationRepository");

exports.getDesignations =
async (
    schoolId
) => {

    return await designationRepository.getAll(
        schoolId
    );

};

exports.getDesignation =
async (
    schoolId,
    designationId
) => {

    const designation =
        await designationRepository.getById(
            schoolId,
            designationId
        );

    if (
        !designation
    ) {

        throw new Error(
            "Designation not found"
        );

    }

    return designation;

};

exports.createDesignation =
async (
    schoolId,
    data
) => {

    if (
        !data.name ||
        !data.name.trim()
    ) {

        throw new Error(
            "Designation is required"
        );

    }

    const name =
        data.name.trim();

    const existingDesignation =
        await designationRepository.getByName(
            schoolId,
            name
        );

    if (
        existingDesignation
    ) {

        throw new Error(
            "Designation already exists"
        );

    }

    const designationId =
        await designationRepository.create({

            schoolId,

            name

        });

    return await designationRepository.getById(
        schoolId,
        designationId
    );

};

exports.updateDesignationStatus =
async (
    schoolId,
    designationId,
    status
) => {

    if (
        status !== "active" &&
        status !== "inactive"
    ) {

        throw new Error(
            "Invalid designation status"
        );

    }

    const designation =
        await designationRepository.getById(
            schoolId,
            designationId
        );

    if (
        !designation
    ) {

        throw new Error(
            "Designation not found"
        );

    }

    await designationRepository.updateStatus(
        schoolId,
        designationId,
        status
    );

    return await designationRepository.getById(
        schoolId,
        designationId
    );

};