const db = require("../db");

const cleanVideoId = value =>
    String(value || "").trim();

const cleanPlaylistName = value =>
    String(value || "").trim();

exports.getFavorites = async userId => {
    return await db.allAsync(
        `
        SELECT
            f.youtube_video_id AS videoId,
            s.title,
            s.artist,
            s.channel_title AS channelTitle,
            s.channel_id AS channelId,
            s.thumbnail_url AS thumbnailUrl,
            s.duration,
            s.language,
            f.created_at AS favoritedAt
        FROM music_favorites f
        LEFT JOIN music_songs s
            ON s.youtube_video_id = f.youtube_video_id
        WHERE f.user_id = ?
        ORDER BY f.created_at DESC
        `,
        [userId]
    );
};

exports.addFavorite = async (userId, videoId) => {
    const cleanId = cleanVideoId(videoId);

    if (!cleanId) {
        throw new Error("videoId is required.");
    }

    await db.runAsync(
        `
        INSERT OR IGNORE INTO music_favorites (
            user_id,
            youtube_video_id
        )
        VALUES (?, ?)
        `,
        [userId, cleanId]
    );

    return true;
};

exports.removeFavorite = async (userId, videoId) => {
    const cleanId = cleanVideoId(videoId);

    if (!cleanId) {
        throw new Error("videoId is required.");
    }

    await db.runAsync(
        `
        DELETE FROM music_favorites
        WHERE user_id = ?
          AND youtube_video_id = ?
        `,
        [userId, cleanId]
    );

    return true;
};

exports.getPlaylists = async userId => {
    const playlists = await db.allAsync(
        `
        SELECT
            id,
            name,
            created_at AS createdAt,
            updated_at AS updatedAt
        FROM music_playlists
        WHERE user_id = ?
        ORDER BY updated_at DESC, id DESC
        `,
        [userId]
    );

    for (const playlist of playlists) {
        playlist.songs = await db.allAsync(
            `
            SELECT
                ps.youtube_video_id AS videoId,
                s.title,
                s.artist,
                s.channel_title AS channelTitle,
                s.channel_id AS channelId,
                s.thumbnail_url AS thumbnailUrl,
                s.duration,
                s.language,
                ps.position,
                ps.added_at AS addedAt
            FROM music_playlist_songs ps
            LEFT JOIN music_songs s
                ON s.youtube_video_id =
                   ps.youtube_video_id
            WHERE ps.playlist_id = ?
            ORDER BY ps.position ASC, ps.id ASC
            `,
            [playlist.id]
        );
    }

    return playlists;
};

exports.createPlaylist = async (userId, name) => {
    const cleanName = cleanPlaylistName(name);

    if (!cleanName) {
        throw new Error("Playlist name is required.");
    }

    const result = await db.runAsync(
        `
        INSERT INTO music_playlists (
            user_id,
            name
        )
        VALUES (?, ?)
        `,
        [userId, cleanName]
    );

    return {
        id: result.lastID,
        name: cleanName,
        songs: []
    };
};

exports.renamePlaylist = async (
    userId,
    playlistId,
    name
) => {
    const cleanName = cleanPlaylistName(name);

    if (!cleanName) {
        throw new Error("Playlist name is required.");
    }

    await db.runAsync(
        `
        UPDATE music_playlists
        SET
            name = ?,
            updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND user_id = ?
        `,
        [cleanName, playlistId, userId]
    );

    return true;
};

exports.deletePlaylist = async (
    userId,
    playlistId
) => {
    await db.runAsync(
        `
        DELETE FROM music_playlists
        WHERE id = ?
          AND user_id = ?
        `,
        [playlistId, userId]
    );

    return true;
};

exports.addSongToPlaylist = async (
    userId,
    playlistId,
    videoId
) => {
    const cleanId = cleanVideoId(videoId);

    if (!cleanId) {
        throw new Error("videoId is required.");
    }

    const playlist = await db.getAsync(
        `
        SELECT id
        FROM music_playlists
        WHERE id = ?
          AND user_id = ?
        `,
        [playlistId, userId]
    );

    if (!playlist) {
        throw new Error("Playlist not found.");
    }

    const positionRow = await db.getAsync(
        `
        SELECT COALESCE(
            MAX(position),
            -1
        ) + 1 AS nextPosition
        FROM music_playlist_songs
        WHERE playlist_id = ?
        `,
        [playlistId]
    );

    await db.runAsync(
        `
        INSERT OR IGNORE INTO music_playlist_songs (
            playlist_id,
            youtube_video_id,
            position
        )
        VALUES (?, ?, ?)
        `,
        [
            playlistId,
            cleanId,
            positionRow?.nextPosition || 0
        ]
    );

    await db.runAsync(
        `
        UPDATE music_playlists
        SET updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND user_id = ?
        `,
        [playlistId, userId]
    );

    return true;
};

exports.removeSongFromPlaylist = async (
    userId,
    playlistId,
    videoId
) => {
    const cleanId = cleanVideoId(videoId);

    if (!cleanId) {
        throw new Error("videoId is required.");
    }

    const playlist = await db.getAsync(
        `
        SELECT id
        FROM music_playlists
        WHERE id = ?
          AND user_id = ?
        `,
        [playlistId, userId]
    );

    if (!playlist) {
        throw new Error("Playlist not found.");
    }

    await db.runAsync(
        `
        DELETE FROM music_playlist_songs
        WHERE playlist_id = ?
          AND youtube_video_id = ?
        `,
        [playlistId, cleanId]
    );

    await db.runAsync(
        `
        UPDATE music_playlists
        SET updated_at = CURRENT_TIMESTAMP
        WHERE id = ?
          AND user_id = ?
        `,
        [playlistId, userId]
    );

    return true;
};
