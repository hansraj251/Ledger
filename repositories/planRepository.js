const db = require("../db");

exports.getAll = async () => {

    return await db.allAsync(
        `
        SELECT
            id,
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type,
            created_at,
            updated_at
        FROM plans
        ORDER BY
        sort_order,
         id
        `
    );

};
exports.getByType = async (
    planType
) => {

    return await db.allAsync(
        `
        SELECT
            id,
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type,
            created_at,
            updated_at
        FROM plans
        WHERE plan_type = ?
        ORDER BY
            sort_order,
            id
        `,
        [
            planType
        ]
    );

};

exports.getActive = async () => {

    return await db.allAsync(
        `
        SELECT
            id,
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type,
            created_at,
            updated_at
        FROM plans
        WHERE status = 'active'
        ORDER BY 
        sort_order,
        id
        `
    );

};

exports.getById = async (id) => {

    return await db.getAsync(
        `
        SELECT
            id,
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type,
            created_at,
            updated_at
        FROM plans
        WHERE id = ?
        `,
        [id]
    );

};

exports.getBySlug = async (slug) => {

    return await db.getAsync(
        `
        SELECT
            id,
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type,
            created_at,
            updated_at
        FROM plans
        WHERE slug = ?
        `,
        [slug]
    );

};

exports.create = async (
    slug,
    displayName,
    description,
    sortOrder,
    status,
    plan_type
) => {

    const result = await db.runAsync(
        `
        INSERT INTO plans (
            slug,
            display_name,
            description,
            sort_order,
            status,
            plan_type
        )
        VALUES (?, ?, ?, ?, ?, ?)
        `,
        [
            slug,
            displayName,
            description,
            sortOrder,
            status,
            plan_type
        ]
    );

    return result.lastID;

};

exports.update = async (
    id,
    displayName,
    description,
    sortOrder,
    status,
    plan_type
) => {

    await db.runAsync(
        `
        UPDATE plans
SET
    display_name = ?,
    description = ?,
    sort_order = ?,
    status = ?,
    plan_type = ?,
    updated_at = CURRENT_TIMESTAMP
WHERE id = ?
        `,
        [
    displayName,
    description,
    sortOrder,
    status,
    plan_type,
    id
]
    );

};

exports.remove = async (id) => {

    await db.runAsync(
        `
        DELETE FROM plans
        WHERE id = ?
        `,
        [id]
    );

};