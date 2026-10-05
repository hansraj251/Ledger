const appVersionRepository =
    require("../repositories/appVersionRepository");

exports.getLatestVersion =
async () => {

    const data =
        await appVersionRepository
            .getLatestVersion();

    return {

        latest_version:
            data
                ? data.latest_version
                : null

    };

};

exports.setLatestVersion =
async (
    version
) => {

    const cleanVersion =
        String(
            version || ""
        ).trim();

    if (!cleanVersion) {

        throw new Error(
            "Version is required."
        );

    }

    await appVersionRepository
        .setLatestVersion(
            cleanVersion
        );

    return {

        latest_version:
            cleanVersion

    };

};