const youtubeMusicService =
    require("../services/youtubeMusicService");
const db =
    require("../db");
exports.search =
    async (req, res) => {

        try {

            const query =
                String(
                    req.query.q || ""
                ).trim();

            const maxResults =
                Number(
                    req.query.limit || 12
                );

            const pageToken =
                String(
                    req.query.pageToken || ""
                ).trim();

            if (!query) {
                return res.status(400).json({
                    success: false,
                    message:
                        "Search query is required."
                });
            }

            const result =
                await youtubeMusicService.searchMusic({
                    query,
                    maxResults,
                    pageToken
                });
            

            return res.json({
                success: true,
                ...result
            });

        }
        catch (error) {

            console.error(
                "Music search error:",
                error
            );

            return res.status(500).json({
                success: false,
                message:
                    error.message ||
                    "Unable to search music."
            });
        }
    };
exports.save =
    async (req, res) => {
        try {
            const videoId =
                String(
                    req.body.videoId || ""
                ).trim();

            const title =
                String(
                    req.body.title || ""
                ).trim();

            if (!videoId || !title) {
                return res.status(400).json({
                    success: false,
                    message:
                        "videoId and title are required."
                });
            }
                        let duration =
                req.body.duration || null;

            if (!duration) {
                duration =
                    await youtubeMusicService.getVideoDuration(
                        videoId
                    );
            }

            await db.runAsync(
                `
                INSERT INTO music_songs (
                    youtube_video_id,
                    title,
                    artist,
                    channel_title,
                    thumbnail_url,
                    duration,
                    language
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(youtube_video_id)
                DO UPDATE SET
                    title = excluded.title,
                    artist = excluded.artist,
                    channel_title = excluded.channel_title,
                    thumbnail_url = excluded.thumbnail_url,
                    duration = excluded.duration,
                    language = excluded.language,
                    updated_at = CURRENT_TIMESTAMP
                `,
                [
                    videoId,
                    title,
                    req.body.artist ||
                    req.body.channelTitle ||
                                            null,
                    req.body.channelTitle || null,
                    req.body.thumbnailUrl || null,
                    duration || null,
                    req.body.language || null
                ]
            );

            return res.json({
                success: true
            });
        }
        catch (error) {
            console.error(
                "Music save error:",
                error
            );

            return res.status(500).json({
                success: false,
                message:
                    "Unable to save music."
            });
        }
    };
exports.discover =
    async (req, res) => {

        try {

            const languages =
                String(
                    req.query.languages || ""
                )
                    .split(",")
                    .map(language =>
                        language.trim()
                    )
                    .filter(Boolean);

            const artists =
                String(
                    req.query.artists || ""
                )
                    .split(",")
                    .map(artist =>
                        artist.trim()
                    )
                    .filter(Boolean);

            const channels =
                String(
                    req.query.channels || ""
                )
                    .split(",")
                    .map(channel =>
                        channel.trim()
                    )
                    .filter(Boolean);
            const channelIds =
    String(
        req.query.channelIds || ""
    )
        .split(",")
        .map(channelId =>
            channelId.trim()
        )
        .filter(Boolean);        

            const favoriteVideoIds =
                String(
                    req.query.favoriteVideoIds || ""
                )
                    .split(",")
                    .map(videoId =>
                        videoId.trim()
                    )
                    .filter(Boolean);

            const limit =
                Number(
                    req.query.limit || 60
                );

            const songs =
    await youtubeMusicService.discoverMusic({
        languages,
        artists,
        channels,
        channelIds,
        favoriteVideoIds,
        limit
    });

            return res.json({
                success: true,
                songs
            });

        }
        catch (error) {

            console.error(
                "Music discover error:",
                error
            );

            return res.status(500).json({
                success: false,
                message:
                    error.message ||
                    "Unable to load music discovery."
            });
        }
    };
const addYouTubeSong = async (req, res) => {
    try {
        const cleanVideoId = String(req.body?.videoId || "").trim();

        if (!/^[A-Za-z0-9_-]{11}$/.test(cleanVideoId)) {
            return res.status(400).json({
                success: false,
                message: "Invalid YouTube video ID."
            });
        }

        const song = await youtubeMusicService.addYouTubeVideo(
            `https://www.youtube.com/watch?v=${cleanVideoId}`
        );

        return res.json({
            success: true,
            song,
            alreadyExists: !!song?.alreadyExists
        });
    } catch (error) {
        console.error("Add YouTube song error:", error);

        return res.status(500).json({
            success: false,
            message: error.message || "Unable to add YouTube video."
        });
    }
};
exports.addYouTubeSong = addYouTubeSong;