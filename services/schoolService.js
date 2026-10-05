const schoolRepository =
    require("../repositories/schoolRepository");

exports.getSchool =
async (
    schoolId
) => {

    const school =
        await schoolRepository.getById(
            schoolId
        );

    if (
        !school
    ) {

        throw new Error(
            "School not found"
        );

    }

    return school;

};
exports.updateProfile =
async (
    schoolId,
    data
) => {

    const school =
        await schoolRepository.getById(
            schoolId
        );

    if (
        !school
    ) {

        throw new Error(
            "School not found"
        );

    }

    const name =
        String(
            data.name || ""
        ).trim();

    const ownerName =
        String(
            data.ownerName || ""
        ).trim();

    const mobile =
        String(
            data.mobile || ""
        ).trim();

    const address =
        String(
            data.address || ""
        ).trim();

    const city =
        String(
            data.city || ""
        ).trim();

    const state =
        String(
            data.state || ""
        ).trim();

    const pincode =
        String(
            data.pincode || ""
        ).trim();
    const receiptFooterMessage =
    String(
        data.receiptFooterMessage || ""
    ).trim();    

    if (
        !name
    ) {

        throw new Error(
            "School name is required"
        );

    }

    if (
        !ownerName
    ) {

        throw new Error(
            "Owner name is required"
        );

    }

    if (
        !mobile
    ) {

        throw new Error(
            "Mobile number is required"
        );

    }

    return await schoolRepository.updateProfile(
        schoolId,
        {
            name,
            ownerName,
            mobile,
            address: address || null,
            city: city || null,
            state: state || null,
            pincode: pincode || null,
            receiptFooterMessage:
    receiptFooterMessage || null
        }
    );

};
exports.updateLogo =
async (
    schoolId,
    logo
) => {

    if (
        !logo
    ) {

        throw new Error(
            "School logo is required"
        );

    }

    await schoolRepository.updateLogo(
        schoolId,
        logo
    );

    return await schoolRepository.getById(
        schoolId
    );

};