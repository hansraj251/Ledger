const fs = require("fs");
const path = require("path");
require("dotenv").config();

const db = require("../db");

function extractYouTubeVideoId(url) {
    try {
        const parsed = new URL(String(url || "").trim());

        if (
            parsed.hostname === "youtube.com" ||
            parsed.hostname === "www.youtube.com" ||
            parsed.hostname === "m.youtube.com"
        ) {
            return parsed.searchParams.get("v") || null;
        }

        if (
            parsed.hostname === "youtu.be" ||
            parsed.hostname === "www.youtu.be"
        ) {
            return parsed.pathname.split("/")[1] || null;
        }

        return null;
    } catch {
        return null;
    }
}

async function importMusicJson(filePath) {
    const absolutePath = path.resolve(filePath);

    if (!fs.existsSync(absolutePath)) {
        throw new Error(`JSON file not found: ${absolutePath}`);
    }

    const raw = fs.readFileSync(absolutePath, "utf8");
    const songs = JSON.parse(raw);

    if (!Array.isArray(songs)) {
        throw new Error("JSON must contain an array of songs.");
    }

    let added = 0;
    let updated = 0;
    let skipped = 0;

    for (const item of songs) {
        const title = String(item?.title || "").trim();
        const artist = String(item?.artist || "").trim();
        const youtubeUrl = String(item?.youtube_url || "").trim();

        const videoId = extractYouTubeVideoId(youtubeUrl);

        if (!title || !artist || !videoId) {
            console.warn(
                "Skipping invalid entry:",
                JSON.stringify(item)
            );
            skipped++;
            continue;
        }

        const existing = await db.getAsync(
            `
            SELECT id
            FROM music_songs
            WHERE youtube_video_id = ?
            LIMIT 1
            `,
            [videoId]
        );

        if (existing) {
            await db.runAsync(
                `
                UPDATE music_songs
                SET
                    title = ?,
                    artist = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE youtube_video_id = ?
                `,
                [
                    title,
                    artist,
                    videoId
                ]
            );

            updated++;
            continue;
        }

        await db.runAsync(
            `
            INSERT INTO music_songs (
                youtube_video_id,
                title,
                artist,
                channel_title,
                channel_id,
                thumbnail_url,
                duration,
                language,
                updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
            `,
            [
                videoId,
                title,
                artist,
                null,
                null,
                `https://i.ytimg.com/vi/${videoId}/hqdefault.jpg`,
                null,
                null
            ]
        );

        added++;
    }

    console.log("");
    console.log("Music JSON import completed.");
    console.log(`Added:   ${added}`);
    console.log(`Updated: ${updated}`);
    console.log(`Skipped: ${skipped}`);
}

const filePath = process.argv[2];

if (!filePath) {
    console.error(
        "Usage: node scripts/importMusicJson.js <json-file>"
    );
    process.exit(1);
}

importMusicJson(filePath)
    .catch(error => {
        console.error("Import failed:", error);
        process.exit(1);
    });
