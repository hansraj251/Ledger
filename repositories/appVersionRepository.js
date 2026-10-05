const db =
    require("../db");

exports.getLatestVersion =
async () => {

    return await db.getAsync(
        `
        SELECT
            latest_version
        FROM app_version
        WHERE id = 1
        `
    );

};

exports.setLatestVersion =
async (
    version
) => {

    await db.runAsync(
        `
        INSERT INTO app_version (
            id,
            latest_version
        )
        VALUES (
            1,
            ?
        )
        ON CONFLICT(id)
        DO UPDATE SET
            latest_version = excluded.latest_version
        `,
        [
            version
        ]
    );

};